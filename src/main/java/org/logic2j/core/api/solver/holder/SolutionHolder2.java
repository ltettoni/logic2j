package org.logic2j.core.api.solver.holder;

import org.logic2j.core.api.model.Solution;
import org.logic2j.core.api.model.exception.MissingSolutionException;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.StateEngineByLookup;
import org.logic2j.core.api.solver.listener.IterableSolutionListener;
import org.logic2j.core.api.solver.listener.MultiVarSolutionListener;
import org.logic2j.core.api.solver.listener.RangeSolutionListener;
import org.logic2j.core.api.solver.listener.SingleVarSolutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Laurent on 26.05.2014.
 */
public class SolutionHolder2<T> {
    private static final Logger logger = LoggerFactory.getLogger(SolutionHolder2.class);

    private final GoalHolder goalHolder;

    private final boolean multiVar;

    private final Var[] vars;

    private final String varName;

    /**
     * Extract one variable or the solution to the goal.
     * @param goalHolder
     * @param varName
     */
    public SolutionHolder2(GoalHolder goalHolder, String varName) {
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
    }

    /**
     * Extract all variables.
     * @param goalHolder
     */
    public SolutionHolder2(GoalHolder goalHolder) {
        this.multiVar = true;
        this.varName = null;
        this.goalHolder = goalHolder;
        this.vars = TermApi.allVars(this.goalHolder.goal).keySet().toArray(new Var[]{});
    }

    public T single() {
        if (multiVar) {
            final MultiVarSolutionListener listener = new MultiVarSolutionListener(goalHolder.prolog, goalHolder.goal);
            initListenerRangesAndSolve(listener, 0, 1);
            return (T) listener.getResults().get(0);
        } else {
            final SingleVarSolutionListener listener = new SingleVarSolutionListener(goalHolder.prolog, goalHolder.goal, varName);
            initListenerRangesAndSolve(listener, 0, 1);
            return (T) listener.getResults().get(0);
        }
    }

    public T unique() {
        if (multiVar) {
            final MultiVarSolutionListener listener = new MultiVarSolutionListener(goalHolder.prolog, goalHolder.goal);
            initListenerRangesAndSolve(listener, 1, 1);
            return (T) listener.getResults().get(0);
        } else {
            final SingleVarSolutionListener listener = new SingleVarSolutionListener(goalHolder.prolog, goalHolder.goal, varName);
            initListenerRangesAndSolve(listener, 1, 1);
            return (T) listener.getResults().get(0);
        }
    }


    public List<T> list() {
        if (multiVar) {
            final MultiVarSolutionListener listener = new MultiVarSolutionListener(goalHolder.prolog, goalHolder.goal);
            initListenerRangesAndSolve(listener, 0, Long.MAX_VALUE);
            return (List<T>) listener.getResults();
        } else {
            final SingleVarSolutionListener listener = new SingleVarSolutionListener(goalHolder.prolog, goalHolder.goal, varName);
            initListenerRangesAndSolve(listener, 0, Long.MAX_VALUE);
            return (List<T>) listener.getResults();
        }
    }

    private void initListenerRangesAndSolve(RangeSolutionListener listener, int min, long max) {
        listener.setMinCount(min);
        listener.setMaxCount(max);
        this.goalHolder.prolog.getSolver().solveGoal(this.goalHolder.goal, new StateEngineByLookup().emptyPoV(), listener);
        listener.checkRange();
    }


    /**
     * Launch the prolog engine in a separate thread to produce solutions while the main caller can consume {@link org.logic2j.core.api.model.Solution}s from this
     * {@link java.util.Iterator} at its own pace. This uses the {@link org.logic2j.core.api.solver.listener.IterableSolutionListener}.
     *
     * @return An iterator for all solutions.
     */
    public Iterator<Object> iterator() {
        final IterableSolutionListener listener = new IterableSolutionListener(SolutionHolder2.this.goalHolder.goal);

        final Runnable prologSolverThread = new Runnable() {

            @Override
            public void run() {
                logger.debug("Started producer (prolog solver engine) thread");
                // Start solving in a parallel thread, and rush to first solution (that will be called back in the listener)
                // and will wait for the main thread to extract it
                SolutionHolder2.this.goalHolder.prolog.getSolver().solveGoal(SolutionHolder2.this.goalHolder.goal, new StateEngineByLookup().emptyPoV(), listener);
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

}
