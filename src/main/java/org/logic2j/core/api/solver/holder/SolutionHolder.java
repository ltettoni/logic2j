package org.logic2j.core.api.solver.holder;

import org.logic2j.core.api.model.exception.MissingSolutionException;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.listener.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Laurent on 26.05.2014.
 */
public class SolutionHolder<T> {
    private static final Logger logger = LoggerFactory.getLogger(SolutionHolder.class);

    private final GoalHolder goalHolder;

    private final boolean multiVar;

    private final Var[] vars;

    private final String varName;

    private final RangeSolutionListener rangeListener;

    /**
     * Extract one variable or the solution to the goal.
     *
     * @param goalHolder
     * @param varName
     */
    public SolutionHolder(GoalHolder goalHolder, String varName) {
        this.multiVar = false;
        this.varName = varName;
        this.goalHolder = goalHolder;
        if (Var.WHOLE_SOLUTION_VAR_NAME.equals(varName)) {
            this.vars = new Var[]{Var.WHOLE_SOLUTION_VAR};
        } else {
            final Var found = TermApi.findVar(this.goalHolder.goal, varName);
            if (found == null) {
                throw new MissingSolutionException("No var named \"" + varName + "\" in term " + this.goalHolder.goal);
            }
            this.vars = new Var[]{found};
        }
        rangeListener = new SingleVarSolutionListener(new SingleVarExtractor(goalHolder.prolog, goalHolder.goal, varName));
    }

    /**
     * Extract all variables.
     *
     * @param goalHolder
     */
    public SolutionHolder(GoalHolder goalHolder) {
        this.multiVar = true;
        this.varName = null;
        this.goalHolder = goalHolder;
        this.vars = TermApi.allVars(this.goalHolder.goal).keySet().toArray(new Var[]{});
        rangeListener = new MultiVarSolutionListener(new MultiVarExtractor(goalHolder.prolog, goalHolder.goal));
    }

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


    public List<T> list() {
        solveAndCheckRanges();
        return (List<T>) rangeListener.getResults();
    }


    public <T> T[] array(T[] ts) {
        return list().toArray(ts);
    }

    private void initListenerRangesAndSolve(int minCount, long maxCount, long maxFetch) {
        this.rangeListener.setMinCount(minCount);
        this.rangeListener.setMaxCount(maxCount);
        this.rangeListener.setMaxFetch(maxFetch);
        solveAndCheckRanges();
    }

    private void solveAndCheckRanges() {
        this.goalHolder.prolog.getSolver().solveGoal(this.goalHolder.goal, this.rangeListener);
        this.rangeListener.checkRange();
    }


    /**
     * Launch the prolog engine in a separate thread to produce solutions while the main caller can consume {@link org.logic2j.core.api.model.Solution}s from this
     * {@link java.util.Iterator} at its own pace. This uses the {@link org.logic2j.core.api.solver.listener.IterableSolutionListener}.
     *
     * @return An iterator for all solutions.
     */
    public Iterator<Object> iterator() {
        final IterableSolutionListener listener = new IterableSolutionListener(SolutionHolder.this.goalHolder.goal);

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

        return new Iterator<Object>() {

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
            public Object next() {
                if (this.solution == null) {
                    throw new PrologNonSpecificError("Program error: next() called when either hasNext() did not return true previously, or next() was called more than once");
                }
                final Object toReturn = this.solution;
                // Indicate that we have just "consumed" the solution, and any subsequent call to next() without first calling hasNext()
                // will fail.
                this.solution = null;
                return toReturn;
            }

            @Override
            public void remove() {
                throw new PrologNonSpecificError("iterator() provides a read-only Term interator, cannot remove elements");
            }

        };
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + this.goalHolder.goal + ')';
    }

}
