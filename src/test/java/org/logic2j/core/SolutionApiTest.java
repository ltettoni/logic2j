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

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.holder.MultipleSolutionsHolder;
import org.logic2j.core.api.solver.holder.UniqueSolutionHolder;
import org.logic2j.core.impl.util.CollectionUtils;
import org.logic2j.core.impl.util.ProfilingInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the solution API (and describe its use cases too).
 * Run this only after DefaultSolverTest is successful.
 */
public class SolutionApiTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolutionApiTest.class);

    @Test
    public void existsTrue() throws Exception {
        assertTrue(getProlog().solve("true").exists());
    }

    @Test
    public void existsFalse() throws Exception {
        assertFalse(getProlog().solve("fail").exists());
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

    // ---------------------------------------------------------------------------
    // Access whole-term solutions
    // ---------------------------------------------------------------------------

    @Test
    public void solutionUnique() throws Exception {
        assertEquals("'='(12, 12)", getProlog().solve("Q=12").solution().unique().toString());
    }

    @Test
    public void solutionSingle() throws Exception {
        assertEquals("'='(12, 12)", getProlog().solve("Q=12").solution().single().toString());
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
        final Map<Var, Object> unique = getProlog().solve("Q=12,R=13").ensureNumber(1).vars().unique();
    }
}
