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
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.exception.Logic2jException;
import org.logic2j.engine.exception.SolverException;
import org.logic2j.engine.model.DataFact;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.TermApi;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.solver.listener.UnifyContextIterator;
import org.logic2j.engine.unify.UnifyContext;
import org.logic2j.engine.util.ProfilingInfo;

import java.util.Iterator;

/**
 * Solve goals - that's the core of the engine, the resolution algorithm is in this class.
 * There are 4 predicates managed directly in this class:
 * "," (AND)
 * ";" (OR)
 * "call(X)"
 * "!" (CUT)
 * ( and in the future, ":-" (RULE) )
 * All other predicates are delegated in implementations of {@link FOPredicate#predicateLogic(UnifyContext)}.
 */
public class Solver extends org.logic2j.engine.solver.Solver {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Solver.class);

  private static final boolean isDebug = logger.isDebugEnabled();

  /**
   * Do we solve the ";" (OR) predicate internally here, or in the predicate.
   * (see note re. processing of OR in CoreLibrary.pro)
   */
  private static final boolean INTERNAL_OR = false;

  /**
   * Do we acquire profiling information (number of inferences, etc)
   */
  private static final boolean PROFILING = true;

  private final PrologImplementation prolog;

  public Solver(PrologImplementation theProlog) {
    this.prolog = theProlog;
  }


  /**
   * This is an alternate entry point when a {@link UnifyContext}
   * is already instantiated; this is needed in custom predicates implementing first-order logic like
   * not(), exists(), etc.
   * You enter here when part of the variables have been bound already.
   */
  public Integer solveGoal(Object goal, UnifyContext currentVars) {
    // Check if we will have to deal with DataFacts in this session of solving.
    // This slightly improves performance - we can bypass calling the method that deals with that
    if (goal instanceof Struct && !((Struct) goal).hasIndex()) {
      throw new InvalidTermException("Struct must be normalized before it can be solved: \"" + goal + "\" - call TermApi.normalize()");
    }
    final Integer cutIntercepted = solveGoalRecursive(goal, currentVars, /* FIXME why this value?*/10);
    return cutIntercepted;
  }

  /**
   * That's the complex method - the heart of the Solver.
   *
   * @param goalTerm
   * @param currentVars
   * @param cutLevel
   * @return
   */
  private Integer solveGoalRecursive(final Object goalTerm, final UnifyContext currentVars, final int cutLevel) {
    final long inferenceCounter = ProfilingInfo.nbInferences;
    if (isDebug) {
      logger.debug("-->> Entering solveRecursive#{}, reifiedGoal = {}", inferenceCounter, currentVars.reify(goalTerm));
      logger.debug("     cutLevel={}", cutLevel);
    }
    if (PROFILING) {
      ProfilingInfo.nbInferences++;
    }
    Integer result = Continuation.CONTINUE;

    // At the moment we don't properly manage atoms as goals...
    final Struct goalStruct;
    if (goalTerm instanceof String) {
      // Yet we are not capable of handing String everywhere below - so use a Struct atom still
      goalStruct = new Struct((String) goalTerm);
        /* Prototype code - does actually not work but could
        } else if (TermApi.isFreeVar(goalTerm)) {
            // Crazy we, we allow a single Var to be considered as a goal - just assuming it is bound to a Struct
            final Object goalReified = currentVars.reify(goalTerm);
            if (TermApi.isFreeVar(goalReified)) {
                throw new UnsupportedOperationException("A free variable cannot be used as a goal in a rule: \"" + goalTerm + '"');
            }
            if (! (goalReified instanceof Struct)) {
                throw new UnsupportedOperationException("Vars used as a goal must always be bound to a Struct, was: \"" + goalReified + '"');
            }
            goalStruct = (Struct) goalReified;
        */
    } else {
      assert goalTerm instanceof Struct :
          "Calling solveGoalRecursive with a goal that is not a Struct but: \"" + goalTerm + "\" of " + goalTerm.getClass();
      goalStruct = (Struct) goalTerm;
    }

    // Extract all features of the goal to solve
    final String functor = goalStruct.getName();
    final int arity = goalStruct.getArity();

    // First we will check the goal against core predicates such as
    // AND (","), OR (";"), CUT ("!") and CALL
    // Then we will check if the goal is a Primitive implemented in a Java library
    // Finally we will handle classic goals matched against Prolog theories

    // The AND predicate !
    if (Struct.FUNCTOR_COMMA == functor) { // Names are {@link String#intern()}alized so OK to check by reference
      // Logical AND. Typically the arity=2 since "," is a binary predicate. But in logic2j we allow more, the same code supports both.

      // Algorithm: for the sequential AND of N goals G1,G2,G3,...,GN, we defined N-1 listeners, and solve G1 against
      // the first listener: all solutions to G1, will be escalated to that listener that handles G2,G3,...,GN
      // Then that listener will solve G2 against the listener for (final G3,...,GN). Finally GN will solve against the
      // "normal" listener received as argument (hence propagating the ANDed solution to our caller).

      // Note that instantiating all these listeners could be costly - if we found a way to have a cache (eg. storing them
      // at parse-time in Clauses) it could improve performance.

      final SolutionListener[] andingListeners = new SolutionListener[arity];
      // The last listener is the one that called us (typically the one of the application, if this is the outermost "AND")
      andingListeners[arity - 1] = currentVars.getSolutionListener();
      // Allocates N-1 andingListeners, usually this means one.
      // On solution, each will trigger solving of the next term
      final Object[] goalStructArgs = goalStruct.getArgs();
      final Object lhs = goalStructArgs[0];
      for (int i = 0; i < arity - 1; i++) {
        final int index = i;
        andingListeners[index] = new SolutionListener() {

          @Override
          public Integer onSolution(UnifyContext currentVars) {
            final int nextIndex = index + 1;
            final Object rhs = goalStructArgs[nextIndex]; // Usually the right-hand-side of a binary ','
            if (isDebug) {
              logger.debug("{}: onSolution() called; will now solve rhs={}", this, rhs);
            }
            final Integer continuationFromSubGoal = solveGoalRecursive(rhs, currentVars.withListener(andingListeners[nextIndex]), cutLevel);
            return continuationFromSubGoal;
          }

          @Override
          public Integer onSolutions(final Iterator<UnifyContext> multiLHS) {
            final int nextIndex = index + 1;
            final Object rhs = goalStructArgs[nextIndex]; // Usually the right-hand-side of a binary ','
            final SolutionListener subListener = new SolutionListener() {
              @Override
              public Integer onSolution(UnifyContext currentVars) {
                throw new UnsupportedOperationException("Should not be here");
              }

              @Override
              public Integer onSolutions(Iterator<UnifyContext> multiRHS) {
                logger.info("AND sub-listener got multiLHS={} and multiRHS={}", multiLHS, multiRHS);
                final UnifyContextIterator combined = new UnifyContextIterator(currentVars, multiLHS, multiRHS);
                return andingListeners[nextIndex].onSolutions(combined);
              }

            };
            final Integer continuationFromSubGoal = solveGoalRecursive(rhs, currentVars.withListener(subListener), cutLevel);
            return continuationFromSubGoal;
          }

          @Override
          public String toString() {
            return "AND sub-listener to " + lhs;
          }
        };
      }
      // Solve the first goal, redirecting all solutions to the first listener defined above
      if (isDebug) {
        logger.debug("Handling AND, arity={}, will now solve lhs={}", arity, currentVars.reify(lhs));
      }
      result = solveGoalRecursive(lhs, currentVars.withListener(andingListeners[0]), cutLevel);
    }
    // The OR predicate
    else if (INTERNAL_OR && Struct.FUNCTOR_SEMICOLON == functor) { // Names are {@link String#intern()}alized so OK to check by reference
      /*
       * This is the Java implementation of N-arity OR
       * We can also implement a binary OR directly in Prolog, see note re. processing of OR in CoreLibrary.pro
       */
      for (int i = 0; i < arity; i++) {
        // Solve all the elements of the "OR", in sequence.
        // For a binary OR, this means solving the left-hand-side and then the right-hand-side
        if (isDebug) {
          logger.debug("Handling OR, element={} of {}", i, goalStruct);
        }
        result = solveGoalRecursive(goalStruct.getArg(i), currentVars, cutLevel);
        if (result != Continuation.CONTINUE) {
          break;
        }
      }
    }
    // The CALL predicate
    else if (Struct.FUNCTOR_CALL == functor) { // Names are {@link String#intern()}alized so OK to check by reference
      // TODO call/1 is handled here for efficiency, see if it's really needed we could as well use the Primitive (already implemented)
      if (arity != 1) {
        throw new InvalidTermException("Primitive \"call\" accepts only one argument, got " + arity);
      }
      final Object callTerm = goalStruct.getArg(0);  // Often a Var
      final Object realCallTerm = currentVars.reify(callTerm); // The real value of the Var
      if (TermApi.isFreeVar(realCallTerm)) {
        throw new SolverException("Cannot call/* on a free variable");
      }
      result = solveGoalRecursive(realCallTerm, currentVars, cutLevel);

    }
    // The CUT functor
    else if (Struct.FUNCTOR_CUT == functor) {
      // This is a "native" implementation of CUT, which works as good as using the primitive in CoreLibrary
      // Doing it inline might improve performance a little although I did not measure much difference.
      // Functionally, this code may be removed

      // Cut IS a valid solution in itself. We just ignore what the application asks (via return value) us to do next.
      final Integer continuationFromCaller =
          currentVars.getSolutionListener().onSolution(currentVars);// Signalling one valid solution, but ignoring return value

      if (continuationFromCaller != Continuation.CONTINUE && continuationFromCaller > 0) {
        result = continuationFromCaller;
      } else {
        // Stopping there for this iteration
        result = cutLevel;
      }
    }
    // ---------------------------------------------------------------------------
    // Primitive implemented in Java
    // ---------------------------------------------------------------------------
    else if (goalStruct.getPrimitiveInfo() != null) {
      final PrimitiveInfo prim = goalStruct.getPrimitiveInfo();


      final Object resultOfPrimitive = prim.invoke(goalStruct, currentVars.getSolutionListener(), currentVars);
      // Extract necessary objects from our current state

      switch (prim.getType()) {
        case PREDICATE:
          // The result will be the continuation code or CUT level
          final Integer primitiveContinuation = (Integer) resultOfPrimitive;
          result = primitiveContinuation;
          break;
        case FUNCTOR:
          if (isDebug) {
            logger.debug("Result of Functor {}: {}", goalStruct, resultOfPrimitive);
          }
          // logger.error("We should not pass here with functors!? Directive {} ignored", goalStruct);
          assert true : "We should not pass here with functors!? Directive " + goalStruct + " ignored";
          break;
        case DIRECTIVE:
          logger.warn("Result of Directive {} not yet used", goalStruct);
          break;
      }
    }
    //---------------------------------------------------------------------------
    // Not any "special" handling
    //---------------------------------------------------------------------------

    else {
    //---------------------------------------------------------------------------
    // Regular prolog inference rule: goal :- subGoal
    //---------------------------------------------------------------------------
      result = solveAgainstClauseProviders(goalTerm, currentVars, cutLevel + 1);

      //---------------------------------------------------------------------------
      // Solve against data facts
      //---------------------------------------------------------------------------
      if (result == Continuation.CONTINUE) {
        result = solveAgainstDataProviders(goalTerm, currentVars, cutLevel + 1);
      }
    }
    if (isDebug) {
      logger.debug("<<-- Exiting  solveRecursive#" + inferenceCounter + ", reifiedGoal = {}, result={}", currentVars.reify(goalTerm), result);
    }
    return result;
  }


  protected Integer solveAgainstClauseProviders(final Object goalTerm, UnifyContext currentVars, final int cutLevel) {
    // Simple "user-defined" goal to demonstrate - find matching goals in the theories loaded
    final long inferenceCounter = ProfilingInfo.nbInferences;
    if (isDebug) {
      logger.debug(" +>> Entering solveAgainstClauseProviders#{}, cutLevel={}", inferenceCounter, cutLevel);
    }
    Integer result = Continuation.CONTINUE;

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
            final Integer continuation = currentVars.getSolutionListener().onSolution(contextAfterHeadUnified);
            result = continuation;
          } else {
            // Not a fact, it's a theorem - it has a body - the body becomes our new goal
            if (isDebug) {
              logger.debug(" Head unified. Clause with head = {} is a theorem, solving body = {}", clauseHead, clauseBody);
            }
            // Solve the body in our current recursion context
            final Integer theoremResult = solveGoalRecursive(clauseBody, contextAfterHeadUnified, cutLevel);
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

  protected Integer solveAgainstDataProviders(final Object goalTerm, final UnifyContext currentVars, final int cutLevel) {
    final boolean hasDataFactProviders = this.prolog.getTheoryManager().hasDataFactProviders();
    if (! hasDataFactProviders) {
      return Continuation.CONTINUE;
    }

    Integer result = Continuation.CONTINUE;
    // Now fetch data
    final Iterable<DataFactProvider> dataProviders = this.prolog.getTheoryManager().getDataFactProviders();
    for (final DataFactProvider dataFactProvider : dataProviders) {
      final Iterable<DataFact> matchingDataFacts = dataFactProvider.listMatchingDataFacts(goalTerm, currentVars);
      for (final DataFact dataFact : matchingDataFacts) {
        final UnifyContext varsAfterHeadUnified = currentVars.unify(goalTerm, dataFact);
        final boolean unified = varsAfterHeadUnified != null;
        if (unified) {
          final Integer continuation = currentVars.getSolutionListener().onSolution(currentVars);
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
