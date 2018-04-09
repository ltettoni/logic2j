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
package org.logic2j.core.impl;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.DataFactProvider;
import org.logic2j.core.api.library.PrimitiveInfo;
import org.logic2j.core.api.model.Clause;
import org.logic2j.engine.model.DataFact;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.unify.UnifyContext;
import org.logic2j.engine.util.ProfilingInfo;

/**
 * Solve goals with extension to {@link DataFact}s and {@link Clause}s.
 */
public class Solver extends org.logic2j.engine.solver.Solver {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Solver.class);

  private static final boolean isDebug = logger.isDebugEnabled();

  private final PrologImplementation prolog;

  public Solver(PrologImplementation theProlog) {
    this.prolog = theProlog;
  }


  /**
   * Do we solve the ";" (OR) predicate internally here, or in the predicate.
   * (see note re. processing of OR in CoreLibrary.pro)
   */
  protected final boolean isInternalOr() { return false; } // FIXME Bug with true on sign5neg test case


  @Override
  protected boolean isJava(Struct goalStruct) {
    return goalStruct.getContent() != null;
  }


  @Override
  protected int invokeJava(Struct goalStruct, UnifyContext currentVars) {
    final PrimitiveInfo prim = ((Struct<PrimitiveInfo>)goalStruct).getContent();

    final Object resultOfPrimitive = prim.invoke(goalStruct, currentVars);
    // Extract necessary objects from our current state

    int result = Continuation.CONTINUE;
    switch (prim.getType()) {
      case PREDICATE:
        // The result will be the continuation code or CUT level
        final int primitiveContinuation = (Integer) resultOfPrimitive;
        result = primitiveContinuation;
        break;
      case FUNCTOR:
        // FIXME Why this code since we never pass here!
        if (isDebug) {
          logger.debug("Result of Functor {}: {}", goalStruct, resultOfPrimitive);
        }
        // logger.error("We should not pass here with functors!? Directive {} ignored", goalStruct);
        assert false : "We should not pass here with functors!? Directive " + goalStruct + " ignored";
        break;
      case DIRECTIVE:
        // FIXME Why this code since we never pass here!
        logger.warn("Result of Directive {} not yet used", goalStruct);
        break;
    }
    return result;
  }


  protected int solveAgainstClauseProviders(final Object goalTerm, UnifyContext currentVars, final int cutLevel) {
    // Simple "user-defined" goal to demonstrate - find matching goals in the theories loaded
    final long inferenceCounter = ProfilingInfo.nbInferences;
    if (isDebug) {
      logger.debug(" +>> Entering solveAgainstClauseProviders#{}, cutLevel={}", inferenceCounter, cutLevel);
    }
    int result = Continuation.CONTINUE;

    // Now ready to iteratively try clause by clause, by first attempting to unify with its headTerm
    final Object[] clauseHeadAndBody = new Object[2];
    final Iterable<ClauseProvider> providers = this.prolog.getTheoryManager().getClauseProviders();
    // Iterate on providers
    loopOnProviders:
    // Specifying a label because of two nested "for" loops - we need to break from the inner one
    for (final ClauseProvider provider : providers) {
      final Iterable<Clause> matchingClauses = provider.listMatchingClauses(goalTerm, currentVars);
      if (matchingClauses == null) {
        continue;
      }
      // Within one provider, iterate on potentially-matching clauses
      for (final Clause clause : matchingClauses) {
        if (isDebug) {
          logger.debug(" Attempting first/next clause: {}", clause);
        }

        clause.headAndBodyForSubgoal(currentVars, clauseHeadAndBody);
        final Object clauseHead = clauseHeadAndBody[0];
                /*
                if (isDebug) {
                    logger.debug("  Unifying goal  : {}", goalTerm);
                    logger.debug("   to clause head: {}", clauseHead);
                }
                */
        final UnifyContext contextAfterHeadUnified = currentVars.unify(goalTerm, clauseHead);
        final boolean headUnified = contextAfterHeadUnified != null;

        if (headUnified) {
          final Object clauseBody = clauseHeadAndBody[1];
          final boolean isFact = clauseBody == null;
          if (isFact) {
            if (isDebug) {
              logger.debug(" Head unified. {} is a fact: notifying one solution", clauseHead);
            }
            // Notify one solution, and handle result if user wants to continue or not.
            final int continuation = currentVars.getSolutionListener().onSolution(contextAfterHeadUnified);
            result = continuation;
          } else {
            // Not a fact, it's a theorem - it has a body - the body becomes our new goal
            if (isDebug) {
              logger.debug(" Head unified. Clause with head = {} is a theorem, solving body = {}", clauseHead, clauseBody);
            }
            // Solve the body in our current recursion context
            final int theoremResult = solveGoalRecursive(clauseBody, contextAfterHeadUnified, cutLevel);
            if (isDebug) {
              logger.debug("  back to having solved theorem's body = {} with theoremResult={}", clauseBody, theoremResult);
            }
            result = theoremResult;
          } // else - was a theorem

          // If not asking for a regular "CONTINUE", handle result from notification of a fact, or solution to a theorem
          if (result != Continuation.CONTINUE) {
            final int intResult = result;
            if (intResult < 0) {
              // User abort
              if (isDebug) {
                logger.debug(" Iteration on clauses detected USER_ABORT - aborting search for clauses");
              }
              break loopOnProviders;
            }
            if (intResult > 0) {
              // Cut somewhere down the processing, or returned from notified solution
              if (isDebug) {
                logger.debug("Got a CUT of resultLevel={}, at currentLevel={}", result, cutLevel);
              }
              if (intResult <= cutLevel) {
                if (isDebug) {
                  logger.debug("Cutting solve#{} for {}", inferenceCounter, goalTerm);
                }
                if (intResult == cutLevel) {
                  if (isDebug) {
                    logger.debug("Reached parent predicate with CUT, stop escalating CUT, continue instead");
                  }
                  result = Continuation.CONTINUE;
                }
                break loopOnProviders;
              }
            }
          }

        } else {
          if (isDebug) {
            logger.debug(" Head not unified - skipping to next clause");
          }
        }
      }
      if (isDebug) {
        logger.debug("Last Clause of \"{}\" iterated", provider);
      }
    }
    if (isDebug) {
      logger
          .debug(" +<< Exiting  solveAgainstClauseProviders#{}: last ClauseProvider iterated for: {}, result=" + result, inferenceCounter, goalTerm);
    }
    return result;
  }

  protected int solveAgainstDataProviders(final Object goalTerm, final UnifyContext currentVars, final int cutLevel) {
    final boolean hasDataFactProviders = this.prolog.getTheoryManager().hasDataFactProviders();
    if (! hasDataFactProviders) {
      return Continuation.CONTINUE;
    }

    final SolutionListener solutionListener = currentVars.getSolutionListener();
    int result = Continuation.CONTINUE;
    // Now fetch data
    final Iterable<DataFactProvider> dataProviders = this.prolog.getTheoryManager().getDataFactProviders();
    for (final DataFactProvider dataFactProvider : dataProviders) {
      final Iterable<DataFact> matchingDataFacts = dataFactProvider.listMatchingDataFacts(goalTerm, currentVars);
      for (final DataFact dataFact : matchingDataFacts) {
        final UnifyContext varsAfterHeadUnified = currentVars.unify(goalTerm, dataFact);
        final boolean unified = varsAfterHeadUnified != null;
        if (unified) {
          final int continuation = solutionListener.onSolution(currentVars);
          if (continuation != Continuation.CONTINUE) {
            result = continuation;
          }
        }
      }
      if (logger.isInfoEnabled()) {
        logger.info("Last DataFact of {} iterated", dataFactProvider);
      }
    }
    return result;
  }


}
