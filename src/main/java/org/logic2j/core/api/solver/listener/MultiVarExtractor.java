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

import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.PrologImplementation;

import java.util.HashMap;
import java.util.Map;

public class MultiVarExtractor implements SolutionExtractor<Map<Var, Object>> {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MultiVarExtractor.class);

    private final PrologImplementation prolog;

    private final Object goal;

    private final Var[] vars;

    public MultiVarExtractor(PrologImplementation prolog, Object goal) {
        this.prolog = prolog;
        this.goal = goal;
        this.vars = TermApi.allVars(goal).keySet().toArray(new Var[]{});
    }


    @Override
    public Map<Var, Object> extractSolution(UnifyContext currentVars) {
        final Map<Var, Object> result = new HashMap<Var, Object>();
        for (Var v : vars) {
            final Object value;
            value = currentVars.reify(v);
            result.put(v, value);
        }
        return result;
    }
}
