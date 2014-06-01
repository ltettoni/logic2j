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

import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.UnifyContext;

/**
 * Interface to access the inference engine algorithm, in order to solve goals.
 * Uses the low-level {@link SolutionListener} callback mechanism to notify solutions.
 */
public interface Solver {

    /**
     * The method to solve a goal starting with all free Vars.
     *
     * @param goal Defines the {@link Term} and the {@link Var} values we are trying to solve.
     * @param theSolutionListener Where solutions should be called back.
     * @return Indicate how the solving has completed, either {@value org.logic2j.core.api.model.Continuation#CONTINUE} for a successful
     *         complete result, or if the solving has been cut or aborted by user callback.
     */
    Continuation solveGoal(Object goal, SolutionListener theSolutionListener);

    /**
     * The method to solve a goal.
     *
     * @param goal Defines the {@link Term} and the {@link Var} values we are trying to solve.
     * @param theSolutionListener Where solutions should be called back.
     * @return Indicate how the solving has completed, either {@value org.logic2j.core.api.model.Continuation#CONTINUE} for a successful
     *         complete result, or if the solving has been cut or aborted by user callback.
     */
    Continuation solveGoal(Object goal, UnifyContext currentVars, SolutionListener theSolutionListener);

    UnifyContext initialContext();

}
