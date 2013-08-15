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
package org.logic2j.core.solver;

import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.Var;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.listener.SolutionListener;

/**
 * Interface to access the inference engine algorithm, in order to solve goals.
 */
public interface Solver {

    /**
     * The method to solve a goal.
     *
     * @param theGoal Defines the {@link Term} and the {@link Var} values we are trying to solve.
     * @param callerFrame
     * @param theSolutionListener Where to send solutions
     */
    void solveGoal(Bindings theGoal, GoalFrame callerFrame, SolutionListener theSolutionListener);

}
