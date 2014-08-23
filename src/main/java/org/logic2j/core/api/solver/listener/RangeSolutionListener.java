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

import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.model.exception.MissingSolutionException;
import org.logic2j.core.api.model.exception.TooManySolutionsException;
import org.logic2j.core.api.unify.UnifyContext;

import java.util.List;

/**
 * A {@link SolutionListener} that will count and limit
 * the number of solutions generated, and possibly handle underflow or overflow.
 */
public class RangeSolutionListener<T> extends SolutionListenerBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RangeSolutionListener.class);
    private static final boolean isDebug = logger.isDebugEnabled();


    private long minCount; // Minimal number of solutions allowed
    private long maxCount; // Maximal number of solutions allowed
    private long maxFetch; // Stop generating after this number of solutions

    /**
     * Current solution counter (number of times {@link #onSolution(org.logic2j.core.api.unify.UnifyContext)} was called)
     */
    protected long counter;

    /**
     * Create a {@link SolutionListener} that will enumerate
     * solutions up to theMaxCount before aborting by "user request". We will usually
     * supply 1 or 2, see derived classes.
     */
    public RangeSolutionListener() {
        this.counter = 0;
        this.minCount = 0;
        this.maxCount = Long.MAX_VALUE;
        this.maxFetch = Long.MAX_VALUE;
    }


    @Override
    public Integer onSolution(UnifyContext currentVars) {
        this.counter++;
        if (this.counter > this.maxCount) {
            // OOps, we already had solutions? This is not desired
            onSuperfluousSolution();
        }
        if (isDebug) {
            logger.debug(" >>>>>>>>> onSolution() #{}", this.counter);
        }
        final Integer continuation = this.counter < this.maxFetch ? Continuation.CONTINUE : Continuation.USER_ABORT;
        return continuation;
    }

    public void checkRange() {
        if (this.counter < this.minCount) {
            onMissingSolution();
        }
    }

    /**
     * Handle the case of more than the maximum number of solutions detected.
     */
    protected void onMissingSolution(){
        // TODO would be really useful to have some context information here, eg. the goal we are trying to solve...
        throw new MissingSolutionException("No solution found by " + this + ", when exactly one was required");

    }

    /**
     * Handle the case of more than the maximum number of solutions detected.
     */
    protected void onSuperfluousSolution() {
        // TODO would be really useful to have some context information here, eg. the goal we are trying to solve...
        throw new TooManySolutionsException("More than " + maxCount + " solution(s) found by " + this + ", got at least " + this.counter);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    /**
     * @param minCount
     */
    public void setMinCount(long minCount) {
        this.minCount = minCount;
    }

    /**
     * @param maxCount The maximal number of solutions to ask the inference engine; specify 1 to only look for the first, irrelevant
     *            whether there might be others; specify 2 to if you want the first and make sure there are no others (the inference engine
     *            will try to continue after the first).
     */
    public void setMaxCount(long maxCount) {
        this.maxCount = maxCount;
    }

    public void setMaxFetch(long maxFetch) {
        this.maxFetch = maxFetch;
    }

    /**
     * @return The number of solutions found
     */
    public long getNbSolutions() {
        return this.counter;
    }

    public List<T> getResults() {
        throw new UnsupportedOperationException("Feature not yet implemented");
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '[' + this.minCount + ".." + this.maxCount + ']';
    }

}
