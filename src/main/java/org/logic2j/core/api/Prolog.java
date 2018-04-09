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
package org.logic2j.core.api;

import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Term;
import org.logic2j.engine.solver.holder.GoalHolder;
import org.logic2j.core.impl.theory.TheoryManager;
import org.logic2j.engine.solver.holder.SolutionHolder;
import org.logic2j.engine.solver.listener.SolutionListener;

/**
 * Interface for using Prolog from an application's perspective.
 */
public interface Prolog {

  // ---------------------------------------------------------------------------
  // Shortcuts or "syntactic sugars" to ease programming.
  // The following methods delegate calls to sub-features of the Prolog engine.
  // ---------------------------------------------------------------------------

  /**
   * The top-level method for solving a goal (exposes the high-level {@link SolutionHolder} API,
   * internally it uses the low-level {@link SolutionListener}).
   * This does NOT YET start solving.
   * If you already have a parsed term, use {@link #solve(Object)} instead.
   *
   * @param theGoal To solve, will be parsed into a Term.
   * @return A {@link org.logic2j.engine.solver.holder.SolutionHolder} that will allow the caller code to dereference solution(s) and their bindings (values of variables).
   */
  GoalHolder solve(CharSequence theGoal);

  /**
   * Solves a goal expressed as a {@link Term} (exposes the high-level {@link SolutionHolder} API, internally it uses the low-level
   * {@link SolutionListener}).
   *
   * @param theGoal The {@link Term} to solve, usually a {@link Struct}
   * @return A {@link SolutionHolder} that will allow the caller code to dereference solution(s) and their bindings (values of variables).
   */
  GoalHolder solve(Object theGoal);

  // ---------------------------------------------------------------------------
  // Accessors to the sub-features of the Prolog engine
  // ---------------------------------------------------------------------------

  /**
   * The current adapter to convert {@link Term}s to and from Java {@link Object}s.
   *
   * @return Our {@link TermAdapter}
   */
  TermAdapter getTermAdapter();

  /**
   * The current theory manager, will allow calling code to add clauses, load theories, etc.
   *
   * @return Our {@link TheoryManager}
   */
  TheoryManager getTheoryManager();


}
