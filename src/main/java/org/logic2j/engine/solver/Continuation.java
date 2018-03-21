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

package org.logic2j.engine.solver;

import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.unify.UnifyContext;

/**
 * Codes that the application or library returns to specify the behaviour that the inference engine should take after
 * a solution was found, via
 * {@link SolutionListener#onSolution(UnifyContext)}.
 * <p>
 * NOTE: Only those two possible values should be returned, see documentation of SolutionListener.
 *
 * @author tettoni
 */
public interface Continuation {
  /**
   * Value that {@link SolutionListener#onSolution(UnifyContext)}
   * must return for the inference engine to continue solving (search for alternate solutions).
   */
  Integer CONTINUE = 0;
  /**
   * Value that {@link SolutionListener#onSolution(UnifyContext)}
   * must return for the inference engine to stop solving (ie. means caller requests abort).
   */
  Integer USER_ABORT = -1;

}
