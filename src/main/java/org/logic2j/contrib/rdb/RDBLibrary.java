/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.contrib.rdb;

import static org.logic2j.engine.model.TermApiLocator.termApiExt;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.logic2j.contrib.rdb.util.CollectionMap;
import org.logic2j.contrib.rdb.util.SqlBuilder3;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Column;
import org.logic2j.contrib.rdb.util.SqlBuilder3.ColumnOperatorParameterCriterion;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Operator;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Table;
import org.logic2j.contrib.rdb.util.SqlRunner;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.core.impl.EnvManager;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.exception.PrologNonSpecificException;
import org.logic2j.engine.model.PrologLists;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Term;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.unify.UnifyContext;
import org.logic2j.engine.util.CollectionUtils;
import org.logic2j.engine.util.TypeUtils;

/**
 * Prolog library that bridges the Prolog engine and a relational database seen as a facts repository.
 * TODO the {@link #select(UnifyContext, Object...)} method should actually take the goal and create a constraint graph, then transform the graph into SQL.
 */
public class RDBLibrary extends LibraryBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RDBLibrary.class);

  /**
   * The name of the tbl/4 predicate to describe a column.
   */
  private static final String TBL_PREDICATE = "tbl";
  private static final Set<String> ALLOWED_OPERATORS;

  static {
    ALLOWED_OPERATORS = new HashSet<>(Arrays.asList("=", "\\=", "<", ">", "=<", ">="));
  }

  private TermAdapter termAdapter;

  public RDBLibrary(PrologImplementation theProlog) {
    super(theProlog);
    this.termAdapter = new RDBBase.AllStringsAsAtoms();
  }

  @Predicate
  public int select(UnifyContext currentVars, Object... theArguments) throws SQLException {
    final Object theDataSource = theArguments[0];
    final Object theExpression = theArguments[1];
    final DataSource ds = bound(theDataSource, currentVars, DataSource.class);

    final Object finalExpression = currentVars.reify(theExpression);
    ensureBindingIsNotAFreeVar(finalExpression, "select/*", 1);
    final Struct<?> conditions = (Struct<?>) finalExpression;

    // Options
    final Set<Object> optionSet = new HashSet<>(Arrays.asList(theArguments).subList(2, theArguments.length));
    final boolean isDistinct = optionSet.contains(new Struct<>("distinct"));

    //
    final String resultVar = "Tbl";

    // The goal we are solving
    Object internalGoal = Struct.valueOf("gd3_solve", conditions, resultVar);
    if (internalGoal != null) {
      // Clone
      internalGoal = new Struct<>((Struct<?>) internalGoal);
    }
    // Watch out this destroys the indexes in the original expression !!!!
    internalGoal = termApiExt().normalize(internalGoal, getProlog().getLibraryManager().wholeContent());

    final Object result = getProlog().solve(internalGoal).var(resultVar).unique();
    if (!(result instanceof Struct)) {
      throw new InvalidTermException("Internal result must be a Struct");
    }
    final Struct<?> plistOfTblPredicates = (Struct<?>) result;
    logger.debug("select/3: Solving {} gives internal solution: {}", plistOfTblPredicates);
    final List<Struct> javaListRoot = PrologLists.javaListFromPList(plistOfTblPredicates, new ArrayList<>(), Struct.class);
    logger.info(CollectionUtils.format("Internal solution, list elements:", javaListRoot, 10));
    final Map<String, Term> assignedVarValue = new HashMap<>();
    final Map<String, String> assignedVarOperator = new HashMap<>();
    // Count number of references to tables
    int nbTbl = 0;
    for (final Struct<?> tbls : javaListRoot) {
      for (final Struct<?> pred : PrologLists.javaListFromPList(tbls, new ArrayList<>(), Struct.class)) {
        final String functor = pred.getName();
        if (functor.equals(TBL_PREDICATE)) {
          nbTbl++;
        }
        // Operator
        else if (ALLOWED_OPERATORS.contains(functor)) {
          final Term term1 = termApiExt().selectTerm(pred, "[1]", Term.class);
          final Term term2 = termApiExt().selectTerm(pred, "[2]", Term.class);
          final String variableName;
          final Term value;
          if (term1 instanceof Var<?> && !(term2 instanceof Var)) {
            variableName = ((Var<?>) term1).getName();
            value = term2;
          } else if (term2 instanceof Var<?> && !(term1 instanceof Var)) {
            variableName = ((Var<?>) term2).getName();
            value = term1;
          } else {
            throw new PrologNonSpecificException("Cannot (yet) handle operators with 2 unbound variables such as " + pred);
          }
          assignedVarValue.put(variableName, value);
          assignedVarOperator.put(variableName, functor);
        }
        // Anything else
        else {
          logger.warn("Functor unknown, ignored: \"{}\"", functor);
        }
      }
    }
    // When there are no table predicates, actually we can just execute the goal that was passed
    // as argument (i.e. the "select" predicate becomes a pass-through)
    if (nbTbl == 0) {
      // Pass through
      logger.error("select/3 did not extract any reference to a table, while processing expression \"{}\" - no matches", conditions);
      return Continuation.CONTINUE;
    }

    // And convert Struct to references to tables, columns and column criteria
    // Meanwhile, check individual predicates
    final SqlBuilder3 builder = new SqlBuilder3();
    final List<SqlBuilder3.ColumnOperatorParameterCriterion> rawColumns = new ArrayList<>();
    int aliasIndex = 1;
    final Set<Var<?>> projectVars = new LinkedHashSet<>();
    for (final Struct<?> tbls : javaListRoot) {
      final String alias = "t" + (aliasIndex++);
      final List<Struct> javaList = PrologLists.javaListFromPList(tbls, new ArrayList<>(), Struct.class);
      for (final Struct<?> tbl : javaList) {
        // Check predicate received must be "tbl" with arity of 4
        if (!tbl.getName().equals(TBL_PREDICATE)) {
          throw new InvalidTermException("Predicate must be \"tbl\" not " + tbl.getName());
        }
        if (tbl.getArity() < 4 || tbl.getArity() > 5) {
          throw new InvalidTermException("Arity of term " + tbl + " must be 4 or 5");
        }
        // Convert this predicate into a Criterion. When variables are specified, use the var as the operand value
        final String tableName = termApiExt().selectTerm(tbl, "tbl[1]", Struct.class).getName();
        final String columnName = termApiExt().selectTerm(tbl, "tbl[2]", Struct.class).getName();
        final Term valueTerm = termApiExt().selectTerm(tbl, "tbl[4]", Term.class);
        Operator operator = SqlBuilder3.Operator.EQ;
        if (tbl.getArity() >= 5) {
          operator = Operator.valueOfProlog(termApiExt().selectTerm(tbl, "tbl[5]", Struct.class).getName());
        }
        final Table table = builder.table(tableName, alias);
        final Column sqlColumn = builder.column(table, columnName);
        if (valueTerm instanceof Var) {
          final Var<?> var = (Var<?>) valueTerm;
          if (var == Var.anon()) {
            // Will ignore any anonymous var
            continue;
          }
          // Check if variable already has a defined value
          final String varName = var.getName();
          final Term varValue = assignedVarValue.get(varName);
          if (varValue == null) {
            // No value defined for variable - leave as a variable
            rawColumns.add(builder.criterion(sqlColumn, var));
          } else {
            // A variable has a defined value, substitute by its direct value
            final Operator specifiedOperator = Operator.valueOfProlog(assignedVarOperator.get(varName));
            if (specifiedOperator != null) {
              operator = specifiedOperator;
            }
            rawColumns.add(builder.criterion(sqlColumn, operator, jdbcFromTerm(varValue)));
            // Although we have a constant value and not a free variable, we will have to project its
            // _real_ value extracted from the database. In case of "=" this is dubious as the DB will
            // of course return the same. But for other operators (e.g. ">"), the real DB value will be needed!
            final Var<?> originalVar = var;

            projectVars.add(originalVar);
            builder.addProjection(sqlColumn);
          }
        } else {
          // A constant
          rawColumns.add(builder.criterion(builder.column(table, columnName), operator, jdbcFromTerm(valueTerm)));
        }
      }
    }
    logger.debug(CollectionUtils.format("rawColumns:", rawColumns, 10));

    // Now collect join conditions: all columns having the same variable
    final CollectionMap<String, SqlBuilder3.ColumnOperatorParameterCriterion> columnsPerVariable = new CollectionMap<>();
    // Join clauses
    for (final SqlBuilder3.ColumnOperatorParameterCriterion column : rawColumns) {
      if (column.getOperand() instanceof Var) {
        final Var<?> var = (Var<?>) column.getOperand();
        projectVars.add(var);
        columnsPerVariable.add(var.getName(), column);
      }
    }
    logger.debug("** colPerVar: {}", columnsPerVariable);

    // Every variable referenced contributes one projection. If more than on column for same variable (-->join), use only first of them
    for (final Collection<ColumnOperatorParameterCriterion> clausesOfOneJoinExpression : columnsPerVariable.values()) {
      builder.addProjection(clausesOfOneJoinExpression.iterator().next().getColumn());
      if (clausesOfOneJoinExpression.size() >= 2) {
        final List<SqlBuilder3.ColumnOperatorParameterCriterion> toJoin = new ArrayList<>(clausesOfOneJoinExpression);
        for (int i = 1; i < toJoin.size(); i++) {
          builder.innerJoin(toJoin.get(0).getColumn(), toJoin.get(i).getColumn());
        }
      }
    }

    // Collect criteria (where value is constant)
    for (final SqlBuilder3.ColumnOperatorParameterCriterion column : rawColumns) {
      if (!(column.getOperand() instanceof Var)) {
        builder.addConjunction(column);
      }
    }

    logger.debug("** Projected bindings: {}", projectVars);
    logger.debug("** Builder       : {}", builder.describe());

    // Generate
    builder.setDistinct(isDistinct);
    if (builder.getNbProjections() == 0) {
      builder.getSelectCount();
    } else {
      builder.getSelect();
    }
    final String effectiveSql = builder.getStatement();
    logger.debug("SQL   : {}", effectiveSql);
    logger.debug("Params: {}", Arrays.asList(builder.getParameters()));
    // Execution
    final SqlRunner sqlRunner = new SqlRunner(ds);
    if (builder.getNbProjections() == 0) {
      final List<Object[]> countOnly = sqlRunner.query(effectiveSql, builder.getParameters());
      if (countOnly.size() != 1) {
        throw new PrologNonSpecificException("Query for counting " + effectiveSql + "did not return a single result set row but " + countOnly.size());
      }
      if (countOnly.get(0).length != 1) {
        throw new PrologNonSpecificException(
                "Query for counting " + effectiveSql + "did not return a single column set row but " + countOnly.get(0).length);
      }
      final Number resultSet = (Number) countOnly.get(0)[0];
      int number = resultSet.intValue();
      while (number-- > 0) {
        // Generates solutions without binding variables, just the right number of them
        notifySolution(currentVars);
      }
    } else {
      throw new UnsupportedOperationException("Feature not yet migrated to unify version");
        /*
      final List<Object[]> resultSet = sqlRunner.query(effectiveSql, builder.getParameters());
      // Vars referenced in projections
      final Var projectedVars[] = new Var[projectVars.size()];
      TermBindings originalBindings = null;
      int counter = 0;
      for (final Var var : projectVars) {
        final Var originalVar = termApi().findVar(conditions, var.getName());
        if (originalVar == null) {
          throw new InvalidTermException("Could no find original var " + var.getName() + " within " + conditions);
        }
        projectedVars[counter] = originalVar;
        final TermBindings bindingsForVar = theBindings.findBindings(originalVar);
        if (bindingsForVar == null) {
          throw new InvalidTermException("Could no find originalBindings for variable " + var.getName());
        }
        if (originalBindings == null) {
          // Initialize
          originalBindings = bindingsForVar;
        } else {
          // Check
          if (bindingsForVar != originalBindings) {
            throw new InvalidTermException("Not all variables share the same bindings - won't be able to unify");
          }
        }
        counter++;
      }
      // Generate solutions, one per row
      for (final Object[] objects : resultSet) {
        unifyAndNotify(projectedVars, objects, originalBindings, theListener);
      }
      */
    }
    return Continuation.CONTINUE;
  }

  //  /**
  //   * Translate prolog operators into SQL operator.
  //   *
  //   * @param theOperator
  //   * @return The valid SQL operator.
  //   */
  //  private String sqlOperator(Operator theOperator) {
  //    return theOperator.getSql();
  ////    if ("=<".equals(theOperator)) {
  ////      return "<=";
  ////    }
  ////    if ("\\=".equals(theOperator)) {
  ////      return "!=";
  ////    }
  ////    return theOperator;
  //  }

  /**
   * @param theTerm
   * @return A JDBC argument from a Term value
   */
  private Object jdbcFromTerm(Object theTerm) {
    if (theTerm instanceof Number) {
      return ((Number) theTerm).longValue();
    } else if (theTerm instanceof Struct) {
      final Struct<?> struct = (Struct<?>) theTerm;
      if (PrologLists.isList(struct)) {
        final Set<Object> javaList = new HashSet<>();
        for (final Term t : PrologLists.javaListFromPList(struct, new ArrayList<>(), Term.class)) {
          javaList.add(jdbcFromTerm(t));
        }
        return javaList;
      }
      return struct.getName();
    } else {
      throw new PrologNonSpecificException("Cannot convert to SQL parameter: " + theTerm.getClass());
    }
  }

  /**
   * @param theBinding
   * @param currentVars
   * @param desiredClassOrInterface @return The object bound to a Term by its name
   */
  private <T> T bound(Object theBinding, UnifyContext currentVars, Class<T> desiredClassOrInterface) {
    final Object value = currentVars.reify(theBinding);
    ensureBindingIsNotAFreeVar(value, "bound/1", 0);
    final String bindingName = String.valueOf(value);

    final Object instance = EnvManager.getThreadVariable(bindingName);
    return TypeUtils.safeCastNotNull("unwrapping binding \"" + instance + '"', instance, desiredClassOrInterface);
  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public TermAdapter getTermAdapter() {
    return this.termAdapter;
  }

  public void setTermAdapter(TermAdapter theTermAdapter) {
    this.termAdapter = theTermAdapter;
  }

}
