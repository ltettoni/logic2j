package org.logic2j.solve.ioc;

import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.IllegalSolutionException;

/**
 * A {@link SolutionListener} that will collect only the first solution yet make sure
 * there is no other solution provided by the solver.
 *
 */
public class FirstSolutionListener extends SingleSolutionListener {

  public FirstSolutionListener(Term theGoal, VarBindings theBindings) {
    // We are only interested in the first result so we will tell the SolutionListener
    // to stop the solver after the first solution. Using this argument we won't be
    // able to tell if there are actually more, or not. But we are not interested.
    super(theGoal, theBindings, 1);
  }

  @Override
  protected void onSuperfluousSolution() {
    throw new IllegalSolutionException("Should not happen we have asked the SolutionListener to stop after one");
  }

}
