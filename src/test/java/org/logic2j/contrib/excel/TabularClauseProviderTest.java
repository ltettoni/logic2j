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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.contrib.excel.TabularClauseProvider.PredicateMode;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.solver.holder.MultipleSolutionsHolder;
import org.logic2j.core.impl.theory.TheoryManager;

public class TabularClauseProviderTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TabularClauseProviderTest.class);

    private TabularData data;

    @Before
    public void setup() {
        this.data = new TabularData();
        this.data.columnNames = new String[] { "countryName", "countryCode", "population" };
        this.data.rowIdentifierColumn = 1;
        this.data.data = new Object[][] { { "Switzerland", "CHE", 7.8 }, { "France", "FRA", 65.0 }, { "Germany", "DEU", 85.4 } };
        this.data.predicateName = "myData";
    }

    @Test
    public void tabularClauseProvider_eav() {
        final ClauseProvider td = new TabularClauseProvider(getProlog(), this.data, PredicateMode.EAV_NAMED);
        final TheoryManager theoryManager = getProlog().getTheoryManager();
        theoryManager.addClauseProvider(td);
        // theoryManager.getClauseProviderResolver().register(data.getPredicateSignature(), td);
        final MultipleSolutionsHolder sixSolutions = assertNSolutions(6, "myData(E,A,V)");
        assertEquals(
                "[{A=countryName, E='CHE', V='Switzerland'}, {A=population, E='CHE', V=7.8}, {A=countryName, E='FRA', V='France'}, {A=population, E='FRA', V=65.0}, {A=countryName, E='DEU', V='Germany'}, {A=population, E='DEU', V=85.4}]",
                sixSolutions.bindings().toString());
        assertEquals("['CHE', 'CHE', 'FRA', 'FRA', 'DEU', 'DEU']", sixSolutions.binding("E").toString());
        assertEquals("[countryName, population, countryName, population, countryName, population]", sixSolutions.binding("A").toString());
        assertEquals("['Switzerland', 7.8, 'France', 65.0, 'Germany', 85.4]", sixSolutions.binding("V").toString());
    }

    @Test
    public void tabularClauseProvider_eavt() {
        final ClauseProvider td = new TabularClauseProvider(getProlog(), this.data, PredicateMode.EAVT);
        final TheoryManager theoryManager = getProlog().getTheoryManager();
        theoryManager.addClauseProvider(td);
        // theoryManager.getClauseProviderResolver().register(data.getPredicateSignature(), td);
        final MultipleSolutionsHolder sixSolutions = assertNSolutions(6, "eavt(E,A,V,myData)");
        assertEquals(
                "[{A=countryName, E='CHE', V='Switzerland'}, {A=population, E='CHE', V=7.8}, {A=countryName, E='FRA', V='France'}, {A=population, E='FRA', V=65.0}, {A=countryName, E='DEU', V='Germany'}, {A=population, E='DEU', V=85.4}]",
                sixSolutions.bindings().toString());
        assertEquals("['CHE', 'CHE', 'FRA', 'FRA', 'DEU', 'DEU']", sixSolutions.binding("E").toString());
        assertEquals("[countryName, population, countryName, population, countryName, population]", sixSolutions.binding("A").toString());
        assertEquals("['Switzerland', 7.8, 'France', 65.0, 'Germany', 85.4]", sixSolutions.binding("V").toString());
    }
}
