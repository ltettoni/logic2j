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
import org.logic2j.core.api.solver.holder.MultipleSolutionsHolder;
import org.logic2j.core.api.solver.holder.UniqueSolutionHolder;
import org.logic2j.core.impl.util.CollectionUtils;
import org.logic2j.core.impl.util.ProfilingInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

/**
 * Test the solution API (and describe its use cases too).
 */
public class SolutionApiTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolutionApiTest.class);

    @Test
    public void placeholderToReproduceError() {
        //
    }


    @Test
    public void exists() throws Exception {
        getProlog().solve("true");
    }
}
