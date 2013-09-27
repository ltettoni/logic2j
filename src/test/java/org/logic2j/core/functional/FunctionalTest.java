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
package org.logic2j.core.functional;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.solver.holder.MultipleSolutionsHolder;
import org.logic2j.core.api.solver.holder.UniqueSolutionHolder;
import org.logic2j.core.impl.util.CollectionUtils;

/**
 * Functional tests of the core features.
 */
public class FunctionalTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionalTest.class);

    @Test
    public void placeholder() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertEquals(term(3), assertOneSolution("sumial(2, X)").binding("X"));
    }

    @Test
    public void corePrimitivesThatYieldUniqueSolution() {
        final String[] SINGLE_SOLUTION_GOALS = new String[] { //
        "true", //
                "true, true", //
                "true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true", //
                "!", //
                "!, !", //
        };
        assertOneSolution(SINGLE_SOLUTION_GOALS);
    }

    @Test
    public void corePrimitivesThatYieldNoSolution() {
        final String[] NO_SOLUTION_GOALS = new String[] { //
        "fail", //
                "fail, fail", //
                "fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail", //
                "true, fail", //
                "fail, true", //
                "true, true, fail", //
                "true, fail, !", //
        };
        assertNoSolution(NO_SOLUTION_GOALS);
    }

    /**
     * This is a special feature of logic2j: AND with any arity
     */
    @Test
    public void nonBinaryAnd() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        final String[] SINGLE_SOLUTION_GOALS = new String[] { //
        "','(true)", //
                "','(true, true)", //
                "','(true, !, true)", //
        };
        assertOneSolution(SINGLE_SOLUTION_GOALS);
    }

    @Test
    public void or() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(2, "';'(true, true)");
        //
        assertNSolutions(2, "true; true");
        assertNSolutions(3, "true; true; true");
        //
        MultipleSolutionsHolder solutions;
        solutions = this.prolog.solve("X=a; X=b; X=c").all();
        assertEquals("[a, b, c]", solutions.binding("X").toString());
    }

    /**
     * This is a special feature of logic2j: OR with any arity
     */
    @Test
    public void nonBinaryOr() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(1, "';'(true)");
        assertNSolutions(2, "';'(true, true)");
        assertNSolutions(3, "';'(true, true, true)");
    }

    @Test
    public void orWithVars() {
        MultipleSolutionsHolder solutions;
        solutions = this.prolog.solve("X=1; Y=2").all();
        assertEquals("[{X=1, Y=Y}, {X=X, Y=2}]", solutions.bindings().toString());
    }

    @Test
    public void orWithClause() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        MultipleSolutionsHolder solutions;
        solutions = this.prolog.solve("or3(X)").all();
        assertEquals("[a, b, c]", solutions.binding("X").toString());
    }

    @Test
    public void not() {
        // Surprisingly enough, the operator \+ means "not provable".
        assertOneSolution("not(fail)", "\\+(fail)");
        assertNoSolution("not(true)", "\\+(true)");
    }

    @Test
    public void rules() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertNSolutions(3, "a(X)");

        assertNSolutions(5, "f(Q)");

        assertNSolutions(9, "a(X), b(Y)", "true, a(X), b(Y)", "a(X), b(Y), true", "a(X), true, b(Y)");

        assertNSolutions(27, "a(X), b(Y), c(Z)");
    }

    @Test
    public void findall() {
        loadTheoryFromTestResourcesDir("test-functional.pl");

        assertEquals("[]", assertOneSolution("findall(1, fail, L)").binding("L").toString());
        assertEquals("[1]", assertOneSolution("findall(1, true, L)").binding("L").toString());
        assertEquals("[1,1,1]", assertOneSolution("findall(1, (true;true;true), L)").binding("L").toString());
        assertEquals("[a(b),a(b)]", assertOneSolution("findall(a(b), (true;fail;true), L)").binding("L").toString());

        assertEquals("[1,2,3]", assertOneSolution("findall(X, a(X), L)").binding("L").toString());
        assertEquals("[b(1),b(2),b(3)]", assertOneSolution("findall(b(X), a(X), L)").binding("L").toString());
        assertEquals("[Z,Z,Z]", assertOneSolution("findall(Z, a(X), L)").binding("L").toString());
        assertNoSolution("findall(X, a(X), [1])");
        assertOneSolution("findall(X, a(X), [1,2,3])");
    }

    @Test
    public void findall_bindFreeVars() {
        // UniqueSolutionHolder sol = assertOneSolution("Res=Z");
        final UniqueSolutionHolder sol = assertOneSolution("findall(X, member(X,[a,B,c]), Res)");
        assertEquals("[a,X,c]", sol.binding("Res").toString());
    }

    @Test
    public void member() {
        loadTheoryFromTestResourcesDir("test-functional.pl");

        assertOneSolution("member(a, [a,b,c])", "member(b, [a,b,c])", "member(c, [a,b,c])");
        assertNoSolution("member(d, [a,b,c])");
        logger.info(CollectionUtils.format("All bindings: ", this.prolog.solve("member(X, [a,b,c])").all().ensureNumber(3).bindings(), 0));

        assertEquals("[1,2,3]", assertOneSolution("append([1],[2,3],X)").binding("X").toString());

        final MultipleSolutionsHolder all = assertNSolutions(3, "append(X,Y,[1,2])");
        assertEquals(termList("[]", "[1]", "[1,2]"), all.binding("X"));
        assertEquals(termList("[1,2]", "[2]", "[]"), all.binding("Y"));
    }

    @Test
    public void sumial() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertEquals(term(0), assertOneSolution("sumial(0, X)").binding("X"));
        assertEquals(term(1), assertOneSolution("sumial(1, X)").binding("X"));
        assertEquals(term(3), assertOneSolution("sumial(2, X)").binding("X"));
        assertEquals(term(15), assertOneSolution("sumial(5, X)").binding("X"));
        assertEquals(term(55), assertOneSolution("sumial(10, X)").binding("X"));
        assertEquals(term(5050), assertOneSolution("sumial(100, X)").binding("X"));
    }

    @Test
    public void unify() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        assertOneSolution("unifyterms(X,X)");
        assertEquals(term(123), assertOneSolution("unifyterms21(X,123)").binding("X"));
        assertEquals(term(123), assertOneSolution("unifyterms21(123, X)").binding("X"));
        assertEquals(term(123), assertOneSolution("unifyterms22(X,123)").binding("X"));
        assertEquals(term(123), assertOneSolution("unifyterms22(123, X)").binding("X"));
    }

    /**
     * Sometimes (when?) X is bound to a term containing a unified var to another of our vars
     */
    @Test
    public void relink_vars() {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        // Below, Y must be equal to g(123,X), but does not solve to X!
        assertEquals(term("g(123,X)"), assertOneSolution("unifyterms3(f(123,X), Y)").binding("Y"));

        // Is this really what we should have??? Free internal bindings popping up to higher levels?
        assertEquals(term("f(_)"), assertOneSolution("final(X)").binding("X"));
    }

    @Test
    public void binding_single_var_1() throws Exception {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        MultipleSolutionsHolder assertNSolutions = assertNSolutions(6, "ab(X,Y)");
        assertEquals("[{X=1, Y=11}, {X=2, Y=12}, {X=3, Y=13}, {X=4, Y=14}, {X=5, Y=15}, {X=6, Y=16}]", assertNSolutions.bindings().toString());
        assertEquals("[1, 2, 3, 4, 5, 6]", assertNSolutions.binding("X").toString());
        assertEquals("[11, 12, 13, 14, 15, 16]", assertNSolutions.binding("Y").toString());
    }

    @Test
    public void binding_single_var_2() throws Exception {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        MultipleSolutionsHolder assertNSolutions = assertNSolutions(6, "ac(X,Y)");
        assertEquals("[{X=1, Y=11}, {X=2, Y=twelve}, {X=3, Y=13}, {X=4, Y=fourteen}, {X=5, Y=15}, {X=6, Y=sixteen}]", assertNSolutions.bindings().toString());
        assertEquals("[1, 2, 3, 4, 5, 6]", assertNSolutions.binding("X").toString());
        assertEquals("[11, twelve, 13, fourteen, 15, sixteen]", assertNSolutions.binding("Y").toString());
    }

}
