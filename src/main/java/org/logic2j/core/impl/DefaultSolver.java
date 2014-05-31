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

import org.logic2j.core.api.*;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.DataFact;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.api.monadic.StateEngineByLookup;
import org.logic2j.core.impl.util.ProfilingInfo;
import org.logic2j.core.impl.util.ReportUtils;
import org.logic2j.core.library.mgmt.PrimitiveInfo;

/**
 * Solve goals - that's the core of the engine.
 */
public class DefaultSolver implements Solver {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultSolver.class);

    static final boolean isDebug = logger.isDebugEnabled();

    protected final PrologImplementation prolog;

    private boolean hasDataFactProviders;

    public DefaultSolver(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    @Override
    public Continuation solveGoal(Object goal, SolutionListener theSolutionListener) {
        final PoV initialPoV = initalPoV();
        if (goal instanceof Struct) {
            // We will need to clone Clauses during resolution, hence the base index
            // for any new var must be higher than any of the currently used vars.
            initialPoV.topVarIndex += ((Struct)goal).getIndex();
        }
        return solveGoal(goal, initialPoV, theSolutionListener);
    }

    /**
     * Just calls the recursive {@link #solveGoalRecursive(Object, org.logic2j.core.api.monadic.PoV, org.logic2j.core.api.SolutionListener)} method. The goal to solve
     * is in the theGoalBindings's referrer.
     */
    @Override
    public Continuation solveGoal(Object goal, PoV pov, final SolutionListener theSolutionListener) {
        // Check if we will have to deal with DataFacts in this session of solving.
        // This slightly improves performance - we can bypass calling the method that deals with that

        return solveGoalRecursive(goal, pov, theSolutionListener);
    }

    @Override
    public PoV initalPoV() {
        final PoV initialPoV = new StateEngineByLookup().emptyPoV();
        return initialPoV;
    }

    public Continuation solveGoalRecursive(final Object goalTerm, final PoV reifier, final SolutionListener theSolutionListener) {
        if (isDebug) {
            logger.debug(">> Entering solveRecursive(\"{}\")", goalTerm);
        }
        if (PrologReferenceImplementation.PROFILING) {
            ProfilingInfo.nbInferences++;
        }
        Continuation result = Continuation.CONTINUE;

        // At the moment we don't properly manage atoms as goals...
        final Struct goalStruct;
        if (goalTerm instanceof String) {
            // Yet we are not capable of handing String below everywhere - so use a Struct still
            goalStruct = new Struct((String) goalTerm);
        } else {
            goalStruct = (Struct) goalTerm;
        }

        // Extract all features of the goal to solve
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
            if (isDebug) {
                logger.debug("Handling AND, arity={}", arity);
            }
            final Object lhs = goalStruct.getArg(0);
            for (int i = 0; i < arity - 1; i++) {
                final int index = i;
                listeners[index] = new SolutionListener() {

                    @Override
                    public Continuation onSolution(PoV theReifier) {
                        if (isDebug) {
                            logger.debug("AND's internal solution listener called for {}", lhs);
                        }
                        final int nextIndex = index + 1;
                        final Object rhs = goalStruct.getArg(nextIndex); // Usually the right-hand-side of a binary ','
                        final Continuation continuationFromSubGoal = solveGoalRecursive(rhs, theReifier, listeners[nextIndex]);
                        return continuationFromSubGoal;
                    }
                };
            }
            // Solve the first goal, redirecting all solutions to the first listener defined above
            result = solveGoalRecursive(lhs, reifier, listeners[0]);
        } else if (Struct.FUNCTOR_SEMICOLON == functor) { // Names are {@link String#intern()}alized so OK to check by reference
            if (isDebug) {
                logger.debug("Handling OR, arity={}", arity);
            }
            /*
            * This is the Java implementation of N-arity OR
            * We can also implement a binary OR directly in Prolog using
            * A ; B :- call(A).
            * A ; B :- call(B).
            * but the simplicity of the code below and its efficiency are preferred.
            */
            for (int i = 0; i < arity; i++) {
                // Solve all the left and right-and-sides, sequentially
                result = solveGoalRecursive(goalStruct.getArg(i), reifier, theSolutionListener);
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
            final Object argumentOfCall = goalStruct.getArg(0);
            final Object argumentOfCall2 = reifier.reify(argumentOfCall);
            result = solveGoalRecursive(argumentOfCall2, reifier, theSolutionListener);

        } else if (Struct.FUNCTOR_CUT == functor) {
            // This is a "native" implementation of CUT, which works as good as using the primitive in CoreLibrary
            // Doing it inline might improve performance a little although I did not measure much
            // Functionally, this code may be removed

            // Cut IS a valid solution in itself. We just ignore what the app tells us to do next.
            theSolutionListener.onSolution(reifier);
            // Stopping there for this iteration
            result = Continuation.CUT;
        } else if (prim != null) {
            // ---------------------------------------------------------------------------
            // Primitive implemented in Java
            // ---------------------------------------------------------------------------

            final Object resultOfPrimitive = prim.invoke(goalStruct, reifier, theSolutionListener);
            // Extract necessary objects from our current state

            switch (prim.getType()) {
                case PREDICATE:
                    result = (Continuation) resultOfPrimitive;
                    break;
                case FUNCTOR:
                    if (isDebug) {
                        logger.debug("Result of Functor {}: {}", goalStruct, resultOfPrimitive);
                    }
                    logger.error("We should not pass here with functors!? Directive {} ignored", goalStruct);
                    break;
                case DIRECTIVE:
                    logger.warn("Result of Directive {} not yet used", goalStruct);
                    break;
            }

        } else {
            result = solveAgainstClauseProviders(goalTerm, reifier, theSolutionListener);

        }
        if (isDebug) {
            logger.debug("<< Exit    solveGoalRecursive(\"{}\"), continuation={}", goalTerm, result);
        }
        return result;
    }


    private Continuation solveAgainstClauseProviders(final Object goalTerm, PoV reifier, final SolutionListener theSolutionListener) {
        // Simple "user-defined" goal to demonstrate - find matching goals in the theories loaded


        Continuation result = Continuation.CONTINUE;
        // Now ready to iteratively try clause by clause, by first attempting to unify with its headTerm
        final Object[] clauseHeadAndBody = new Object[2];
        final Iterable<ClauseProvider> providers = this.prolog.getTheoryManager().getClauseProviders();
        for (final ClauseProvider provider : providers) {
            final Iterable<Clause> matchingClauses = provider.listMatchingClauses(goalTerm, reifier);
            if (matchingClauses == null) {
                continue;
            }
            // logger.info("matchingClauses: {}", ((List<?>) matchingClauses).size());
            for (final Clause clause : matchingClauses) {
                if (result == Continuation.CUT) {
                    if (isDebug) {
                        logger.debug("Current status is {}: stop finding more clauses", result);
                    }
                    break;
                }
                if (result == Continuation.USER_ABORT) {
                    if (isDebug) {
                        logger.debug("Current status is {}: abort finding more clauses", result);
                    }
                    break;
                }
                if (isDebug) {
                    logger.debug("Trying clause {}, current status={}", clause, result);
                }
//                final Struct clonedClause;
//                final Object clauseHead;
//                if (clause.needCloning()) {
//                    // Clone the variables so that we won't mutate our current clause's ones
//                    // clonedClause = (Struct) reifier.cloneClauseAndRemapIndexes(clause);
//                    clonedClause = (Struct)clause.cloned(reifier).getContent();
//                    final boolean isRule = Struct.FUNCTOR_CLAUSE == clonedClause.getName() && clonedClause.getArity()==2;
//                    clauseHead = isRule ? clonedClause.getArg(0) : clonedClause;
//
//                } else {
//                    clonedClause = clause.getContent();
//                }
                // BOGUS!!!
                // final boolean isFact = clause.isFact();
                // final Object clauseHead = isFact ? clonedClause : clonedClause.getArg(0);

                clause.headAndBodyForSubgoal(reifier, clauseHeadAndBody);
                final Object clauseHead = clauseHeadAndBody[0];

                if (isDebug) {
                    logger.debug(" Unifying goal  : {}", goalTerm);
                    logger.debug("  to clause head: {}", clauseHead);
                }

                final PoV reifier2 = reifier.unify(goalTerm, clauseHead);
                boolean headUnified = reifier2 != null;
                if (isDebug) {
                    logger.debug(" headUnified={}", headUnified);
                }

                if (headUnified) {
                    final Continuation continuation;
                    final Object clauseBody = clauseHeadAndBody[1];
                    final boolean isFact = clauseBody == null;
                    if (isFact) {
                        if (isDebug) {
                            logger.debug("{} is a fact, callback one solution", clauseHead);
                        }
                        // Notify one solution, and handle result if user wants to continue or not.
                        continuation = theSolutionListener.onSolution(reifier2);
                        if (continuation == Continuation.CUT || continuation == Continuation.USER_ABORT) {
                            result = continuation;
                        }
                    } else {
                        // Not a fact, it's a theorem - it has a body
                        final Object newGoalTerm = clauseBody;
                        if (isDebug) {
                            logger.debug("Clause {} is a theorem whose body is {}", clauseHead, newGoalTerm);
                        }
                        // Solve the body in our current recursion context
                        continuation = solveGoalRecursive(newGoalTerm, reifier2, theSolutionListener);
                        if (isDebug) {
                            logger.debug("  back to clause {} with continuation={}", clause, continuation);
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

                }
            }
            if (isDebug) {
                logger.debug("Last Clause of {} iterated", provider);
            }
        }
        if (isDebug) {
            logger.debug("Last ClauseProvider iterated");
        }

        return result;
    }

//    private Continuation solveAgainstDataProviders(final Object goalTerm, final TermBindings theGoalBindings, final SolutionListener theSolutionListener) {
//        final Unifier unifier = this.prolog.getUnifier();
//        Continuation result = Continuation.CONTINUE;
//        // Now fetch data
//        final Iterable<DataFactProvider> dataProviders = this.prolog.getTheoryManager().getDataFactProviders();
//        for (final DataFactProvider dataProvider : dataProviders) {
//            final Iterable<DataFact> matchingDataFacts = dataProvider.listMatchingDataFacts(goalTerm);
//            for (final DataFact dataFact : matchingDataFacts) {
//                // We should probably try/finally between unification and deunification. However since we unify with data
//                // and need efficiency, and we won't call any user code, we can assume not to.
//                final boolean unifiedWithData = unifier.unify(goalTerm, theGoalBindings, dataFact);
//                if (unifiedWithData) {
//                    final Continuation continuation = theSolutionListener.onSolution();
//                    if (continuation == Continuation.CUT || continuation == Continuation.USER_ABORT) {
//                        result = continuation;
//                    }
//                    unifier.deunify();
//                }
//            }
//            if (logger.isInfoEnabled()) {
//                logger.info("Last DataFact of {} iterated", dataProvider);
//            }
//        }
//        return result;
//    }

    @Override
    public String toString() {
        return ReportUtils.shortDescription(this);
    }

}
