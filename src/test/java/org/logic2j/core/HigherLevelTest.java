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
package org.logic2j.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;
import org.logic2j.core.library.impl.IOLibrary;
import org.logic2j.engine.model.PrologLists;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.solver.holder.GoalHolder;

/**
 * Run higher-level tests such as whole programs.
 * When this works, there is much chance that the {@link org.logic2j.core.impl.PrologImplementation} is satisfactory for solving real-life problems, although full
 * test coverage is far from guaranteed.
 * For performance testing see {@link BenchmarkTest}.
 */
public class HigherLevelTest extends PrologTestBase {

  @Override
  protected InitLevel initLevel() {
    return InitLevel.L2_BASE_LIBRARIES;
  }

  /**
   * Reasonably-sided Towers of Hanoi. See also {@link BenchmarkTest#hanoi()}
   */
  @Test
  public void hanoi() {
    loadTheoryFromTestResourcesDir("hanoi.pro");
    uniqueSolution("move(5, left, right, center)"); // Watch out 7 is the limit with Java's ridiculous default stack size
  }

  /**
   * This simple Prolog program checks or generates change adding up to a dollar consisting of half-dollars, quarters, dimes, nickels, and
   * pennies.
   */
  @Test
  public void changeForOneDollar() {
    final IOLibrary library = new IOLibrary(this.prolog);
    loadLibrary(library);
    loadTheoryFromTestResourcesDir("dollar.pro");
    nSolutions(292, "change([H,Q,D,N,P])");
  }

  /**
   * N-Queens problem, lighter ones.
   * See also {@link #queensHeavierForThePatientOne()}
   */
  @Test
  public void queensLighter() {
    loadTheoryFromTestResourcesDir("queens.pro");

    assertThat(uniqueSolution("queens(1, Positions)").var("Positions").unique().toString()).isEqualTo("[1]");
    GoalHolder solutions;
    //
    //
    solutions = this.prolog.solve("queens(4, Positions)");
    assertThat(solutions.var("Positions").list().toString()).isEqualTo("[[3,1,4,2], [2,4,1,3]]");
    //
    solutions = this.prolog.solve("queens(5, Positions)");
    assertThat(solutions.var("Positions").list().toString()).isEqualTo(
            "[[4,2,5,3,1], [3,5,2,4,1], [5,3,1,4,2], [4,1,3,5,2], [5,2,4,1,3], [1,4,2,5,3], [2,5,3,1,4], [1,3,5,2,4], [3,1,4,2,5], [2,4,1,3,5]]");
    //
    solutions = this.prolog.solve("queens(6, Positions)");
    assertThat(solutions.var("Positions").list().toString()).isEqualTo("[[5,3,1,6,4,2], [4,1,5,2,6,3], [3,6,2,5,1,4], [2,4,6,1,3,5]]");
    //
    nSolutions(0, "queens(2, _)");
    nSolutions(0, "queens(3, _)");
    nSolutions(2, "queens(4, _)");
    nSolutions(10, "queens(5, _)");
    nSolutions(4, "queens(6, _)");
    nSolutions(40, "queens(7, _)");
    nSolutions(92, "queens(8, _)");
    nSolutions(352, "queens(9, _)");
  }

  /**
   * N-Queens problem, heavy ones.
   * See {@link #queensLighter()}
   */
  //@Ignore("Very CPU intensive and quite long - enable when needed - does not bring much in functional testing")
  @Test
  public void queensHeavierForThePatientOne() {
    loadTheoryFromTestResourcesDir("queens.pro");
    nSolutions(724, "queens(10, _)");
    nSolutions(2680, "queens(11, _)"); // tuProlog (GUI) needs 261s on my machine
  }


  @Test
  public void queensWithFindall() {
    loadTheoryFromTestResourcesDir("queens.pro");
    final String goal = "findall(X, queens(5, X), List)";
    // Numbers
    final Struct<?> plist = getProlog().solve(goal).var("List", Struct.class).unique();
    assertThat(PrologLists.listSize(plist)).isEqualTo(10);
    assertThat(plist.toString())
            .isEqualTo("[[4,2,5,3,1],[3,5,2,4,1],[5,3,1,4,2],[4,1,3,5,2],[5,2,4,1,3],[1,4,2,5,3],[2,5,3,1,4],[1,3,5,2,4],[3,1,4,2,5],[2,4,1,3,5]]");
  }

  @Test
  public void takeout() {
    loadTheoryFromTestResourcesDir("sorting.pro");
    GoalHolder solutions;
    //
    nSolutions(0, "takeout(a, [], X)");
    //
    solutions = this.prolog.solve("takeout(a, [a], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[]]");
    //
    nSolutions(0, "takeout(k, [a], X)");
    //
    solutions = this.prolog.solve("takeout(a, [a, b, c], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[b,c]]");
    //
    solutions = this.prolog.solve("takeout(b, [a, b, c], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[a,c]]");
    //
    solutions = this.prolog.solve("takeout(c, [a, b, c], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[a,b]]");
    //
    nSolutions(0, "takeout(k, [a, b, c], X)");
    //
    solutions = this.prolog.solve("takeout(X, [a, b, c], Y)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[a, b, c]");
    assertThat(solutions.var("Y").list().toString()).isEqualTo("[[b,c], [a,c], [a,b]]");
    //
    nSolutions(10, "takeout(_, [_,_,_,_,_,_,_,_,_,_], _)");
  }

