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
package org.logic2j.core.solver;

import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.mgmt.PrimitiveInfo;
import org.logic2j.core.model.Clause;
import org.logic2j.core.model.exception.InvalidTermException;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.listener.Continuation;
import org.logic2j.core.solver.listener.SolutionListener;
import org.logic2j.core.theory.ClauseProvider;
import org.logic2j.core.util.ReportUtils;

/**
 * Solve goals - that's the core of the engine.
 */
public class DefaultSolver implements Solver {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultSolver.class);
    private static final boolean debug = logger.isDebugEnabled();

    public int internalCounter = 0;
    private final PrologImplementation prolog;

    public DefaultSolver(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    /**
     * Just calls the recursive {@link #solveGoalRecursive(Term, Bindings, GoalFrame, SolutionListener)} method. The referrer goal to solve
     * is in the callerFrame
     * 
     * @param theSolutionListener
     * @param theGoalBindings
     */
    @Override
    public Continuation solveGoal(final Bindings theGoalBindings, final SolutionListener theSolutionListener) {
        return solveGoalRecursive(theGoalBindings.getReferrer(), theGoalBindings, new GoalFrame(), theSolutionListener);
    }

    // TODO This method only used once - possibly not needed, check if specifying the GoalFrame is needed
    @Override
    public Continuation solveGoal(final Bindings theGoalBindings, final GoalFrame callerFrame, final SolutionListener theSolutionListener) {
        return solveGoalRecursive(theGoalBindings.getReferrer(), theGoalBindings, callerFrame, theSolutionListener);
    }

    private Continuation solveGoalRecursive(final Term goalTerm, final Bindings theGoalBindings, final GoalFrame callerFrame, final SolutionListener theSolutionListener) {
        if (debug) {
            logger.debug("Entering solveRecursive({}), callerFrame={}", goalTerm, callerFrame);
        }
        if (!(goalTerm instanceof Struct)) {
            throw new InvalidTermException("Goal \"" + goalTerm + "\" is not a Struct and cannot be solved");
        }
        // Extract all features of the goal to solve
        final Struct goalStruct = (Struct) goalTerm;
        final PrimitiveInfo prim = goalStruct.getPrimitiveInfo();
        final String functor = goalStruct.getName();
        final int arity = goalStruct.getArity();

        // Check if goal is a system predicate or a simple one to match against the theory
        if (Struct.FUNCTOR_COMMA == functor) { // Names are {@link String#intern()}alized so OK to check by reference
            // Logical AND
            final SolutionListener[] listeners = new SolutionListener[arity];
            // The last listener is the one of this overall COMMA sequence
            listeners[arity - 1] = theSolutionListener;
            // Allocates N-1 listeners. On solution, each will trigger solving of the next term
            for (int i = 0; i < arity - 1; i++) {
                final int index = i;
                listeners[index] = new SolutionListener() {

                    @Override
                    public Continuation onSolution() {
                        DefaultSolver.this.internalCounter++;
                        final int nextIndex = index + 1;
                        solveGoalRecursive(goalStruct.getArg(nextIndex), theGoalBindings, callerFrame, listeners[nextIndex]);
                        return Continuation.CONTINUE;
                    }
                };
            }
            // Solve the first goal, redirecting all solutions to the first listener defined above
            solveGoalRecursive(goalStruct.getArg(0), theGoalBindings, callerFrame, listeners[0]);
        } else if (Struct.FUNCTOR_SEMICOLON == functor) { // Names are {@link String#intern()}alized so OK to check by reference
            // Logical OR
            for (int i = 0; i < arity; i++) {
                // Solve all the left and right-and-sides, sequentially
                solveGoalRecursive(goalStruct.getArg(i), theGoalBindings, callerFrame, theSolutionListener);
            }
        } else if (Struct.FUNCTOR_CALL == functor) { // Names are {@link String#intern()}alized so OK to check by reference
            // TODO: call/1 is handled here for efficiency, see if it's really needed we could as well use the Primitive (already
            // implemented)
            if (arity != 1) {
                throw new InvalidTermException("Primitive 'call' accepts only one argument, got " + arity);
            }
            final Bindings effectiveGoalBindings = theGoalBindings.focus(goalStruct.getArg(0), Term.class);
            if (effectiveGoalBindings == null) {
                throw new InvalidTermException("Argument to primitive 'call' may not be a free variable, was " + goalStruct.getArg(0));
            }
            final Term target = effectiveGoalBindings.getReferrer();
            if (debug) {
                logger.debug("Calling FUNCTOR_CALL ------------------ {}", target);
            }
            solveGoalRecursive(target, effectiveGoalBindings, callerFrame, theSolutionListener);
        } else if (prim != null) {
            // Primitive implemented in Java
            final Object resultOfPrimitive = prim.invoke(goalStruct, theGoalBindings, callerFrame, theSolutionListener);
            // Extract necessary objects from our current state

            switch (prim.getType()) {
            case PREDICATE:
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
            final GoalFrame subFrameForClauses = new GoalFrame(callerFrame);

            final Iterable<ClauseProvider> providers = this.prolog.getTheoryManager().getClauseProviderResolver().providersFor(goalStruct);
            for (final ClauseProvider provider : providers) {
                for (final Clause clause : provider.listMatchingClauses(goalStruct, theGoalBindings)) {

                    if (debug) {
                        logger.debug("Trying clause {}", clause);
                    }
                    // Handle user cancellation at beginning of loop, not at the end.
                    // This is in case user code returns onSolution()=false (do not continue)
                    // on what happens to be the last normal solution - in this case we can't tell if
                    // we are exiting because user requested it, or because there's no other solution!
                    if (subFrameForClauses.isUserCanceled()) {
                        if (debug) {
                            logger.debug("!!! Stopping on SolutionListener's request");
                        }
                        break;
                    }
                    if (subFrameForClauses.isCut()) {
                        if (debug) {
                            logger.debug("!!! cut found in clause");
                        }
                        break;
                    }
                    if (subFrameForClauses.hasCutInSiblingSubsequentGoal()) {
                        if (debug) {
                            logger.debug("!!! Stopping because of cut in sibling subsequent goal");
                        }
                        break;
                    }
                    // Clone the variables so that we won't mutate our current clause's ones
                    final Bindings immutableVars = clause.getBindings();
                    final Bindings clauseVars = new Bindings(immutableVars);
                    final Term clauseHead = clause.getHead();
                    if (debug) {
                        logger.debug("Unifying: goal={}        with  goalVars={}", goalTerm, theGoalBindings);
                        logger.debug("      to: clauseHead={}  with  clauseVars={}", clauseHead, clauseVars);
                    }

                    // Now unify - this is the only place where free variables may become bound, and
                    // the trailFrame will remember this.
                    // Solutions will be notified from within this method.
                    // As a consequence, deunification can happen immediately afterwards, in this method, not outside in the caller
                    final boolean headUnified = this.prolog.getUnifier().unify(goalTerm, theGoalBindings, clauseHead, clauseVars, subFrameForClauses);
                    if (debug) {
                        logger.debug("  result=" + headUnified + ", goalVars={}, clauseVars={}", theGoalBindings, clauseVars);
                    }

                    if (headUnified) {
                        try {
                            if (clause.isFact()) {
                                if (debug) {
                                    logger.debug("{} is a fact", clauseHead);
                                }
                                // Notify one solution, and handle result if user wants to continue or not.
                                final Continuation continuation = theSolutionListener.onSolution();
                                if (continuation.isUserAbort()) {
                                    subFrameForClauses.raiseUserCanceled();
                                }
                            } else {
                                // Not a fact, it's a theorem - it has a body
                                final Term newGoalTerm = clause.getBody();
                                if (debug) {
                                    logger.debug(">> RECURS: {} is a theorem, body={}", clauseHead, newGoalTerm);
                                }
                                // Solve the body in our current subFrame
                                solveGoalRecursive(newGoalTerm, clauseVars, subFrameForClauses, theSolutionListener);
                                if (debug) {
                                    logger.debug("<< RECURS");
                                }
                            }
                        } finally {
                            // We have now fired our solution(s), we no longer need our bound bindings and can deunify
                            // Go to next solution: start by clearing our trailing bindings
                            this.prolog.getUnifier().deunify(subFrameForClauses);
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
            logger.debug("Leaving solveGoalRecursive({})", goalTerm);
        }
        return Continuation.CONTINUE;
    }

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }

}
