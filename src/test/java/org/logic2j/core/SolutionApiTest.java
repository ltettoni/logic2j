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
import org.logic2j.core.api.model.exception.TooManySolutionsException;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.holder.GoalHolder;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.core.impl.util.ProfilingInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

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


    @Test
    public void varUnique() throws Exception {
        assertEquals(new Long(12), getProlog().solve("Q=12").var("Q", Long.class).unique());
    }


    // ---------------------------------------------------------------------------
    // Access to bindings of all vars
    // ---------------------------------------------------------------------------

    @Test
    public void varsUnique() throws Exception {
        final Map<Var, Object> unique = getProlog().solve("Q=12,R=13").vars().unique();
    }


    @Test
    public void varsList() throws Exception {
        final List<Map<Var, Object>> map = getProlog().solve("Q=12;R=13").vars().list();
        assertEquals(2, map.size());
    }

    // ---------------------------------------------------------------------------
    // Goodies
    // ---------------------------------------------------------------------------

    @Test
    public void ensureNumber() throws Exception {
        final Map<Var, Object> unique = getProlog().solve("Q=12,R=13").exactCount(1).vars().unique();
    }

    // ---------------------------------------------------------------------------
    //
    // ---------------------------------------------------------------------------


    @Test
    public void permCount() throws IOException {
        final String goal = "perm([a,b,c,d,e,f,g], X)";
        final GoalHolder holder = getProlog().solve(goal);
        ProfilingInfo.setTimer1();
//        final long count = holder.count();
        final long count = holder.exists() ? 1 : 0;
        ProfilingInfo.reportAll("Number of solutions to " + goal + " is " + count);
//        assertEquals(5040, count);
    }


}
