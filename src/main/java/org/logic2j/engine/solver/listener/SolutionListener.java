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
package org.logic2j.engine.solver.listener;

import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.solver.holder.SolutionHolder;
import org.logic2j.engine.solver.listener.multi.MultiResult;
import org.logic2j.engine.unify.UnifyContext;

/**
 * The lowest-level API through which the inference engine provides solutions. For easier programming, consider using
 * {@link org.logic2j.core.api.Prolog#solve(CharSequence)} and the {@link SolutionHolder} API.
 * The return values of the two methods are defined in interface Continuation.
 * Never return a positive value this is used internally to manage the CUT predicate.
 */
public interface SolutionListener {

  /**
   * The inference engine notifies the caller code that a solution was proven; the real content to the solution must be retrieved from the
   * goal's variables.
   *
   * @return The caller must return {@link Continuation#CONTINUE} for the inference engine to continue searching for other solutions, or
   * {@link Continuation#USER_ABORT} to break the search for other solutions (ie. user cancellation). Never return a positive number.
   */
  Integer onSolution(UnifyContext currentVars);

  /**
   * Experimental: multiple solutions passing between predicates.
   *
   * @param multi
   * @return The caller must return {@link Continuation#CONTINUE} for the inference engine to continue searching for other solutions, or
   * {@link Continuation#USER_ABORT} to break the search for other solutions (ie. user cancellation). Never return a positive number.
   */
  Integer onSolutions(MultiResult multi);
}
