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

import java.util.Map;

import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.api.model.var.Bindings.FreeVarRepresentation;

/**
 * Describes one of the solution(s) to a goal; this includes the resolved {@link Term} (with all bound variables substituted to their actual
 * values - only free variables remaining), and all variable bindings exposed.<br/>
 * The internal storage is denormalized at the time the object is instantiated, see {@link #Solution(Bindings)}.
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
    private final Map<String, Object> bindings;

    /**
     * Build a solution for the current variable bindings. This will calculate the substituted value of bound variables, i.e. "denormalize"
     * the result and store all bindings as explicit denormalized terms.
     * 
     * @note This costs a little: in logic2j the solver is very efficient, but extracting results is a little more costly. This is caused by
     *       the shared-structures approach.
     * @note Maybe it could be more efficient to just clone the {@link Bindings} and then calculate the solution on demand?
     * @param theBindings
     */
    public Solution(Bindings theBindings) {
        this.solution = TermApi.substitute(theBindings.getReferrer(), theBindings, null);
        this.bindings = theBindings.explicitBindings(FreeVarRepresentation.NULL);
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
     * @return All bindings, a {@link Map} of Terms by their {@link Var}iable name.
     */
    public Map<String, Object> getBindings() {
        return this.bindings;
    }

    /**
     * @return The actual value bound to a {@link Var}iable by its name.
     */
    public Object getBinding(String theVariableName) {
        if (this.bindings == null) {
            throw new IllegalArgumentException("No bindings");
        }
        if (!this.bindings.containsKey(theVariableName)) {
            throw new IllegalArgumentException("No variable named \"" + theVariableName + "\" or variable is not bound");
        }
        return this.bindings.get(theVariableName);
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ':' + getSolution() + ", " + getBindings();
    }

}
