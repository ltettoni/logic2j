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

import static org.junit.Assert.assertEquals;

/**
 * Test the FunctionLibrary's mapping predicates.
 */
public class FunctionLibraryTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionLibraryTest.class);

    /**
     * See "mapping.pro" for test predicates
     */
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
        // Stack overflow
//        GoalHolder sol = uniqueSolution("gd3(owner(Q, 68), Q)");
    }

    @Test
    public void anonymousAndFreeVarsAreNotTransformed() {
        assertTransformation(MAPPING_PREDICATE, "_", "Q", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "Q", "Q", FunctionLibrary.OPTION_ONE); // Free var
    }

    @Test
    public void atomicNotTransformed() {
        assertTransformation(MAPPING_PREDICATE, "atom", "atom", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "123", "123", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "123.456", "123.456", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "9", "9", FunctionLibrary.OPTION_ONE);
    }

    @Test
    public void atomicTransformed() {
        assertTransformation(MAPPING_PREDICATE, "t1", "t2", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "t2", "t3", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "1", "one", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "10", "ten", FunctionLibrary.OPTION_ONE);
    }

    @Test
    public void atomicNotTransformable() {
        assertWrongMapping("1", "should-be-one", FunctionLibrary.OPTION_ONE);
    }

    @Test
    public void structNotTransformed() {
        assertTransformation(MAPPING_PREDICATE, "f(a,b,c)", "f(a, b, c)", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "[1,2]", "[1,2]", FunctionLibrary.OPTION_ONE);
    }

    @Test
    public void structTransformed() {
        assertTransformation(MAPPING_PREDICATE, "original(a)", "transformed(a)", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "original(G)", "transformed(G)", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "original(_)", "transformed(X)", FunctionLibrary.OPTION_ONE); // Dubious
        assertTransformation(MAPPING_PREDICATE, "transformed(X, Y)", "transformed(X, Y)", FunctionLibrary.OPTION_ONE);
    }

    @Test
    public void mapNonIterative() {
        assertTransformation(MAPPING_PREDICATE, "t4", "t4", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "t3", "t4", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "t2", "t3", FunctionLibrary.OPTION_ONE);
        assertTransformation(MAPPING_PREDICATE, "t1", "t2", FunctionLibrary.OPTION_ONE);
    }

    @Test
    public void mapIterative() {
        assertTransformation(MAPPING_PREDICATE, "t4", "t4", FunctionLibrary.OPTION_ITER);
        assertTransformation(MAPPING_PREDICATE, "t3", "t4", FunctionLibrary.OPTION_ITER);
        assertTransformation(MAPPING_PREDICATE, "t2", "t4", FunctionLibrary.OPTION_ITER);
        assertTransformation(MAPPING_PREDICATE, "t1", "t4", FunctionLibrary.OPTION_ITER);
    }

    @Test
    public void structTransformedRecursiveBefore() {
        assertTransformation(MAPPING_PREDICATE, "[1,10]", "[one,ten]", FunctionLibrary.OPTION_BEFORE);
        assertTransformation(MAPPING_PREDICATE, "f(1,2)", "f(one, 2)", FunctionLibrary.OPTION_BEFORE);
        assertTransformation(MAPPING_PREDICATE, "g(1, f(1,2))", "g(one, f(one, 2))", FunctionLibrary.OPTION_BEFORE);
    }

    @Test
    public void structTransformedRecursiveAfter() {
        assertTransformation(MAPPING_PREDICATE, "f1", "f(one)", FunctionLibrary.OPTION_AFTER);
        assertTransformation(MAPPING_PREDICATE, "11", "[ten,one]", FunctionLibrary.OPTION_AFTER);
        assertTransformation(MAPPING_PREDICATE, "h(11)", "h([ten,one])", FunctionLibrary.OPTION_AFTER);
    }

    /**
     * @param transformationPredicate
     * @param termToTransform
     */
    private void assertTransformation(String transformationPredicate, String termToTransform, String theExpectedToString, String options) {
        final String goalText = "map(" + transformationPredicate + ", " + termToTransform + ", Q, " + options + ")";
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

    @Test
    public void moreComplicated1() {
        GoalHolder sol = uniqueSolution("gd3(tcNumber(a, b), Q)");
        final Object q = sol.var("Q").unique();
        logger.info("Solution: {}", q.toString());
        assertEquals("['='(col(committee, id), a),'='(col(committee, tcNum), b)]", q.toString());
    }

    @Test
    public void moreComplicated2() {
        GoalHolder sol = uniqueSolution("gd3((tcNumber(A, B), tcNumber(A, C)), Q)");
        final Object q = sol.var("Q").unique();
        logger.info("Solution: {}", q.toString());
        assertEquals("','(['='(col(committee, id), A),'='(col(committee, tcNum), B)], ['='(col(committee, id), A),'='(col(committee, tcNum), C)])", q.toString());
    }

    @Test
    public void moreComplicated3() {
        GoalHolder sol = uniqueSolution("gd3(organization(iso_id), Q)");
        final Object q = sol.var("Q").unique();
        logger.info("Solution: {}", q.toString());
        assertEquals("'='(col(organization, id), 68)", q.toString());
    }

    @Test
    public void moreComplicated4() {
        GoalHolder sol = uniqueSolution("gd3(commIso(A), Q)");
        final Object q = sol.var("Q").unique();
        logger.info("Solution: {}", q.toString());
        assertEquals("','('='(col(committee, id), A), ['='(col(pred_owner, id), A),'='(pred_owner(pred_owner, owner), 68)])", q.toString());
    }


    @Test
    public void dessoc() {
        assertTransformation("dessoc", "(a)", "a", FunctionLibrary.OPTION_BEFORE);
        assertTransformation("dessoc", "(a,b)", "op(and, [a,b])", FunctionLibrary.OPTION_BEFORE);
        assertTransformation("dessoc", "(a,b,c)", "op(and, [a,b,c])", FunctionLibrary.OPTION_BEFORE);

        assertTransformation("dessoc", "(a;b)", "op(or, [a,b])", FunctionLibrary.OPTION_BEFORE);
        assertTransformation("dessoc", "(a;b;c)", "op(or, [a,b,c])", FunctionLibrary.OPTION_BEFORE);

        assertTransformation("dessoc", "(a,b;c)", "op(or, [op(and, [a,b]),c])", FunctionLibrary.OPTION_BEFORE);
        assertTransformation("dessoc", "(a;b,c)", "op(or, [a,op(and, [b,c])])", FunctionLibrary.OPTION_BEFORE);

        assertTransformation("dessoc", "(a,(b;c))", "op(and, [a,op(or, [b,c])])", FunctionLibrary.OPTION_BEFORE);
        assertTransformation("dessoc", "((a;b),c)", "op(and, [op(or, [a,b]),c])", FunctionLibrary.OPTION_BEFORE);
    }

}
