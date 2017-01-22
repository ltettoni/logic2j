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
import org.logic2j.core.api.library.annotation.Functor;
import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.solver.listener.SolutionListenerBase;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.NotListener;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.Solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Provide the core primitives of the Prolog language.
 * Most is implemented in Java, but there is an associated Prolog theory at:
 * /src/main/prolog/org/logic2j/core/library/impl/core/CoreLibrary.pro
 */
@SuppressWarnings("StringEquality")
public class CoreLibrary extends LibraryBase {

    public CoreLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    private static final ComparisonFunction COMPARE_GT = new ComparisonFunction() {
        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() > val2.doubleValue();
        }

        @Override
        public boolean apply(CharSequence val1, CharSequence val2) {
            return val1.toString().compareTo(val2.toString()) > 0;
        }
    };

    private static final ComparisonFunction COMPARISON_LT = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() < val2.doubleValue();
        }

        @Override
        public boolean apply(CharSequence val1, CharSequence val2) {
            return val1.toString().compareTo(val2.toString()) < 0;
        }
    };

    private static final ComparisonFunction COMPARISON_GE = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() >= val2.doubleValue();
        }

        @Override
        public boolean apply(CharSequence val1, CharSequence val2) {
            return val1.toString().compareTo(val2.toString()) >= 0;
        }
    };

    private static final ComparisonFunction COMPARISON_LE = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() <= val2.doubleValue();
        }

        @Override
        public boolean apply(CharSequence val1, CharSequence val2) {
            return val1.toString().compareTo(val2.toString()) <= 0;
        }
    };

    private static final ComparisonFunction COMPARISON_EQ = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() == val2.doubleValue();
        }

        @Override
        public boolean apply(CharSequence val1, CharSequence val2) {
            return val1.toString().compareTo(val2.toString()) == 0;
        }
    };

    private static final ComparisonFunction COMPARISON_NE = new ComparisonFunction() {

        @Override
        public boolean apply(Number val1, Number val2) {
            return val1.doubleValue() != val2.doubleValue();
        }

        @Override
        public boolean apply(CharSequence val1, CharSequence val2) {
            return val1.toString().compareTo(val2.toString()) != 0;
        }
    };

    private static final AggregationFunction AGGREGATION_PLUS = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() + val2.intValue();
            }
            return (double) (val1.intValue() + val2.intValue());
        }

    };

    private static final AggregationFunction AGGREGATION_MINUS = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() - val2.intValue();
            }
            return (double) (val1.intValue() - val2.intValue());
        }

    };

    private static final AggregationFunction AGGREGRATION_TIMES = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return val1.intValue() * val2.intValue();
            }
            return (double) (val1.intValue() * val2.intValue());
        }

    };

    private static final AggregationFunction AGGREGATION_NEGATE = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Integer && val2 instanceof Integer) {
                return -val1.intValue();
            }
            return (double) -val1.intValue();
        }

    };


    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, SolutionListener theListener, UnifyContext currentVars) {
        final Object result;
        // Argument methodName is {@link String#intern()}alized so OK to check by reference
        final Object[] goalStructArgs = theGoalStruct.getArgs();
        final int arity = goalStructArgs.length;
        if (arity == 0) {
            if (theMethodName == "fail") {
                result = fail(theListener, currentVars);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 1) {
            final Object arg0 = goalStructArgs[0];
            if (theMethodName == "not") {
                result = not(theListener, currentVars, arg0);
            } else if (theMethodName == "atom") {
                result = atom(theListener, currentVars, arg0);
            } else if (theMethodName == "atomic") {
                result = atomic(theListener, currentVars, arg0);
            } else if (theMethodName == "var") {
                result = var(theListener, currentVars, arg0);
            } else if (theMethodName == "exists") {
                result = exists(theListener, currentVars, arg0);
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
            } else if (theMethodName == "length") {
                result = length(theListener, currentVars, arg0, arg1);
            } else if (theMethodName == "count") {
                result = count(theListener, currentVars, arg0, arg1);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 3) {
            final Object arg0 = goalStructArgs[0];
            final Object arg1 = goalStructArgs[1];
            final Object arg2 = goalStructArgs[2];
            if (theMethodName == "findall") {
                result = findall(theListener, currentVars, arg0, arg1, arg2);
            } else if (theMethodName == "distinct") {
                result = distinct(theListener, currentVars, arg0, arg1, arg2);
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

    @Predicate(name = Struct.FUNCTOR_TRUE)
    // We can't name the method "true" it's a Java reserved word...
    public Integer trueFunctor(SolutionListener theListener, UnifyContext currentVars) {
        return notifySolution(theListener, currentVars);
    }

    @Predicate
    public Integer fail(@SuppressWarnings("unused") SolutionListener theListener, @SuppressWarnings("unused") UnifyContext currentVars) {
        // Do not propagate a solution - that's all
        return Continuation.CONTINUE;
    }

    @Predicate
    public Integer var(SolutionListener theListener, UnifyContext currentVars, Object t1) {
        Integer continuation = Continuation.CONTINUE;
        if (t1 instanceof Var) {
            final Var<?> var = (Var) t1;
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

    @Predicate
    public Integer atom(SolutionListener theListener, UnifyContext currentVars, Object theTerm) {
        final Object value = currentVars.reify(theTerm);
        if (TermApi.isAtom(value)) {
            return notifySolution(theListener, currentVars);
        }
        return Continuation.CONTINUE;
    }

    @Predicate
    public Integer atomic(SolutionListener theListener, UnifyContext currentVars, Object theTerm) {
        final Object value = currentVars.reify(theTerm);
        if (TermApi.isAtomic(value)) {
            return notifySolution(theListener, currentVars);
        }
        return Continuation.CONTINUE;
    }

    @Predicate
    public Integer number(SolutionListener theListener, UnifyContext currentVars, Object theTerm) {
        final Object value = currentVars.reify(theTerm);
        ensureBindingIsNotAFreeVar(value, "number/1", 0);
        if (value instanceof Number) {
            return notifySolution(theListener, currentVars);
        }
        return Continuation.CONTINUE;
    }

    @Predicate(name = "=")
    public Integer unify(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return unifyAndNotify(theListener, currentVars, t1, t2);
    }

    @Predicate(name = "\\=")
    public Integer notUnify(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        final UnifyContext after = currentVars.unify(t1, t2);
        if (after == null) {
            // Not unified
            return notifySolution(theListener, currentVars);
        }
        // Unified
        return Continuation.CONTINUE;
    }

    // Surprisingly enough the operator \+ means "not provable".
    @Predicate(synonyms = "\\+")
    public Integer not(final SolutionListener theListener, UnifyContext currentVars, Object theGoal) {

        final NotListener callListener = new NotListener();

        Solver solver = getProlog().getSolver();
        solver.solveGoal(theGoal, callListener, currentVars);
        final Integer continuation;
        if (callListener.exists()) {
            continuation = Continuation.CONTINUE;
        } else {
            // Not found - notify a solution (that's the purpose of not/1 !)
            continuation = theListener.onSolution(currentVars);
        }
        return continuation;
    }

    @Predicate
    public Integer atom_length(SolutionListener theListener, UnifyContext currentVars, Object theAtom, Object theLength) {
        final Object value = currentVars.reify(theAtom);
        ensureBindingIsNotAFreeVar(value, "atom_length/2", 0);
        final String atomText = value.toString();
        final Long atomLength = (long) atomText.length();
        return unify(theListener, currentVars, atomLength, theLength);
    }

    /**
     * Check existence of subGoal. Without binding its variables, and stopping any solving after the first solution
     * of subGoal is proven. This is a most efficient implementation but may have dubious effects - maybe to study
     * a little in details...
     *
     * @param theListener
     * @param currentVars
     * @param theGoal
     * @return
     */
    @Predicate
    public Integer exists(SolutionListener theListener, final UnifyContext currentVars, final Object theGoal) {
        final CountingSolutionListener listenerForSubGoal = new CountingSolutionListener() {
            @Override
            public Integer onSolution(UnifyContext currentVars) {
                super.onSolution(currentVars);
                // Upon the first solution found, notify the engine to stop generating
                return Continuation.USER_ABORT;
            }
        };
        // Now solve the target sub goal
        final Object effectiveGoal = currentVars.reify(theGoal);
        getProlog().getSolver().solveGoal(effectiveGoal, listenerForSubGoal, currentVars);

        // And unify with result
        final Long counted = listenerForSubGoal.count();
        // Note: won't ever be greater than one due to our listener that stops generation
        if (counted > 0) {
            return notifySolution(theListener, currentVars);
        }
        return Continuation.CONTINUE;
    }

    private void collectReifiedResults(UnifyContext currentVars, final Object theTemplate, Object theGoal, final Collection<Object> javaResults) {
        final SolutionListener listenerForSubGoal = new SolutionListenerBase() {

            @Override
            public Integer onSolution(UnifyContext currentVars) {
                final Object templateReified = currentVars.reify(theTemplate);
                javaResults.add(templateReified);
                return Continuation.CONTINUE;
            }
        };
        // Now solve the target sub goal
        final Object effectiveGoal = currentVars.reify(theGoal);
        getProlog().getSolver().solveGoal(effectiveGoal, listenerForSubGoal, currentVars);
    }


    @Predicate
    public Integer count(SolutionListener theListener, final UnifyContext currentVars, final Object theGoal, final Object theNumber) {
        final CountingSolutionListener listenerForSubGoal = new CountingSolutionListener();
        // Now solve the target sub goal
        final Object effectiveGoal = currentVars.reify(theGoal);
        getProlog().getSolver().solveGoal(effectiveGoal, listenerForSubGoal, currentVars);

        // And unify with result
        final Integer counted = (int)listenerForSubGoal.count();
        return unify(theListener, currentVars, theNumber, counted);
    }

    @Predicate
    public Integer findall(SolutionListener theListener, final UnifyContext currentVars, final Object theTemplate, final Object theGoal, final Object theResult) {
        final ArrayList<Object> allReifiedResults = new ArrayList<Object>(100); // Our internal collection of results
        collectReifiedResults(currentVars, theTemplate, theGoal, allReifiedResults);

        // Convert all results into a prolog list structure
        // Note on var indexes: all variables present in the projection term will be
        // copied into the resulting plist, so there's no need to reindex.
        // However, the root level Struct that makes up the list does contain a bogus
        // index value but -1.
        final Struct plist = Struct.createPList(allReifiedResults);

        // And unify with result
        return unify(theListener, currentVars, theResult, plist);
    }

    @Predicate
    public Integer distinct(SolutionListener theListener, final UnifyContext currentVars, final Object theTemplate, final Object theGoal, final Object theResult) {
        final LinkedHashSet<Object> distinctReifiedResults = new LinkedHashSet<Object>(100); // Our internal collection of results
        collectReifiedResults(currentVars, theTemplate, theGoal, distinctReifiedResults);

        // Convert all results into a prolog list structure
        // Note on var indexes: all variables present in the projection term will be
        // copied into the resulting plist, so there's no need to reindex.
        // However, the root level Struct that makes up the list does contain a bogus
        // index value but -1.
        final Struct plist = Struct.createPList(distinctReifiedResults);

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
    @Predicate
    public Integer length(SolutionListener theListener, UnifyContext currentVars, Object theList, Object theLength) {
        final Object value = currentVars.reify(theList);
        ensureBindingIsNotAFreeVar(value, "length/2", 0);
        if (!TermApi.isList(value)) {
            throw new InvalidTermException("A Prolog list is required for length/2,  was " + value);
        }
        final ArrayList<Object> javalist = ((Struct) value).javaListFromPList(new ArrayList<Object>(), Object.class);
        final Long listLength = (long) javalist.size();
        return unify(theListener, currentVars, listLength, theLength);
    }
    @Predicate
    public Integer clause(SolutionListener theListener, UnifyContext currentVars, Object theHead, Object theBody) {
        final Object headValue = currentVars.reify(theHead);
        final Object[] clauseHeadAndBody = new Object[2];
        for (final ClauseProvider cp : getProlog().getTheoryManager().getClauseProviders()) {
            for (final Clause clause : cp.listMatchingClauses(headValue, currentVars)) {
                // Clone the clause so that we can unify against its bindings
                clause.headAndBodyForSubgoal(currentVars, clauseHeadAndBody);
                final Object clauseHead = clauseHeadAndBody[0];
                final UnifyContext varsAfterHeadUnified = currentVars.unify(headValue, clauseHead);
                final boolean headUnified = varsAfterHeadUnified != null;
                if (headUnified) {
                    // Determine body
                    final boolean isRule = clauseHeadAndBody[1] != null;
                    final Object clauseBody = isRule ? clauseHeadAndBody[1] : Struct.ATOM_TRUE;
                    // Unify Body
                    final UnifyContext varsAfterBodyUnified = varsAfterHeadUnified.unify(clauseBody, theBody);
                    if (varsAfterBodyUnified != null) {
                        final Integer continuation = notifySolution(theListener, varsAfterBodyUnified);
                        if (continuation != Continuation.CONTINUE) {
                            return continuation;
                        }
                    }
                }
            }
        }
        return Continuation.CONTINUE;
    }

    @Predicate(name = "=..")
    public Integer predicate2PList(final SolutionListener theListener, UnifyContext currentVars, Object thePredicate, Object theList) {
        final Object predicateValue = currentVars.reify(thePredicate);
        if (predicateValue instanceof Var) {
            // thePredicate is still free, going ot match from theList
            final Object listValue = currentVars.reify(theList);
            ensureBindingIsNotAFreeVar(listValue, "=../2", 1);
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

    @Predicate
    public Integer is(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        final Object evaluated = TermApi.evaluate(t2, currentVars);
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

        boolean apply(CharSequence str1, CharSequence str2);
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
    private Integer binaryComparisonPredicate(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2, ComparisonFunction theEvaluationFunction) {
        final Object effectiveT1 = TermApi.evaluate(t1, currentVars);
        final Object effectiveT2 = TermApi.evaluate(t2, currentVars);
        Integer continuation = Continuation.CONTINUE;
        if (effectiveT1 instanceof Number && effectiveT2 instanceof Number) {
            final boolean condition = theEvaluationFunction.apply((Number) effectiveT1, (Number) effectiveT2);
            if (condition) {
                continuation = notifySolution(theListener, currentVars);
            }
            return continuation;
        }
        if (effectiveT1 instanceof CharSequence && effectiveT2 instanceof CharSequence) {
            final boolean condition = theEvaluationFunction.apply((CharSequence) effectiveT1, (CharSequence) effectiveT2);
            if (condition) {
                continuation = notifySolution(theListener, currentVars);
            }
            return continuation;
        }
        return continuation;
    }


    @Predicate(name = ">")
    public Integer expression_greater_than(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARE_GT);
    }

    @Predicate(name = "<")
    public Integer expression_lower_than(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_LT);
    }

    @Predicate(name = ">=")
    public Integer expression_greater_equal_than(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_GE);
    }

    @Predicate(name = "=<")
    public Integer expression_lower_equal_than(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_LE);
    }

    @Predicate(name = "=:=")
    public Integer expression_equals(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_EQ);
    }

    @Predicate(name = "=\\=")
    public Integer expression_not_equals(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, currentVars, t1, t2, COMPARISON_NE);
    }

    // ---------------------------------------------------------------------------
    // Functors
    // ---------------------------------------------------------------------------

    private static interface AggregationFunction {
        Number apply(Number val1, Number val2);
    }

    private Object binaryFunctor(@SuppressWarnings("unused") SolutionListener theListener, UnifyContext currentVars, Object theTerm1, Object theTerm2, AggregationFunction theEvaluationFunction) {
        final Object t1 = TermApi.evaluate(theTerm1, currentVars);
        final Object t2 = TermApi.evaluate(theTerm2, currentVars);
        if (t1 instanceof Number && t2 instanceof Number) {
            if (t1 instanceof Integer && t2 instanceof Integer) {
                return theEvaluationFunction.apply((Number) t1, (Number) t2).intValue();
            }
            return theEvaluationFunction.apply((Number) t1, (Number) t2).doubleValue();
        }
        throw new InvalidTermException("Could not add because 2 terms are not Numbers: " + t1 + " and " + t2);
    }

    @Functor(name = "+")
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
    @Functor(name = "-")
    public Object minus(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryFunctor(theListener, currentVars, t1, t2, AGGREGATION_MINUS);
    }

    /**
     * @param theListener
     * @param t1
     * @param t2
     * @return Binary multiply
     */
    @Functor(name = "*")
    public Object multiply(SolutionListener theListener, UnifyContext currentVars, Object t1, Object t2) {
        return binaryFunctor(theListener, currentVars, t1, t2, AGGREGRATION_TIMES);
    }

    /**
     * @param theListener
     * @param currentVars
     * @param t1
     * @return Unary minus (negate)
     */
    @Functor(name = "-")
    public Object minus(SolutionListener theListener, UnifyContext currentVars, Object t1) {
        return binaryFunctor(theListener, currentVars, t1, 0L, AGGREGATION_NEGATE);
    }
}
