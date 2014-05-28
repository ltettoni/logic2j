package org.logic2j.core.library.impl.core;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;

import static org.junit.Assert.assertEquals;

public class CoreLibraryTest extends PrologTestBase {


    @Test
    public void placeholderToReproduceError() {
        //
    }

    @Test
    public void var() {
        countOneSolution("var(_)");
        countOneSolution("var(V)");
        countNoSolution("var(1)");
        countNoSolution("var(a)");
        countNoSolution("var(a(b))");
        countNoSolution("var(X, Y, Z)");
    }

    @Test
    public void is() {
        assertEquals(term(6), uniqueSolution("X is 2+4").longValue("X"));
        assertEquals(term(-5), uniqueSolution("X is 5-10").longValue("X"));
        assertEquals(term(-12), uniqueSolution("X is -12").longValue("X"));
        assertEquals(term(1), uniqueSolution("X is 6 - (2+3)").longValue("X"));
        assertEquals(term(9), uniqueSolution("N=10, M is N-1").longValue("M"));
    }

    @Test
    public void call() {
        countNoSolution("call(false)");
        countOneSolution("call(true)");
        assertGoalMustFail("X=true, X");
        assertGoalMustFail("call(X)");

        loadTheoryFromTestResourcesDir("test-functional.pl");
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
        countOneSolution("not(fail)" , "\\+(fail)");
        countNoSolution("not(true)", "\\+(true)");
    }


//    @Test
//    public void unify() {
//        final String[] SINGLE_SOLUTION_GOALS = new String[]{"2=2", //
//        "X=2", //
//        "2\\=4", //
//        "X=2, X\\=4", //
//        "X=2, 4\\=X", //
//        "X=2, Y=3, X\\=Y", //
//        };
//        countOneSolution(SINGLE_SOLUTION_GOALS);
//
//        final String[] NO_SOLUTION_GOALS = new String[]{"1=2", //
//        "X=2,X=3", //
//        "4\\=4", //
//        "X=4, X\\=4", //
//        "X=4, 4\\=X", //
//        "X=2, Y=2, X\\=Y", //
//        };
//        countNoSolution(NO_SOLUTION_GOALS);
//    }
//

//
//    // TODO Some uncertainties re. the desired behaviour of no-op binding of free bindings. To be clarified.
//    @Ignore("Need to clarify the behaviour of no-op binding of free bindings")
//    @Test
//    public void solvePrimitivePredicates_representation_FREE() {
//        assertEquals(term("X"), countOneSolution("X=X").binding("X"));
//        assertEquals(term("Y"), countOneSolution("X=Y").binding("X"));
//        assertEquals(term("Y"), countOneSolution("X=Y").binding("Y"));
//        assertEquals(term(2), countOneSolution("X=2").binding("X"));
//        //
//        assertEquals(term("Z"), countOneSolution("p(X,Y) = p(Z,Z)").binding("X"));
//        assertEquals(term(2), countOneSolution("X=Y, 2=X").binding("X"));
//        assertEquals(term(2), countOneSolution("X=Y, 2=X").binding("Y"));
//        //
//        countOneSolution("2>1");
//        countOneSolution("A=1, 2>A");
//    }
//
//    @Test
//    public void solvePrimitivePredicates_representation_NULL() {
//        assertEquals(null, countOneSolution("X=X").binding("X"));
//        assertEquals(null, countOneSolution("X=Y").binding("X"));
//        assertEquals(null, countOneSolution("X=Y").binding("Y"));
//        assertEquals(term(2), countOneSolution("X=2").binding("X"));
//        //
//        assertEquals(null, countOneSolution("p(X,Y) = p(Z,Z)").binding("X"));
//        assertEquals(term(2), countOneSolution("X=Y, 2=X").binding("X"));
//        assertEquals(term(2), countOneSolution("X=Y, 2=X").binding("Y"));
//        //
//        countOneSolution("2>1");
//        countOneSolution("A=1, 2>A");
//    }
//
 //
//    @Test
//    public void predicate2PList_1() {
//        countNoSolution("a(b,c,d) =.. f");
//        assertEquals("[a,b,c,d]", countOneSolution("a(b,c,d) =.. X").binding("X").toString());
//        assertEquals("a(b, c)", countOneSolution("X =.. [a,b,c]").binding("X").toString());
//    }
//
//    @Test
//    public void predicate2PList_2() {
//        Map<String, Object> bindings = countOneSolution("Expr=coco(Com), Expr=..[Pred, Arg]").bindings();
//
//        assertEquals("coco", bindings.get("Pred").toString());
//        assertNull(bindings.get("Arg"));
//        assertNull(bindings.get("Com"));
//
//        assertEquals("coco(Com)", bindings.get("Expr").toString());
//        //        assertEquals("coco(Arg)", bindings.get("Expr").toString());
//    }
//
//    @Test
//    public void reverse() {
//        assertEquals(term("[c,b,a]"), this.prolog.solve("reverse([a,b,c], L)").unique().binding("L"));
//    }
//
//    @Test
//    public void perm() {
//        assertEquals(720, this.prolog.solve("perm([a,b,c,d,e,f], L)").count());
//    }
//

//
//    @Test
//    public void clause() {
//        loadTheoryFromTestResourcesDir("test-functional.pl");
//
//        assertGoalMustFail("clause(X,_)", "clause(_,_)", "clause(1,_)");
//        countNoSolution("clause(a)", "clause(a,b,c)");
//        countNoSolution("clause(a(1), false)");
//        countOneSolution("clause(a(1), true)");
//        countNSolutions(3, "clause(a(_), true)");
//        countOneSolution("clause(a(X), true), X=1");
//        countOneSolution("clause(a(X), true), X=2");
//        countOneSolution("clause(a(X), true), X=3");
//        countNSolutions(3, "clause(a(X), Z)");
//        countNSolutions(3, "clause(a(X), Z), Z=true");
//        assertEquals(termList("true", "true", "true"), countNSolutions(3, "clause(a(X), Z), Z\\=false").binding("Z"));
//        countNSolutions(3, "clause(a(X), Z), Z\\=false").binding("Z");
//        assertEquals(termList("1", "2", "3"), countNSolutions(3, "clause(a(X), true)").binding("X"));
//        countNSolutions(5, "clause(f(_), true)");
//        assertEquals(termList("2"), countNSolutions(1, "clause(cut2(X), !)").binding("X"));
//    }
//
//    @Test
//    public void unify_2() {
//        loadTheoryFromTestResourcesDir("test-functional.pl");
//
//        countNSolutions(5, "bool_3t_2f(X)");
//        countNSolutions(3, "bool_3t_2f(X), X=true");
//        countNSolutions(3, "bool_3t_2f(X), X\\=false");
//        countNSolutions(2, "bool_3t_2f(X), X=false");
//        countNSolutions(2, "bool_3t_2f(X), X\\=true");
//    }
//
//    @Test
//    public void atom_length() {
//        countOneSolution("X=abc, atom_length(X, 3)");
//        //
//        countOneSolution("atom_length(a, 1)");
//        countOneSolution("atom_length(abc, 3)");
//        countNoSolution("atom_length(abc, 1)");
//        //
//        countOneSolution("X=abc, atom_length(X, 3)");
//        countNoSolution("X=abc, atom_length(X, 1)");
//        //
//        countOneSolution("atom_length(abc, X), X=3");
//    }
//
//    @Test
//    public void arrow() {
//        countOneSolution("true->true");
//        countNoSolution("true->fail");
//        countNoSolution("fail->true");
//        countNoSolution("fail->fail");
//    }
//
//    // FIXME This fails with an Exception - check what proper behaviour should be and fix
//    @Ignore("FIXME This fails with a PrologNonSpecificError - check what proper behaviour should be, and fix")
//    org.logic2j.core.api.model.exception.PrologNonSpecificError: Cannot call primitive atom_length/2 with a free variable goal
//    @Test
//    public void atom_length_free_var() {
//        countNoSolution("atom_length(X, 3)");
//    }
}
