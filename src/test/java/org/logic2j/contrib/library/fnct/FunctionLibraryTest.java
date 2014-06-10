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

import org.junit.Before;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.solver.holder.GoalHolder;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;
import static org.junit.Assert.*;

public class FunctionLibraryTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionLibraryTest.class);

    private static final String MAPPING_PREDICATE = "remap";

    private FunctionLibrary functionLibrary;

    @Before
    public void loadFunctionLibrary() {
        this.functionLibrary = new FunctionLibrary(this.prolog);
        loadLibrary(this.functionLibrary);
        loadTheoryFromTestResourcesDir("mapping.pro");
    }

    protected InitLevel initLevel() {
        return InitLevel.L2_BASE_LIBRARIES;
    }

    @Test
    public void placeholder() {
        //
    }

    // To be reworked completely - now that we don't have Bindings any longer

    @Test
    public void anonymousAndFreeVarsAreNotTransformed() {
        assertMapping("Q", "_");
        assertMapping("Q", "X"); // Free var
    }

    @Test
    public void atomicNotTransformed() {
        assertMapping("atom", "atom");
        assertMapping("123", "123");
        assertMapping("123.456", "123.456");
    }

    @Test
    public void atomicTransformed() {
        assertMapping("t2", "t1");
        assertMapping("t3", "t2");
        assertMapping("one", "1");
        assertMapping("ten", "10");
    }

    @Test
    public void structNotTransformed() {
        assertMapping("f(a, b, c)", "f(a,b,c)");
        assertMapping("[1,2]", "[1,2]");
    }

    @Test
    public void structTransformed() {
        assertMapping("transformed(a)", "original(a)");
        assertMapping("transformed(G)", "original(G)");
        assertMapping("transformed(X)", "original(_)"); // Dubious
        assertMapping("transformed(X, Y)", "transformed(X, Y)");
    }

//    @Test
//    public void mapNonIterative() {
//        assertNotTransformed("t4", false, false, false);
//        assertMapping("t4", "t3", false, false, false);
//        assertMapping("t3", "t2", false, false, false);
//        assertMapping("t2", "t1", false, false, false);
//    }
//
//    @Test
//    public void mapIterative() {
//        assertNotTransformed("t4", true, false, false);
//        assertMapping("t4", "t3", true, false, false);
//        assertMapping("t4", "t2", true, false, false);
//        assertMapping("t4", "t1", true, false, false);
//    }
//
//    @Test
//    public void structTransformedRecursiveBefore() {
//        assertMapping("[one,ten]", "[1,10]", false, true, false);
//        assertMapping("f(one, 2)", "f(1,2)", false, true, false);
//        assertMapping("g(one, f(one, 2))", "g(1, f(1,2))", false, true, false);
//    }
//
//    @Test
//    public void structTransformedRecursiveAfter() {
//        assertMapping("h([ten,one])", "h(11)", false, true, true);
//        assertMapping("[ten,one]", "11", false, true, true);
//    }

    /**
     * @param termToTransform
     *
     */
    private void assertMapping(String theExpectedToString, String termToTransform) {
        final String goalText = "map(" + MAPPING_PREDICATE + ", " + termToTransform + ", Q)";
        final Object goal = unmarshall(goalText);
        logger.info("Transformation goal: \"{}\"", goal);
        final GoalHolder holder = this.prolog.solve(goal);
        assertEquals(1, holder.count());
        final Object unique = holder.var("Q").unique();
        assertEquals(theExpectedToString, unique.toString());
    }


//    @Test
//    public void map() {
//        assertEquals("transformed(a)", uniqueSolution("map(map, original(a), X)").var("X").unique().toString());
//
//        assertEquals("transformed([ten,one])", uniqueSolution("map(map, original(11), X)").var("X").unique().toString());
//
//        //
//        // Free vars and anonymous should not be mapped
//        uniqueSolution("map(map, _, anything)");
//        uniqueSolution("map(map, Free, Free)");
//        uniqueSolution("map(map, Free, X), X=Free");
//
//        //
//        // Mapped atoms
//        uniqueSolution("map(map, 1, one)");
//        noSolutions("map(map, 1, other)");
//        assertEquals("one", uniqueSolution("IN=1, map(map, IN, X)").var("X").unique().toString());
//
//        //
//        // Unmapped atoms
//        uniqueSolution("map(map, 2, 2)");
//        noSolutions("map(map, 2, other)");
//        assertEquals(2L, uniqueSolution("IN=2, map(map, IN, X)").var("X").unique().toString());
//
//        // Free var
//        uniqueSolution("map(map, X, X)");
//        assertEquals("f(X)", uniqueSolution("map(map, f(X), Result)").var("Result").unique().toString());
//    }
//
//    @Ignore("FIXME not yet functional")
//    @Test
//    public void mapGoesToInfiniteLoop() {
//        GoalHolder sol = uniqueSolution("gd3((tcNumber(a, b), c))");
////        UniqueSolutionHolder sol = uniqueSolution("map(dbBinding, (tcNumber(a, b), c), Z)");
//        logger.info("Solution: {}", sol.var("Z").unique().toString());
//    }

}
