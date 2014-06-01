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
package org.logic2j.core.api.model;

import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.unify.UnifyContext;

import java.util.Map;

/**
 * Describes one of the solution(s) to a goal; this includes the resolved {@link Term} (with all bound variables substituted to their actual
 * values - only free variables remaining), and all variable varValues exposed.<br/>
 * If the goal to be solved was
 * g(X, a(Y, Z)) with X already bound to 2, and Z bound to 3 when the goal was solved, then Solution provides: {@link #getSolution()}==g(2,
 * a(Y, 3)) , {@link #getBindings()}=={X -> 2, Y -> null, Z -> 3} , {@link #getBinding(String)}==2 when argument is "X".
 */
public class Solution {

    /**
     * @see #getSolution()
     */
    private final Object solution;

    /**
     * @see #getBindings()
     */
    private final Map<String, Object> varValues;

    /**
     * Build a solution for the current variable varValues. This will calculate the substituted value of bound variables, i.e. "denormalize"
     * the result and store all varValues as explicit denormalized terms.
     * 
     * @note This costs a little: in logic2j the solver is very efficient, but extracting results is a little more costly. This is caused by
     *       the shared-structures approach.
     * @param currentVars
     */
    public Solution(Object theTerm, UnifyContext currentVars) {
        this.solution = currentVars.reify(theTerm);
        this.varValues = currentVars.bindings(theTerm);
    }

    public Solution(Object substituted, Map<String, Object> varValues) {
        this.solution = substituted;
        this.varValues = varValues;
    }

    // ---------------------------------------------------------------------------
    // Methods
    // ---------------------------------------------------------------------------

    /**
     * @return The solution to a goal, expressed as the goal itself (with all bound variables substituted to their actual values - only free
     *         variables remaining).
     */
    public Object getSolution() {
        return this.solution;
    }

    /**
     * @return All varValues, a {@link java.util.Map} of Terms by their {@link Var}iable name.
     */
    public Map<String, Object> getBindings() {
        return this.varValues;
    }

    /**
     * @return The actual value bound to a {@link Var}iable by its name.
     */
    public Object getBinding(String theVariableName) {
        if (this.varValues == null) {
            throw new IllegalArgumentException("No varValues");
        }
        if (!this.varValues.containsKey(theVariableName)) {
            throw new IllegalArgumentException("No variable named \"" + theVariableName + "\" or variable is not bound");
        }
        final Object val = this.varValues.get(theVariableName);
        return val;
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ':' + getSolution() + ", " + getBindings();
    }

}
