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
package org.logic2j.core.impl;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.Solver;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.util.ReportUtils;
import org.logic2j.core.library.mgmt.PrimitiveInfo;

/**
 * Solve goals - that's the core of the engine.
 */
public class DefaultSolver implements Solver {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultSolver.class);
    private static final boolean debug = logger.isDebugEnabled();

    private final PrologImplementation prolog;

    public DefaultSolver(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    /**
     * Just calls the recursive {@link #solveGoalRecursive(Term, Bindings, SolutionListener)} method. The referrer goal to solve
     * is in the callerFrame
     * 
     * @param theSolutionListener
     * @param theGoalBindings
     */
    @Override
    public Continuation solveGoal(final Bindings theGoalBindings, final SolutionListener theSolutionListener) {
        return solveGoalRecursive(theGoalBindings.getReferrer(), theGoalBindings, theSolutionListener);
    }

    private Continuation solveGoalRecursive(final Term goalTerm, final Bindings theGoalBindings, final SolutionListener theSolutionListener) {
        if (debug) {
            logger.debug(">> Entering solveRecursive(\"{}\")", goalTerm);
        }
        if (!(goalTerm instanceof Struct)) {
            throw new InvalidTermException("Goal \"" + goalTerm + "\" is not a Struct and cannot be solved");
        }
        Continuation result = Continuation.CONTINUE;

        // Extract all features of the goal to solve
        final Struct goalStruct = (Struct) goalTerm;
        final PrimitiveInfo prim = goalStruct.getPrimitiveInfo();
        final String functor = goalStruct.getName();
        final int arity = goalStruct.getArity();

        // Check if goal is a system predicate or a simple one to match against the theory
        if (Struct.FUNCTOR_COMMA == functor) { // Names are {@link String#intern()}alized so OK to check by reference
            // Logical AND. Typically the arity=2 since "," is a binary predicate. But in logic2j we allow more.
            final SolutionListener[] listeners = new SolutionListener[arity];
            // The last listener is the one that called us (usually callbacks into the application)
            listeners[arity - 1] = theSolutionListener;
            // Allocates N-1 listeners, usually this means one.
            // On solution, each will trigger solving of the next term
            if (debug) {
                logger.debug("Handing AND, arity={}", arity);
            }
            for (int i = 0; i < arity - 1; i++) {
                final int index = i;
                listeners[index] = new SolutionListener() {

                    @Override
                    public Continuation onSolution() {
                        final int nextIndex = index + 1;
                        final Term rhs = goalStruct.getArg(nextIndex); // Usually the right-hand-side of a binary ','
                        final Continuation continuationFromSubGoal = solveGoalRecursive(rhs, theGoalBindings, listeners[nextIndex]);
                        return continuationFromSubGoal;
                    }
                };
            }
            // Solve the first goal, redirecting all solutions to the first listener defined above
            final Term lhs = goalStruct.getArg(0);
            result = solveGoalRecursive(lhs, theGoalBindings, listeners[0]);
        } else if (Struct.FUNCTOR_SEMICOLON == functor) { // Names are {@link String#intern()}alized so OK to check by reference

            /*
            * This is the Java implementation of N-arity OR
            * We can also implement a binary OR directly in Prolog using
            * A ; B :- call(A).
            * A ; B :- call(B).
            * but the simplicity of the code below and its efficiency are preferred.
            */
            for (int i = 0; i < arity; i++) {
                // Solve all the left and right-and-sides, sequentially
                // TODO what do we do with the "Continuation" result of the method?
                result = solveGoalRecursive(goalStruct.getArg(i), theGoalBindings, theSolutionListener);
                if (result == Continuation.CUT) {
                    break;
                }
            }
        } else if (Struct.FUNCTOR_CALL == functor) { // Names are {@link String#intern()}alized so OK to check by reference
            // TODO call/1 is handled here for efficiency, see if it's really needed we could as well use the Primitive (already
            // implemented)
            if (arity != 1) {
                throw new InvalidTermException("Primitive 'call' accepts only one argument, got " + arity);
            }
            final Term argumentOfCall = goalStruct.getArg(0);
            final Bindings effectiveGoalBindings = theGoalBindings.focus(argumentOfCall, Term.class);
            if (effectiveGoalBindings == null) {
                throw new InvalidTermException("Argument to primitive 'call' may not be a free variable, was " + goalStruct.getArg(0));
            }
            final Term target = effectiveGoalBindings.getReferrer();
            if (debug) {
                logger.debug("Invoking call({})", target);
            }
            result = solveGoalRecursive(target, effectiveGoalBindings, theSolutionListener);
        } else if (prim != null) {
            // ---------------------------------------------------------------------------
            // Primitive implemented in Java
            // ---------------------------------------------------------------------------

            final Object resultOfPrimitive = prim.invoke(goalStruct, theGoalBindings, theSolutionListener);
            // Extract necessary objects from our current state

            switch (prim.getType()) {
            case PREDICATE:
                result = (Continuation) resultOfPrimitive;
                break;
            case FUNCTOR:
                if (debug) {
                    logger.debug("Result of Functor {}: {}", goalStruct, resultOfPrimitive);
                }
                logger.error("We should not pass here with functors!? Directive {} ignored", goalStruct);
                break;
            case DIRECTIVE:
                logger.warn("Result of Directive {} not yet used", goalStruct);
                break;
            }

        } else {
            // Simple "user-defined" goal to demonstrate - find matching goals in the theories loaded

            // Now ready to iteratively try clause by clause, by first attempting to unify with its headTerm

            final Iterable<ClauseProvider> providers = this.prolog.getTheoryManager().getClauseProviderResolver().providersFor(goalStruct);
            for (final ClauseProvider provider : providers) {
                final Iterable<Clause> matchingClauses = provider.listMatchingClauses(goalStruct, theGoalBindings);
                if (matchingClauses == null) {
                    continue;
                }
                for (final Clause clause : matchingClauses) {
                    if (result == Continuation.CUT) {
                        if (debug) {
                            logger.debug("Current status is {}: stop finding more clauses", result);
                        }
                        break;
                    }
                    if (result == Continuation.USER_ABORT) {
                        if (debug) {
                            logger.debug("Current status is {}: abort finding more clauses", result);
                        }
                        break;
                    }
                    if (debug) {
                        logger.debug("Trying clause \"{}\", current status={}", clause, result);
                    }

                    // Clone the variables so that we won't mutate our current clause's ones
                    final Bindings immutableVars = clause.getBindings();

                    // TODO apparently we cannot afford a shallow copy?
                    // final Bindings clauseVars = Bindings.shallowCopyWithSameReferrer(immutableVars);
                    final Bindings clauseVars = Bindings.deepCopyWithSameReferrer(immutableVars);

                    final Term clauseHead = clause.getHead();
                    if (debug) {
                        logger.debug("Unifying: goal={}        with  goalVars={}", goalTerm, theGoalBindings);
                        logger.debug("      to: clauseHead={}  with  clauseVars={}", clauseHead, clauseVars);
                    }

                    // Now unify - this is the only place where free variables may become bound, and
                    // the trailFrame will remember this.
                    // Solutions will be notified from within this method.
                    // As a consequence, deunification can happen immediately afterwards, in this method, not outside in the caller
                    final boolean headUnified = this.prolog.getUnifier().unify(goalTerm, theGoalBindings, clauseHead, clauseVars);
                    if (debug) {
                        logger.debug("  result=" + headUnified + ", goalVars={}, clauseVars={}", theGoalBindings, clauseVars);
                    }

                    if (headUnified) {
                        try {
                            final Continuation continuation;
                            if (clause.isFact()) {
                                if (debug) {
                                    logger.debug("{} is a fact, callback one solution", clauseHead);
                                }
                                // Notify one solution, and handle result if user wants to continue or not.
                                continuation = theSolutionListener.onSolution();
                                if (continuation == Continuation.CUT) {
                                    result = Continuation.CUT;
                                } else if (continuation == Continuation.USER_ABORT) {
                                    // TODO should we just "return" from here?
                                    result = Continuation.USER_ABORT;
                                }
                            } else {
                                // Not a fact, it's a theorem - it has a body
                                final Term newGoalTerm = clause.getBody();
                                if (debug) {
                                    logger.debug("Clause {} is a theorem, clause's body is \"{}\"", clauseHead, newGoalTerm);
                                }
                                // Solve the body in our current subFrame
                                continuation = solveGoalRecursive(newGoalTerm, clauseVars, theSolutionListener);
                                if (debug) {
                                    logger.debug("  back to clause \"{}\" with continuation={}", clause, continuation);
                                }
                                if (continuation == Continuation.USER_ABORT) {
                                    // TODO should we just "return" from here?
                                    result = Continuation.USER_ABORT;
                                }

                                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                                // TODO There is something really ugly here but I'm in the middle of a big refactoring and
                                // did not find anything better yet.
                                // When we solve a goal such as "a,b,c,!,d", the cut must ascend up to the first ",", so that further
                                // attempt to use clauses for a() are also cut.
                                // However, when we solve main :- sub., then if sub has a cut inside, the cut must not ascend back to the
                                // main.
                                // we deal with that here.
                                // Any other solution (much) welcome.
                                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                                if (newGoalTerm instanceof Struct) {
                                    final String bodyFunctor = ((Struct) newGoalTerm).getName();
                                    if (bodyFunctor == Struct.FUNCTOR_CUT || bodyFunctor == Struct.FUNCTOR_COMMA) {
                                        if (continuation == Continuation.CUT) {
                                            result = Continuation.CUT;
                                        }
                                    }
                                }
                            }

                        } finally {
                            // We have now fired our solution(s), we no longer need our bound bindings and can deunify
                            // Go to next solution: start by clearing our trailing bindings
                            this.prolog.getUnifier().deunify();
                        }
                    }
                }
                if (debug) {
                    logger.debug("Last Clause of {} iterated", provider);
                }
            }
            if (debug) {
                logger.debug("Last ClauseProvider iterated");
            }
        }
        if (debug) {
            logger.debug("<< Exit    solveGoalRecursive(\"{}\") with {}", goalTerm, result);
        }
        return result;
    }

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }

}
