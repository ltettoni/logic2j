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

package org.logic2j.contrib.excel;

import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.TermAdapter.AssertionMode;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Read Excel as TabularData and register into Prolog as TabularDataFactProvider.
 */
public abstract class ExcelClauseProviderTestBase extends PrologTestBase {

    protected void setExcelClauseProvider(String filename, AssertionMode theMode) throws IOException {
        final File file = new File(TEST_RESOURCES_DIR, filename);
        ensureResourceFile(file);
        final TabularData td = new ExcelReader(file, true, 0).readCached();
        td.addClauseProviderTo(getProlog(), theMode);
    }

    protected void setExcelDataFactProvider(String filename, AssertionMode theMode) throws IOException {
        final File file = new File(TEST_RESOURCES_DIR, filename);
        ensureResourceFile(file);
        final TabularData td = new ExcelReader(file, true, 0).readCached();
        getProlog().getTheoryManager().addDataFactProvider(new TabularDataFactProvider(td, theMode));
    }

    private void ensureResourceFile(File file) {
        if (! file.canRead()) {
            throw new IllegalStateException("Test resource \"" + file + "\" missing - check " + this + ", check maven or gradle build or read documentation near test class");
        }
    }

}
