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
package org.logic2j.core.library.impl.core;

import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.Solver;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.impl.DefaultSolver;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;

import java.util.ArrayList;

/**
 * Provide the core primitives of the Prolog language.
 * Most is implemented in Java, but there is an associated Prolog theory at:
 * /src/main/prolog/org/logic2j/core/library/impl/core/CoreLibrary.prolog
 */
public class CoreLibrary extends LibraryBase {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoreLibrary.class);

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
                return Long.valueOf(val1.longValue() + val2.longValue());
            }
            return Double.valueOf(val1.longValue() + val2.longValue());
        }

    };

    private static final AggregationFunction AGGREGATION_MINUS = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return Long.valueOf(val1.longValue() - val2.longValue());
            }
            return Double.valueOf(val1.longValue() - val2.longValue());
        }

    };

    private static final AggregationFunction AGGREGRATION_TIMES = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return Long.valueOf(val1.longValue() * val2.longValue());
            }
            return Double.valueOf(val1.longValue() * val2.longValue());
        }

    };

    private static final AggregationFunction AGGREGATION_NEGATE = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return Long.valueOf(-val1.longValue());
            }
            return Double.valueOf(-val1.longValue());
        }

    };

    public CoreLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, PoV pov, SolutionListener theListener) {
        final Object result;
        // Argument methodName is {@link String#intern()}alized so OK to check by reference
        final int arity = theGoalStruct.getArity();
        if (arity == 1) {
            final Object arg0 = theGoalStruct.getArg(0);
            if (theMethodName == "not") {
                result = not(theListener, pov, arg0);
            } else if (theMethodName == "var") {
                result = var(theListener, pov, arg0);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 2) {
            final Object arg0 = theGoalStruct.getArg(0);
            final Object arg1 = theGoalStruct.getArg(1);
            if (theMethodName == "unify") {
                result = unify(theListener, pov, arg0, arg1);
            } else if (theMethodName == "expression_greater_equal_than") {
                result = expression_greater_equal_than(theListener, pov, arg0, arg1);
            } else if (theMethodName == "expression_greater_than") {
                result = expression_greater_than(theListener, pov, arg0, arg1);
            } else if (theMethodName == "expression_lower_than") {
                result = expression_lower_than(theListener, pov, arg0, arg1);
            } else if (theMethodName == "expression_lower_equal_than") {
                result = expression_lower_equal_than(theListener, pov, arg0, arg1);
            } else if (theMethodName == "expression_equals") {
                result = expression_equals(theListener, pov, arg0, arg1);
            } else if (theMethodName == "is") {
                result = is(theListener, pov, arg0, arg1);
            } else if (theMethodName == "plus") {
                result = plus(theListener, pov, arg0, arg1);
            } else if (theMethodName == "minus") {
                result = minus(theListener, pov, arg0, arg1);
            } else if (theMethodName == "multiply") {
                result = multiply(theListener, pov, arg0, arg1);
            } else if (theMethodName == "notUnify") {
                result = notUnify(theListener, pov, arg0, arg1);
//            } else if (theMethodName == "clause") {
//                result = clause(theListener, pov, arg0, arg1);
//            } else if (theMethodName == "predicate2PList") {
//                result = predicate2PList(theListener, pov, arg0, arg1);
            } else if (theMethodName == "atom_length") {
                result = atom_length(theListener, pov, arg0, arg1);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 3) {
            final Object arg0 = theGoalStruct.getArg(0);
            final Object arg1 = theGoalStruct.getArg(1);
            final Object arg2 = theGoalStruct.getArg(2);
            if (theMethodName == "findall") {
                result = findall(theListener, pov, arg0, arg1, arg2);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 0) {
            if (theMethodName == "trueFunctor") {
                result = trueFunctor(theListener, pov);
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
    public Continuation trueFunctor(SolutionListener theListener, PoV pov) {
        return notifySolution(theListener, pov);
    }

    @Primitive
    public Continuation fail(@SuppressWarnings("unused") SolutionListener theListener, @SuppressWarnings("unused") PoV pov) {
        // Do not propagate a solution - that's all
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation var(SolutionListener theListener, PoV pov, Object t1) {
        Continuation continuation = Continuation.CONTINUE;
        if (t1 instanceof Var) {
            final Var var = (Var) t1;
            if (var.isAnonymous()) {
                notifySolution(theListener, pov);
            } else {
                final Object value = pov.reify(t1);
                if (value instanceof Var) {
                    continuation = notifySolution(theListener, pov);
                }
            }
        }
        return continuation;
    }

    @Primitive
    public Continuation atomic(SolutionListener theListener, PoV pov, Object theTerm) {
        final Object value = pov.reify(theTerm);
        ensureBindingIsNotAFreeVar(value, "atomic/1");
        if (value instanceof Struct || value instanceof Number) {
            return notifySolution(theListener, pov);
        }
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation number(SolutionListener theListener, PoV pov, Object theTerm) {
        final Object value = pov.reify(theTerm);
        ensureBindingIsNotAFreeVar(value, "number/1");
        if (value instanceof Number) {
            return notifySolution(theListener, pov);
        }
        return Continuation.CONTINUE;
    }

    /**
     * Note: this implmentation is valid, yet may be bypassed by a "native" implementation in {@link DefaultSolver}.
     *
     * @param theListener
     * @param theTermBindings
     * @return {@link Continuation#CUT}
     */
    @Primitive(name = Struct.FUNCTOR_CUT)
    public Continuation cut(SolutionListener theListener, PoV pov) {
        // This is a complex behaviour - read on DefaultSolver
        // Cut is a "true" solution to a goal, just notify one as such
        notifySolution(theListener, pov);
        return Continuation.CUT;
    }

    @Primitive(name = "=")
    public Continuation unify(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        final PoV after = pov.unify(t1, t2);
        if (after == null) {
            // Not unified
            return Continuation.CONTINUE;
        }
        // Unified
        return notifySolution(theListener, after);
    }

    @Primitive(name = "\\=")
    public Continuation notUnify(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        final PoV after = pov.unify(t1, t2);
        if (after == null) {
            // Not unified
            return notifySolution(theListener, pov);
        }
        // Unified
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation atom_length(SolutionListener theListener, PoV pov, Object theAtom, Object theLength) {
        final Object value = pov.reify(theAtom);
        ensureBindingIsNotAFreeVar(value, "atom_length/2");
        final String atomText = value.toString();
        final Long atomLength = Long.valueOf(atomText.length());
        return unify(theListener, pov, atomLength, theLength);
    }

    @Primitive(synonyms = "\\+")
    // Surprisingly enough the operator \+ means "not provable".
    public Continuation not(final SolutionListener theListener, PoV pov, Object theGoal) {
        // final Term target = resolveNonVar(theGoal, pov, "not");
        final class NegationListener implements SolutionListener {
            boolean found = false;

            @Override
            public Continuation onSolution(PoV theReifier) {
                // Do NOT relay the solution further, just remember there was one
                this.found = true;
                return Continuation.USER_ABORT; // No need to seek for further solutions
            }
        }
        final NegationListener callListener = new NegationListener();

        Solver solver = getProlog().getSolver();
        solver.solveGoal(theGoal, pov, callListener);
        if (!callListener.found) {
            theListener.onSolution(pov);
        }

        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation findall(SolutionListener theListener, final PoV pov, final Object theTemplate, final Object theGoal, final Object theResult) {
        final ArrayList<Object> javaResults = new ArrayList<Object>(100); // Our internal collection of results
        final SolutionListener listenerForSubGoal = new SolutionListener() {

            @Override
            public Continuation onSolution(PoV pov) {
                final Object templateReified = pov.reify(theTemplate);
                javaResults.add(templateReified);
                return Continuation.CONTINUE;
            }
        };
        // Now solve the target sub goal
        getProlog().getSolver().solveGoal(theGoal, pov, listenerForSubGoal);

        // Convert all results into a prolog list structure
        // Note on var indexes: all variables present in the projection term will be
        // copied into the resulting plist, so there's no need to reindex.
        // However, the root level Struct that makes up the list does contain a bogus
        // index value but -1.
        final Struct plist = Struct.createPList(javaResults);

        // And unify with result
        return unify(theListener, pov, theResult, plist);
    }

    /**
     * @param theListener
     * @param theTermBindings
     * @param theList
     * @param theLength
     * @return Length of a prolog list
     */
    @Primitive
    public Continuation length(SolutionListener theListener, PoV pov, Object theList, Object theLength) {
        final Object value = pov.reify(theList);
        ensureBindingIsNotAFreeVar(value, "length/2");
        if (!TermApi.isList(value)) {
            throw new InvalidTermException("A Prolog list is required for length/2,  was " + value);
        }
        final ArrayList<Object> javalist = ((Struct) value).javaListFromPList(new ArrayList<Object>(), Object.class);
        final Long listLength = Long.valueOf(javalist.size());
        return unify(theListener, pov, listLength, theLength);
    }

//    @Primitive
//    public Continuation clause(SolutionListener theListener, PoV pov, Object theHead, Object theBody) {
//        final Binding dereferencedBinding;
//        if (theHead instanceof Var) {
//            dereferencedBinding = ((Var) theHead).bindingWithin(theTermBindings).followLinks();
//        } else {
//            dereferencedBinding = Binding.newLiteral(theHead, theTermBindings);
//        }
//        final Struct realHead = ReflectUtils.safeCastNotNull("dereferencing argumnent for clause/2", dereferencedBinding.getTerm(), Struct.class);
//        for (final ClauseProvider cp : getProlog().getTheoryManager().getClauseProviders()) {
//            for (final Clause clause : cp.listMatchingClauses(realHead, theTermBindings)) {
//                // Clone the clause so that we can unify against its bindings
//                final Clause clauseToUnify = new Clause(clause);
//                final boolean headUnified = unify(clauseToUnify.getHead(), clauseToUnify.getTermBindings(), realHead, dereferencedBinding.getTermBindings());
//                if (headUnified) {
//                    try {
//                        final boolean bodyUnified = unify(clauseToUnify.getBody(), clauseToUnify.getTermBindings(), theBody, theTermBindings);
//                        if (bodyUnified) {
//                            try {
//                                notifySolution(theListener);
//                            } finally {
//                                deunify();
//                            }
//                        }
//                    } finally {
//                        deunify();
//                    }
//                }
//            }
//        }
//        return Continuation.CONTINUE;
//    }
//
//    @Primitive(name = "=..")
//    public Continuation predicate2PList(final SolutionListener theListener, PoV pov, Object thePredicate, Object theList) {
//        TermBindings resolvedBindings = theTermBindings.narrow(thePredicate, Object.class);
//        Continuation continuation = Continuation.CONTINUE;
//
//        if (resolvedBindings.isFreeReferrer()) {
//            // thePredicate is still free, going ot match from theList
//            resolvedBindings = theTermBindings.narrow(theList, Term.class);
//            ensureBindingIsNotAFreeVar(resolvedBindings, "=../2");
//            final Struct lst2 = (Struct) resolvedBindings.getReferrer();
//            final Struct flattened = lst2.predicateFromPList();
//            final boolean unified = unify(thePredicate, pov, flattened, resolvedBindings);
//            continuation = notifyIfUnified(unified, theListener);
//        } else {
//            final Object predResolved = resolvedBindings.getReferrer();
//            if (predResolved instanceof Struct) {
//                final Struct struct = (Struct) predResolved;
//                final ArrayList<Object> elems = new ArrayList<Object>();
//                elems.add(new Struct(struct.getName())); // Only copying the functor as an atom, not a deep copy of the struct!
//                final int arity = struct.getArity();
//                for (int i = 0; i < arity; i++) {
//                    elems.add(struct.getArg(i));
//                }
//                final Struct plist = Struct.createPList(elems);
//                final boolean unified = unify(theList, pov, plist, resolvedBindings);
//                continuation = notifyIfUnified(unified, theListener);
//            }
//        }
//        return continuation;
//    }

    @Primitive
    public Continuation is(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        final Object evaluated = evaluate(pov, t2);
        if (evaluated == null) {
            // No solution
            return Continuation.CONTINUE;
        }
        return unify(theListener, pov, t1, evaluated);
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
     * @param theTermBindings
     * @param t1
     * @param t2
     * @param theEvaluationFunction
     * @return The {@link Continuation} as returned by the solution notified.
     */
    private Continuation binaryComparisonPredicate(SolutionListener theListener, PoV pov, Object t1, Object t2, ComparisonFunction theEvaluationFunction) {
        final Object effectiveT1 = evaluate(pov, t1);
        final Object effectiveT2 = evaluate(pov, t2);
        Continuation continuation = Continuation.CONTINUE;
        if (effectiveT1 instanceof Number && effectiveT2 instanceof Number) {
            final boolean condition = theEvaluationFunction.apply((Number) effectiveT1, (Number) effectiveT2);
            if (condition) {
                continuation = notifySolution(theListener, pov);
            }
            return continuation;
        }
        return continuation;
    }


    @Primitive(name = ">")
    public Continuation expression_greater_than(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, pov, t1, t2, COMPARE_GT);
    }

    @Primitive(name = "<")
    public Continuation expression_lower_than(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, pov, t1, t2, COMPARISON_LT);
    }

    @Primitive(name = ">=")
    public Continuation expression_greater_equal_than(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, pov, t1, t2, COMPARISON_GE);
    }

    @Primitive(name = "=<")
    public Continuation expression_lower_equal_than(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, pov, t1, t2, COMPARISON_LE);
    }

    @Primitive(name = "=:=")
    public Continuation expression_equals(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, pov, t1, t2, COMPARISON_EQ);
    }

    @Primitive(name = "=\\=")
    public Continuation expression_not_equals(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, pov, t1, t2, COMPARISON_NE);
    }

    // ---------------------------------------------------------------------------
    // Functors
    // ---------------------------------------------------------------------------

    private static interface AggregationFunction {
        Number apply(Number val1, Number val2);
    }

    private Object binaryFunctor(@SuppressWarnings("unused") SolutionListener theListener, PoV pov, Object theTerm1, Object theTerm2, AggregationFunction theEvaluationFunction) {
        final Object t1 = evaluate(pov, theTerm1);
        final Object t2 = evaluate(pov, theTerm2);
        if (t1 instanceof Number && t2 instanceof Number) {
            if (t1 instanceof Long && t2 instanceof Long) {
                return Long.valueOf(theEvaluationFunction.apply((Number) t1, (Number) t2).longValue());
            }
            return Double.valueOf(theEvaluationFunction.apply((Number) t1, (Number) t2).doubleValue());
        }
        throw new InvalidTermException("Could not add because 2 terms are not Numbers: " + t1 + " and " + t2);
    }

    @Primitive(name = "+")
    public Object plus(SolutionListener theListener, PoV pov, Object t1, Object t2) {

        return binaryFunctor(theListener, pov, t1, t2, AGGREGATION_PLUS);
    }

    /**
     * @param theListener
     * @param theTermBindings
     * @param t1
     * @param t2
     * @return Binary minus (subtract)
     */
    @Primitive(name = "-")
    public Object minus(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        return binaryFunctor(theListener, pov, t1, t2, AGGREGATION_MINUS);

    }

    /**
     * @param theListener
     * @param t1
     * @param t2
     * @return Binary multiply
     */
    @Primitive(name = "*")
    public Object multiply(SolutionListener theListener, PoV pov, Object t1, Object t2) {
        return binaryFunctor(theListener, pov, t1, t2, AGGREGRATION_TIMES);

    }

    /**
     * @param theListener
     * @param theTermBindings
     * @param t1
     * @return Unary minus (negate)
     */
    @Primitive(name = "-")
    public Object minus(SolutionListener theListener, PoV pov, Object t1) {
        return binaryFunctor(theListener, pov, t1, 0L, AGGREGATION_NEGATE);

    }


    private Object evaluate(PoV pov, Object t1) {
        final Object reify = pov.reify(t1);
        final Object evaluate = TermApi.evaluate(reify, pov);
        return evaluate;
    }
}
