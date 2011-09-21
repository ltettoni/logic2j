package org.logic2j.solve.ioc;

import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.Solution;

/**
 * A {@link SolutionListener} that will focus on the first solution only.
 *
 */
public abstract class SingleSolutionListener implements SolutionListener {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SingleSolutionListener.class);

  private final int maxCount;
  protected int counter;
  private Solution solution;

  protected Term goal;
  protected VarBindings bindings;

  /**
   * Create a {@link SolutionListener} that will enumerate solutions up to theMaxCount
   * before aborting by "user request". We will usually supply 1 or 2, see derived classes.
   * @param theBindings 
   * @param theGoal 
   * @param theMaxCount
   */
  public SingleSolutionListener(Term theGoal, VarBindings theBindings, int theMaxCount) {
    super();
    this.maxCount = theMaxCount;
    this.counter = 0;
    this.solution = null;
    this.goal = theGoal;
    this.bindings = theBindings;
  }

  @Override
  public boolean onSolution() {
    if (this.counter > 0) {
      // OOps, we already had solutions? This is not desired
      onSuperfluousSolution();
    }
    logger.debug(" >>>>>>>>> onSolution(), iter=#{}", this.counter);
    this.solution = new Solution(this.goal, this.bindings);
    this.counter++;
    return this.counter < this.maxCount;
  }

  /**
   * Handle the case of more than one single solution detected.
   */
  protected abstract void onSuperfluousSolution();

  /**
   * @return The number of solutions found.
   */
  public int getNbSolutions() {
    return this.counter;
  }

  /**
   * @return the solution
   */
  public Solution getSolution() {
    return this.solution;
  }

}
