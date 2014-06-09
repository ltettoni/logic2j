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

import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.solver.extractor.MultiVarExtractor;
import org.logic2j.core.api.solver.extractor.SingleVarExtractor;
import org.logic2j.core.api.solver.extractor.SolutionExtractor;
import org.logic2j.core.api.solver.listener.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Launch the solver with appropriate SolutionListener o obtain what the user asks:
 * cardinality (single-value or multi-valued)
 * obligation (optional, mandatory result, at-least or at-most values)
 * modality (iterable or all-in-memory)
 * storage (List or array)
 */
public class SolutionHolder<T> implements Iterable<T> {
    private static final Logger logger = LoggerFactory.getLogger(SolutionHolder.class);

    private final GoalHolder goalHolder;

    private RangeSolutionListener rangeListener;

    private long minNbr = 0;

    private long maxNbr = Long.MAX_VALUE - 1; // Keep possibility to add one because the RangeSolutionListener "fetch" property

    // I think this could be clarified a little - those are impl of the same interface, but one is generic the other not
    private final SingleVarExtractor<T> singleVarExtractor;

    private final MultiVarExtractor multiVarExtractor;

    /**
     * Extract one particular variable or the solution to the goal.
     *  @param goalHolder
     * @param varName
     * @param desiredTypeOfResult
     */
    public SolutionHolder(GoalHolder goalHolder, String varName, Class<? extends T> desiredTypeOfResult) {
        this.goalHolder = goalHolder;
        this.singleVarExtractor = new SingleVarExtractor<T>(goalHolder.goal, varName, desiredTypeOfResult);
        this.singleVarExtractor.setTermAdapter(goalHolder.prolog.getTermAdapter());
        this.multiVarExtractor = null;
    }

    /**
     * Extract all variables.
     *
     * @param goalHolder
     */
    public SolutionHolder(GoalHolder goalHolder) {
        this.goalHolder = goalHolder;
        this.singleVarExtractor = null;
        this.multiVarExtractor = new MultiVarExtractor(goalHolder.goal);
    }

    /**
     * For cloning.
     * @param goalHolder
     * @param rangeListener
     * @param singleVarExtractor
     * @param multiVarExtractor
     */
    private SolutionHolder(GoalHolder goalHolder, RangeSolutionListener rangeListener, SingleVarExtractor<T> singleVarExtractor, MultiVarExtractor multiVarExtractor) {
        this.goalHolder = goalHolder;
        this.rangeListener = rangeListener;
        this.singleVarExtractor = singleVarExtractor;
        this.multiVarExtractor = multiVarExtractor;
    }

    /**
     * Copy constructor
     *
     * @param original
     */
    public <Tgt> SolutionHolder(SolutionHolder<T> original, Class<Tgt> targetClass) {
        this(original.goalHolder, original.rangeListener, original.singleVarExtractor, original.multiVarExtractor);
    }

    // ---------------------------------------------------------------------------
    // Scalar extractors (zero or one solution)
    // ---------------------------------------------------------------------------

    /**
     * @return The only solution or null if none, but will throw an Exception if more than one.
     */
    public T single() {
        initListenerRangesAndSolve(0, 1, 2);
        if (rangeListener.getNbSolutions() == 0) {
            return null;
        }
        return (T) rangeListener.getResults().get(0);
    }

    /**
     * @return The first solution, or null if none. Will not generate any further - there may be or not - you won't notice.
     */
    public T first() {
        initListenerRangesAndSolve(0, 1, 1);
        if (rangeListener.getNbSolutions() == 0) {
            return null;
        }
        return (T) rangeListener.getResults().get(0);
    }

    /**
     * @return Single and only solution. Will throw an Exception if zero or more than one.
     */
    public T unique() {
        initListenerRangesAndSolve(1, 1, 2);
        return (T) rangeListener.getResults().get(0);
    }

    // ---------------------------------------------------------------------------
    // Type conversion
    // ---------------------------------------------------------------------------


    // ---------------------------------------------------------------------------
    // Vectorial extractors (collections, arrays, iterables)
    // ---------------------------------------------------------------------------

    public List<T> list() {
        initListenerRangesAndSolve(this.minNbr, this.maxNbr, this.maxNbr + 1);
        return (List<T>) rangeListener.getResults();
    }


    public <ArrayType> ArrayType[] array(ArrayType[] destinationArray) {
        return list().toArray(destinationArray);
    }


