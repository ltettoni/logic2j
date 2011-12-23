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

/**
 * Provide {@link Clause}s (facts or rules) from various content sources to 
 * the {@link org.logic2j.solve.GoalSolver} inference engine.
 * The most typical implementation is that clauses are parsed from one (or several)
 * theories' textual content. Other implementations include database back-ends, or online
 * resources.<br/>
 * Notice the {@link Iterable} nature of the returned clauses. This allows implementors to
 * return iterable results sets, for example from database cursors. The {@link org.logic2j.solve.GoalSolver} does
 * not need all clauses in memory at once!
 * 
 * Contract: The {@link org.logic2j.solve.GoalSolver} will never cache the result from
 * {@link #listMatchingClauses(Struct, Bindings)}, therefore think of caching in case of remote content.
 */
public interface ClauseProvider {

  /**
   * Provide {@link Clause}s (facts or rules) potentially matching theGoal argument, which often
   * is a Struct with bound or unbound variables.<br/>
   * All clauses that could (but may eventually not) match theGoal must 
   * be returned by this method. This implies that the match may be broader than actually needed,
   * the {@link org.logic2j.solve.GoalSolver} will determine by unification
   * if {@link Clause}s returned by this method will be eligible for inference.
   * 
   * @param theGoal
   * @return An ordered {@link Iterable} of {@link Clause}s that are 
   * candidates for unifying with theGoal. Aside from performance aspects, it is not critical to 
   * return {@link Clause}s whose head would eventually not be used by 
   * the {@link org.logic2j.solve.GoalSolver}.
   */
  public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings);

}
