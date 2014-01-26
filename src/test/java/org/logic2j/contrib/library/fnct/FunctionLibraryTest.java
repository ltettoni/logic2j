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
import org.logic2j.core.api.model.var.TermBindings;

public class FunctionLibraryTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionLibraryTest.class);

    private static final String MAPPING_PREDICATE = "map";

    private FunctionLibrary functionLibrary;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.functionLibrary = new FunctionLibrary(this.prolog);
        loadLibrary(this.functionLibrary);
        loadTheoryFromTestResourcesDir("mapping.pl");
    }

    @Test
    public void placeholder() {
        //
    }

    @Test
    public void anonymousAndFreeVarsAreNotTransformed() {
        assertNotTransformed("_", false, false, false);
        assertNotTransformed("X", false, false, false); // Free var
    }

    @Test
    public void atomicNotTransformed() {
        assertNotTransformed("atom", false, false, false);
        assertNotTransformed("123", false, false, false);
        assertNotTransformed("123.456", false, false, false);
    }

    @Test
    public void atomicTransformed() {
        assertTransformed("t4", "t3", false, false, false);
        assertTransformed("one", "1", false, false, false);
    }

    @Test
    public void structNotTransformed() {
        assertNotTransformed("f(a,b,c)", false, false, false);
        assertNotTransformed("[1,2]", false, false, false);
    }

    @Test
    public void structTransformed() {
        assertTransformed("transformed(a)", "original(a)", false, false, false);
        assertTransformed("transformed(X)", "original(X)", false, false, false);
        assertTransformed("transformed(X)", "original(_)", false, false, false); // Dubious
        assertNotTransformed("transformed(X, Y)", false, false, false);
    }

    @Test
    public void mapNonIterative() {
        assertNotTransformed("t4", false, false, false);
        assertTransformed("t4", "t3", false, false, false);
        assertTransformed("t3", "t2", false, false, false);
        assertTransformed("t2", "t1", false, false, false);
    }

    @Test
    public void mapIterative() {
        assertNotTransformed("t4", true, false, false);
        assertTransformed("t4", "t3", true, false, false);
        assertTransformed("t4", "t2", true, false, false);
        assertTransformed("t4", "t1", true, false, false);
    }

    @Test
    public void structTransformedRecursiveBefore() {
        assertTransformed("[one,ten]", "[1,10]", false, true, false);
        assertTransformed("f(one, 2)", "f(1,2)", false, true, false);
        assertTransformed("g(one, f(one, 2))", "g(1, f(1,2))", false, true, false);
    }

    @Test
    public void structTransformedRecursiveAfter() {
        assertTransformed("h([ten,one])", "h(11)", false, true, true);
        assertTransformed("[ten,one]", "11", false, true, true);
    }

    /**
     * @param termToParse
     * @param childrenBefore True for recursive pre-transformation (bottom-up)
     * @param childrenAfter True for recursive post-transformation (top-down)
     */
    private void assertNotTransformed(String termToParse, boolean iterative, boolean childrenBefore, boolean childrenAfter) {
        final Object originalTerm = unmarshall(termToParse);
        final TermBindings originalBindings = new TermBindings(originalTerm);
        logger.info("Instantiated term to transform: term={} , bindings={}", originalTerm, originalBindings);
        final Object[] termAndBindings = new Object[] { originalTerm, originalBindings };
        final boolean transform = iterative ? this.functionLibrary.transformAll(MAPPING_PREDICATE, termAndBindings, childrenBefore, childrenAfter) : this.functionLibrary.transformOnce(
                MAPPING_PREDICATE, termAndBindings, childrenBefore, childrenAfter);
        assertFalse(transform);
        assertSame(termAndBindings[0], originalTerm);
        assertSame(termAndBindings[1], originalBindings);
    }

    /**
     * @param toStringExpected
     * @param termToParse
     * @param childrenBefore True for recursive pre-transformation (bottom-up)
     * @param childrenAfter True for recursive post-transformation (top-down)
     * @return
     */
    private Object[] assertTransformed(String toStringExpected, String termToParse, boolean iterative, boolean childrenBefore, boolean childrenAfter) {
        final Object originalTerm = unmarshall(termToParse);
        final TermBindings originalBindings = new TermBindings(originalTerm);
        logger.debug("Instantiated term to transform: term={} , bindings={}", originalTerm, originalBindings);
        final Object[] termAndBindings = new Object[] { originalTerm, originalBindings };
        final boolean transform = iterative ? this.functionLibrary.transformAll(MAPPING_PREDICATE, termAndBindings, childrenBefore, childrenAfter) : this.functionLibrary.transformOnce(
                MAPPING_PREDICATE, termAndBindings, childrenBefore, childrenAfter);
        assertTrue(transform);
        final String substituted = TermApi.substitute(termAndBindings[0], (TermBindings) termAndBindings[1]).toString();
        assertEquals(toStringExpected, substituted);
        return termAndBindings;
    }

    @Test
    public void map() {
        assertEquals("transformed(a)", assertOneSolution("map(map, original(a), X)").binding("X").toString());

        assertEquals("transformed([ten,one])", assertOneSolution("map(map, original(11), X)").binding("X").toString());

        //
        // Free vars and anonymous should not be mapped
        assertOneSolution("map(map, _, anything)");
        assertOneSolution("map(map, Free, Free)");
        assertOneSolution("map(map, Free, X), X=Free");

        //
        // Mapped atoms
        assertOneSolution("map(map, 1, one)");
        assertNoSolution("map(map, 1, other)");
        assertEquals("one", assertOneSolution("IN=1, map(map, IN, X)").binding("X"));

        //
        // Unmapped atoms
        assertOneSolution("map(map, 2, 2)");
        assertNoSolution("map(map, 2, other)");
        assertEquals(2L, assertOneSolution("IN=2, map(map, IN, X)").binding("X"));

        // Free var
        assertOneSolution("map(map, X, X)");
        assertEquals("f(X)", assertOneSolution("map(map, f(X), Result)").binding("Result").toString());
    }

}