    /**
     * Launch the prolog engine in a separate thread to produce solutions while the main caller can consume
     * from this {@link java.util.Iterator} at its own pace.
     * This uses the {@link org.logic2j.core.api.solver.listener.IterableSolutionListener}.
     * Note: there is no bounds checking when using iterator()
     *
     * @return An iterator for all solutions.
     */
    public Iterator<T> iterator() {
        SolutionExtractor<?> effectiveExtractor;
        if (SolutionHolder.this.singleVarExtractor != null) {
            effectiveExtractor = SolutionHolder.this.singleVarExtractor;
        } else {
            effectiveExtractor = SolutionHolder.this.multiVarExtractor;
        }
        final IterableSolutionListener listener = new IterableSolutionListener(effectiveExtractor);

        final Runnable prologSolverThread = new Runnable() {

            @Override
            public void run() {
                logger.debug("Started producer (prolog solver engine) thread");
                // Start solving in a parallel thread, and rush to first solution (that will be called back in the listener)
                // and will wait for the main thread to extract it
                SolutionHolder.this.goalHolder.prolog.getSolver().solveGoal(SolutionHolder.this.goalHolder.goal, listener);
                logger.debug("Producer (prolog solver engine) thread finishes");
                // Last solution was extracted. Producer's callback won't now be called any more - so to
                // prevent the consumer for listening forever for the next solution that won't come...
                // We wait from a last notify from our client
                listener.clientToEngineInterface().waitUntilAvailable();
                // And we tell it we are aborting. No solution transferred for this last "hang up" message
                listener.engineToClientInterface().wakeUp();
                // Notice the 2 lines above are exactly the sames as those in the listener's onSolution()
            }
        };
        new Thread(prologSolverThread).start();

        return new Iterator<T>() {

            private Object solution;

            @Override
            public boolean hasNext() {
                // Now ask engine to run...
                listener.clientToEngineInterface().wakeUp();
                // And wait for a solution. Store it in any case we need it in next()
                this.solution = listener.engineToClientInterface().waitUntilAvailable();
                // Did it get one?
                return this.solution != null;
            }

            @Override
            public T next() {
                if (this.solution == null) {
                    throw new PrologNonSpecificError("Program error: next() called when either hasNext() did not return true previously, or next() was called more than once");
                }
                final Object toReturn = this.solution;
                // Indicate that we have just "consumed" the solution, and any subsequent call to next() without first calling hasNext()
                // will fail.
                this.solution = null;
                return (T) toReturn;
            }

            @Override
            public void remove() {
                throw new PrologNonSpecificError("iterator() provides a read-only Term iterator, cannot remove elements");
            }

        };
    }

    // ---------------------------------------------------------------------------
    // Enforcement of cardinality
    // ---------------------------------------------------------------------------

    /**
     * Specify that the number of solutions to be extracted by this SolutionHolder
     * must be exactly as specified
     *
     * @param expectedNumberOfSolutions
     * @return
     * @note Does not apply to #iterator().
     */
    public SolutionHolder<T> exactly(int expectedNumberOfSolutions) {
        return atLeast(expectedNumberOfSolutions).atMost(expectedNumberOfSolutions);
    }


    /**
     * Specify that the number of solutions to be extracted by this SolutionHolder
     * must at be at least minimalNumberOfSolutions.
     *
     * @param minimalNumberOfSolutions
     * @return this instance
     * @note Does not apply to #iterator().
     */
    public SolutionHolder<T> atLeast(int minimalNumberOfSolutions) {
        this.minNbr = minimalNumberOfSolutions;
        return this;
    }

    /**
     * Specify that the number of solutions to be extracted by this SolutionHolder
     * must at be at most minimalNumberOfSolutions.
     *
     * @param maximalNumberOfSolutions
     * @return this instance
     * @note Does not apply to #iterator().
     */
    public SolutionHolder<T> atMost(int maximalNumberOfSolutions) {
        this.maxNbr = maximalNumberOfSolutions;
        return this;
    }

    // ---------------------------------------------------------------------------
    // Support methods
    // ---------------------------------------------------------------------------

    private void initListenerRangesAndSolve(long minCount, long maxCount, long maxFetch) {
        if (this.singleVarExtractor!=null) {
            this.rangeListener = new SingleVarSolutionListener(this.singleVarExtractor);
        } else {
            assert this.multiVarExtractor != null : "neither single nor multiple var extractor";
            this.rangeListener = new MultiVarSolutionListener(this.multiVarExtractor);
        }
        this.rangeListener.setMinCount(minCount);
        this.rangeListener.setMaxCount(maxCount);
        this.rangeListener.setMaxFetch(maxFetch);
        solveAndCheckRanges();
    }

    private void solveAndCheckRanges() {
        this.goalHolder.prolog.getSolver().solveGoal(this.goalHolder.goal, this.rangeListener);
        this.rangeListener.checkRange();
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + this.goalHolder.goal + ')';
    }

}
