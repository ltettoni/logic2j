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

import org.junit.Test;
import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Var;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExcelReaderTest extends ExcelClauseProviderTestBase {

    public static final String EXCEL_TEST_XLS = "excel/TEST.xls";

    @Test
    public void read() throws IOException {
        final File file = new File(TEST_RESOURCES_DIR, EXCEL_TEST_XLS);
        final TabularData data = new ExcelReader(file, true, -1).read();
        assertNotNull(data);
        assertEquals(10, data.getNbRows());
        assertEquals(11, data.getNbColumns());
    }

    @Test
    public void listMatchingClauses() throws IOException {
        final File file = new File(TEST_RESOURCES_DIR, EXCEL_TEST_XLS);
        final TabularData data = new ExcelReader(file, true, 0).read();
        final ClauseProvider td = new TabularDataClauseProvider(getProlog(), data, TermAdapter.AssertionMode.EAVT);
        final Struct theGoal = new Struct("eavt", Var.ANONYMOUS_VAR, Var.ANONYMOUS_VAR, Var.ANONYMOUS_VAR, Var.ANONYMOUS_VAR);
        final Iterable<Clause> listMatchingClauses = td.listMatchingClauses(theGoal, null);
        assertNotNull(listMatchingClauses);
        assertNotNull(listMatchingClauses.iterator());
        assertNotNull(listMatchingClauses.iterator().next());
    }

    @Test
    public void readAndSolve_eavt() throws IOException {
        setExcelClauseProvider(EXCEL_TEST_XLS, TermAdapter.AssertionMode.EAVT);
        nSolutions(1, "eavt('129/2008', 'Resolution title', 'Laboratory equipment', 'TEST')");
    }

    @Test
    public void readAndSolve_record() throws IOException {
        setExcelClauseProvider(EXCEL_TEST_XLS, TermAdapter.AssertionMode.RECORD);
        //
        nSolutions(1, "'TEST'('129/2008', B, C, 'ISO/TC 48', E, F, G, H, I, J, K)");
        nSolutions(1, "'TEST'('129/2008', B, C, 'ISO/TC 48', E, 4.0, G, H, I, J, K)");
        nSolutions(1, "'TEST'('A2', 'B2', 'C2', 'D2', 'E2', 'F2', 'G2', 'H2', 'I2', 'J2', 'K2')");
        // Failing tests
        nSolutions(0, "'TEST'('129/2008', B, C, 'ISO/TC 48', E, '4.0', G, H, I, J, K)");
        nSolutions(1, "'TEST'('A2', 'B2', 'C2', 'D2', 'E2', 'F2', 'G2', 'H2', 'I2', 'J2', 'K2')");
    }

}
