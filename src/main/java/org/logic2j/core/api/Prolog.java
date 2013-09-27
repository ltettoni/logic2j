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
package org.logic2j.core.api;

import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.solver.holder.SolutionHolder;
import org.logic2j.core.impl.theory.TheoryManager;

/**
 * Interface for using Prolog from an application perspective.
 * TODO (issue) See if we can minimize the interface (only add if absolutely required, otherwise add to the PrologImplementation), see
 * https://github.com/ltettoni/logic2j/issues/8
 */
public interface Prolog {

    // ---------------------------------------------------------------------------
    // Shortcuts or "syntactic sugars" to ease programming.
    // The following methods delegate calls to sub-features of the Prolog engine.
    // ---------------------------------------------------------------------------

    /**
     * The top-level method for solving a goal (exposes the high-level {@link SolutionHolder} API,
     * internally it usess the low-level {@link SolutionListener}).
     * This does NOT YET start solving.
     * If you already have a parsed {@link Term}, use {@link #solve(Term)} instead.
     * 
     * @param theGoal To solve, will be parsed into a Term.
     * @return A {@link SolutionHolder} that will allow the caller code to dereference solution(s) and their bindings (values of variables).
     */
    SolutionHolder solve(CharSequence theGoal);

    /**
     * Solves a goal expressed as a {@link Term} (exposes the high-level {@link SolutionHolder} API, internally it usess the low-level
     * {@link SolutionListener}).
     * 
     * @param theGoal The {@link Term} to solve, usually a {@link Struct}
     * @return A {@link SolutionHolder} that will allow the caller code to dereference solution(s) and their bindings (values of variables).
     */
    SolutionHolder solve(Object theGoal);

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
