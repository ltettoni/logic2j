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
package org.logic2j.library.impl.core;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.solve.SolutionHolder.UniqueSolutionHolder;

public class CoreLibraryTest extends PrologTestBase {

  @Test
  public void test_reproduce_error() throws IOException {
    // Nothing at the moment
  }

  @Test
  public void test_unify() {
    final String[] SINGLE_SOLUTION_GOALS = new String[] { "2=2", //
        "X=2", //
        "2\\=4", //
        "X=2, X\\=4", //
        "X=2, 4\\=X", //
        "X=2, Y=3, X\\=Y", //
    };
    assertOneSolution(SINGLE_SOLUTION_GOALS);

    final String[] NO_SOLUTION_GOALS = new String[] { "1=2", //
        "X=2,X=3", //
        "4\\=4", //
        "X=4, X\\=4", //
        "X=4, 4\\=X", //
        "X=2, Y=2, X\\=Y", //
    };
    assertNoSolution(NO_SOLUTION_GOALS);
  }

  @Test
  public void test_var() {
    assertOneSolution("var(_)");
    assertOneSolution("var(V)");
    assertNoSolution("var(1)");
    assertNoSolution("var(a)");
    assertNoSolution("var(a(b))");
    assertNoSolution("var(X, Y, Z)");
  }

  // TODO Some uncertainties re the behaviour of no-op binding of free bindings. To be clarified.
  @Ignore // See note above
  @Test
  public void testSolvePrimitivePredicates_representation_FREE() {
    assertEquals(term("X"), assertOneSolution("X=X").binding("X"));
    assertEquals(term("Y"), assertOneSolution("X=Y").binding("X"));
    assertEquals(term("Y"), assertOneSolution("X=Y").binding("Y"));
    assertEquals(term(2), assertOneSolution("X=2").binding("X"));
    //
    assertEquals(term("Z"), assertOneSolution("p(X,Y) = p(Z,Z)").binding("X"));
    assertEquals(term(2), assertOneSolution("X=Y, 2=X").binding("X"));
    assertEquals(term(2), assertOneSolution("X=Y, 2=X").binding("Y"));
    //
    assertOneSolution("2>1");
    assertOneSolution("A=1, 2>A");
  }

  @Test
  public void testSolvePrimitivePredicates_representation_NULL() {
    assertEquals(null, assertOneSolution("X=X").binding("X"));
    assertEquals(null, assertOneSolution("X=Y").binding("X"));
    assertEquals(null, assertOneSolution("X=Y").binding("Y"));
    assertEquals(term(2), assertOneSolution("X=2").binding("X"));
    //
    assertEquals(null, assertOneSolution("p(X,Y) = p(Z,Z)").binding("X"));
    assertEquals(term(2), assertOneSolution("X=Y, 2=X").binding("X"));
    assertEquals(term(2), assertOneSolution("X=Y, 2=X").binding("Y"));
    //
    assertOneSolution("2>1");
    assertOneSolution("A=1, 2>A");
  }

  @Test
  public void test_is() {
    assertEquals(term(6), assertOneSolution("X is 2+4").binding("X"));
    assertEquals(term(-5), assertOneSolution("X is 5-10").binding("X"));
    assertEquals(term(-12), assertOneSolution("X is -12").binding("X"));
    assertEquals(term(1), assertOneSolution("X is 6 - (2+3)").binding("X"));
    assertEquals(term(9), assertOneSolution("N=10, M is N-1").binding("M"));
  }

  @Test
  public void test_predicate2PList() {
    assertNoSolution("a(b,c,d) =.. f");
    assertEquals("[a,b,c,d]", assertOneSolution("a(b,c,d) =.. X").binding("X").toString());
    assertEquals("a(b, c)", assertOneSolution("X =.. [a,b,c]").binding("X").toString());
    UniqueSolutionHolder sol = assertOneSolution("Expr=coco(Com), Expr=..[Pred, Arg]");
    assertEquals("coco(Com)", sol.binding("Expr").toString(DEFAULT_FORMATTER));
    assertEquals("coco", sol.binding("Pred").toString());
    assertNull(sol.binding("Arg"));
//    assertEquals("Com", sol.binding("Arg").toString());
    //    assertEquals("a(b,c)", getProlog().solve("X =.. atom").unique().binding("X").toString());
  }

