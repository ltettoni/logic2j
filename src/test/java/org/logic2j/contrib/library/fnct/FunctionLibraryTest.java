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
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.var.Bindings;

public class FunctionLibraryTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionLibraryTest.class);

    private FunctionLibrary functionLibrary;

    @Before
    public void setUp() {
        super.setUp();
        functionLibrary = new FunctionLibrary(this.prolog);
        loadLibrary(functionLibrary);
        loadTheoryFromTestResourcesDir("mapping.pl");
    };

    @Test
    public void placeholder() {
    }

    @Test
    public void anonymousAndFreeVarsAreNotTransformed() {
        assertNotTransformed("_", false, 0, 0);
        assertNotTransformed("X", false, 0, 0); // Free var
    }

    @Test
    public void atomicNotTransformed() {
        assertNotTransformed("atom", false, 0, 0);
        assertNotTransformed("123", false, 0, 0);
        assertNotTransformed("123.456", false, 0, 0);
    }

    @Test
    public void atomicTransformed() {
        assertTransformed("t4", "t3", false, 0, 0);
        assertTransformed("one", "1", false, 0, 0);
    }

    @Test
    public void structNotTransformed() {
        assertNotTransformed("f(a,b,c)", false, 0, 0);
        assertNotTransformed("[1,2]", false, 0, 0);
    }

    @Test
    public void structTransformed() {
        assertTransformed("transformed(a)", "original(a)", false, 0, 0);
        assertTransformed("transformed(X)", "original(X)", false, 0, 0);
        assertTransformed("transformed(X)", "original(_)", false, 0, 0); // Dubious
        assertNotTransformed("transformed(X, Y)", false, 0, 0);
    }

    @Test
    public void mapNonIterative() {
        assertNotTransformed("t4", false, 0, 0);
        assertTransformed("t4", "t3", false, 0, 0);
        assertTransformed("t3", "t2", false, 0, 0);
        assertTransformed("t2", "t1", false, 0, 0);
    }

    @Test
    public void mapIterative() {
        assertNotTransformed("t4", true, 0, 0);
        assertTransformed("t4", "t3", true, 0, 0);
        assertTransformed("t4", "t2", true, 0, 0);
        assertTransformed("t4", "t1", true, 0, 0);
    }

    @Test
    public void structTransformedRecursiveBefore() {
        assertTransformed("[one,ten]", "[1,10]", false, 1, 0);
        assertTransformed("f(one, 2)", "f(1,2)", false, 1, 0);
        assertTransformed("g(one, f(one, 2))", "g(1, f(1,2))", false, 1, 0);
    }

    @Test
    public void structTransformedRecursiveAfter() {
        assertTransformed("h([ten,one])", "h(11)", false, 1, 1);
        assertTransformed("[ten,one]", "11", false, 1, 1);
    }

    /**
     * @param termToParse
     * @param childrenBefore TODO
     * @param childrenAfter TODO
     */
    private void assertNotTransformed(String termToParse, boolean iterative, int childrenBefore, int childrenAfter) {
        final Object originalTerm = getProlog().getTermExchanger().unmarshall(termToParse);
        final Bindings originalBindings = new Bindings(originalTerm);
        logger.info("Instantiated term to transform: term={} , bindings={}", originalTerm, originalBindings);
        final Object[] termAndBindings = new Object[] { originalTerm, originalBindings };
        final boolean transform = iterative ? functionLibrary.transformAll("map", termAndBindings) : functionLibrary.transformOnce("map", termAndBindings, childrenBefore, childrenAfter);
        assertFalse(transform);
        assertSame(termAndBindings[0], originalTerm);
        assertSame(termAndBindings[1], originalBindings);
    }

    /**
     * @param childrenBefore TODO
     * @param childrenAfter TODO
     * @param string
     * @return
     */
    private Object[] assertTransformed(String toStringExpected, String termToParse, boolean iterative, int childrenBefore, int childrenAfter) {
        final Object originalTerm = getProlog().getTermExchanger().unmarshall(termToParse);
        final Bindings originalBindings = new Bindings(originalTerm);
        logger.debug("Instantiated term to transform: term={} , bindings={}", originalTerm, originalBindings);
        final Object[] termAndBindings = new Object[] { originalTerm, originalBindings };
        final boolean transform = iterative ? functionLibrary.transformAll("map", termAndBindings) : functionLibrary.transformOnce("map", termAndBindings, childrenBefore, childrenAfter);
        assertTrue(transform);
        String substituted = TermApi.substitute(termAndBindings[0], (Bindings) termAndBindings[1]).toString();
        String toString = termAndBindings[0].toString();
        assertEquals(toStringExpected, substituted);
        return termAndBindings;
    }

    @Test
    public void mapBottomUp() {
        assertEquals("transformed(a)", assertOneSolution("mapBottomUp(map, original(a), X)").binding("X").toString());

        assertEquals("[ten,one]", assertOneSolution("mapBottomUp(map, 11, X)").binding("X").toString());

        assertEquals("transformed([ten,one])", assertOneSolution("mapBottomUp(map, original(11), X)").binding("X").toString());

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
