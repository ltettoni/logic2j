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

package org.logic2j.core.solver.holder;

import java.util.Map;

import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.solver.listener.Solution;

/**
 * A relay object to provide access to the results of the (expected) unique solution to a goal.
 */
public class UniqueSolutionHolder {

  private Solution solution;

  /**
   * @param theSolution 
   */
  UniqueSolutionHolder(Solution theSolution) {
    this.solution = theSolution;
  }

  /**
   * @param theVariableName
   * @return The value of var theVariableName
   * @see Solution#getBinding(String)
   */
  public Term binding(String theVariableName) {
    return this.solution.getBinding(theVariableName);
  }

  /**
   * @return All bindings.
   */
  public Map<String, Term> bindings() {
    return this.solution.getBindings();
  }

  /**
   * @return The solution as a {@link Term}.
   */
  public Term solution() {
    return this.solution.getSolution();
  }
}