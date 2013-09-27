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

import java.util.ArrayList;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Binding;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.api.solver.listener.SolutionListenerBase;
import org.logic2j.core.impl.DefaultSolver;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.util.ReflectUtils;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;

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
            } else {
                return Double.valueOf(val1.longValue() + val2.longValue());
            }
        }

    };
    private static final AggregationFunction AGGREGATION_MINUS = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return Long.valueOf(val1.longValue() - val2.longValue());
            } else {
                return Double.valueOf(val1.longValue() - val2.longValue());
            }
        }

    };
    private static final AggregationFunction AGGREGRATION_TIMES = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return Long.valueOf(val1.longValue() * val2.longValue());
            } else {
                return Double.valueOf(val1.longValue() * val2.longValue());
            }
        }

    };
    private static final AggregationFunction AGGREGATION_NEGATE = new AggregationFunction() {
        @Override
        public Number apply(Number val1, Number val2) {
            if (val1 instanceof Long && val2 instanceof Long) {
                return Long.valueOf(-val1.longValue());
            } else {
                return Double.valueOf(-val1.longValue());
            }
        }

    };

    public CoreLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, Bindings theGoalVars, SolutionListener theListener) {
        final Object result;
        final int arity = theGoalStruct.getArity();
        if (arity == 1) {
            final Object arg0 = theGoalStruct.getArg(0);
            if (theMethodName == "not") {
                result = not(theListener, theGoalVars, arg0);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 2) {
            final Object arg0 = theGoalStruct.getArg(0);
            final Object arg1 = theGoalStruct.getArg(1);
            if (theMethodName == "unify") {
                result = unify(theListener, theGoalVars, arg0, arg1);
            } else if (theMethodName == "expression_greater_equal_than") {
                result = expression_greater_equal_than(theListener, theGoalVars, arg0, arg1);
            } else if (theMethodName == "expression_equals") {
                result = expression_equals(theListener, theGoalVars, arg0, arg1);
            } else if (theMethodName == "is") {
                result = is(theListener, theGoalVars, arg0, arg1);
            } else if (theMethodName == "plus") {
                result = plus(theListener, theGoalVars, arg0, arg1);
            } else if (theMethodName == "minus") {
                result = minus(theListener, theGoalVars, arg0, arg1);
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
    public Continuation trueFunctor(SolutionListener theListener, Bindings theBindings) {
        return notifySolution(theListener);
    }

    @Primitive
    public Continuation fail(SolutionListener theListener, Bindings theBindings) {
        // Do not propagate a solution - that's all
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation var(SolutionListener theListener, Bindings theBindings, Object t1) {
        Continuation continuation = Continuation.CONTINUE;
        if (t1 instanceof Var) {
            final Var var = (Var) t1;
            if (var.isAnonymous()) {
                notifySolution(theListener);
            } else {
                final Binding binding = var.bindingWithin(theBindings).followLinks();
                if (!binding.isLiteral()) {
                    // Not ending on a literal, we end up on a free var!
                    continuation = notifySolution(theListener);
                }
            }
        }
        return continuation;
    }

    @Primitive
    public Continuation atomic(SolutionListener theListener, Bindings theBindings, Object theTerm) {
        final Bindings b = theBindings.focus(theTerm, Object.class);
        ensureBindingIsNotAFreeVar(b, "atomic/1");
        final Object effectiveTerm = b.getReferrer();
        if (effectiveTerm instanceof Struct || effectiveTerm instanceof Number) {
            return notifySolution(theListener);
        }
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation number(SolutionListener theListener, Bindings theBindings, Object theTerm) {
        final Bindings b = theBindings.focus(theTerm, Object.class);
        ensureBindingIsNotAFreeVar(b, "number/1");
        final Object effectiveTerm = b.getReferrer();
        if (effectiveTerm instanceof Number) {
            return notifySolution(theListener);
        }
        return Continuation.CONTINUE;
    }

    /**
     * Note: this implmentation is valid, yet may be bypassed by a "native" implemetatino in {@link DefaultSolver}.
     * 
     * @param theListener
     * @param theBindings
     * @return
     */
    @Primitive(name = Struct.FUNCTOR_CUT)
    public Continuation cut(SolutionListener theListener, Bindings theBindings) {
        // This is a complex behaviour - read on DefaultSolver
        // Cut is a "true" solution to a goal, just notify one as such
        notifySolution(theListener);
        return Continuation.CUT;
    }

    @Primitive(name = "=")
    public Continuation unify(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {
        final boolean unified = unify(t1, theBindings, t2, theBindings);
        return notifyIfUnified(unified, theListener);
    }

    @Primitive(name = "\\=")
    public Continuation notUnify(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {
        final boolean unified = unify(t1, theBindings, t2, theBindings);
        Continuation continuation = Continuation.CONTINUE;
        if (!unified) {
            continuation = notifySolution(theListener);
        }
        // TODO Why not "else"?
        if (unified) {
            deunify();
        }
        return continuation;
    }

    @Primitive
    public Continuation atom_length(SolutionListener theListener, Bindings theBindings, Object theAtom, Object theLength) {
        final Bindings atomBindings = theBindings.focus(theAtom, Object.class);
        ensureBindingIsNotAFreeVar(atomBindings, "atom_length/2");
        final Object atom = atomBindings.getReferrer();
        final String atomText = atom.toString();
        final Long atomLength = Long.valueOf(atomText.length());
        final boolean unified = unify(atomLength, atomBindings, theLength, theBindings);
        return notifyIfUnified(unified, theListener);
    }

    @Primitive(synonyms = "\\+")
    // Surprisingly enough the operator \+ means "not provable".
    public Continuation not(final SolutionListener theListener, Bindings theBindings, Object theGoal) {
        final Bindings subGoalBindings = theBindings.focus(theGoal, Object.class);
        ensureBindingIsNotAFreeVar(subGoalBindings, "\\+/1");

        // final Term target = resolveNonVar(theGoal, theBindings, "not");
        final class NegationListener implements SolutionListener {
            private boolean found = false;

            @Override
            public Continuation onSolution() {
                // Do NOT relay the solution further, just remember there was one
                this.found = true;
                return Continuation.USER_ABORT; // No need to seek for further solutions
            }
        }
        final NegationListener callListener = new NegationListener();
        // The following line seems to work OK but unsure if we can afford this I doubt
        // getProlog().getSolver().solveGoal(subGoalBindings, callListener);
        getProlog().getSolver().solveGoal(subGoalBindings, callListener);
        if (!callListener.found) {
            theListener.onSolution();
        }
        return Continuation.CONTINUE;
    }

    @Primitive
    public Continuation findall(SolutionListener theListener, final Bindings theBindings, final Object theTemplate, final Object theGoal, final Object theResult) {
        final Bindings subGoalBindings = theBindings.focus(theGoal, Object.class);
        ensureBindingIsNotAFreeVar(subGoalBindings, "findall/3");

        // Define a listener to collect all solutions for the goal specified
        final ArrayList<Object> javaResults = new ArrayList<Object>(); // Our internal collection of results
        final SolutionListener adHocListener = new SolutionListenerBase() {

            @Override
            public Continuation onSolution() {
                // Calculate the substituted goal value (resolve bindings)
                @SuppressWarnings("synthetic-access")
                // FIXME !!! This is most certainly wrong: how can we call substitute on a variable expressed in a different bindings?????
                // The case is : findall(X, Expr, Result) where Expr -> something -> expr(a,b,X,c)
                final Object substitute = TermApi.substitute(theTemplate, subGoalBindings, null);
                // Map<String, Term> explicitBindings = goalBindings.explicitBindings(FreeVarRepresentation.FREE);
                // And add as extra solution
                javaResults.add(substitute);
                return Continuation.CONTINUE;
            }

        };

        // Now solve the target goal, this may find several values of course
        getProlog().getSolver().solveGoal(subGoalBindings, adHocListener);

        // Convert all results into a prolog list structure
        // Note on var indexes: all variables present in the projection term will be
        // copied into the resulting plist, so there's no need to reindex.
        // However, the root level Struct that makes up the list does contain a bogus
        // index value but -1.
        final Struct plist = Struct.createPList(javaResults);

        // And unify with result
        final boolean unified = unify(theResult, theBindings, plist, theBindings);
        return notifyIfUnified(unified, theListener);
    }

    @Primitive
    public Continuation clause(SolutionListener theListener, Bindings theBindings, Object theHead, Object theBody) {
        final Binding dereferencedBinding = dereferencedBinding(theHead, theBindings);
        final Struct realHead = ReflectUtils.safeCastNotNull("dereferencing argumnent for clause/2", dereferencedBinding.getTerm(), Struct.class);
        for (final ClauseProvider cp : getProlog().getTheoryManager().getClauseProviders()) {
            for (final Clause clause : cp.listMatchingClauses(realHead, theBindings)) {
                // Clone the clause so that we can unify against its bindings
                final Clause clauseToUnify = new Clause(clause);
                final boolean headUnified = unify(clauseToUnify.getHead(), clauseToUnify.getBindings(), realHead, dereferencedBinding.getLiteralBindings());
                if (headUnified) {
                    try {
                        final boolean bodyUnified = unify(clauseToUnify.getBody(), clauseToUnify.getBindings(), theBody, theBindings);
                        if (bodyUnified) {
                            try {
                                notifySolution(theListener);
                            } finally {
                                deunify();
                            }
                        }
                    } finally {
                        deunify();
                    }
                }
            }
        }
        return Continuation.CONTINUE;
    }

    @Primitive(name = "=..")
    public Continuation predicate2PList(final SolutionListener theListener, Bindings theBindings, Object thePredicate, Object theList) {
        Bindings resolvedBindings = theBindings.focus(thePredicate, Object.class);
        Continuation continuation = Continuation.CONTINUE;

        if (resolvedBindings.isFreeReferrer()) {
            // thePredicate is still free, going ot match from theList
            resolvedBindings = theBindings.focus(theList, Term.class);
            ensureBindingIsNotAFreeVar(resolvedBindings, "=../2");
            final Struct lst2 = (Struct) resolvedBindings.getReferrer();
            final Struct flattened = lst2.predicateFromPList();
            final boolean unified = unify(thePredicate, theBindings, flattened, resolvedBindings);
            continuation = notifyIfUnified(unified, theListener);
        } else {
            final Object predResolved = resolvedBindings.getReferrer();
            if (predResolved instanceof Struct) {
                final Struct struct = (Struct) predResolved;
                final ArrayList<Object> elems = new ArrayList<Object>();
                elems.add(new Struct(struct.getName())); // Only copying the functor as an atom, not a deep copy of the struct!
                final int arity = struct.getArity();
                for (int i = 0; i < arity; i++) {
                    elems.add(struct.getArg(i));
                }
                final Struct plist = Struct.createPList(elems);
                final boolean unified = unify(theList, theBindings, plist, resolvedBindings);
                continuation = notifyIfUnified(unified, theListener);
            }
        }
        return continuation;
    }

    @Primitive
    public Continuation is(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {
        final Object evaluated = evaluate(t2, theBindings);
        if (evaluated == null) {
            return Continuation.CONTINUE;
        }
        final boolean unified = unify(t1, theBindings, evaluated, theBindings);
        return notifyIfUnified(unified, theListener);
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
     * @param theBindings
     * @param t1
     * @param t2
     * @param AGGREGRATION_TIMES
     * @return
     */
    private Continuation binaryComparisonPredicate(SolutionListener theListener, Bindings theBindings, Object t1, Object t2, ComparisonFunction theEvaluationFunction) {
        final Object effectiveT1 = evaluate(t1, theBindings);
        final Object effectiveT2 = evaluate(t2, theBindings);
        Continuation continuation = Continuation.CONTINUE;
        if (effectiveT1 instanceof Number && effectiveT2 instanceof Number) {
            final boolean condition = theEvaluationFunction.apply((Number) effectiveT1, (Number) effectiveT2);
            if (condition) {
                continuation = notifySolution(theListener);
            }
            return continuation;
        }
        return continuation;
    }

    @Primitive(name = ">")
    public Continuation expression_greater_than(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {

        return binaryComparisonPredicate(theListener, theBindings, t1, t2, COMPARE_GT);
    }

    @Primitive(name = "<")
    public Continuation expression_lower_than(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, theBindings, t1, t2, COMPARISON_LT);
    }

    @Primitive(name = ">=")
    public Continuation expression_greater_equal_than(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, theBindings, t1, t2, COMPARISON_GE);
    }

    @Primitive(name = "=<")
    public Continuation expression_lower_equal_than(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {
        return binaryComparisonPredicate(theListener, theBindings, t1, t2, COMPARISON_LE);
    }

    @Primitive(name = "=:=")
    public Continuation expression_equals(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {

        return binaryComparisonPredicate(theListener, theBindings, t1, t2, COMPARISON_EQ);
    }

    @Primitive(name = "=\\=")
    public Continuation expression_not_equals(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {

        return binaryComparisonPredicate(theListener, theBindings, t1, t2, COMPARISON_NE);
    }

    // ---------------------------------------------------------------------------
    // Functors
    // ---------------------------------------------------------------------------

    private static interface AggregationFunction {
        Number apply(Number val1, Number val2);
    }

    private Object binaryFunctor(SolutionListener theListener, Bindings theBindings, Object t1, Object t2, AggregationFunction theEvaluationFunction) {
        t1 = evaluate(t1, theBindings);
        t2 = evaluate(t2, theBindings);
        if (t1 instanceof Number && t2 instanceof Number) {
            if (t1 instanceof Long && t2 instanceof Long) {
                return Long.valueOf(theEvaluationFunction.apply((Number) t1, (Number) t2).longValue());
            } else {
                return Double.valueOf(theEvaluationFunction.apply((Number) t1, (Number) t2).doubleValue());
            }
        }
        throw new InvalidTermException("Could not add because 2 terms are not Numbers: " + t1 + " and " + t2);
    }

    @Primitive(name = "+")
    public Object plus(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {

        return binaryFunctor(theListener, theBindings, t1, t2, AGGREGATION_PLUS);
    }

    /**
     * @param theListener
     * @param theBindings
     * @param t1
     * @param t2
     * @return Binary minus (subtract)
     */
    @Primitive(name = "-")
    public Object minus(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {
        return binaryFunctor(theListener, theBindings, t1, t2, AGGREGATION_MINUS);

    }

    /**
     * @param theListener
     * @param t1
     * @param t2
     * @return Binary multiply
     */
    @Primitive(name = "*")
    public Object multiply(SolutionListener theListener, Bindings theBindings, Object t1, Object t2) {
        return binaryFunctor(theListener, theBindings, t1, t2, AGGREGRATION_TIMES);

    }

    /**
     * @param theListener
     * @param theBindings
     * @param t1
     * @return Unary minus (negate)
     */
    @Primitive(name = "-")
    public Object minus(SolutionListener theListener, Bindings theBindings, Object t1) {
        return binaryFunctor(theListener, theBindings, t1, 0L, AGGREGATION_NEGATE);

    }
}
