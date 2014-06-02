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
package org.logic2j.core;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.core.api.model.exception.MissingSolutionException;
import org.logic2j.core.api.model.exception.TooManySolutionsException;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.holder.GoalHolder;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.core.impl.util.CollectionUtils;
import org.logic2j.core.impl.util.ProfilingInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test the solution API (and describe its use cases too).
 * Run this only after DefaultSolverTest is successful.
 */
public class SolutionApiTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolutionApiTest.class);

    @Before
    public void loadTheory() {
        loadTheoryFromTestResourcesDir("hex-chars.pl");
    }

    @Test
    public void existsFalse() throws Exception {
        assertFalse(getProlog().solve("fail").exists());
    }

    @Test
    public void existsTrue() throws Exception {
        assertTrue(getProlog().solve("true").exists());
    }

    @Test
    public void existsTrue2() throws Exception {
        assertTrue(getProlog().solve("true;true").exists());
    }

    @Test
    public void count0() throws Exception {
        assertEquals(0L, getProlog().solve("fail").count());
    }


    @Test
    public void count1() throws Exception {
        assertEquals(1L, getProlog().solve("true").count());
    }


    @Test
    public void count2() throws Exception {
        assertEquals(2L, getProlog().solve("true;true").count());
    }

    @Test
    public void count6() throws Exception {
        assertEquals(6L, getProlog().solve("hex_char(_,_)").count());
    }

    // ---------------------------------------------------------------------------
    // Access whole-term solutions
    // ---------------------------------------------------------------------------

    @Test
    public void solutionSingle1() throws Exception {
        assertEquals("hex_char(12, 'C')", marshall(getProlog().solve("hex_char(Q,'C')").solution().single()));
    }

    @Test
    public void solutionSingle0() throws Exception {
        assertNull(getProlog().solve("hex_char(Q,'Z')").solution().single());
    }

    @Test(expected = TooManySolutionsException.class)
    public void solutionSingle6() throws Exception {
        assertEquals("hex_char(12, 'C')", marshall(getProlog().solve("hex_char(Q,_)").solution().single()));
    }

    @Test
    public void solutionFirst1() throws Exception {
        assertEquals("hex_char(12, 'C')", marshall(getProlog().solve("hex_char(Q,'C')").solution().first()));
    }

    @Test
    public void solutionFirst0() throws Exception {
        assertNull(getProlog().solve("hex_char(Q,'Z')").solution().first());
    }

    @Test
    public void solutionFirst6() throws Exception {
        assertEquals("hex_char(10, 'A')", marshall(getProlog().solve("hex_char(Q,C)").solution().first()));
    }

    @Test
    public void solutionUnique() throws Exception {
        assertEquals("hex_char(12, 'C')", marshall(getProlog().solve("hex_char(Q,'C')").solution().unique()));
    }


    @Test
    public void solutionList() throws Exception {
        final List<Object> list = getProlog().solve("Q=12;Q=13").solution().list();
        assertEquals(2, list.size());
    }

    // ---------------------------------------------------------------------------
    // Access to bindings of vars
    // ---------------------------------------------------------------------------

    @Test(expected = MissingSolutionException.class)
    public void varMissing() throws Exception {
        getProlog().solve("1=2").var("Q");
    }


    @Test
    public void varUnique() throws Exception {
        assertEquals(new Long(12), getProlog().solve("Q=12").var("Q", Long.class).unique());
    }


    @Test
    public void varFree() throws Exception {
        assertEquals("Q", getProlog().solve("Q=Q").var("Q").unique().toString());
    }

    @Test
    public void varBoundToFreeVar() throws Exception {
        assertEquals("Q", getProlog().solve("Q=Z").var("Q").unique().toString());
    }


    // ---------------------------------------------------------------------------
    // Access to bindings of all vars
    // ---------------------------------------------------------------------------

    @Test
    public void varsUnique() throws Exception {
        final Map<Var, Object> unique = getProlog().solve("Q=12,R=13").vars().unique();
        assertTrue(unique.toString().contains("Q=12"));
        assertTrue(unique.toString().contains("R=13"));
    }


    @Test
    public void varsList() throws Exception {
        final List<Map<Var, Object>> map = getProlog().solve("Q=12;R=13").vars().list();
        assertEquals(2, map.size());
    }

    // ---------------------------------------------------------------------------
    // Goodies
    // ---------------------------------------------------------------------------

    @Test(expected = TooManySolutionsException.class)
    public void exactly1() throws Exception {
        getProlog().solve("Q=12;Q=13").vars().exactly(1).list();
    }

    @Test
    public void exactly2() throws Exception {
        getProlog().solve("Q=12;Q=13").vars().exactly(2).list();
    }

    @Test(expected = MissingSolutionException.class)
    public void exactly3() throws Exception {
        getProlog().solve("Q=12;Q=13").vars().exactly(3).list();
    }


    @Test
    public void atLeast1() throws Exception {
        getProlog().solve("Q=12;Q=13").vars().atLeast(1).list();
    }


    @Test
    public void atLeast2() throws Exception {
        getProlog().solve("Q=12;Q=13").vars().atLeast(2).list();
    }

    @Test(expected = MissingSolutionException.class)
    public void atLeast3() throws Exception {
        getProlog().solve("Q=12;Q=13").vars().atLeast(3).list();
    }


    @Test(expected = TooManySolutionsException.class)
    public void atMost1() throws Exception {
        getProlog().solve("Q=12;Q=13").vars().atMost(1).list();
    }


    @Test
    public void atMost2() throws Exception {
        getProlog().solve("Q=12;Q=13").vars().atMost(2).list();
    }

    @Test
    public void atMost3() throws Exception {
        getProlog().solve("Q=12;Q=13").vars().atMost(3).list();
    }


    // ---------------------------------------------------------------------------
    // Solution API on iterative predicate
    // Shows that exists() is almost immediate, count takes a more time
    // solution() and var() are quite efficient.
    // ---------------------------------------------------------------------------

    @Test
    public void permExists() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g,h], X)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
        final boolean exists = holder.exists();
        ProfilingInfo.reportAll("Existence of solutions to " + goal + " is " + exists);
        assertTrue(exists);
    }

    @Test
    public void permCount() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
        final long count = holder.count();
        ProfilingInfo.reportAll("Number of solutions to " + goal + " is " + count);
        assertEquals(40320, count);
    }


    @Test
    public void permSolutions() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
        final List<Object> solutions = holder.solution().list();
        ProfilingInfo.reportAll("list()");
        logger.info(CollectionUtils.format("Solutions to " + goal + " are ", solutions, 10));
        assertEquals(40320, solutions.size());
    }

    @Test
    public void permSolutionsIterator() throws IOException {
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
        assertEquals(40320, counter);
    }

    @Test
    public void permVarList() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
        final List<Struct> values = holder.var("Q", Struct.class).list();
        ProfilingInfo.reportAll("var()");
        logger.info(CollectionUtils.format("Solutions to " + goal + " are ", values, 10));
        assertEquals(40320, values.size());
    }

    @Test
    public void permVarArray() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
        final Struct[] values = holder.var("Q").array(new Struct[]{});
        ProfilingInfo.reportAll("var()");
        logger.info(CollectionUtils.format("Solutions to " + goal + " are ", values, 10));
        assertEquals(40320, values.length);
    }

    @Test
    public void permVarIterator() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
        final Iterator<Struct> iter = holder.var("Q", Struct.class).iterator();
        int counter = 0;
        while (iter.hasNext()) {
            final Struct next = iter.next();
            if (counter < 10) {
                logger.info("Value via iterator: {}", next);
            }
            counter++;
        }
        ProfilingInfo.reportAll("iterator()");
        assertEquals(40320, counter);
    }

    @Test
    public void permVarIterable() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
        int counter = 0;
        for (Struct next : holder.var("Q", Struct.class)) {
            if (counter < 10) {
                logger.info("Value via iterable: {}", next);
            }
            counter++;
        }
        ProfilingInfo.reportAll("iterator()");
        assertEquals(40320, counter);
    }

    @Test
    public void permVars() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
        final List<Map<Var, Object>> values = holder.vars().list();
        ProfilingInfo.reportAll("vars()");
        logger.info(CollectionUtils.format("Solutions to " + goal + " are ", values, 10));
        assertEquals(40320, values.size());
    }


    @Test
    public void permVarsIterator() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g,h], Q)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
        final Iterator<Map<Var, Object>> iter = holder.vars().iterator();
        int counter = 0;
        while (iter.hasNext()) {
            final Map<Var, Object> next = iter.next();
            if (counter < 10) {
                logger.info("Vars via iterator: {}", next);
            }
            counter++;
        }
        ProfilingInfo.reportAll("iterator()");
        assertEquals(40320, counter);
    }
}
