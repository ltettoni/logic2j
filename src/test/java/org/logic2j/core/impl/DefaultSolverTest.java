package org.logic2j.core.impl;

import org.junit.Test;
import org.logic2j.core.LoggingAndCountingSolutionListener;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.api.monadic.StateEngineByLookup;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;

import static org.junit.Assert.*;

/**
 * Lowest-level tests of the Solver
 */
public class DefaultSolverTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultSolverTest.class);

    @Test
    public void atomUndefined() {
        final Object goal = unmarshall("undefined_atom");
        LoggingAndCountingSolutionListener listener = new LoggingAndCountingSolutionListener(goal);
        getProlog().getSolver().solveGoal(goal, new StateEngineByLookup().emptyPoV(), listener);
        listener.report();
        assertEquals(0, listener.getCounter());
    }

    @Test
    public void primitiveTrue() {
        final Object goal = unmarshall("true");
        LoggingAndCountingSolutionListener listener = new LoggingAndCountingSolutionListener(goal);
        getProlog().getSolver().solveGoal(goal, new StateEngineByLookup().emptyPoV(), listener);
        listener.report();
        assertEquals(1, listener.getCounter());
    }


    @Test
    public void primitiveFail() {
        final Object goal = unmarshall("fail");
        LoggingAndCountingSolutionListener listener = new LoggingAndCountingSolutionListener(goal);
        getProlog().getSolver().solveGoal(goal, new StateEngineByLookup().emptyPoV(), listener);
        listener.report();
        assertEquals(0, listener.getCounter());
    }


    @Test
    public void primitiveCut() {
        final Object goal = unmarshall("!");
        LoggingAndCountingSolutionListener listener = new LoggingAndCountingSolutionListener(goal);
        getProlog().getSolver().solveGoal(goal, new StateEngineByLookup().emptyPoV(), listener);
        listener.report();
        assertEquals(1, listener.getCounter());
    }

}