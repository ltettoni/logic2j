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
