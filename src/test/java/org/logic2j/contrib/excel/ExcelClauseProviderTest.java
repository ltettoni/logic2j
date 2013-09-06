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

package org.logic2j.contrib.excel;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.symbol.Struct;

public class ExcelClauseProviderTest extends PrologTestBase {

    private ExcelClauseProvider clauseProvider;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        // try {
        // getProlog().getTheoryManager().addTheory(getProlog().getTheoryManager().load(new File("src/test/resources/test-config2.pl")));
        // } catch (IOException exception) {
        // fail("Unable to load \"test-config2.pl\" file.");
        // }
        String fileName = TEST_RESOURCES_DIR + "/excel/TEST.xls";
        clauseProvider = new ExcelClauseProvider(getProlog(), fileName, true);
    }

    @Ignore("Work in progress")
    @Test
    public void listMatchingClauses() {
        // Test for xls files
        final Struct theGoalXls = new Struct("testfile_xls");
        Iterator<Clause> iteratorXls = clauseProvider.listMatchingClauses(theGoalXls, null).iterator();
        assertTrue(iteratorXls.hasNext());
    }

    @Ignore("Work in progress")
    @Test
    public void matchClausesFromProlog() {
        // ================================XLS FILES==================================================
        System.out.println("XLS FILES===============================================================");
        // Good tests
        assertNSolutions(1, "testfile_xls('129/2008 ', B, C, 'ISO/TC 48', E, F, G, H, I, J, K)");
        assertNSolutions(1, "testfile_xls('129/2008 ', B, C, 'ISO/TC 48', E, 4.0, G, H, I, J, K)");
        assertNSolutions(1, "testfile_xls('A2', 'B2', 'C2', 'D2', 'E2', 'F2', 'G2', 'H2', 'I2', 'J2', 'K2')");
        // Failing tests
        assertNSolutions(0, "testfile_xls('129/2008 ', B, C, 'ISO/TC 48', E, '4.0', G, H, I, J, K)");
        assertNSolutions(1, "testfile_xls('A2', 'B2', 'C2', 'D2', 'E2', 'F2', 'G2', 'H2', 'I2', 'J2', 'K2')");
    }
}