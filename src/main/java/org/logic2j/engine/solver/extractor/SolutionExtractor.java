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
package org.logic2j.engine.solver.extractor;

import org.logic2j.engine.unify.UnifyContext;

/**
 * Extract content from a solution, this is called during solving from one of our SolutionListeners,
 * at a moment when we need to "reify" what we want to remember from the solution.
 * The unique method receives the monad with all the current state of variables, whereas the implementers of
 * this interface will remember what Term or Var needs to be extracted.
 */
public interface SolutionExtractor<T> {

  T extractSolution(UnifyContext currentVars);

}
