package org.logic2j.core.api.solver.holder;

import org.logic2j.core.api.model.exception.MissingSolutionException;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.StateEngineByLookup;
import org.logic2j.core.api.solver.listener.MultiVarSolutionListener;
import org.logic2j.core.api.solver.listener.RangeSolutionListener;
import org.logic2j.core.api.solver.listener.SingleVarSolutionListener;

import java.util.List;

/**
 * Created by Laurent on 26.05.2014.
 */
public class SolutionHolder2<T> {
    private final GoalHolder goalHolder;

    private final boolean multiVar;

    private final Var[] vars;

    private final String varName;

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

    protected void initListenerRangesAndSolve(RangeSolutionListener listener, int min, long max) {
        listener.setMinCount(min);
        listener.setMaxCount(max);
        this.goalHolder.prolog.getSolver().solveGoal(this.goalHolder.goal, new StateEngineByLookup().emptyPoV(), listener);
        listener.checkRange();
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

}
