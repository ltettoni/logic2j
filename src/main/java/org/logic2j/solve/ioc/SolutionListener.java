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

import org.logic2j.Prolog;
import org.logic2j.solve.holder.SolutionHolder;

/**
 * The core, lowest-level method by which the inference engine provides solutions.
 * For easier programming, consider using {@link Prolog#solve(CharSequence)} and the
 * {@link SolutionHolder} API.
 */
public interface SolutionListener {

  /**
   * The inference engine notifies the caller code that a solution 
   * was demonstrated; the real content to the solution must be
   * retrieved from the goal's variables.
   * 
   * @return The caller must return true for the inference engine to
   * continue searching for other solutions, or false
   * to break (user cancellation) the search for other solutions.
   */
  public boolean onSolution();

}
