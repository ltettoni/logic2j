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

import java.util.Map;

import org.logic2j.core.api.model.Solution;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.impl.util.ReflectUtils;

/**
 * A relay object to provide access to the results of the (expected) unique solution to a goal.
 * TODO Should it be templated to the type of the solution?
 */
public class UniqueSolutionHolder {

    private final Solution solution;

    /**
     * @param theSolution
     */
    UniqueSolutionHolder(Solution theSolution) {
        this.solution = theSolution;
    }

    /**
     * @param theVariableName
     * @return The value of var theVariableName
     * @see Solution#getBinding(String)
     */
    public Object binding(String theVariableName) {
        return this.solution.getBinding(theVariableName);
    }

    /**
     * @param theVariableName
     * @param theTargetClass
     */
    public <T> T binding(String theVariableName, Class<T> theTargetClass) {
        final Object t = binding(theVariableName);
        return ReflectUtils.safeCastNotNull("extracting binding for variable " + theVariableName, t, theTargetClass);
    }

    /**
     * @return All bindings.
     */
    public Map<String, Object> bindings() {
        return this.solution.getBindings();
    }

    /**
     * @return The solution as a {@link Term}.
     */
    public Object solution() {
        return this.solution.getSolution();
    }

}