  @Test
  public void permutations() {
    loadTheoryFromTestResourcesDir("sorting.pro");
    GoalHolder solutions;
    //
    solutions = this.prolog.solve("perm([], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[]]");
    //
    solutions = this.prolog.solve("perm([a], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[a]]");
    //
    solutions = this.prolog.solve("perm([a,b], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[a,b], [b,a]]");
    //
    solutions = this.prolog.solve("perm([a,b,c], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[a,b,c], [a,c,b], [b,a,c], [b,c,a], [c,a,b], [c,b,a]]");
    //
    nSolutions(24, "perm([_,_,_,_], _)");
    nSolutions(40320, "perm([_,_,_,_,_,_,_,_], _)");
  }

  @Test
  public void naive_sort() {
    loadTheoryFromTestResourcesDir("sorting.pro");
    GoalHolder solutions;
    //
    solutions = this.prolog.solve("naive_sort([6,3,9,1], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,3,6,9]]");
    //
    solutions = this.prolog.solve("naive_sort([1,7,9,3,2,4,5,8], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,2,3,4,5,7,8,9]]");
  }

  @Test
  public void insert_sort() {
    loadTheoryFromTestResourcesDir("sorting.pro");
    GoalHolder solutions;
    //
    solutions = this.prolog.solve("insert_sort([6,3,9,1], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,3,6,9]]");
    //
    solutions = this.prolog.solve("insert_sort([1,7,9,3,2,4,5,8], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,2,3,4,5,7,8,9]]");
  }

  @Test
  public void bubble_sort() {
    loadTheoryFromTestResourcesDir("sorting.pro");
    GoalHolder solutions;
    //
    solutions = this.prolog.solve("bubble_sort([6,3,9,1], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,3,6,9]]");
    //
    solutions = this.prolog.solve("bubble_sort([1,7,9,3,2,4,5,8], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,2,3,4,5,7,8,9]]");
  }

  @Test
  public void merge_sort() {
    loadTheoryFromTestResourcesDir("sorting.pro");
    GoalHolder solutions;
    //
    solutions = this.prolog.solve("merge_sort([6,3,9,1], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,3,6,9]]");
    //
    solutions = this.prolog.solve("merge_sort([1,7,9,3,2,4,5,8], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,2,3,4,5,7,8,9]]");
  }

  @Test
  public void quick_sort() {
    loadTheoryFromTestResourcesDir("sorting.pro");
    GoalHolder solutions;
    //
    solutions = this.prolog.solve("quick_sort([6,3,9,1], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,3,6,9]]");
    //
    solutions = this.prolog.solve("quick_sort([1,7,9,3,2,4,5,8], X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[[1,2,3,4,5,7,8,9]]");
  }

  @Test
  public void fibonacci() {
    loadTheoryFromTestResourcesDir("fibonacci.pro");
    GoalHolder solutions;
    //
    solutions = this.prolog.solve("fib(2, X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[1]");
    //
    solutions = this.prolog.solve("fib(3, X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[2]");
    //
    solutions = this.prolog.solve("fib(4, X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[3]");
    //
    solutions = this.prolog.solve("fib(8, X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[21]");
    // Requires a lot of stack, use with -Xss10m
    // solutions = this.prolog.solve("fib(10, X)");
    // assertEquals("[55]", solutions.var("X").list().toString());
  }

  @Test
  public void mappingTransformer() {
    loadTheoryFromTestResourcesDir("transformations.pro");
    assertThat(this.prolog.solve("transformForContext(main(13), Z)").vars().list().toString())
            .isEqualTo("[{Z=eav(13, classification, LEVEL_MAIN)}]");
    assertThat(this.prolog.solve("transformForContext(tc(ID), Z)").vars().list().toString())
            .isEqualTo("[{ID=ID, Z=','(eav(ID, class, Committee), eav(ID, classification, LEVEL_MAIN))}]");
    assertThat(this.prolog.solve("transformForContext(committee(ID), Z)").vars().list().toString())
            .isEqualTo("[{ID=ID, Z=eav(ID, class, Committee)}]");
    //
    assertThat(this.prolog.solve("transformForContext(11, Z)").vars().list().toString()).isEqualTo("[{Z=','(one, ten)}]");
    assertThat(this.prolog.solve("transformForContext(10, Z)").vars().list().toString()).isEqualTo("[{Z=ten}]");
    assertThat(this.prolog.solve("transformForContext(1, Z)").vars().list().toString()).isEqualTo("[{Z=one}]");
    assertThat(this.prolog.solve("transformForContext(4, Z)").vars().list().toString()).isEqualTo("[{Z=4}]");
  }
}
