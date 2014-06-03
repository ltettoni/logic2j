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

import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.extractor.SolutionExtractor;
import org.logic2j.core.api.unify.UnifyContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link SolutionListener} that will count and limit
 * the number of solutions generated, and possibly handle underflow or overflow.
 */
public class SingleVarSolutionListener<T> extends RangeSolutionListener<T> {
    private final SolutionExtractor<T> extractor;

    private final List<T> results;

    /**
     * Create a {@link SolutionListener} that will enumerate
     * solutions up to theMaxCount before aborting by "user request". We will usually
     * supply 1 or 2, see derived classes.
     */
    public SingleVarSolutionListener(SolutionExtractor<T> extractor) {
        this.extractor = extractor;
        this.results = new ArrayList<T>();
    }


    @Override
    public Continuation onSolution(UnifyContext currentVars) {
        results.add(extractor.extractSolution(currentVars));
        return super.onSolution(currentVars);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------


    public List<T> getResults() {
        return results;
    }
}
