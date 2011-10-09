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
import org.logic2j.solve.IllegalSolutionException;
import org.logic2j.solve.MissingSolutionException;
import org.logic2j.solve.Solution;

/**
 * A {@link SolutionListener} that will collect only the first solution yet make sure
 * there is no other solution provided by the solver.
 *
 */
public class UniqueSolutionListener extends SingleSolutionListener {

  public UniqueSolutionListener(Term theGoal, Bindings theBindings) {
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
