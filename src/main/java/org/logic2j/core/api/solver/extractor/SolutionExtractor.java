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
package org.logic2j.core.api.solver.extractor;

import org.logic2j.core.api.unify.UnifyContext;

/**
 * Extract content from a solution, this is called during solving from one of our SolutionListeners,
 * at a moment when we need to "reify" what we want to remember from the solution.
 * The unique method receives the monad with all the current state of variables, whereas the implementers of
 * this interface will remember what Term or Var needs to be extracted.
 */
public interface SolutionExtractor<T> {

    T extractSolution(UnifyContext currentVars);

}
