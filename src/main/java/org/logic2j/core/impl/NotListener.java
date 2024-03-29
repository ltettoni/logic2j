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

import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.unify.UnifyContext;

/**
 * A SolutionListener that implements the logical not.
 */
public class NotListener implements SolutionListener {
  private boolean atLeastOneSolution = false;

  @Override
  public int onSolution(UnifyContext currentVars) {
    // Do NOT relay the solution further, just remember there was one
    this.atLeastOneSolution = true;
    // No need to seek for further solutions. Watch out this means the goal will stop evaluating on first success.
    // Fixme Should rather say the enumeration was cancelled on purpose (optimized like in AND statements)
    return Continuation.USER_ABORT;
  }

  public boolean exists() {
    return atLeastOneSolution;
  }
}
