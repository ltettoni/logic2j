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
package org.logic2j.core.api.solver.listener;

import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.listener.multi.MultiResult;
import org.logic2j.core.api.unify.UnifyContext;

/**
 * The lowest-level API through which the inference engine provides solutions. For easier programming, consider using
 * {@link org.logic2j.core.api.Prolog#solve(CharSequence)} and the {@link org.logic2j.core.api.solver.holder.SolutionHolder} API.
 * The return values of the two methods are defined in interface Continuation.
 * Never return a positive value this is used internally to manage the CUT predicate.
 */
public interface SolutionListener {

    /**
     * The inference engine notifies the caller code that a solution was proven; the real content to the solution must be retrieved from the
     * goal's variables.
     * 
     * @return The caller must return {@link Continuation#CONTINUE} for the inference engine to continue searching for other solutions, or
     *         {@link Continuation#USER_ABORT} to break the search for other solutions (ie. user cancellation). Never return a positive number.
     */
    Integer onSolution(UnifyContext currentVars);

    /**
     * Experimental: multiple solutions passing between predicates.
     * @param multi
     * @return The caller must return {@link Continuation#CONTINUE} for the inference engine to continue searching for other solutions, or
     *         {@link Continuation#USER_ABORT} to break the search for other solutions (ie. user cancellation). Never return a positive number.
     */
    Integer onSolutions(MultiResult multi);
}
