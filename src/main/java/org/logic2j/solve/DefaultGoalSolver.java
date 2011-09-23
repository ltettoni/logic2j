package org.logic2j.solve;

import org.logic2j.ClauseProvider;
import org.logic2j.PrologImplementor;
import org.logic2j.model.Clause;
import org.logic2j.model.InvalidTermException;
import org.logic2j.model.prim.PrimitiveInfo;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.ioc.SolutionListener;
import org.logic2j.util.ReportUtils;

/**
 * Solve goals.
 *
 */
public class DefaultGoalSolver implements GoalSolver {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultGoalSolver.class);
  private static final boolean debug = logger.isDebugEnabled();

  private static final TermApi TERM_API = new TermApi();
  public int internalCounter = 0;
  private final PrologImplementor prolog;

  public DefaultGoalSolver(PrologImplementor theProlog) {
    this.prolog = theProlog;
  }

  @Override
  public void solveGoal(final Term goalTerm, final VarBindings goalVars, final GoalFrame callerFrame,
      final SolutionListener theSolutionListener) {
    solveGoalRecursive(goalTerm, goalVars, callerFrame, theSolutionListener);
  }

  @Override
  public void solveGoalRecursive(final Term goalTerm, final VarBindings goalVars, final GoalFrame callerFrame,
      final SolutionListener theSolutionListener) {
    if (debug) {
      logger.debug("Entering solveRecursive({}), callerFrame={}", goalTerm, callerFrame);
    }
    if (!(goalTerm instanceof Struct)) {
      throw new InvalidTermException("Goal term " + goalTerm + " is not a Struct and cannot be solved");
    }
    // Check if goal is a system predicate or a simple one to match against the theory
    final Struct goalStruct = (Struct) goalTerm;
    final PrimitiveInfo prim = goalStruct.getPrimitiveInfo();

    final String functor = goalStruct.roName;
    if (Struct.FUNCTOR_COMMA == functor) {
      // Logical AND
      final int arity = goalStruct.roArity;
      final SolutionListener[] listeners = new SolutionListener[arity];
      // The last listener is the one of this overall COMMA sequence
      listeners[arity - 1] = theSolutionListener;
      // Allocates N-1 listeners. On solution, each will trigger solving of the next term
      for (int i = 0; i < arity - 1; i++) {
        final int index = i;
        listeners[index] = new SolutionListener() {

          @Override
          public boolean onSolution() {
            DefaultGoalSolver.this.internalCounter++;
            final int index2 = index + 1;
            solveGoalRecursive(goalStruct.getArg(index2), goalVars, callerFrame, listeners[index2]);
            return true;
          }
        };
      }
      // Solve the first goal, redirecting all solutions to the first listener defined above
      solveGoalRecursive(goalStruct.getArg(0), goalVars, callerFrame, listeners[0]);
    } else if (Struct.FUNCTOR_SEMICOLON == functor) {
      // Logical OR
      final int arity = goalStruct.roArity;
      for (int i = 0; i < arity; i++) {
        // Solve all the left and right-and-sides, sequentially
        solveGoalRecursive(goalStruct.getArg(i), goalVars, callerFrame, theSolutionListener);
      }
    } else if (Struct.FUNCTOR_CALL == functor) {
      // call/1 is handled here for efficiency
      if (goalStruct.roArity != 1) {
        throw new InvalidTermException("Primitive 'call' accepts only one argument, got " + goalStruct.getArity());
      }
      Term target = TERM_API.substitute(goalStruct.getArg(0), goalVars, null);
      if (target instanceof Var) {
        throw new InvalidTermException("Argument to primitive 'call' may not be a variable, was" + target);
      }
      if (debug) {
        logger.debug("Calling FUNCTOR_CALL ------------------ {}", target);
      }
      solveGoalRecursive(target, goalVars, callerFrame, theSolutionListener);
    } else if (prim != null) {
      // Primitive implemented in Java
      final Object resultOfPrimitive = prim.invoke(goalStruct, goalVars, callerFrame, theSolutionListener);
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
      // Simple "user" goal to demonstrate - find matching goals

      // Now ready to iteratively try clause by first attempting to unify with its headTerm
      // For this we need a new TrailFrame
      final GoalFrame frameForAttemptingClauses = new GoalFrame(callerFrame);

      for (ClauseProvider provider : this.prolog.getClauseProviders()) {
        for (Clause clause : provider.listMatchingClauses(goalStruct)) {

          if (debug) {
            logger.debug("Trying clause {}", clause);
          }
          // Handle user cancellation at beginning of loop, not at the end.
          // This is in case user code returns onSolution()=false (do not continue)
          // on what happens to be the last normal solution - in this case we can't tell if 
          // we are exiting because user requested it, or because there's no other solution!
          if (frameForAttemptingClauses.isUserCanceled()) {
            if (debug) {
              logger.debug("!!! Stopping on SolutionListener's request");
            }
            break;
          }
          if (frameForAttemptingClauses.isCut()) {
            if (debug) {
              logger.debug("!!! cut found in clause");
            }
            break;
          }
          if (frameForAttemptingClauses.hasCutInSiblingSubsequentGoal()) {
            if (debug) {
              logger.debug("!!! Stopping because of cut in sibling subsequent goal");
            }
            break;
          }

          final VarBindings immutableVars = clause.getVars();
          final VarBindings clauseVars = new VarBindings(immutableVars);
          final Term clauseHead = clause.getHead();
          if (debug) {
            logger.debug("Unifying: goal={}  with  goalVars={}", goalTerm, goalVars);
            logger.debug("     and: clauseHead={}  with  clauseVars={}", clauseHead, clauseVars);
          }

          // Now unify - this is the only place where free variables may become bound, and 
          // the trailFrame will remember this.
          // Solutions will be notified from within this method.
          // As a consequence, deunification can happen immediately afterwards, in this method, not outside in the caller
          final boolean unified = this.prolog.getUnifyer().unify(goalTerm, goalVars, clauseHead, clauseVars,
              frameForAttemptingClauses);
          if (debug) {
            logger.debug("      unified=" + unified + ", goalVars={}, clauseVars={}", goalVars, clauseVars);
          }

          if (unified) {
            if (clause.isFact()) {
              if (debug) {
                logger.debug("{} is a fact", clauseHead);
              }
              final boolean userContinue = theSolutionListener.onSolution();
              if (!userContinue) {
                frameForAttemptingClauses.raiseUserCanceled();
              }
            } else {
              if (debug) {
                logger.debug(">> RECURS: {} is a theorem", clause);
              }
              final Term newGoalTerm = clause.getBody();
              solveGoalRecursive(newGoalTerm, clauseVars, frameForAttemptingClauses, theSolutionListener);
              if (debug) {
                logger.debug("<< RECURS");
              }
            }
            // We have now fired our solution(s), we no longer need our bound vars and can deunify
            // Go to next solution: start by clearing our trailing vars
            this.prolog.getUnifyer().deunify(frameForAttemptingClauses);
          }
        }
        if (debug) {
          logger.debug("Leaving loop on Clauses of " + provider);
        }
      }
      if (debug) {
        logger.debug("Last ClauseProvider done");
      }
    }
    if (debug) {
      logger.debug("Leaving solveGoalRecursive({})", goalTerm);
    }
  }

  @Override
  public String toString() {
    return ReportUtils.shortDescription(this);
  }

}