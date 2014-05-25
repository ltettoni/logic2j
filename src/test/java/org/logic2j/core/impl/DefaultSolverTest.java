package org.logic2j.core.impl;

import org.junit.Test;
import org.logic2j.core.ExtractingSolutionListener;
import org.logic2j.core.PrologTestBase;

import static org.junit.Assert.*;

/**
 * Lowest-level tests of the Solver: check core primitives: true, fail, cut, and, or. Check basic unification.
 * See other test classes for testing the solver against theories.
 */
public class DefaultSolverTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultSolverTest.class);

    // ---------------------------------------------------------------------------
    // Simplest primitives and undefined goal
    // ---------------------------------------------------------------------------

    @Test
    public void primitiveFail() {
        final Object goal = unmarshall("fail");
        final long nbSolutions = solveWithLoggingAndCountingListener(goal);
        assertEquals(0, nbSolutions);
    }


    @Test
    public void primitiveTrue() {
        final Object goal = unmarshall("true");
        final long nbSolutions = solveWithLoggingAndCountingListener(goal);
        assertEquals(1, nbSolutions);
    }

    @Test
    public void primitiveCut() {
        final Object goal = unmarshall("!");
        final long nbSolutions = solveWithLoggingAndCountingListener(goal);
        assertEquals(1, nbSolutions);
    }

    @Test
    public void atomUndefined() {
        final Object goal = unmarshall("undefined_atom");
        final long nbSolutions = solveWithLoggingAndCountingListener(goal);
        assertEquals(0, nbSolutions);
    }


    @Test
    public void primitiveTrueAndTrue() {
        final Object goal = unmarshall("true,true");
        final long nbSolutions = solveWithLoggingAndCountingListener(goal);
        assertEquals(1, nbSolutions);
    }


    @Test
    public void primitiveTrueOrTrue() {
        final Object goal = unmarshall("true;true");
        final long nbSolutions = solveWithLoggingAndCountingListener(goal);
        assertEquals(2, nbSolutions);
    }

    // ---------------------------------------------------------------------------
    // Basic unification
    // ---------------------------------------------------------------------------


    @Test
    public void unifyLiteralsNoSolution() {
        final Object goal = unmarshall("a=b");
        final long nbSolutions = solveWithLoggingAndCountingListener(goal);
        assertEquals(0, nbSolutions);
    }


    @Test
    public void unifyLiteralsOneSolution() {
        final Object goal = unmarshall("c=c");
        final long nbSolutions = solveWithLoggingAndCountingListener(goal);
        assertEquals(1, nbSolutions);
    }



    @Test
    public void unifyAnonymousToAnonymous() {
        final Object goal = unmarshall("_=_");
        final long nbSolutions = solveWithLoggingAndCountingListener(goal);
        assertEquals(1, nbSolutions);
    }


    @Test
    public void unifyVarToLiteral() {
        final Object goal = unmarshall("Q=d");
        final ExtractingSolutionListener listener = solveWithExtractingListener(goal);
        assertEquals(1, listener.getCounter());
        assertEquals("[Q]", listener.getVariables().toString());
        assertEquals("[d = d]", marshall(listener.getValues(".")));
        assertEquals("[d]", marshall(listener.getValues("Q")));
    }

    @Test
    public void unifyVarToAnonymous() {
        final Object goal = unmarshall("Q=_");
        final ExtractingSolutionListener listener = solveWithExtractingListener(goal);
        assertEquals(1, listener.getCounter());
        assertEquals("[Q]", listener.getVariables().toString());
        assertEquals("[Q = _]", marshall(listener.getValues(".")));
        assertEquals("[Q]", marshall(listener.getValues("Q")));
    }


    @Test
    public void unifyVarToVar() {
        final Object goal = unmarshall("Q=Z");
        final ExtractingSolutionListener listener = solveWithExtractingListener(goal);
        assertEquals(1, listener.getCounter());
        assertEquals("[., Q, Z]", listener.getVarNames().toString());
        assertEquals("[Z = Z]", marshall(listener.getValues(".")));
        assertEquals("[Z]", marshall(listener.getValues("Q")));
        assertEquals("[Z]", marshall(listener.getValues("Z")));
    }
}