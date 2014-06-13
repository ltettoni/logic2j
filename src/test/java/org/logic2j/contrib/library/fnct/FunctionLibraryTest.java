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
import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.solver.holder.GoalHolder;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;
import static org.junit.Assert.*;

public class FunctionLibraryTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionLibraryTest.class);

    private static final String MAPPING_PREDICATE = "remap";

    private FunctionLibrary functionLibrary;

    private final String OPTION_ONE = "one";
    private final String OPTION_ITER = "iter";
    private final String OPTION_BEFORE = "before";

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
        assertMapping("Q", "_", OPTION_ONE);
        assertMapping("Q", "X", OPTION_ONE); // Free var
    }

    @Test
    public void atomicNotTransformed() {
        assertMapping("atom", "atom", OPTION_ONE);
        assertMapping("123", "123", OPTION_ONE);
        assertMapping("123.456", "123.456", OPTION_ONE);
    }

    @Test
    public void atomicWrong() {
        assertWrongMapping("a", "b", OPTION_ONE);
    }

    @Test
    public void atomicTransformed() {
        assertMapping("t2", "t1", OPTION_ONE);
        assertMapping("t3", "t2", OPTION_ONE);
        assertMapping("one", "1", OPTION_ONE);
        assertMapping("ten", "10", OPTION_ONE);
    }

    @Test
    public void structNotTransformed() {
        assertMapping("f(a, b, c)", "f(a,b,c)", OPTION_ONE);
        assertMapping("[1,2]", "[1,2]", OPTION_ONE);
    }

    @Test
    public void structTransformed() {
        assertMapping("transformed(a)", "original(a)", OPTION_ONE);
        assertMapping("transformed(G)", "original(G)", OPTION_ONE);
        assertMapping("transformed(X)", "original(_)", OPTION_ONE); // Dubious
        assertMapping("transformed(X, Y)", "transformed(X, Y)", OPTION_ONE);
    }

    @Test
    public void mapNonIterative() {
        assertMapping("t4", "t4", OPTION_ONE);
        assertMapping("t4", "t3", OPTION_ONE);
        assertMapping("t3", "t2", OPTION_ONE);
        assertMapping("t2", "t1", OPTION_ONE);
    }

    @Test
    public void mapIterative() {
        assertMapping("t4", "t4", OPTION_ITER);
        assertMapping("t4", "t3", OPTION_ITER);
        assertMapping("t4", "t2", OPTION_ITER);
        assertMapping("t4", "t1", OPTION_ITER);
    }

    @Test
    public void structTransformedRecursiveBefore() {
        assertMapping("[one,ten]", "[1,10]", OPTION_BEFORE);
        assertMapping("f(one, 2)", "f(1,2)", OPTION_BEFORE);
        assertMapping("g(one, f(one, 2))", "g(1, f(1,2))", OPTION_BEFORE);
    }
//
//    @Test
//    public void structTransformedRecursiveAfter() {
//        assertMapping("h([ten,one])", "h(11)", false, true, true);
//        assertMapping("[ten,one]", "11", false, true, true);
//    }

    /**
     * @param termToTransform
     */
    private void assertMapping(String theExpectedToString, String termToTransform, String options) {
//                final String options = "one";
        final String goalText = "map(" + MAPPING_PREDICATE + ", " + termToTransform + ", Q, " + options + ")";
        final Object goal = unmarshall(goalText);
        logger.info("Transformation goal: \"{}\"", goal);
        final GoalHolder holder = this.prolog.solve(goal);
        assertEquals(1, holder.count());
        final Object unique = holder.var("Q").unique();
        assertEquals(theExpectedToString, unique.toString());
    }


    private void assertWrongMapping(String t1, String t2, String options) {
        final String goalText = "map(" + MAPPING_PREDICATE + ", " + t1 + ", " + t2 + ", " + options + ")";
        final Object goal = unmarshall(goalText);
        logger.info("Transformation goal: \"{}\"", goal);
        final GoalHolder holder = this.prolog.solve(goal);
        assertEquals(0, holder.count());
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
