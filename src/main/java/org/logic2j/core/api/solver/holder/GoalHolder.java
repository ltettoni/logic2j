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
package org.logic2j.core.api.solver.holder;

import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.logic2j.core.api.solver.listener.RangeSolutionListener;
import org.logic2j.core.impl.PrologReferenceImplementation;

import java.util.Map;

/**
 * An intermediate class in the fluent API to extract solutions; a GoalHolder holds the state of what the user
 * wishes to calculate, being either the existence of, the number of solutions, or individual or multiple values
 * of one particular variable or of all vars.
 * This object will launch the solver only for methods exists() or count(). For other
 * methods it just returns instances of SolutionHolder which further delay the execution.
 */
public class GoalHolder {
    final PrologReferenceImplementation prolog;

    final Object goal;

    public GoalHolder(PrologReferenceImplementation prolog, Object theGoal) {
        this.prolog = prolog;
        this.goal = theGoal;
    }

    public boolean exists() {
        final RangeSolutionListener listener = new RangeSolutionListener();
        listener.setMaxCount(2); // We won't get there - but we don't want to put a max to 1 otherwise an Exception will be thrown
        listener.setMaxFetch(1);
        prolog.getSolver().solveGoal(goal, listener);
        return listener.getNbSolutions()>=1;
    }

    public long count() {
        final CountingSolutionListener listener = new CountingSolutionListener();
        prolog.getSolver().solveGoal(goal, listener);
        return listener.getCounter();
    }

    /**
     * @return Solution to the whole goal. If the goal was a(X), will return a(1), a(2), etc.
     */
    public SolutionHolder<Object> solution() {
        return new SolutionHolder<Object>(this, Var.WHOLE_SOLUTION_VAR_NAME, Object.class);
    }

     /**
     * Seek solutions for only one variable of the goal, of the desired type. Does not yet execute the goal.
     * @param varName The name of the variable to solve for.
     * @param desiredTypeOfResult
     * @param <T>
     * @return A SolutionHolder for only the specified variable.
     */
    public <T> SolutionHolder<T> var(String varName, Class<? extends T> desiredTypeOfResult) {
        final SolutionHolder<T> solutionHolder = new SolutionHolder<T>(this, varName, desiredTypeOfResult);
        return solutionHolder;
    }

    /**
     * Seek solutions for onle one variable of the goal, of any type.
     * @param varName The name of the variable to solve for.
     * @return A SolutionHolder for only the specified variable.
     */
    public SolutionHolder<Object> var(String varName) {
        return var(varName, Object.class);
    }

    public SolutionHolder<Map<Var<?>, Object>> vars() {
        return new SolutionHolder<Map<Var<?>, Object>>(this);
    }

    // ---------------------------------------------------------------------------
    // Syntactic sugars
    // ---------------------------------------------------------------------------


    public Object intValue(String varName) {
        return var(varName, Integer.class).unique();
    }

    public String toString(String varName) {
        return var(varName).unique().toString();
    }
}
