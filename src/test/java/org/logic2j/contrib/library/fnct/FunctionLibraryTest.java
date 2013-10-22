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

package org.logic2j.contrib.library.fnct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Bindings;

public class FunctionLibraryTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionLibraryTest.class);

    @Before
    public void setUp() {
        super.setUp();
        loadLibrary(new FunctionLibrary(this.prolog));
        loadTheoryFromTestResourcesDir("mapping.pl");
    };

    @Test
    public void placeholder() {
        //
    }

    @Test
    public void anonymousAndFreeVarsAreNotTransformed() {
        assertNotTransformed("_", false);
        assertNotTransformed("X", false); // Free var
    }

    @Test
    public void atomicNotTransformed() {
        assertNotTransformed("atom", false);
        assertNotTransformed("123", false);
        assertNotTransformed("123.456", false);
    }

    @Test
    public void atomicTransformed() {
        assertTransformed("t4", "t3", false);
        assertTransformed("one", "1", false);
    }

    @Test
    public void structNotTransformed() {
        assertNotTransformed("f(a,b,c)", false);
        assertNotTransformed("[1,2]", false);
    }

    @Test
    public void structTransformed() {
        Object[] termAndBindings;
        termAndBindings = assertTransformed("transformed(a)", "original(a)", false);
        termAndBindings = assertTransformed("transformed(X)", "original(X)", false);
        termAndBindings = assertTransformed("transformed(X)", "original(_)", false); // Dubious
        assertNotTransformed("transformed(X, Y)", false);
    }

    @Test
    public void mapNonIterative() {
        assertNotTransformed("t4", false);
        assertTransformed("t4", "t3", false);
        assertTransformed("t3", "t2", false);
        assertTransformed("t2", "t1", false);
    }

    @Test
    public void mapIterative() {
        assertNotTransformed("t4", true);
        assertTransformed("t4", "t3", true);
        assertTransformed("t4", "t2", true);
        assertTransformed("t4", "t1", true);
    }

    /**
     * @param termToParse
     */
    private void assertNotTransformed(String termToParse, boolean iterative) {
        final Object originalTerm = getProlog().getTermExchanger().unmarshall(termToParse);
        final Bindings originalBindings = new Bindings(originalTerm);
        logger.info("Instantiated term to transform: term={} , bindings={}", originalTerm, originalBindings);
        final Object[] termAndBindings = new Object[] { originalTerm, originalBindings };
        final boolean transform = iterative ? transformAll("map", termAndBindings) : transformOnce("map", termAndBindings);
        assertFalse(transform);
        assertSame(termAndBindings[0], originalTerm);
        assertSame(termAndBindings[1], originalBindings);
    }

    /**
     * @param string
     * @return
     */
    private Object[] assertTransformed(String toStringExpected, String termToParse, boolean iterative) {
        final Object originalTerm = getProlog().getTermExchanger().unmarshall(termToParse);
        final Bindings originalBindings = new Bindings(originalTerm);
        logger.debug("Instantiated term to transform: term={} , bindings={}", originalTerm, originalBindings);
        final Object[] termAndBindings = new Object[] { originalTerm, originalBindings };
        final boolean transform = iterative ? transformAll("map", termAndBindings) : transformOnce("map", termAndBindings);
        assertTrue(transform);
        String substituted = TermApi.substitute(termAndBindings[0], (Bindings) termAndBindings[1]).toString();
        String toString = termAndBindings[0].toString();
        assertEquals(toStringExpected, substituted);
        return termAndBindings;
    }

    /**
     * @param termAndBindings
     */
    public boolean transformAll(final String transformationPredicate, final Object[] termAndBindings) {
        boolean anyTransformed = false;
        boolean transformed;
        int iterationLimiter = 10;
        do {
            transformed = transformOnce(transformationPredicate, termAndBindings);
            anyTransformed |= transformed;
            iterationLimiter--;
        } while (transformed && iterationLimiter > 0);
        return anyTransformed;
    }

    /**
     * @param termAndBindings
     */
    public boolean transformOnce(final String transformationPredicate, final Object[] termAndBindings) {
        final Object inputTerm = termAndBindings[0];
        if (inputTerm instanceof Var) {
            Var var = (Var) inputTerm;
            if (var.isAnonymous()) {
                // Anonymous var not transformed
                return false;
            }
            final Bindings inputBindings = (Bindings) termAndBindings[1];
            if (var.bindingWithin(inputBindings).followLinks().isFree()) {
                // Free variable, no transformation
                return false;
            }
        }
        final Var transIn = new Var("TransIn");
        final Var transOut = new Var("TransOut");
        final Struct transformationGoal = (Struct) TermApi.normalize(new Struct((String) transformationPredicate, transIn, transOut), null);
        final Bindings transformationBindings = new Bindings(transformationGoal);

        // Now bind our transIn var to the original term. Note: we won't have to unbind here since our modified bindings are a local var!
        transIn.bindingWithin(transformationBindings).bindTo(termAndBindings[0], (Bindings) termAndBindings[1]);

        // Now solving
        final SolutionListener singleMappingResultListener = new SolutionListener() {
            @Override
            public Continuation onSolution() {
                logger.debug("solution: transformationBindings={}", transformationBindings);
                final Bindings narrowed = transformationBindings.narrow(transOut, Object.class);
                termAndBindings[0] = narrowed.getReferrer();
                termAndBindings[1] = Bindings.deepCopyWithSameReferrer(narrowed);
                logger.debug("solution: narrow={} bindings={}", termAndBindings[0], termAndBindings[1]);
                // Don't need anything more than the first solution. Also this value will be returned
                // from Solver.solveGoal() and this will indicate we reached one solution!
                return Continuation.USER_ABORT;
            }
        };
        final Continuation continuation = getProlog().getSolver().solveGoal(transformationBindings, singleMappingResultListener);
        final boolean oneSolutionFound = continuation == Continuation.USER_ABORT;
        return oneSolutionFound;
    }

    @Test
    public void mapBottomUp() {
        assertEquals("transformed(a)", assertOneSolution("mapBottomUp(map, original(a), X)").binding("X").toString());

        assertEquals("[10,1]", assertOneSolution("mapBottomUp(map, 11, X)").binding("X").toString());

        assertEquals("transformed([10,1])", assertOneSolution("mapBottomUp(map, original(11), X)").binding("X").toString());

        //
        // Mapped struct

        assertEquals("f(one, 2)", assertOneSolution("mapBottomUp(map, f(1,2), X)").binding("X").toString());
        assertEquals("g(one, f(one, 2))", assertOneSolution("mapBottomUp(map, g(1, f(1,2)), X)").binding("X").toString());
        assertEquals("[one,ten]", assertOneSolution("mapBottomUp(map, [1,10], X)").binding("X").toString());

        //
        // Free vars and anonymous should not be mapped
        assertOneSolution("mapBottomUp(map, _, anything)");
        assertOneSolution("mapBottomUp(map, Free, Free)");
        assertOneSolution("mapBottomUp(map, Free, X), X=Free");

        //
        // Mapped atoms
        assertOneSolution("mapBottomUp(map, 1, one)");
        assertNoSolution("mapBottomUp(map, 1, other)");
        assertEquals("one", assertOneSolution("IN=1, mapBottomUp(map, IN, X)").binding("X"));

        //
        // Unmapped atoms
        assertOneSolution("mapBottomUp(map, 2, 2)");
        assertNoSolution("mapBottomUp(map, 2, other)");
        assertEquals(2L, assertOneSolution("IN=2, mapBottomUp(map, IN, X)").binding("X"));

        // Free var
        assertOneSolution("mapBottomUp(map, X, X)");
        assertEquals("f(X)", assertOneSolution("mapBottomUp(map, f(X), Result)").binding("Result").toString());
    }

}
