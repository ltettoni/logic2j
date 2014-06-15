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
package org.logic2j.core.library.impl;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.Solver;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.util.TypeUtils;
import org.logic2j.core.api.library.Primitive;

import java.util.ArrayList;

/**
 * Provide the core primitives of the Prolog language.
 * Most is implemented in Java, but there is an associated Prolog theory at:
 * /src/main/prolog/org/logic2j/core/library/impl/core/CoreLibrary.pro
 */
@SuppressWarnings("StringEquality")
public class CoreLibrary extends LibraryBase {

    private static final ComparisonFunction COMPARE_GT = new ComparisonFunction() {
        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() > val2.doubleValue();
        }
    };

    private static final ComparisonFunction COMPARISON_LT = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() < val2.doubleValue();
        }
    };

    private static final ComparisonFunction COMPARISON_GE = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() >= val2.doubleValue();
        }
    };

    private static final ComparisonFunction COMPARISON_LE = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() <= val2.doubleValue();
        }
    };

    private static final ComparisonFunction COMPARISON_EQ = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() == val2.doubleValue();
        }
    };

    private static final ComparisonFunction COMPARISON_NE = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() != val2.doubleValue();
        }
    };

    private static final AggregationFunction AGGREGATION_PLUS = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return val1.longValue() + val2.longValue();
            }
            return (double) (val1.longValue() + val2.longValue());
        }

    };

    private static final AggregationFunction AGGREGATION_MINUS = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return val1.longValue() - val2.longValue();
            }
            return (double) (val1.longValue() - val2.longValue());
        }

    };

    private static final AggregationFunction AGGREGRATION_TIMES = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return val1.longValue() * val2.longValue();
            }
            return (double) (val1.longValue() * val2.longValue());
        }

    };

    private static final AggregationFunction AGGREGATION_NEGATE = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return -val1.longValue();
            }
            return (double) -val1.longValue();
        }

    };

    public CoreLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, UnifyContext currentVars, SolutionListener theListener) {
        final Object result;
        // Argument methodName is {@link String#intern()}alized so OK to check by reference
        final Object[] goalStructArgs = theGoalStruct.getArgs();
        final int arity = goalStructArgs.length;
        if (arity == 1) {
            final Object arg0 = goalStructArgs[0];
            if (theMethodName == "not") {
                result = not(theListener, currentVars, arg0);
            } else if (theMethodName == "var") {
                result = var(theListener, currentVars, arg0);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 2) {
            final Object arg0 = goalStructArgs[0];
            final Object arg1 = goalStructArgs[1];
            if (theMethodName == "unify") {
                result = unify(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "expression_greater_equal_than") {
                result = expression_greater_equal_than(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "expression_greater_than") {
                result = expression_greater_than(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "expression_lower_than") {
                result = expression_lower_than(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "expression_lower_equal_than") {
                result = expression_lower_equal_than(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "expression_equals") {
                result = expression_equals(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "is") {
                result = is(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "plus") {
                result = plus(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "minus") {
                result = minus(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "multiply") {
                result = multiply(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "notUnify") {
                result = notUnify(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "clause") {
                result = clause(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "predicate2PList") {
                result = predicate2PList(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "atom_length") {
                result = atom_length(theListener, currentVars, arg0, arg1);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 3) {
            final Object arg0 = goalStructArgs[0];
            final Object arg1 = goalStructArgs[1];
            final Object arg2 = goalStructArgs[2];
            if (theMethodName == "findall") {
                result = findall(theListener, currentVars, arg0, arg1, arg2);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 0) {
            if (theMethodName == "trueFunctor") {
                result = trueFunctor(theListener, currentVars);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else {
            result = NO_DIRECT_INVOCATION_USE_REFLECTION;
        }
        return result;
    }

    @Primitive(name = Struct.FUNCTOR_TRUE)
    // We can't name the method "true" it's a Java reserved word...
    public Continuation trueFunctor(SolutionListener theListener, UnifyContext currentVars) {
        return notifySolution(theListener, currentVars);
    }

    @Primitive
    public Continuation fail(@SuppressWarnings("unused") SolutionListener theListener, @SuppressWarnings("unused") UnifyContext currentVars) {
        // Do not propagate a solution - that's all
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation var(SolutionListener theListener, UnifyContext currentVars, Object t1) {
        Continuation continuation = Continuation.CONTINUE;
        if (t1 instanceof Var) {
            final Var var = (Var) t1;
            if (var.isAnonymous()) {
                notifySolution(theListener, currentVars);
            } else {
                final Object value = currentVars.reify(t1);
                if (value instanceof Var) {
                    continuation = notifySolution(theListener, currentVars);
                }
            }
        }
        return continuation;
    }

    @Primitive
    public Continuation atomic(SolutionListener theListener, UnifyContext currentVars, Object theTerm) {
        final Object value = currentVars.reify(theTerm);
        ensureBindingIsNotAFreeVar(value, "atomic/1");
        if (value instanceof Struct || value instanceof Number) {
            return notifySolution(theListener, currentVars);
        }
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation number(SolutionListener theListener, UnifyContext currentVars, Object theTerm) {
        final Object value = currentVars.reify(theTerm);
        ensureBindingIsNotAFreeVar(value, "number/1");
        if (value instanceof Number) {
            return notifySolution(theListener, currentVars);
        }
        return Continuation.CONTINUE;
    }

    /**
     * Note: this implmentation is valid, yet may be bypassed by a "native" implementation in {@link org.logic2j.core.impl.Solver}.
     *
     * @param theListener
     * @param currentVars
     * @return {@link Continuation#CUT}
     */
    @Primitive(name = Struct.FUNCTOR_CUT)
    public Continuation cut(SolutionListener theListener, UnifyContext currentVars) {
        // This is a complex behaviour - read on DefaultSolver
        // Cut is a "true" solution to a goal, just notify one as such
        notifySolution(theListener, currentVars);
        return Continuation.CUT;
    }

    @Primitive(name = "=")
    public Continuation unify(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return unifyInternal(theListener, currentVars, t1, t2);
    }

    @Primitive(name = "\\=")
    public Continuation notUnify(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        final UnifyContext after = currentVars.unify(t1, t2);
        if (after == null) {
            // Not unified
            return notifySolution(theListener, currentVars);
        }
        // Unified
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation atom_length(SolutionListener theListener, UnifyContext currentVars, Object theAtom, Object theLength) {
        final Object value = currentVars.reify(theAtom);
        ensureBindingIsNotAFreeVar(value, "atom_length/2");
        final String atomText = value.toString();
        final Long atomLength = (long) atomText.length();
        return unify(theListener, currentVars, atomLength, theLength);
    }

    @Primitive(synonyms = "\\+")
    // Surprisingly enough the operator \+ means "not provable".
    public Continuation not(final SolutionListener theListener, UnifyContext currentVars, Object theGoal) {
        // final Term target = resolveNonVar(theGoal, currentVars, "not");
        final class NegationListener implements SolutionListener {
            boolean found = false;

            @Override
            public Continuation onSolution(UnifyContext currentVars) {
                // Do NOT relay the solution further, just remember there was one
                this.found = true;
                return Continuation.USER_ABORT; // No need to seek for further solutions
            }
        }
        final NegationListener callListener = new NegationListener();

        Solver solver = getProlog().getSolver();
        solver.solveGoal(theGoal, currentVars, callListener);
        final Continuation continuation;
        if (callListener.found) {
            continuation = Continuation.CONTINUE;
        } else {
            // Not found - notify a solution (that's the purpose of not/1 !)
            continuation = theListener.onSolution(currentVars);
        }
        return continuation;
    }

    @Primitive
    public Continuation findall(SolutionListener theListener, final UnifyContext currentVars, final Object theTemplate, final Object theGoal, final Object theResult) {
        final ArrayList<Object> javaResults = new ArrayList<Object>(100); // Our internal collection of results
        final SolutionListener listenerForSubGoal = new SolutionListener() {

            @Override
            public Continuation onSolution(UnifyContext currentVars) {
                final Object templateReified = currentVars.reify(theTemplate);
                javaResults.add(templateReified);
                return Continuation.CONTINUE;
            }
        };
        // Now solve the target sub goal
        getProlog().getSolver().solveGoal(theGoal, currentVars, listenerForSubGoal);

        // Convert all results into a prolog list structure
        // Note on var indexes: all variables present in the projection term will be
        // copied into the resulting plist, so there's no need to reindex.
        // However, the root level Struct that makes up the list does contain a bogus
        // index value but -1.
        final Struct plist = Struct.createPList(javaResults);

        // And unify with result
        return unify(theListener, currentVars, theResult, plist);
    }

    /**
     * @param theListener
     * @param currentVars
     * @param theList
     * @param theLength
     * @return Length of a prolog list
     */
    @Primitive
    public Continuation length(SolutionListener theListener, UnifyContext currentVars, Object theList, Object theLength) {
        final Object value = currentVars.reify(theList);
        ensureBindingIsNotAFreeVar(value, "length/2");
        if (!TermApi.isList(value)) {
            throw new InvalidTermException("A Prolog list is required for length/2,  was " + value);
        }
        final ArrayList<Object> javalist = ((Struct) value).javaListFromPList(new ArrayList<Object>(), Object.class);
        final Long listLength = (long) javalist.size();
        return unify(theListener, currentVars, listLength, theLength);
    }

    @Primitive
    public Continuation clause(SolutionListener theListener, UnifyContext currentVars, Object theHead, Object theBody) {
        final Object headValue = currentVars.reify(theHead);
        final Struct realHead = TypeUtils.safeCastNotNull("dereferencing argument for clause/2", headValue, Struct.class);
        final Object[] clauseHeadAndBody = new Object[2];
        for (final ClauseProvider cp : getProlog().getTheoryManager().getClauseProviders()) {
            for (final Clause clause : cp.listMatchingClauses(realHead, currentVars)) {
                // Clone the clause so that we can unify against its bindings
                clause.headAndBodyForSubgoal(currentVars, clauseHeadAndBody);
                final Object clauseHead = clauseHeadAndBody[0];
                final UnifyContext varsAfterHeadUnified = currentVars.unify(realHead, clauseHead);
                final boolean headUnified = varsAfterHeadUnified != null;
                if (headUnified) {
                    // Determine body
                    final boolean isRule = clauseHeadAndBody[1] != null;
                    final Object clauseBody = isRule ? clauseHeadAndBody[1] : Struct.ATOM_TRUE;
                    // Unify Body
                    final UnifyContext varsAfterBodyUnified = varsAfterHeadUnified.unify(clauseBody, theBody);
                    if (varsAfterBodyUnified != null) {
                        final Continuation continuation = notifySolution(theListener, varsAfterBodyUnified);
                        if (continuation != Continuation.CONTINUE) {
                            return continuation;
                        }
                    }
                }
            }
        }
        return Continuation.CONTINUE;
    }

    @Primitive(name = "=..")
    public Continuation predicate2PList(final SolutionListener theListener, UnifyContext currentVars, Object thePredicate, Object theList) {
        final Object predicateValue = currentVars.reify(thePredicate);
        if (predicateValue instanceof Var) {
            // thePredicate is still free, going ot match from theList
            final Object listValue = currentVars.reify(theList);
            ensureBindingIsNotAFreeVar(listValue, "=../2");
            final Struct lst2 = (Struct) listValue;
            final Struct flattened = lst2.predicateFromPList();

            return unify(theListener, currentVars, predicateValue, flattened);
        } else {
            // thePredicate is bound
            if (predicateValue instanceof Struct) {
                final Struct struct = (Struct) predicateValue;
                final int arity = struct.getArity();
                final ArrayList<Object> elems = new ArrayList<Object>(1 + arity);
                elems.add(struct.getName());
                for (Object arg : struct.getArgs()) {
                    elems.add(arg);
                }
                final Struct plist = Struct.createPList(elems);
                return unify(theListener, currentVars, plist, theList);
            }
        }
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation is(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        final Object evaluated = evaluate(currentVars, t2);
        if (evaluated == null) {
            // No solution
            return Continuation.CONTINUE;
        }
        return unify(theListener, currentVars, t1, evaluated);
    }

    // ---------------------------------------------------------------------------
    // Binary numeric predicates
    // ---------------------------------------------------------------------------

    private static interface ComparisonFunction {
        boolean apply(Number val1, Number val2);
    }

    /**
     * For all binary predicates that compare numeric values.
     *
     * @param theListener
     * @param currentVars
     * @param t1
     * @param t2
     * @param theEvaluationFunction
     * @return The {@link Continuation} as returned by the solution notified.
     */
    private Continuation binaryComparisonPredicate(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2, ComparisonFunction theEvaluationFunction) {
        final Object effectiveT1 = evaluate(currentVars, t1);
        final Object effectiveT2 = evaluate(currentVars, t2);
        Continuation continuation = Continuation.CONTINUE;
        if (effectiveT1 instanceof Number && effectiveT2 instanceof Number) {
            final boolean condition = theEvaluationFunction.apply((Number) effectiveT1, (Number) effectiveT2);
            if (condition) {
                continuation = notifySolution(theListener, currentVars);
            }
            return continuation;
        }
        return continuation;
    }


    @Primitive(name = ">")
    public Continuation expression_greater_than(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARE_GT);
    }

    @Primitive(name = "<")
    public Continuation expression_lower_than(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_LT);
    }

    @Primitive(name = ">=")
    public Continuation expression_greater_equal_than(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_GE);
    }

    @Primitive(name = "=<")
    public Continuation expression_lower_equal_than(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_LE);
    }

    @Primitive(name = "=:=")
    public Continuation expression_equals(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_EQ);
    }

    @Primitive(name = "=\\=")
    public Continuation expression_not_equals(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_NE);
    }

    // ---------------------------------------------------------------------------
    // Functors
    // ---------------------------------------------------------------------------

    private static interface AggregationFunction {
        Number apply(Number val1, Number val2);
    }

    private Object binaryFunctor(@SuppressWarnings("unused") SolutionListener theListener, UnifyContext currentVars, Object theTerm1, Object theTerm2, AggregationFunction theEvaluationFunction) {
        final Object t1 = evaluate(currentVars, theTerm1);
        final Object t2 = evaluate(currentVars, theTerm2);
        if (t1 instanceof Number && t2 instanceof Number) {
            if (t1 instanceof Long && t2 instanceof Long) {
                return theEvaluationFunction.apply((Number) t1, (Number) t2).longValue();
            }
            return theEvaluationFunction.apply((Number) t1, (Number) t2).doubleValue();
        }
        throw new InvalidTermException("Could not add because 2 terms are not Numbers: " + t1 + " and " + t2);
    }

    @Primitive(name = "+")
    public Object plus(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {

        return binaryFunctor(theListener, currentVars, t1, t2, AGGREGATION_PLUS);
    }

    /**
     * @param theListener
     * @param currentVars
     * @param t1
     * @param t2
     * @return Binary minus (subtract)
     */
    @Primitive(name = "-")
    public Object minus(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryFunctor(theListener, currentVars, t1, t2, AGGREGATION_MINUS);

    }

    /**
     * @param theListener
     * @param t1
     * @param t2
     * @return Binary multiply
     */
    @Primitive(name = "*")
    public Object multiply(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryFunctor(theListener, currentVars, t1, t2, AGGREGRATION_TIMES);

    }

    /**
     * @param theListener
     * @param currentVars
     * @param t1
     * @return Unary minus (negate)
     */
    @Primitive(name = "-")
    public Object minus(SolutionListener theListener, UnifyContext currentVars, Object t1) {
        return binaryFunctor(theListener, currentVars, t1, 0L, AGGREGATION_NEGATE);

    }


    private Object evaluate(UnifyContext currentVars, Object t1) {
        final Object reify = currentVars.reify(t1);
        final Object evaluate = TermApi.evaluate(reify, currentVars);
        return evaluate;
    }
}