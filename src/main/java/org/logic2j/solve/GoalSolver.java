package org.logic2j.solve;

import org.logic2j.PrologImpl;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.ioc.SolutionListener;

/**
 * Interface allowing {@link PrologImpl} to access the inference engine algorithm,
 * in order to solve goals or invoke primitives.
 * This interface allows to provide various implementations such as plug-ins.
 *
 */
public interface GoalSolver {

  /**
   * The top-level method to solve a high-level goal.
   * @param goalTerm
   * @param goalVars
   * @param callerFrame
   * @param theSolutionListener
   */
  public void solveGoal(final Term goalTerm, final VarBindings goalVars, final GoalFrame callerFrame,
      final SolutionListener theSolutionListener);

  /**
   * The lower-level method to solve sub-goals.
   * @param goalTerm
   * @param goalVars
   * @param callerFrame
   * @param theSolutionListener
   */
  // TODO Why should we make it public? Seems not normal
  public void solveGoalRecursive(final Term goalTerm, final VarBindings goalVars, final GoalFrame callerFrame,
      final SolutionListener theSolutionListener);

}
