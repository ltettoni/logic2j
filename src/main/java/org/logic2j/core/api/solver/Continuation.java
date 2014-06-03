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

package org.logic2j.core.api.solver;

/**
 * Allows the application or library code to specify to the behaviour that the inference engine should take after
 * a solution was found, via {@link org.logic2j.core.api.solver.listener.SolutionListener#onSolution(org.logic2j.core.api.unify.UnifyContext)}.
 *
 * @author tettoni
 */
public enum Continuation {
    /**
     * Value that {@link org.logic2j.core.api.solver.listener.SolutionListener#onSolution(org.logic2j.core.api.unify.UnifyContext)} must return for the inference engine to continue solving (search for alternate solutions).
     */
    CONTINUE,
    /**
     * Value that {@link org.logic2j.core.api.solver.listener.SolutionListener#onSolution(org.logic2j.core.api.unify.UnifyContext)} must return for the inference engine to stop solving (ie. means caller requests abort).
     */
    USER_ABORT,
    /**
     * A cut "!" has been found - this will prune the search tree to the last solution found.
     */
    CUT

}
