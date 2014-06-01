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

import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.unify.UnifyContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link org.logic2j.core.api.SolutionListener} that will count and limit
 * the number of solutions generated, and possibly handle underflow or overflow.
 */
public class MultiVarSolutionListener extends RangeSolutionListener {
    private final SolutionExtractor<Map<Var, Object>> extractor;
    private final List<Map<Var, Object>> results;

    /**
     * Create a {@link org.logic2j.core.api.SolutionListener} that will enumerate
     * solutions up to theMaxCount before aborting by "user request". We will usually
     * supply 1 or 2, see derived classes.
     */
    public MultiVarSolutionListener(SolutionExtractor<Map<Var, Object>> extractor) {
        this.extractor = extractor;
        this.results = new ArrayList<Map<Var, Object>>();
    }


    @Override
    public Continuation onSolution(UnifyContext currentVars) {
        results.add(extractor.extractSolution(currentVars));
        return super.onSolution(currentVars);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------


    public List<Map<Var, Object>> getResults() {
        return results;
    }


}
