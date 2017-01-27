/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.logic2j.core.library.impl;

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.solver.holder.GoalHolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CoreLibraryTest extends PrologTestBase {


    @Test
    public void placeholderToReproduceError() {
        // Empty yet
    }

    @Test
    public void var() {
        uniqueSolution("var(_)");
        uniqueSolution("var(V)");
        countNoSolution("var(1)");
        countNoSolution("var(a)");
        countNoSolution("var(a(b))");
        countNoSolution("var(X, Y, Z)");
    }

    @Test
    public void is() {
        assertEquals(term(6), uniqueSolution("X is 2+4").intValue("X"));
        assertEquals(term(-5), uniqueSolution("X is 5-10").intValue("X"));
        assertEquals(term(-12), uniqueSolution("X is -12").intValue("X"));
        assertEquals(term(1), uniqueSolution("X is 6 - (2+3)").intValue("X"));
        assertEquals(term(9), uniqueSolution("N=10, M is N-1").intValue("M"));
    }

    @Test
    public void call() {
        countNoSolution("call(false)");
        uniqueSolution("call(true)");
        assertGoalMustFail("X=true, X");
        assertGoalMustFail("call(X)");

        loadTheoryFromTestResourcesDir("test-functional.pro");
        String arg = "X";
        int n = 3;
        countNSolutions(n, "call_check(" + arg + ")");
        countNSolutions(n, "call(call_check(" + arg + "))");
        countNSolutions(n, "call(call(call_check(" + arg + ")))");
        countNSolutions(n, "call(call(call(call_check(" + arg + "))))");
        countNSolutions(n, "call_over_call(call_check(" + arg + "))");
        countNSolutions(n, "call_over_call(call(call_check(" + arg + ")))");
        countNSolutions(n, "call(call_over_call(call_check(" + arg + ")))");
        //
        arg = "2";
        n = 1;
        countNSolutions(n, "call_check(" + arg + ")");
        countNSolutions(n, "call(call_check(" + arg + "))");
        countNSolutions(n, "call(call(call_check(" + arg + ")))");
        countNSolutions(n, "call(call(call(call_check(" + arg + "))))");
        countNSolutions(n, "call_over_call(call_check(" + arg + "))");
        countNSolutions(n, "call_over_call(call(call_check(" + arg + ")))");
        countNSolutions(n, "call(call_over_call(call_check(" + arg + ")))");
        //
        arg = "4";
        n = 0;
        countNSolutions(n, "call_check(" + arg + ")");
        countNSolutions(n, "call(call_check(" + arg + "))");
        countNSolutions(n, "call(call(call_check(" + arg + ")))");
        countNSolutions(n, "call(call(call(call_check(" + arg + "))))");
        countNSolutions(n, "call_over_call(call_check(" + arg + "))");
        countNSolutions(n, "call_over_call(call(call_check(" + arg + ")))");
        countNSolutions(n, "call(call_over_call(call_check(" + arg + ")))");
    }


    @Test
    public void not() {
        // Surprisingly enough, the operator \+ means "not provable".
        uniqueSolution("not(fail)", "\\+(fail)");
        countNoSolution("not(true)", "\\+(true)");
    }


    @Test
    public void unify() {
        final String[] SINGLE_SOLUTION_GOALS = new String[]{"2=2", //
        "X=2", //
        "2\\=4", //
        "X=2, X\\=4", //
        "X=2, 4\\=X", //
        "X=2, Y=3, X\\=Y", //
        };
        uniqueSolution(SINGLE_SOLUTION_GOALS);

        final String[] NO_SOLUTION_GOALS = new String[]{"1=2", //
        "X=2,X=3", //
        "4\\=4", //
        "X=4, X\\=4", //
        "X=4, 4\\=X", //
        "X=2, Y=2, X\\=Y", //
        };
        countNoSolution(NO_SOLUTION_GOALS);
    }

    // TODO Some uncertainties re. the desired behaviour of no-op binding of free bindings. To be clarified.
    @Ignore("Need to clarify the behaviour of no-op binding of free bindings")
    @Test
    public void solvePrimitivePredicates_representation_FREE() {
        assertEquals(term("X"), uniqueSolution("X=X").var("X").unique());
        assertEquals(term("Y"), uniqueSolution("X=Y").var("X").unique());
        assertEquals(term("Y"), uniqueSolution("X=Y").var("Y").unique());
        assertEquals(term(2), uniqueSolution("X=2").var("X").unique());
        //
        assertEquals(term("Z"), uniqueSolution("p(X,Y) = p(Z,Z)").var("X").unique());
        assertEquals(term(2), uniqueSolution("X=Y, 2=X").var("X").unique());
        assertEquals(term(2), uniqueSolution("X=Y, 2=X").var("Y").unique());
        //
        uniqueSolution("2>1");
        uniqueSolution("A=1, 2>A");
    }

    @Test
    public void solvePrimitivePredicates_representation_NULL() {
        assertEquals("X", uniqueSolution("X=X").var("X").unique().toString());
        assertEquals("X", uniqueSolution("X=Y").var("X").unique().toString());
        assertEquals("X", uniqueSolution("X=Y").var("Y").unique().toString());
        assertEquals(term(2), uniqueSolution("X=2").var("X").unique());
        //
        assertEquals("Y", uniqueSolution("p(X,Y) = p(Z,Z)").var("X").unique().toString());
        assertEquals(term(2), uniqueSolution("X=Y, 2=X").var("X").unique());
        assertEquals(term(2), uniqueSolution("X=Y, 2=X").var("Y").unique());
        //
        uniqueSolution("2>1");
        uniqueSolution("A=1, 2>A");
    }


    @Test
    public void predicate2PList_1() {
        countNoSolution("a(b,c,d) =.. f");
        assertEquals("[a,b,c,d]", uniqueSolution("a(b,c,d) =.. X").var("X").unique().toString());
        assertEquals("a(b, c)", uniqueSolution("X =.. [a,b,c]").var("X").unique().toString());
    }

    @Test
    public void predicate2PList_2() {
        final String string = varsSortedToString(uniqueSolution("Expr=coco(Com), Expr=..[Pred, Arg]"));
        assertEquals("[{Arg=Com, Com=Com, Expr=coco(Com), Pred=coco}]", string);
    }

    @Test
    public void reverse() {
        assertEquals(term("[c,b,a]"), this.prolog.solve("reverse([a,b,c], L)").var("L").unique());
    }

    @Test
    public void perm() {
        assertEquals(720, this.prolog.solve("perm([a,b,c,d,e,f], L)").count());
    }


    @Test
    public void clause() {
        loadTheoryFromTestResourcesDir("test-functional.pro");

        assertGoalMustFail("clause(X,_)", "clause(_,_)", "clause(1,_)");
        countNoSolution("clause(a)", "clause(a,b,c)");
        countNoSolution("clause(a(1), false)");
        uniqueSolution("clause(a(1), true)");
        countNSolutions(3, "clause(a(_), true)");
        uniqueSolution("clause(a(X), true), X=1");
        uniqueSolution("clause(a(X), true), X=2");
        uniqueSolution("clause(a(X), true), X=3");
        countNSolutions(3, "clause(a(X), Z)");
        countNSolutions(3, "clause(a(X), Z), Z=true");
        assertEquals(termList("true", "true", "true"), nSolutions(3, "clause(a(X), Z), Z\\=false").var("Z").list());
        countNSolutions(3, "clause(a(X), Z), Z\\=false");
        assertEquals(termList("1", "2", "3"), nSolutions(3, "clause(a(X), true)").var("X").list());
        countNSolutions(5, "clause(f(_), true)");
        assertEquals(term("2"), uniqueSolution("clause(cut2(X), !)").var("X").unique());
    }


    @Test
    public void clauseCanMatchVarForHead() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        countNSolutions(105, "clause(CLAUSE, BODY)");
    }


    @Test
    public void unify_2() {
        loadTheoryFromTestResourcesDir("test-functional.pro");

        countNSolutions(5, "bool_3t_2f(X)");
        countNSolutions(3, "bool_3t_2f(X), X=true");
        countNSolutions(3, "bool_3t_2f(X), X\\=false");
        countNSolutions(2, "bool_3t_2f(X), X=false");
        countNSolutions(2, "bool_3t_2f(X), X\\=true");
    }

    public void atom_length() {
        uniqueSolution("X=abc, atom_length(X, 3)");
        //
        uniqueSolution("atom_length(a, 1)");
        uniqueSolution("atom_length(abc, 3)");
        countNoSolution("atom_length(abc, 1)");
        //
        uniqueSolution("X=abc, atom_length(X, 3)");
        countNoSolution("X=abc, atom_length(X, 1)");
        //
        uniqueSolution("atom_length(abc, X), X=3");
    }

    @Test
    public void arrow() {
        uniqueSolution("true->true");
        countNoSolution("true->fail");
        countNoSolution("fail->true");
        countNoSolution("fail->fail");
    }

    @Test(expected = InvalidTermException.class)
    public void atomLengthOnFreeVarWhatShouldItDo() {
        countNoSolution("atom_length(X, 3)");
    }

    @Test
    public void count() {
        loadTheoryFromTestResourcesDir("test-data.pro");
        countNSolutions(10, "int10(_)");
        uniqueSolution("count(int10(_), 10)");
        countNoSolution("count(int10(_), 11)");
    }

    @Test
    public void exists() {
        loadTheoryFromTestResourcesDir("test-data.pro");
        final GoalHolder holder = uniqueSolution("exists(int10(X))");
        // Contrary to once/1, exists/1 does NOT bind variables
        assertTrue(holder.var("X").isFree());
        countNoSolution("exists(int10(56))");
    }

    @Test
    public void once() {
        loadTheoryFromTestResourcesDir("test-data.pro");
        final GoalHolder holder = uniqueSolution("once(int10(X))");
        assertEquals(1, holder.var("X").unique());
    }
}
