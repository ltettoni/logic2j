package org.logic2j.library.impl.core;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.PrologTestBase;

/**
 */
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

  @Ignore
  @Test
  public void testSolvePrimitivePredicates_behaviour_FREE() {
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
  public void testSolvePrimitivePredicates_behaviour_NULL() {
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

  /*
  @Test
  public void test_copy_term() {
    // Unify X with a term, then copy X to Y, then fail: X will be deunified. 
    // Finally take the second branch  of the "or" and succeed: Y must have kept the value!
    UniqueSolutionHolder sol = assertOneSolution("(X=12, copy_term(X, Y), fail) ; true");
    assertEquals(null, sol.binding("X"));
    assertEquals(term(12), sol.binding("Y"));
  }
  */

  @Test
  public void test_call() {
    assertNoSolution("call(false)");
    assertOneSolution("call(true)");
    assertNSolutions(3, "call(true;true;true)");
    assertGoalMustFail("X=true, X");
    assertGoalMustFail("call(X)");
  }

  @Test
  public void test_clause() throws IOException {
    addTheory("test/input/test-functional.pl");

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
    addTheory("test/input/test-functional.pl");

    assertNSolutions(5, "bool_3t_2f(X)");
    assertNSolutions(3, "bool_3t_2f(X), X=true");
    assertNSolutions(3, "bool_3t_2f(X), X\\=false");
    assertNSolutions(2, "bool_3t_2f(X), X=false");
    assertNSolutions(2, "bool_3t_2f(X), X\\=true");
  }
}