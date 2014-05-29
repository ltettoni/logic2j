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
import org.logic2j.core.api.model.exception.MissingSolutionException;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.impl.PrologImplementation;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link org.logic2j.core.api.SolutionListener} that will count and limit
 * the number of solutions generated, and possibly handle underflow or overflow.
 */
public class SingleVarSolutionListener<T> extends RangeSolutionListener<T> {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SingleVarSolutionListener.class);

    private final SolutionExtractor<T> extractor;

    private final List<T> results;

    /**
     * Create a {@link org.logic2j.core.api.SolutionListener} that will enumerate
     * solutions up to theMaxCount before aborting by "user request". We will usually
     * supply 1 or 2, see derived classes.
     */
    public SingleVarSolutionListener(SolutionExtractor<T> extractor) {
        this.extractor = extractor;
        this.results = new ArrayList<T>();
    }


    @Override
    public Continuation onSolution(PoV pov) {
        results.add(extractor.extractSolution(pov));
        return super.onSolution(pov);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------


    public List<T> getResults() {
        return results;
    }
}
