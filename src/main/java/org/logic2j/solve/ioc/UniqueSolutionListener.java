package org.logic2j.solve.ioc;

import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.IllegalSolutionException;
import org.logic2j.solve.MissingSolutionException;
import org.logic2j.solve.Solution;

/**
 * A {@link SolutionListener} that will collect only the first solution yet make sure
 * there is no other solution provided by the solver.
 *
 */
public class UniqueSolutionListener extends SingleSolutionListener {

  public UniqueSolutionListener(Term theGoal, VarBindings theBindings) {
    // We wish to make sure the first solution is the only one, so we 
    // are going to try to reach further, at least 2 solutions. 
    // Then we will be able to determine for sure if there was actually 
    // only one, or more.
    super(theGoal, theBindings, 2);
  }

  @Override
  public Solution getSolution() {
    if (getNbSolutions() < 1) {
      onMissingSolution();
    }
    return super.getSolution();
  }

  private void onMissingSolution() {
    // TODO would be nice to have some context information here
    throw new MissingSolutionException("No solution found, where exactly one was required");
  }

  @Override
  protected void onSuperfluousSolution() {
    // TODO would be nice to have some context information here
    throw new IllegalSolutionException("More than one solution found");
  }

}
