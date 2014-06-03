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
package org.logic2j.core.api.solver.extractor;

import org.logic2j.core.api.model.exception.MissingSolutionException;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.unify.UnifyContext;

/**
 * A {@link org.logic2j.core.api.solver.listener.SolutionListener} that will count and limit
 * the number of solutions generated, and possibly handle underflow or overflow.
 */
public class SingleVarExtractor<T> implements SolutionExtractor<T> {
    private final Object goal;

    private final Var var;

    /**
     * Create a {@link org.logic2j.core.api.solver.listener.SolutionListener} that will enumerate
     * solutions up to theMaxCount before aborting by "user request". We will usually
     * supply 1 or 2, see derived classes.
     */
    public SingleVarExtractor(Object goal, String varName) {
        this.goal = goal;
        final Var found = TermApi.findVar(goal, varName);
        if (found == null) {
            throw new MissingSolutionException("No var named \"" + varName + "\" in term " + goal);
        }
        this.var = found;
    }


    @Override
    public T extractSolution(UnifyContext currentVars) {
        if (var == Var.WHOLE_SOLUTION_VAR) {
            return (T)currentVars.reify(goal);
        } else {
            return (T)currentVars.reify(var);
        }
    }
}
