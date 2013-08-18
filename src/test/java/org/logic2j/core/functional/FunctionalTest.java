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
import org.logic2j.core.library.impl.io.IOLibrary;
import org.logic2j.core.solver.holder.MultipleSolutionsHolder;
import org.logic2j.core.solver.holder.UniqueSolutionHolder;
import org.logic2j.core.util.CollectionUtils;

public class FunctionalTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionalTest.class);

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

    @Test
    public void nArityAndOr() {
        loadTheoryFromTestResourceDir("test-functional.pl");
        final String[] SINGLE_SOLUTION_GOALS = new String[] { //
        "','(true)", //
                "','(true, true)", //
                "','(true, !, true)", //
        };
        assertOneSolution(SINGLE_SOLUTION_GOALS);
        assertNSolutions(3, "';'(true, true, true)");

    }

    @Test
    public void not() {
        // Surprisingly enough, the operator \+ means "not provable".
        assertOneSolution("not(fail)", "\\+(fail)");
        assertNoSolution("not(true)", "\\+(true)");
    }

    @Test
    public void cut_OneToStudy() {
        loadTheoryFromTestResourceDir("test-functional.pl");
        assertNSolutions(3, "a(X), b(Y), !, c(Z)");
    }

    @Test
    public void changeForOneDollar() {
        final IOLibrary library = new IOLibrary(prolog);
        loadLibrary(library);
        loadTheoryFromTestResourceDir("dollar.pl");
        assertNSolutions(292, "change([H,Q,D,N,P])");
    }

    @Test
    public void rules() {
        loadTheoryFromTestResourceDir("test-functional.pl");

        assertNSolutions(1, "cut1(_)", "a(X), b(Y), c(Z), !", "p(X), X=4");

        assertNSolutions(2, "cut2(_)");
        assertNSolutions(3, "p(_)", "p(X)", "a(X), !, b(Y)", "a(X), b(Y), !, c(Z)", "!, a(X), b(Y), !, c(Z)");
        assertNSolutions(0, "pc(X)");
        assertNSolutions(3, "p(X), X>1");
        // Highly suspicious goal here - should provide 0 results - seems cut does not work at this level!
        // assertNSolutions(0, "p(X), !, X>1");

        assertNSolutions(1, "a(X), !, cut1(Y)");
        assertNSolutions(4, "cut4", "cut4b");
        assertNSolutions(5, "f(Q)");

        assertNSolutions(9, "a(X), b(Y)", "true, a(X), b(Y)", "a(X), b(Y), true", "a(X), true, b(Y)", "a(X), !, b(Y), c(Z)", "a(X), !, !, b(Y), c(Z)");

        assertNSolutions(27, "a(X), b(Y), c(Z)", "!, a(X), b(Y), c(Z)", "!, true, !, a(X), b(Y), c(Z)");
    }

    @Test
    public void findall() {
        loadTheoryFromTestResourceDir("test-functional.pl");

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
        loadTheoryFromTestResourceDir("test-functional.pl");

        assertOneSolution("member(a, [a,b,c])", "member(b, [a,b,c])", "member(c, [a,b,c])");
        assertNoSolution("member(d, [a,b,c])");
        logger.info(CollectionUtils.format("All bindings: ", prolog.solve("member(X, [a,b,c])").all().ensureNumber(3).bindings(), 0));

        assertEquals("[1,2,3]", assertOneSolution("append([1],[2,3],X)").binding("X").toString());

        final MultipleSolutionsHolder all = assertNSolutions(3, "append(X,Y,[1,2])");
        assertEquals(termList("[]", "[1]", "[1,2]"), all.binding("X"));
        assertEquals(termList("[1,2]", "[2]", "[]"), all.binding("Y"));
    }

    @Test
    public void sumial() {
        loadTheoryFromTestResourceDir("test-functional.pl");
        assertEquals(term(15), assertOneSolution("sumial(5, X)").binding("X"));
        assertEquals(term(55), assertOneSolution("sumial(10, X)").binding("X"));
        assertEquals(term(5050), assertOneSolution("sumial(100, X)").binding("X"));
    }

    @Test
    public void unify() {
        loadTheoryFromTestResourceDir("test-functional.pl");
        assertOneSolution("unifyterms(X,X)");
        assertEquals(term(123), assertOneSolution("unifyterms21(X,123)").binding("X"));
        assertEquals(term(123), assertOneSolution("unifyterms21(123, X)").binding("X"));
        assertEquals(term(123), assertOneSolution("unifyterms22(X,123)").binding("X"));
        assertEquals(term(123), assertOneSolution("unifyterms22(123, X)").binding("X"));
    }

    /**
     * Sometimes (when?) X is bound to a term containing a unified var to another of our
     * 
     * 
     */
    @Test
    public void relink_vars() {
        loadTheoryFromTestResourceDir("test-functional.pl");
        // Below, Y must be equal to g(123,X), but does not solve to X!
        assertEquals(term("g(123,X)"), assertOneSolution("unifyterms3(f(123,X), Y)").binding("Y"));

        // Is this really what we should have??? Free internal bindings popping up to higher levels?
        assertEquals(term("f(_)"), assertOneSolution("final(X)").binding("X"));
    }

}
