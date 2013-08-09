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
package org.logic2j.core;

import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.solve.holder.SolutionHolder;
import org.logic2j.core.theory.TheoryManager;

/**
 * Interface for using Prolog from an application perspective.
 * TODO See if we can minimize the interface (only add if absolutely required, otherwise add to the PrologImplementor), 
 * see https://github.com/ltettoni/logic2j/issues/8
 */
public interface Prolog {

  /**
   * A shortcut method to create a {@link Term} by delegating instantiation to the current {@link TermFactory}.
   * @param theSource Any instance of {@link Object} that may be converted to a {@link Term}.
   * @return A valid {@link Term}, ready for unification or inference within the current {@link Prolog} engine.
   */
  Term term(Object theSource);

  /**
   * The entry point for solving a goal (this is the higer-level API, internal solving uses
   * a listener).
   * @param theGoal
   * @return A {@link SolutionHolder} that will allow the caller code to dereference 
   * solution(s) and their bindings (values of variables).
   */
  SolutionHolder solve(CharSequence theGoal);

  SolutionHolder solve(Term theGoal);

  TermFactory getTermFactory();

  /**
   * Needed in order to manage the theories loaded into the engine.
   * @return The {@link TheoryManager} currently registered.
   */
  TheoryManager getTheoryManager();
}
