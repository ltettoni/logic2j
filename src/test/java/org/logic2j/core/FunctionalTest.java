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
package org.logic2j.core;

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.api.solver.holder.GoalHolder;
import org.logic2j.core.impl.util.CollectionUtils;
import org.logic2j.core.impl.util.ProfilingInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

/**
 * Functional tests of the core features.
 */
public class FunctionalTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionalTest.class);

    @Test
    public void placeholderToReproduceError() {
       //
    }

    @Test
    public void rules() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        countNSolutions(3, "a(X)");
        countNSolutions(5, "f(Q)");
        countNSolutions(9, "a(X), b(Y)", "true, a(X), b(Y)", "a(X), b(Y), true", "a(X), true, b(Y)");
        countNSolutions(27, "a(X), b(Y), c(Z)");
    }

    @Test
    public void member() {
        countOneSolution("member(a, [a,b,c])", "member(b, [a,b,c])", "member(c, [a,b,c])");
        countNoSolution("member(d, [a,b,c])");
        logger.info(CollectionUtils.format("All bindings: ", this.prolog.solve("member(X, [a,b,c])").exactCount(3).vars().list(), 0));

        assertEquals("[1,2,3]", uniqueSolution("append([1],[2,3],X)").var("X").unique().toString());

        final GoalHolder all = nSolutions(3, "append(X,Y,[1,2])");
        assertEquals(termList("[]", "[1]", "[1,2]"), all.var("X").list());
        assertEquals(termList("[1,2]", "[2]", "[]"), all.var("Y").list());
    }

    @Test
    public void sumial() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertEquals(term(0), uniqueSolution("sumial(0, X)").longValue("X"));
        assertEquals(term(1), uniqueSolution("sumial(1, X)").longValue("X"));
        assertEquals(term(3), uniqueSolution("sumial(2, X)").longValue("X"));
        assertEquals(term(15), uniqueSolution("sumial(5, X)").longValue("X"));
        assertEquals(term(55), uniqueSolution("sumial(10, X)").longValue("X"));
        assertEquals(term(5050), uniqueSolution("sumial(100, X)").longValue("X"));
    }

    @Test
    public void unify() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        uniqueSolution("unifyterms(X,X)");
        assertEquals(term(123), uniqueSolution("unifyterms21(X,123)").longValue("X"));
        assertEquals(term(123), uniqueSolution("unifyterms21(123, X)").longValue("X"));
        assertEquals(term(123), uniqueSolution("unifyterms22(X,123)").longValue("X"));
        assertEquals(term(123), uniqueSolution("unifyterms22(123, X)").longValue("X"));
    }



//    /**
//     * Sometimes (when?) X is bound to a term containing a unified var to another of our vars
//     */
//    @Test
//    public void relink_vars() {
//        loadTheoryFromTestResourcesDir("test-functional.pl");
//        // Below, Y must be equal to g(123,X), but does not solve to X!
//        assertEquals(term("g(123,X)"), assertOneSolution("unifyterms3(f(123,X), Y)").binding("Y"));
//
//        // Is this really what we should have? // TuProlog returns a binding of X to an anonymous internal var
//        assertEquals(term("f(FinalVar)"), assertOneSolution("final(X)").binding("X"));
//    }
//
//    @Test
//    public void binding_single_var_1() {
//        loadTheoryFromTestResourcesDir("test-functional.pl");
//        final MultipleSolutionsHolder assertNSolutions = assertNSolutions(6, "ab(X,Y)");
//        assertEquals("[{X=1, Y=11}, {X=2, Y=12}, {X=3, Y=13}, {X=4, Y=14}, {X=5, Y=15}, {X=6, Y=16}]", assertNSolutions.bindings().toString());
//        assertEquals("[1, 2, 3, 4, 5, 6]", assertNSolutions.binding("X").toString());
//        assertEquals("[11, 12, 13, 14, 15, 16]", assertNSolutions.binding("Y").toString());
//    }
//
//    @Test
//    public void binding_single_var_2() {
//        loadTheoryFromTestResourcesDir("test-functional.pl");
//        final MultipleSolutionsHolder assertNSolutions = assertNSolutions(6, "ac(X,Y)");
//        assertEquals("[{X=1, Y=11}, {X=2, Y=twelve}, {X=3, Y=13}, {X=4, Y=fourteen}, {X=5, Y=15}, {X=6, Y=sixteen}]", assertNSolutions.bindings().toString());
//        assertEquals("[1, 2, 3, 4, 5, 6]", assertNSolutions.binding("X").toString());
//        assertEquals("[11, twelve, 13, fourteen, 15, sixteen]", assertNSolutions.binding("Y").toString());
//    }
//
    @Test
    public void findall() {
        loadTheoryFromTestResourcesDir("test-functional.pl");

        assertEquals("[]", uniqueSolution("findall(1, fail, L)").toString("L"));
        assertEquals("[1]", uniqueSolution("findall(1, true, L)").toString("L"));
        assertEquals("[1,1,1]", uniqueSolution("findall(1, (true;true;true), L)").toString("L"));
        assertEquals("[a(b),a(b)]", uniqueSolution("findall(a(b), (true;fail;true), L)").toString("L"));

        assertEquals("[1,2,3]", uniqueSolution("findall(X, a(X), L)").toString("L"));
        assertEquals("[b(1),b(2),b(3)]", uniqueSolution("findall(b(X), a(X), L)").toString("L"));
        assertEquals("[Z,Z,Z]", uniqueSolution("findall(Z, a(X), L)").toString("L"));
        countNoSolution("findall(X, a(X), [1])");
        uniqueSolution("findall(X, a(X), [1,2,3])");
    }

    @Test
    public void findall_bindFreeVars() {
        final GoalHolder sol = uniqueSolution("findall(X, member(X,[a,B,c]), Res)");
        assertEquals("[a,B,c]", sol.toString("Res"));
    }

}
