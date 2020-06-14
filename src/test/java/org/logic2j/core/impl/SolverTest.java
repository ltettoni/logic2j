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

package org.logic2j.core.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.logic2j.core.ExtractingSolutionListener;
import org.logic2j.core.PrologTestBase;
import org.logic2j.engine.solver.holder.GoalHolder;

/**
 * Lowest-level tests of the Solver: check core primitives: true, fail, cut, and, or. Check basic unification.
 * See other test classes for testing the solver against theories.
 */
public class SolverTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolverTest.class);

  // ---------------------------------------------------------------------------
  // Simplest primitives and undefined goal
  // ---------------------------------------------------------------------------


  @Test
  public void primitiveFail() {
    final Object goal = unmarshall("fail");
    final int nbSolutions = solveWithExtractingListener(goal).count();
    assertThat(nbSolutions).isEqualTo(0);
  }


  @Test
  public void primitiveTrue() {
    final Object goal = unmarshall("true");
    final int nbSolutions = solveWithExtractingListener(goal).count();
    assertThat(nbSolutions).isEqualTo(1);
  }

  @Test
  public void primitiveCut() {
    final Object goal = unmarshall("!");
    final int nbSolutions = solveWithExtractingListener(goal).count();
    assertThat(nbSolutions).isEqualTo(1);
  }

  @Test
  public void atomUndefined() {
    final Object goal = unmarshall("undefined_atom");
    final int nbSolutions = solveWithExtractingListener(goal).count();
    assertThat(nbSolutions).isEqualTo(0);
  }


  @Test
  public void primitiveTrueAndTrue() {
    final Object goal = unmarshall("true,true");
    final int nbSolutions = solveWithExtractingListener(goal).count();
    assertThat(nbSolutions).isEqualTo(1);
  }


  @Test
  public void primitiveTrueOrTrue() {
    final Object goal = unmarshall("true;true");
    final int nbSolutions = solveWithExtractingListener(goal).count();
    assertThat(nbSolutions).isEqualTo(2);
  }

  @Test
  public void corePrimitivesThatYieldUniqueSolution() {
    final String[] SINGLE_SOLUTION_GOALS = new String[]{ //
            "true", //
            "true, true", //
            "true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true", //
            "!", //
            "!, !", //
    };
    countOneSolution(SINGLE_SOLUTION_GOALS);
  }


  @Test
  public void corePrimitivesThatYieldNoSolution() {
    final String[] NO_SOLUTION_GOALS = new String[]{ //
            "fail", //
            "fail, fail", //
            "fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail, fail", //
            "true, fail", //
            "fail, true", //
            "true, true, fail", //
            "true, fail, !", //
    };
    countNoSolution(NO_SOLUTION_GOALS);
  }


  /**
   * This is a special feature of logic2j: AND with any arity
   */
  @Test
  public void nonBinaryAnd() {
    loadTheoryFromTestResourcesDir("test-functional.pro");
    final String[] SINGLE_SOLUTION_GOALS = new String[]{ //
            "','(true)", //
            "','(true, true)", //
            "','(true, !, true)", //
    };
    countOneSolution(SINGLE_SOLUTION_GOALS);
  }


  @Test
  public void or() {
    loadTheoryFromTestResourcesDir("test-functional.pro");
    countNSolutions(2, "';'(true, true)");
    countNSolutions(2, "true; true");
    //
    countNSolutions(3, "true; true; true");
    //
    GoalHolder solutions;
    solutions = this.prolog.solve("X=a; X=b; X=c");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[a, b, c]");
  }


  /**
   * This is a special feature of logic2j: OR with any arity
   */
  @Test
  public void nonBinaryOr() {
    loadTheoryFromTestResourcesDir("test-functional.pro");
    countNSolutions(2, "';'(true, true)");
//        if (Solver.INTERNAL_OR) {
//            countNSolutions(1, "';'(true)");
//            countNSolutions(3, "';'(true, true, true)");
//        }
    countNSolutions(1, "true");
    countNSolutions(3, "true; true; true");
  }


  @Test
  public void orWithVars() {
    GoalHolder solutions;
    solutions = this.prolog.solve("X=1; Y=2");
    final String actual = solutions.vars().list().toString();
    assertThat("[{Y=Y, X=1}, {Y=2, X=X}]".equals(actual) || "[{X=1, Y=Y}, {X=X, Y=2}]".equals(actual)).isTrue();
  }

  @Test
  public void orWithClause() {
    loadTheoryFromTestResourcesDir("test-functional.pro");
    GoalHolder solutions;
    solutions = this.prolog.solve("or3(X)");
    assertThat(solutions.var("X").list().toString()).isEqualTo("[a, b, c]");
  }

  @Test
  public void not() {
    // Surprisingly enough, the operator \+ means "not provable".
    uniqueSolution("not(fail)", "\\+(fail)");
    nSolutions(0, "not(true)", "\\+(true)");
  }


  // ---------------------------------------------------------------------------
  // Basic unification
  // ---------------------------------------------------------------------------


  @Test
  public void unifyLiteralsNoSolution() {
    final Object goal = unmarshall("a=b");
    final int nbSolutions = solveWithExtractingListener(goal).count();
    assertThat(nbSolutions).isEqualTo(0);
  }


  @Test
  public void unifyLiteralsOneSolution() {
    final Object goal = unmarshall("c=c");
    final int nbSolutions = solveWithExtractingListener(goal).count();
    assertThat(nbSolutions).isEqualTo(1);
  }


  @Test
  public void unifyAnonymousToAnonymous() {
    final Object goal = unmarshall("_=_");
    final int nbSolutions = solveWithExtractingListener(goal).count();
    assertThat(nbSolutions).isEqualTo(1);
  }


  @Test
  public void unifyVarToLiteral() {
    final Object goal = unmarshall("Q=d");
    final ExtractingSolutionListener listener = solveWithExtractingListener(goal);
    assertThat(listener.count()).isEqualTo(1);
    assertThat(listener.getVariables().toString()).isEqualTo("[Q]");
    assertThat(marshall(listener.getValues("."))).isEqualTo("[d = d]");
    assertThat(marshall(listener.getValues("Q"))).isEqualTo("[d]");
  }

  @Test
  public void unifyVarToAnonymous() {
    final Object goal = unmarshall("Q=_");
    final ExtractingSolutionListener listener = solveWithExtractingListener(goal);
    assertThat(listener.count()).isEqualTo(1);
    assertThat(listener.getVariables().toString()).isEqualTo("[Q]");
    assertThat(marshall(listener.getValues("."))).isEqualTo("[_ = _]");
    assertThat(marshall(listener.getValues("Q"))).isEqualTo("[_]");
  }


  @Test
  public void unifyVarToVar() {
    final Object goal = unmarshall("Q=Z");
    final ExtractingSolutionListener listener = solveWithExtractingListener(goal);
    assertThat(listener.count()).isEqualTo(1);
    assertThat(listener.getVarNames().toString()).isEqualTo("[., Q, Z]");
    assertThat(marshall(listener.getValues("."))).isEqualTo("[Q = Q]");
    assertThat(marshall(listener.getValues("Q"))).isEqualTo("[Q]");
    assertThat(marshall(listener.getValues("Z"))).isEqualTo("[Q]");
  }
}
