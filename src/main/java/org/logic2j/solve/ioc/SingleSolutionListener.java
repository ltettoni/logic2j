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
package org.logic2j.solve.ioc;

import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.Bindings;
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
  protected Bindings bindings;

  /**
   * Create a {@link SolutionListener} that will enumerate solutions up to theMaxCount
   * before aborting by "user request". We will usually supply 1 or 2, see derived classes.
   * @param theBindings 
   * @param theGoal 
   * @param theMaxCount
   */
  public SingleSolutionListener(Term theGoal, Bindings theBindings, int theMaxCount) {
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
