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

import org.junit.Before;
import org.junit.Test;
import org.logic2j.engine.exception.MissingSolutionException;
import org.logic2j.engine.exception.TooManySolutionsException;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.holder.GoalHolder;
import org.logic2j.engine.util.CollectionUtils;
import org.logic2j.engine.util.ProfilingInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the solution API (and describe its use cases too).
 * Run this only after DefaultSolverTest is successful.
 */
public class SolutionApiTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolutionApiTest.class);

  @Before
  public void loadTheory() {
    loadTheoryFromTestResourcesDir("hex-chars.pro");
  }

  // ---------------------------------------------------------------------------
  // Check existence of solution
  // ---------------------------------------------------------------------------

  @Test
  public void existsFalse() {
    assertThat(getProlog().solve("fail").isPresent()).isFalse();
  }

  @Test
  public void existsTrue() {
    assertThat(getProlog().solve("true").isPresent()).isTrue();
  }

  @Test
  public void existsTrue2() {
    assertThat(getProlog().solve("true;true").isPresent()).isTrue();
  }

  // ---------------------------------------------------------------------------
  // Number of solutions
  // ---------------------------------------------------------------------------

  @Test
  public void count0() {
    assertThat(getProlog().solve("fail").count()).isEqualTo(0L);
  }


  @Test
  public void count1() {
    assertThat(getProlog().solve("true").count()).isEqualTo(1L);
  }


  @Test
  public void count2() {
    assertThat(getProlog().solve("true;true").count()).isEqualTo(2L);
  }

  @Test
  public void count6() {
    assertThat(getProlog().solve("hex_char(_,_)").count()).isEqualTo(6L);
  }

  // ---------------------------------------------------------------------------
  // Access whole-term solutions (the query term with all its variables resolved)
  // ---------------------------------------------------------------------------

  @Test
  public void solutionSingle1() {
    assertThat(marshall(getProlog().solve("hex_char(Q,'C')").solution().get())).isEqualTo("hex_char(12, 'C')");
  }

  @Test
  public void solutionSingle0() {
    assertThat(getProlog().solve("hex_char(Q,'Z')").solution().get()).isNull();
  }

  @Test(expected = TooManySolutionsException.class)
  public void solutionSingle6() {
    marshall(getProlog().solve("hex_char(Q,_)").solution().unique());
  }

  @Test
  public void solutionFirst1() {
    assertThat(marshall(getProlog().solve("hex_char(Q,'C')").solution().first().get())).isEqualTo("hex_char(12, 'C')");
  }

  @Test
  public void solutionFirst0() {
    assertThat(getProlog().solve("hex_char(Q,'Z')").solution().get()).isNull();
  }

  @Test
  public void solutionFirst6() {
    assertThat(marshall(getProlog().solve("hex_char(Q,C)").solution().first().get())).isEqualTo("hex_char(10, 'A')");
  }

  @Test
  public void solutionUnique() {
    assertThat(marshall(getProlog().solve("hex_char(Q,'C')").solution().unique())).isEqualTo("hex_char(12, 'C')");
  }


  @Test
  public void solutionList() {
    final List<Object> list = getProlog().solve("Q=12;Q=13").solution().list();
    assertThat(list.size()).isEqualTo(2);
  }

  // ---------------------------------------------------------------------------
  // Access to values of a particular variable
  // ---------------------------------------------------------------------------

  @Test(expected = MissingSolutionException.class)
  public void varMissing() {
    getProlog().solve("1=2").var("Q");
  }


  @Test
  public void varUnique() {
    assertThat(getProlog().solve("Q=12").var("Q", Integer.class).unique().intValue()).isEqualTo(12);
  }


  @Test
  public void varFree() {
    assertThat(getProlog().solve("Q=Q").var("Q").unique().toString()).isEqualTo("Q");
  }

  @Test
  public void varBoundToFreeVar() {
    assertThat(getProlog().solve("Q=Z").var("Q").unique().toString()).isEqualTo("Q");
  }


  @Test
  public void varUniqueConverted() {
    final String unique = getProlog().solve("Q=12").var("Q", String.class).unique();
    assertThat(unique).isEqualTo("12");
  }

  // ---------------------------------------------------------------------------
  // Access to bindings of all vars
  // ---------------------------------------------------------------------------

  @Test
  public void varsUnique() {
    final Map<Var<?>, Object> unique = getProlog().solve("Q=12,R=13").vars().unique();
    assertThat(unique.toString().contains("Q=12")).isTrue();
    assertThat(unique.toString().contains("R=13")).isTrue();
  }


  @Test
  public void varsList() {
    final List<Map<Var<?>, Object>> map = getProlog().solve("Q=12;R=13").vars().list();
    assertThat(map.size()).isEqualTo(2);
  }

  // ---------------------------------------------------------------------------
  // Goodies
  // ---------------------------------------------------------------------------

  @Test(expected = TooManySolutionsException.class)
  public void exactly1() {
    getProlog().solve("Q=12;Q=13").vars().exactly(1).list();
  }

  @Test
  public void exactly2() {
    getProlog().solve("Q=12;Q=13").vars().exactly(2).list();
  }

  @Test(expected = MissingSolutionException.class)
  public void exactly3() {
    getProlog().solve("Q=12;Q=13").vars().exactly(3).list();
  }


  @Test
  public void atLeast1() {
    getProlog().solve("Q=12;Q=13").vars().atLeast(1).list();
  }


  @Test
  public void atLeast2() {
    getProlog().solve("Q=12;Q=13").vars().atLeast(2).list();
  }

  @Test(expected = MissingSolutionException.class)
  public void atLeast3() {
    getProlog().solve("Q=12;Q=13").vars().atLeast(3).list();
  }


  @Test(expected = TooManySolutionsException.class)
  public void atMost1() {
    getProlog().solve("Q=12;Q=13").vars().atMost(1).list();
  }


  @Test
  public void atMost2() {
    getProlog().solve("Q=12;Q=13").vars().atMost(2).list();
  }

  @Test
  public void atMost3() {
    getProlog().solve("Q=12;Q=13").vars().atMost(3).list();
  }


  // ---------------------------------------------------------------------------
  // Solution API on iterative predicate
  // Shows that exists() is almost immediate, count takes a more time
  // solution() and var() are quite efficient.
  // ---------------------------------------------------------------------------

  @Test
  public void permExists() {
    final String goal = "perm([a,b,c,d,e,f,g,h], X)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    final boolean exists = holder.isPresent();
    ProfilingInfo.reportAll("Existence of solutions to " + goal + " is " + exists);
    assertThat(exists).isTrue();
  }

  @Test
  public void permCount() {
    final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    final int count = holder.count();
    ProfilingInfo.reportAll("Number of solutions to " + goal + " is " + count);
    assertThat(count).isEqualTo(40320);
  }


  @Test
  public void permSolutions() {
    final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    final List<Object> solutions = holder.solution().list();
    ProfilingInfo.reportAll("list()");
    logger.info(CollectionUtils.format("Solutions to " + goal + " are ", solutions, 10));
    assertThat(solutions.size()).isEqualTo(40320);
  }

  @Test
  public void permSolutionsIterator() {
    final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    final Iterator<Object> iter = holder.solution().iterator();
    int counter = 0;
    while (iter.hasNext()) {
      final Object next = iter.next();
      if (counter < 10) {
        logger.info("Solution via iterator: {}", next);
      }
      counter++;
    }
    ProfilingInfo.reportAll("iterator()");
    assertThat(counter).isEqualTo(40320);
  }

  @Test
  public void permVarList() {
    final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    final List<Struct> values = holder.var("Q", Struct.class).list();
    ProfilingInfo.reportAll("var()");
    logger.info(CollectionUtils.format("Solutions to " + goal + " are ", values, 10));
    assertThat(values.size()).isEqualTo(40320);
  }

  @Test
  public void permVarArray() {
    final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    final Object[] values = holder.var("Q").array(new Object[]{});
    ProfilingInfo.reportAll("var()");
    logger.info(CollectionUtils.format("Solutions to " + goal + " are ", values, 10));
    assertThat(values.length).isEqualTo(40320);
  }

  @Test
  public void permVarIterator() {
    final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    final Iterator<Struct> iter = holder.var("Q", Struct.class).iterator();
    int counter = 0;
    while (iter.hasNext()) {
      final Struct next = iter.next();
      if (counter < 10) {
        logger.debug("Value via iterator: {}", next);
      }
      counter++;
    }
    ProfilingInfo.reportAll("iterator()");
    assertThat(counter).isEqualTo(40320);
  }

  @Test
  public void permVarIterable() {
    final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    int counter = 0;
    for (Struct next : holder.var("Q", Struct.class)) {
      if (counter < 10) {
        logger.debug("Value via iterable: {}", next);
      }
      counter++;
    }
    ProfilingInfo.reportAll("iterable()");
    assertThat(counter).isEqualTo(40320);
  }

  @Test
  public void permVars() {
    final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    final List<Map<Var<?>, Object>> values = holder.vars().list();
    ProfilingInfo.reportAll("vars()");
    logger.info(CollectionUtils.format("Solutions to " + goal + " are ", values, 10));
    assertThat(values.size()).isEqualTo(40320);
  }


  @Test
  public void permVarsIterator() {
    final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
    final GoalHolder holder = getProlog().solve(goal);
    ProfilingInfo.setTimer1();
    final Iterator<Map<Var<?>, Object>> iter = holder.vars().iterator();
    int counter = 0;
    while (iter.hasNext()) {
      final Map<Var<?>, Object> next = iter.next();
      if (counter < 10) {
        logger.debug("Vars via iterator: {}", next);
      }
      counter++;
    }
    ProfilingInfo.reportAll("iterator()");
    assertThat(counter).isEqualTo(40320);
  }
}
