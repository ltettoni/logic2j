/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.logic2j.contrib.rdb;

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

import org.logic2j.contrib.library.pojo.PojoLibrary;
import org.logic2j.contrib.rdb.util.CollectionMap;
import org.logic2j.contrib.rdb.util.SqlBuilder3;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Column;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Table;
import org.logic2j.contrib.rdb.util.SqlRunner;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.api.solver.listener.UniqueSolutionListener;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.util.CollectionUtils;
import org.logic2j.core.impl.util.ReflectUtils;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;

/**
 * Prolog library that bridges the Prolog engine and a relational database seen as a facts repository.
 * TODO the {@link #select(SolutionListener, Bindings, Term...)} method should actually take the goal and create a constraint graph, then
 * transform the graph into SQL.
 */
public class RDBLibrary extends LibraryBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RDBLibrary.class);

    /**
     * The name of the tbl/4 predicate to describe a column.
     */
    private static final String TBL_PREDICATE = "tbl";
    private static final Set<String> ALLOWED_OPERATORS;

    static {
        ALLOWED_OPERATORS = new HashSet<String>(Arrays.asList(new String[] { "=", "\\=", "<", ">", "=<", ">=" }));
    }

    private TermAdapter termAdapter;

    public RDBLibrary(PrologImplementation theProlog) {
        super(theProlog);
        this.termAdapter = new RDBBase.AllStringsAsAtoms(theProlog);
    }

    @Primitive
    public Continuation select(SolutionListener theListener, Bindings theBindings, Object... theArguments) throws SQLException {
        final Object theDataSource = theArguments[0];
        final Object theExpression = theArguments[1];
        final DataSource ds = bound(theDataSource, theBindings, DataSource.class);

        final Bindings expressionBindings = theBindings.focus(theExpression, Struct.class);
        ensureBindingIsNotAFreeVar(expressionBindings, "select/*");
        final Struct conditions = (Struct) expressionBindings.getReferrer();

        // Options
        final Set<Object> optionSet = new HashSet<Object>(Arrays.asList(theArguments).subList(2, theArguments.length));
        final boolean isDistinct = optionSet.contains(new Struct("distinct"));

        //
        final String resultVar = "Tbl";

        // The goal we are solving
        Object internalGoal = Struct.valueOf("gd3_solve", conditions, resultVar);
        if (internalGoal instanceof Struct) {
            // Clone
            internalGoal = new Struct((Struct) internalGoal);
        }
        // Watch out this destroys the indexes in the original expression !!!!
        internalGoal = TermApi.normalize(internalGoal, getProlog().getLibraryManager().wholeContent());
        final Bindings internalBindings = new Bindings(internalGoal);
        final UniqueSolutionListener internalListener = new UniqueSolutionListener(internalBindings);
        getProlog().getSolver().solveGoal(internalBindings, internalListener);
        final Object result = internalListener.getSolution().getBinding(resultVar);
        if (!(result instanceof Struct)) {
            throw new InvalidTermException("Internal result must be a Struct");
        }
        final Struct plistOfTblPredicates = (Struct) result;
        logger.debug("select/3: Solving {} gives internal solution: {}", plistOfTblPredicates);
        final List<Struct> javaListRoot = plistOfTblPredicates.javaListFromPList(new ArrayList<Struct>(), Struct.class);
        logger.info(CollectionUtils.format("Internal solution, list elements:", javaListRoot, 10));
        final Map<String, Term> assignedVarValue = new HashMap<String, Term>();
        final Map<String, String> assignedVarOperator = new HashMap<String, String>();
        // Count number of references to tables
        int nbTbl = 0;
        for (final Struct tbls : javaListRoot) {
            for (final Struct pred : tbls.javaListFromPList(new ArrayList<Struct>(), Struct.class)) {
                final String functor = pred.getName();
                if (functor.equals(TBL_PREDICATE)) {
                    nbTbl++;
                }
                // Operator
                else if (ALLOWED_OPERATORS.contains(functor)) {
                    final Term term1 = TermApi.selectTerm(pred, "[1]", Term.class);
                    final Term term2 = TermApi.selectTerm(pred, "[2]", Term.class);
                    final String variableName;
                    final Term value;
                    if (term1 instanceof Var && !(term2 instanceof Var)) {
                        variableName = ((Var) term1).getName();
                        value = term2;
                    } else if (term2 instanceof Var && !(term1 instanceof Var)) {
                        variableName = ((Var) term2).getName();
                        value = term1;
                    } else {
                        throw new PrologNonSpecificError("Cannot (yet) handle operators with 2 unbound variables such as " + pred);
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
        final List<SqlBuilder3.Criterion> rawColumns = new ArrayList<SqlBuilder3.Criterion>();
        int aliasIndex = 1;
        final Set<Var> projectVars = new LinkedHashSet<Var>();
        for (final Struct tbls : javaListRoot) {
            final String alias = "t" + (aliasIndex++);
            final List<Struct> javaList = tbls.javaListFromPList(new ArrayList<Struct>(), Struct.class);
            for (final Struct tbl : javaList) {
                // Check predicate received must be "tbl" with arity of 4
                if (!tbl.getName().equals(TBL_PREDICATE)) {
                    throw new InvalidTermException("Predicate must be \"tbl\" not " + tbl.getName());
                }
                if (tbl.getArity() < 4 || tbl.getArity() > 5) {
                    throw new InvalidTermException("Arity of term " + tbl + " must be 4 or 5");
                }
                // Convert this predicate into a Criterion. When variables are specified, use the var as the operand value
                final String tableName = TermApi.selectTerm(tbl, "tbl[1]", Struct.class).getName();
                final String columnName = TermApi.selectTerm(tbl, "tbl[2]", Struct.class).getName();
                final Term valueTerm = TermApi.selectTerm(tbl, "tbl[4]", Term.class);
                String operator = SqlBuilder3.OPERATOR_EQ_OR_IN;
                if (tbl.getArity() >= 5) {
                    operator = TermApi.selectTerm(tbl, "tbl[5]", Struct.class).getName();
                }
                final Table table = builder.table(tableName, alias);
                final Column sqlColumn = builder.column(table, columnName);
                if (valueTerm instanceof Var) {
                    final Var var = (Var) valueTerm;
                    if (var.isAnonymous()) {
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
                        final String specifiedOperator = assignedVarOperator.get(varName);
                        if (specifiedOperator != null) {
                            operator = specifiedOperator;
                        }
                        rawColumns.add(builder.criterion(sqlColumn, sqlOperator(operator), jdbcFromTerm(varValue)));
                        // Although we have a constant value and not a free variable, we will have to project its
                        // _real_ value extracted from the database. In case of "=" this is dubious as the DB will
                        // of course return the same. But for other operators (e.g. ">"), the real DB value will be needed!
                        final Var originalVar = var;

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
        final CollectionMap<String, SqlBuilder3.Criterion> columnsPerVariable = new CollectionMap<String, SqlBuilder3.Criterion>();
        // Join clauses
        for (final SqlBuilder3.Criterion column : rawColumns) {
            if (column.getOperand()[0] instanceof Var) {
                final Var var = (Var) column.getOperand()[0];
                projectVars.add(var);
                columnsPerVariable.add(var.getName(), column);
            }
        }
        logger.debug("** colPerVar: {}", columnsPerVariable);

        // Every variable referenced contributes one projection. If more than on column for same variable (-->join), use only first of them
        for (final Collection<SqlBuilder3.Criterion> clausesOfOneJoinExpression : columnsPerVariable.values()) {
            builder.addProjection(clausesOfOneJoinExpression.iterator().next().getColumn());
            if (clausesOfOneJoinExpression.size() >= 2) {
                final List<SqlBuilder3.Criterion> toJoin = new ArrayList<SqlBuilder3.Criterion>(clausesOfOneJoinExpression);
                for (int i = 1; i < toJoin.size(); i++) {
                    builder.innerJoin(toJoin.get(0).getColumn(), toJoin.get(i).getColumn());
                }
            }
        }

        // Collect criteria (where value is constant)
        for (final SqlBuilder3.Criterion column : rawColumns) {
            if (!(column.getOperand()[0] instanceof Var)) {
                builder.addConjunction(column);
            }
        }

        logger.debug("** Projected bindings: {}", projectVars);
        logger.debug("** Builder       : {}", builder.describe());

        // Generate
        builder.setDistinct(isDistinct);
        if (builder.getNbProjections() == 0) {
            builder.generateSelectCount();
        } else {
            builder.generateSelect();
        }
        final String effectiveSql = builder.getSql();
        logger.debug("SQL   : {}", effectiveSql);
        logger.debug("Params: {}", Arrays.asList(builder.getParameters()));
        // Execution
        final SqlRunner sqlRunner = new SqlRunner(ds);
        if (builder.getNbProjections() == 0) {
            final List<Object[]> countOnly = sqlRunner.query(effectiveSql, builder.getParameters());
            if (countOnly.size() != 1) {
                throw new PrologNonSpecificError("Query for counting " + effectiveSql + "did not return a single result set row but " + countOnly.size());
            }
            if (countOnly.get(0).length != 1) {
                throw new PrologNonSpecificError("Query for counting " + effectiveSql + "did not return a single column set row but " + countOnly.get(0).length);
            }
            final Number resultSet = (Number) countOnly.get(0)[0];
            int number = resultSet.intValue();
            while (number-- > 0) {
                // Generates solutions without binding variables, just the right number of them
                notifySolution(theListener);
            }
        } else {
            final List<Object[]> resultSet = sqlRunner.query(effectiveSql, builder.getParameters());
            // Vars referenced in projections
            final Var projectedVars[] = new Var[projectVars.size()];
            Bindings originalBindings = null;
            int counter = 0;
            for (final Var var : projectVars) {
                final Var originalVar = TermApi.findVar(conditions, var.getName());
                if (originalVar == null) {
                    throw new InvalidTermException("Could no find original var " + var.getName() + " within " + conditions);
                }
                projectedVars[counter] = originalVar;
                final Bindings bindingsForVar = theBindings.findBindings(originalVar);
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
        }
        return Continuation.CONTINUE;
    }

    /**
     * Translate prolog operators into SQL operator.
     * 
     * @param theOperator
     * @return The valid SQL operator.
     */
    private String sqlOperator(String theOperator) {
        if ("=<".equals(theOperator)) {
            return "<=";
        }
        if ("\\=".equals(theOperator)) {
            return "!=";
        }
        return theOperator;
    }

    /**
     * @param theTerm
     * @return A JDBC argument from a Term value
     */
    private Object jdbcFromTerm(Object theTerm) {
        if (theTerm instanceof Number) {
            return ((Number) theTerm).longValue();
        } else if (theTerm instanceof Struct) {
            final Struct struct = (Struct) theTerm;
            if (TermApi.isList(struct)) {
                final Set<Object> javaList = new HashSet<Object>();
                for (final Term t : struct.javaListFromPList(new ArrayList<Term>(), Term.class)) {
                    javaList.add(jdbcFromTerm(t));
                }
                return javaList;
            }
            return struct.getName();
        } else {
            throw new PrologNonSpecificError("Cannot convert to SQL parameter: " + theTerm.getClass());
        }
    }

    /**
     * @param theBinding
     * @param desiredClassOrInterface
     * @return The object bound to a Term by its name
     */
    private <T> T bound(Object theBinding, Bindings theBindings, Class<T> desiredClassOrInterface) {
        final Bindings b = theBindings.focus(theBinding, Struct.class);
        ensureBindingIsNotAFreeVar(b, "bound");
        final Struct bindingName = (Struct) b.getReferrer();

        final Object instance = PojoLibrary.extract(bindingName.getName());
        return ReflectUtils.safeCastNotNull("unwrapping binding \"" + instance + '"', instance, desiredClassOrInterface);
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
