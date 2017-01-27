/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.core.api.solver.extractor;

import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.unify.UnifyContext;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link org.logic2j.core.api.solver.extractor.SolutionExtractor} that will extract values of
 * a set of variables, returned as a Map. Typically used to find all bindings of a multi-variable goal, but not the
 * very efficient way (lots of memory and CPU required for large result sets).
 */
public class MapExtractor implements SolutionExtractor<Map<Var, Object>> {

    private final Var<?>[] vars;

    public MapExtractor(Object goal) {
        final Var<?>[] distinctVars = TermApi.distinctVars(goal);
        // Actually we don't need to clone:  this.vars = Arrays.copyOf(distinctVars, distinctVars.length);
        this.vars = distinctVars;
    }


    /**
     * @param currentVars
     * @return Actually a HashMap, meaning there is no particular order in the Var keys.
     */
    @Override
    public Map<Var, Object> extractSolution(UnifyContext currentVars) {
        final Map<Var, Object> result = new HashMap<Var, Object>();
        for (Var<?> var : vars) {
            final Object value = currentVars.reify(var);
            result.put(var, value);
        }
        return result;
    }
}
