package org.logic2j.core.api.solver.holder;

import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.StateEngineByLookup;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.logic2j.core.api.solver.listener.RangeSolutionListener;
import org.logic2j.core.impl.PrologReferenceImplementation;

import java.util.Map;

/**
 * Created by Laurent on 26.05.2014.
 */
public class GoalHolder {
    final PrologReferenceImplementation prolog;

    final Object goal;

    public GoalHolder(PrologReferenceImplementation prolog, Object theGoal) {
        this.prolog = prolog;
        this.goal = theGoal;
    }

    public boolean exists() {
        final RangeSolutionListener listener = new RangeSolutionListener() {
            @Override
            protected void onSuperfluousSolution() {
                // Nothing wrong having more solutions - we just ignore them
            }
        };
        listener.setMaxCount(1);
        prolog.getSolver().solveGoal(goal, new StateEngineByLookup().emptyPoV(), listener);
        return listener.getNbSolutions()>=1;
    }

    public long count() {
        final CountingSolutionListener listener = new CountingSolutionListener();
        prolog.getSolver().solveGoal(goal, new StateEngineByLookup().emptyPoV(), listener);
        return listener.getCounter();
    }

    public SolutionHolder<Object> solution() {
        return new SolutionHolder<Object>(this, Var.WHOLE_SOLUTION_VAR_NAME);
    }

    public <T> SolutionHolder<T> var(String varName, Class<? extends T> desiredTypeOfResult) {
        return new SolutionHolder<T>(this, varName);
    }

    public SolutionHolder<Object> var(String varName) {
        return var(varName, Object.class);
    }

    public SolutionHolder<Map<Var, Object>> vars() {
        return new SolutionHolder<Map<Var, Object>>(this);
    }

    public GoalHolder exactCount(int exactNumberExpected) {
        return this;
    }

    // ---------------------------------------------------------------------------
    // Syntactic sugars
    // ---------------------------------------------------------------------------


    public Object longValue(String varName) {
        return var(varName, Long.class).unique();
    }

    public String toString(String varName) {
        return var(varName).unique().toString();
    }
}
