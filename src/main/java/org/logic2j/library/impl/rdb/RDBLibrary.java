package org.logic2j.library.impl.rdb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.logic2j.PrologImplementor;
import org.logic2j.TermFactory;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.impl.pojo.PojoLibrary;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.TNumber;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;
import org.logic2j.solve.ioc.UniqueSolutionListener;
import org.logic2j.theory.jdbc.SqlBuilder3;
import org.logic2j.theory.jdbc.SqlBuilder3.Table;
import org.logic2j.util.CollectionMap;
import org.logic2j.util.CollectionUtils;
import org.logic2j.util.ReflectUtils;
import org.logic2j.util.SqlRunner;

/**
 * Prolog library that bridges the Prolog engine and
 * a relational database seen as a facts repository.
 *
 */
public class RDBLibrary extends LibraryBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RDBLibrary.class);

  /**
   * The name of the tbl/4 predicate to describe a column.
   */
  private static final String TBL_PREDICATE = "tbl";

  private TermFactory termFactory;

  public RDBLibrary(PrologImplementor theProlog) {
    super(theProlog);
    this.termFactory = new RDBBase.AllStringsAsAtoms(theProlog);
  }

  @Primitive
  public void select(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term... theArguments)
      throws SQLException {
    Term theDataSource = theArguments[0];
    Term theExpression = theArguments[1];
    Set<Term> optionSet = new HashSet<Term>(Arrays.asList(theArguments).subList(2, theArguments.length));
    boolean isDistinct = optionSet.contains(new Struct("distinct"));

    final DataSource ds = bound(theDataSource, vars, DataSource.class);
    // Watch out - by resolving, the variables remaining free have new offets! We won't be able to bind them in the original goal!!!
    final Struct conditions = resolve(theExpression, vars, Struct.class);
    String resultVar = "Tbl";
    // The goal we are solving
    Term internalGoal = new Struct("solve", conditions, resultVar);
    internalGoal = internalGoal.cloneIt();
    // Watch out this destroys the indexes in the original expression !!!!
    internalGoal = getProlog().getTermFactory().normalize(internalGoal);
    final VarBindings internalBindings = new VarBindings(internalGoal);
    final UniqueSolutionListener internalListener = new UniqueSolutionListener(internalGoal, internalBindings);
    getProlog().getSolver().solveGoal(internalGoal, internalBindings, new GoalFrame(), internalListener);
    Term result = internalListener.getSolution().getBinding(resultVar);
    if (!(result instanceof Struct)) {
      throw new InvalidTermException("Internal result must be a Struct");
    }
    Struct plistOfTblPredicates = (Struct) result;
    logger.debug("Internal solution: {}", plistOfTblPredicates);
    List<Struct> javaListRoot = plistOfTblPredicates.javaListFromPList(new ArrayList<Struct>(), Struct.class);
    logger.info(CollectionUtils.format("Internal solution:", javaListRoot, 10));
    // And convert Struct to references to tables, columns and column criteria
    // Meanwhile, check individual predicates
    final SqlBuilder3 builder = new SqlBuilder3();
    List<SqlBuilder3.Criterion> rawColumns = new ArrayList<SqlBuilder3.Criterion>();
    int aliasIndex = 1;
    for (Struct tbls : javaListRoot) {
      final String alias = "t" + (aliasIndex++);
      List<Struct> javaList = tbls.javaListFromPList(new ArrayList<Struct>(), Struct.class);
      for (Struct tbl : javaList) {
        // Check predicate received must be "tbl" with arity of 4
        if (!tbl.getName().equals(TBL_PREDICATE)) {
          throw new InvalidTermException("Predicate must be \"tbl\" not " + tbl.getName());
        }
        if (tbl.getArity() < 4 || tbl.getArity() > 5) {
          throw new InvalidTermException("Arity of term " + tbl + " must be 4 or 5");
        }
        // Convert this predicate into a Criterion. When variables are specified, use the var as the operand value
        final String tableName = TERM_API.selectTerm(tbl, "tbl[1]", Struct.class).getName();
        final String columnName = TERM_API.selectTerm(tbl, "tbl[2]", Struct.class).getName();
        final Term valueTerm = TERM_API.selectTerm(tbl, "tbl[4]", Term.class);
        String operator = SqlBuilder3.OPERATOR_EQ_OR_IN;
        if (tbl.getArity() >= 5) {
          operator = TERM_API.selectTerm(tbl, "tbl[5]", Struct.class).getName();
        }
        final Table table = builder.table(tableName, alias);
        if (valueTerm instanceof Var) {
          if (((Var) valueTerm).isAnonymous()) {
            // Will ignore any anonymous var
            continue;
          }
          rawColumns.add(builder.criterion(builder.column(table, columnName), valueTerm));
        } else {
          // A constant
          rawColumns.add(builder.criterion(builder.column(table, columnName), operator, jdbcFromTerm(valueTerm)));
        }
      }
    }
    logger.debug(CollectionUtils.format("rawColumns:", rawColumns, 10));

    // Now collect join conditions: all columns having the same variable
    Set<Var> projectVars = new LinkedHashSet<Var>();
    CollectionMap<String, SqlBuilder3.Criterion> columnsPerVariable = new CollectionMap<String, SqlBuilder3.Criterion>(); // Join clauses
    for (SqlBuilder3.Criterion column : rawColumns) {
      if (column.getOperand()[0] instanceof Var) {
        Var var = (Var) column.getOperand()[0];
        projectVars.add(var);
        columnsPerVariable.add(var.getName(), column);
      }
    }
    logger.debug("** colPerVar: {}", columnsPerVariable);

    // Every variable referenced contributes one projection. If more than on column for same variable (-->join), use only first of them
    for (Collection<SqlBuilder3.Criterion> clausesOfOneJoinExpression : columnsPerVariable.values()) {
      builder.addProjection(clausesOfOneJoinExpression.iterator().next().getColumn());
      if (clausesOfOneJoinExpression.size() >= 2) {
        List<SqlBuilder3.Criterion> toJoin = new ArrayList<SqlBuilder3.Criterion>(clausesOfOneJoinExpression);
        for (int i = 1; i < toJoin.size(); i++) {
          builder.innerJoin(toJoin.get(0).getColumn(), toJoin.get(i).getColumn());
        }
      }
    }

    // Collect criteria (where value is constant)
    for (SqlBuilder3.Criterion column : rawColumns) {
      if (!(column.getOperand()[0] instanceof Var)) {
        builder.addConjunction(column);
      }
    }

    logger.debug("** Projected vars: {}", projectVars);
    logger.debug("** Builder       : {}", builder.describe());

    // Generate
    builder.setDistinct(isDistinct);
    final String effectiveSql;
    if (builder.getNbProjections() == 0) {
      effectiveSql = builder.getSelectCount();
    } else {
      effectiveSql = builder.getSelect();
    }
    logger.debug("SQL   : {}", effectiveSql);
    logger.debug("Params: {}", Arrays.asList(builder.getParameters()));
    // Execution
    final SqlRunner sqlRunner = new SqlRunner(ds);
    if (builder.getNbProjections() == 0) {
      List<Object[]> countOnly = sqlRunner.query(effectiveSql, builder.getParameters());
      if (countOnly.size() != 1) {
        throw new IllegalStateException("Query for counting " + effectiveSql + "did not return a single result set row but "
            + countOnly.size());
      }
      if (countOnly.get(0).length != 1) {
        throw new IllegalStateException("Query for counting " + effectiveSql + "did not return a single column set row but "
            + countOnly.get(0).length);
      }
      final Number resultSet = (Number) countOnly.get(0)[0];
      int number = resultSet.intValue();
      while (number-- > 0) {
        // Generates solutions without binding variables, just the right number of them
        notifySolution(theGoalFrame, theListener);
      }
    } else {
      final List<Object[]> resultSet = sqlRunner.query(effectiveSql, builder.getParameters());
      // Vars referenced in projections
      Var projectedVars[] = new Var[projectVars.size()];
      VarBindings originalVarBindings = null;
      int counter = 0;
      for (Var var : projectVars) {
        final Var originalVar = conditions.findVar(var.getName());
        if (originalVar == null) {
          throw new InvalidTermException("Could no find original var " + var.getName() + " within " + conditions);
        }
        projectedVars[counter] = originalVar;
        final VarBindings bindingsForVar = vars.findBindings(originalVar);
        if (bindingsForVar == null) {
          throw new InvalidTermException("Could no find originalVarBindings for variable " + var.getName());
        }
        if (originalVarBindings == null) {
          // Initialize
          originalVarBindings = bindingsForVar;
        } else {
          // Check
          if (bindingsForVar != originalVarBindings) {
            throw new InvalidTermException("Not all variables share the same bindings - won't be able to unify");
          }
        }
        counter++;
      }
      // Generate solutions, one per row
      for (Object[] objects : resultSet) {
        unifyAndNotify(projectedVars, objects, originalVarBindings, theGoalFrame, theListener);
      }
    }
  }

  /**
   * @param theTerm
   * @return A JDBC argument from a Term value
   */
  private Object jdbcFromTerm(Term theTerm) {
    if (theTerm instanceof TNumber) {
      return ((TNumber) theTerm).longValue();
    } else if (theTerm instanceof Struct) {
      final Struct struct = (Struct) theTerm;
      if (struct.isList()) {
        Set<Object> javaList = new HashSet<Object>();
        for (Term t : struct.javaListFromPList(new ArrayList<Term>(), Term.class)) {
          javaList.add(jdbcFromTerm(t));
        }
        return javaList;
      }
      return struct.getName();
    } else {
      throw new IllegalArgumentException("Cannot convert to SQL parameter: " + theTerm.getClass());
    }
  }

  /**
   * @param theBinding
   * @param desiredClassOrInterface
   * @return Unwrap a StructObject bound term to a pojo.
   */
  private <T> T bound(Term theBinding, VarBindings vars, Class<T> desiredClassOrInterface) {
    //    final StructObject<T> structObject = resolve(theBinding, vars, StructObject.class);
    //    final Object instance = structObject.getObject();
    final Struct bindingName = resolve(theBinding, vars, Struct.class);
    final Object instance = PojoLibrary.extract(bindingName.getName());
    return ReflectUtils.safeCastNotNull("unwrapping binding \"" + instance + '"', instance, desiredClassOrInterface);
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public TermFactory getTermFactory() {
    return this.termFactory;
  }

  public void setTermFactory(TermFactory theTermFactory) {
    this.termFactory = theTermFactory;
  }

}
