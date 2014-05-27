package org.logic2j.core.api.solver.holder;

import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.StateEngineByLookup;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.logic2j.core.api.solver.listener.FirstSolutionListener;
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
        final FirstSolutionListener listener = new FirstSolutionListener();
        prolog.getSolver().solveGoal(goal, new StateEngineByLookup().emptyPoV(), listener);
        return listener.getNbSolutions()>=1;
    }

    public long count() {
        final CountingSolutionListener listener = new CountingSolutionListener();
        prolog.getSolver().solveGoal(goal, new StateEngineByLookup().emptyPoV(), listener);
        return listener.getCounter();
    }

    public SolutionHolder2<Object> solution() {
        return new SolutionHolder2<Object>(this, Var.WHOLE_SOLUTION_VAR_NAME);
    }

    public <T> SolutionHolder2<T> var(String varName, Class<? extends T> desiredTypeOfResult) {
        return new SolutionHolder2<T>(this, varName);
    }

    public SolutionHolder2<Object> var(String varName) {
        return var(varName, Object.class);
    }

    public SolutionHolder2<Map<Var, Object>> vars() {
        return new SolutionHolder2<Map<Var, Object>>(this);
    }

    public GoalHolder ensureNumber(int exactNumberExpected) {
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