  @Test
  public void test_reverse() {
    assertEquals(term("[c,b,a]"), getProlog().solve("reverse([a,b,c], L)").unique().binding("L"));
  }

  @Test
  public void test_perm() {
    assertEquals(720, getProlog().solve("perm([a,b,c,d,e,f], L)").number());
  }

  @Test
  public void call() throws IOException {
    assertNoSolution("call(false)");
    assertOneSolution("call(true)");
    assertNSolutions(3, "call(true;true;true)");
    assertGoalMustFail("X=true, X");
    assertGoalMustFail("call(X)");
    
    addTheory("src/test/resources/test-functional.pl");
    String arg = "X";
    int n = 3;
    assertNSolutions(n, "call_check(" + arg + ")");
    assertNSolutions(n, "call(call_check(" + arg + "))");
    assertNSolutions(n, "call(call(call_check(" + arg + ")))");
    assertNSolutions(n, "call(call(call(call_check(" + arg + "))))");
    assertNSolutions(n, "call_over_call(call_check(" + arg + "))");
    assertNSolutions(n, "call_over_call(call(call_check(" + arg + ")))");
    assertNSolutions(n, "call(call_over_call(call_check(" + arg + ")))");
    //
    arg = "2";
    n = 1;
    assertNSolutions(n, "call_check(" + arg + ")");
    assertNSolutions(n, "call(call_check(" + arg + "))");
    assertNSolutions(n, "call(call(call_check(" + arg + ")))");
    assertNSolutions(n, "call(call(call(call_check(" + arg + "))))");
    assertNSolutions(n, "call_over_call(call_check(" + arg + "))");
    assertNSolutions(n, "call_over_call(call(call_check(" + arg + ")))");
    assertNSolutions(n, "call(call_over_call(call_check(" + arg + ")))");
    //
    arg = "4";
    n = 0;
    assertNSolutions(n, "call_check(" + arg + ")");
    assertNSolutions(n, "call(call_check(" + arg + "))");
    assertNSolutions(n, "call(call(call_check(" + arg + ")))");
    assertNSolutions(n, "call(call(call(call_check(" + arg + "))))");
    assertNSolutions(n, "call_over_call(call_check(" + arg + "))");
    assertNSolutions(n, "call_over_call(call(call_check(" + arg + ")))");
    assertNSolutions(n, "call(call_over_call(call_check(" + arg + ")))");
  }

  @Test
  public void clause() throws IOException {
    addTheory("src/test/resources/test-functional.pl");

    assertGoalMustFail("clause(X,_)", "clause(_,_)", "clause(1,_)");
    assertNoSolution("clause(a)", "clause(a,b,c)");
    assertNoSolution("clause(a(1), false)");
    assertOneSolution("clause(a(1), true)");
    assertNSolutions(3, "clause(a(_), true)");
    assertOneSolution("clause(a(X), true), X=1");
    assertOneSolution("clause(a(X), true), X=2");
    assertOneSolution("clause(a(X), true), X=3");
    assertNSolutions(3, "clause(a(X), Z)");
    assertNSolutions(3, "clause(a(X), Z), Z=true");
    assertEquals(termList(true, true, true), assertNSolutions(3, "clause(a(X), Z), Z\\=false").binding("Z"));
    System.out.println(assertNSolutions(3, "clause(a(X), Z), Z\\=false").binding("Z"));
    assertEquals(termList(1, 2, 3), assertNSolutions(3, "clause(a(X), true)").binding("X"));
    assertNSolutions(5, "clause(f(_), true)");
    assertEquals(termList(2), assertNSolutions(1, "clause(c2(X), !)").binding("X"));
  }

  @Test
  public void test_unify_2() throws IOException {
    addTheory("src/test/resources/test-functional.pl");

    assertNSolutions(5, "bool_3t_2f(X)");
    assertNSolutions(3, "bool_3t_2f(X), X=true");
    assertNSolutions(3, "bool_3t_2f(X), X\\=false");
    assertNSolutions(2, "bool_3t_2f(X), X=false");
    assertNSolutions(2, "bool_3t_2f(X), X\\=true");
  }
  
  @Test
  public void atom_length() throws Exception {
    assertOneSolution("atom_length(a, 1)");
    assertNoSolution("atom_length(ab, 1)");
    //
    assertOneSolution("atom_length(abc, X), X=3");
    // TODO: This fails with an Exception - improve
    // assertNoSolution("atom_length(X, 3)");
  }
}
