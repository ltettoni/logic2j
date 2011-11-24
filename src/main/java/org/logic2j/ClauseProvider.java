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
package org.logic2j;

import org.logic2j.model.Clause;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.var.Bindings;
import org.logic2j.solve.GoalSolver;

/**
 * Provide clauses (facts or rules) from various content sources to 
 * the {@link GoalSolver} inference engine.
 * The most typical implementation is that clauses are parsed from one (or several)
 * theories' textual content. Other implementations include database back-ends, or online
 * resources.<br/>
 * Notice the {@link Iterable} nature of the returned clauses. This allows implementors to
 * return iterable results sets, for example from database cursors. The {@link GoalSolver} does
 * not need all clauses in memory at once!
 * 
 * Contract: The {@link GoalSolver} will never cache the result from
 * {@link #listMatchingClauses(Struct)}, therefore think of caching in case of remote content.
 */
public interface ClauseProvider {

  /**
   * List clauses (facts or rules) potentially matching the specified goal.
   * All clauses that could (but may eventually not) match theGoal must 
   * be returned by this method. This implies that the match is broader than needed,
   * and the {@link GoalSolver} will actually determine by unification
   * if returned clauses will actually be useable or not.
   * 
   * @param theGoal
   * @param 
   * @return An ordered iterable of {@link Clause}s that are 
   * candidates for unifying with theGoal. Allows nice foreach construct!
   */
  public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings);

}
