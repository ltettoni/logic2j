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
import static org.logic2j.engine.model.SimpleBindings.bind;
import static org.logic2j.engine.model.Var.intVar;

import org.junit.Test;
import org.logic2j.engine.model.Binding;
import org.logic2j.engine.model.TermApi;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.predicates.impl.Eq;
import org.logic2j.engine.predicates.impl.generator.Digit;
import org.logic2j.engine.predicates.impl.math.compare.LT;
import org.logic2j.engine.solver.listener.CountingSolutionListener;

/**
 * Check how the interaction is done with the lower-level logic2j-engine features.
 */
public class InteractionWithEngineTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InteractionWithEngineTest.class);

  @Test
  public void testUsingFOPredicateLt() {
    final Object term = new LT(bind(3), bind(4));
    final Object goal = new TermApi().normalize(term);
    final CountingSolutionListener listener = new CountingSolutionListener();
    getProlog().getSolver().solveGoal(term, listener);
    assertThat(listener.count()).as("Solving term \"" + term + '"').isEqualTo(1);
  }

  @Test
  public void testUsingFOPredicateEq() {
    final Var<Integer> Z = intVar("Z");
    final Object term = new Eq(bind(2,3,4), (Binding) Z);
    final Object goal = new TermApi().normalize(term);
    final ExtractingSolutionListener listener = solveWithExtractingListener(goal);
    assertThat(listener.count()).isEqualTo(3);
    assertThat(listener.getValues("Z")).contains(2,3,4);
  }

  @Test
  public void testUsingFOPredicateDigit() {
    final Var<Integer> Z = intVar("Z");
    final Object term = new Digit(Z);
    final Object goal = new TermApi().normalize(term);
    final ExtractingSolutionListener listener = solveWithExtractingListener(goal);
    assertThat(listener.count()).isEqualTo(10);
    assertThat(listener.getValues("Z")).contains(0,1,2,3,4,5,6,7,8,9);
  }

  @Test
  public void fromGoalSimple() {
    countOneSolution("2 < 3");
  }

  @Test
  public void fromGoalFOPredicate() {
    countNSolutions(10, "digit(_)");
  }

  @Test
  public void fromTheory() {
    loadTheoryFromTestResourcesDir("test-engine.pro");
    countOneSolution("twoLessThanThree");
  }

}
