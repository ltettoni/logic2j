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
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter.AssertionMode;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Var;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class ExcelClauseProviderTestBase extends PrologTestBase {

    protected void setExcelClauseProvider(String filename, AssertionMode theMode) throws IOException {
        final File file = new File(TEST_RESOURCES_DIR, filename);
        final TabularData td = new ExcelReader(file, true, 0).readCached();
        td.addClauseProviderTo(getProlog(), theMode);
    }

    protected void setExcelDataFactProvider(String filename, AssertionMode theMode) throws IOException {
        final File file = new File(TEST_RESOURCES_DIR, filename);
        final TabularData td = new ExcelReader(file, true, 0).readCached();
        getProlog().getTheoryManager().addDataFactProvider(new TabularDataFactProvider(td, theMode));
    }

}
