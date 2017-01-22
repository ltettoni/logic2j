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
import org.logic2j.core.api.unify.UnifyContext;

/**
 * A base implementation of {@link SolutionListener} that holds a counter of the number of solutions reached.
 * The {@link #onSolution(org.logic2j.core.api.unify.UnifyContext)} method always returns Continuation.CONTINUE (dangerously allowing for potential
 * infinite generation). Derive from this class to ease the programming of
 * {@link SolutionListener}s in application code, and DO NOT FORGET to call super.onSolution() so that it will count!
 */
public class CountingSolutionListener extends SolutionListenerBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CountingSolutionListener.class);
    private static final boolean DEBUG_ENABLED = logger.isDebugEnabled();

    /**
     * Number of solutions (so far).
     */
    private long counter = 0;


    @Override
    public Integer onSolution(UnifyContext currentVars) {
        this.counter++;
        if (DEBUG_ENABLED) {
            logger.debug(" onSolution(#{})", this.counter);
        }
        return Continuation.CONTINUE;
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public long count() {
        return this.counter;
    }

    public boolean exists() {
        return this.counter > 0;
    }

}
