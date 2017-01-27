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

import org.junit.Test;
import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.holder.GoalHolder;
import org.logic2j.core.impl.theory.TheoryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class TabularDataClauseProviderTest extends TabularDataTestBase {

    @Test
    public void tabularClauseProvider_eav() {
        final ClauseProvider td = new TabularDataClauseProvider(getProlog(), smallData(), TermAdapter.AssertionMode.EAV_NAMED);
        final TheoryManager theoryManager = getProlog().getTheoryManager();
        theoryManager.addClauseProvider(td);
        final GoalHolder sixSolutions = nSolutions(6, "smallData(E,A,V)");
        assertEquals(
                "[{A=countryName, E=CHE, V=Switzerland}, {A=population, E=CHE, V=7.8}, {A=countryName, E=FRA, V=France}, {A=population, E=FRA, V=65.0}, {A=countryName, E=DEU, V=Germany}, {A=population, E=DEU, V=85.4}]",
        varsSortedToString(sixSolutions));
        assertEquals("[CHE, CHE, FRA, FRA, DEU, DEU]", sixSolutions.var("E").list().toString());
        assertEquals("[countryName, population, countryName, population, countryName, population]", sixSolutions.var("A").list().toString());
        assertEquals("[Switzerland, 7.8, France, 65.0, Germany, 85.4]", sixSolutions.var("V").list().toString());
    }

    @Test
    public void tabularClauseProvider_eavt() {
        final ClauseProvider td = new TabularDataClauseProvider(getProlog(), smallData(), TermAdapter.AssertionMode.EAVT);
        final TheoryManager theoryManager = getProlog().getTheoryManager();
        theoryManager.addClauseProvider(td);
        final GoalHolder sixSolutions = nSolutions(6, "eavt(E,A,V,smallData)");
        assertEquals(
                "[{A=countryName, E=CHE, V=Switzerland}, {A=population, E=CHE, V=7.8}, {A=countryName, E=FRA, V=France}, {A=population, E=FRA, V=65.0}, {A=countryName, E=DEU, V=Germany}, {A=population, E=DEU, V=85.4}]",
        varsSortedToString(sixSolutions));
        assertEquals("[CHE, CHE, FRA, FRA, DEU, DEU]", sixSolutions.var("E").list().toString());
        assertEquals("[countryName, population, countryName, population, countryName, population]", sixSolutions.var("A").list().toString());
        assertEquals("[Switzerland, 7.8, France, 65.0, Germany, 85.4]", sixSolutions.var("V").list().toString());
    }

    @Test
    public void tabularClauseProvider_record() {
        final ClauseProvider td = new TabularDataClauseProvider(getProlog(), smallData(), TermAdapter.AssertionMode.RECORD);
        final TheoryManager theoryManager = getProlog().getTheoryManager();
        theoryManager.addClauseProvider(td);
        final GoalHolder sixSolutions = nSolutions(3, "smallData(Country,Code,Pop)");
        assertEquals("[{Code=CHE, Country=Switzerland, Pop=7.8}, {Code=FRA, Country=France, Pop=65.0}, {Code=DEU, Country=Germany, Pop=85.4}]", varsSortedToString(sixSolutions));
        assertEquals("[Switzerland, France, Germany]", sixSolutions.var("Country").list().toString());
        assertEquals("[CHE, FRA, DEU]", sixSolutions.var("Code").list().toString());
        assertEquals("[7.8, 65.0, 85.4]", sixSolutions.var("Pop").list().toString());
    }

    @Test
    public void tabularClauseProvider_eav_big() {
        final ClauseProvider td = new TabularDataClauseProvider(getProlog(), largeData(), TermAdapter.AssertionMode.EAV_NAMED);
        final TheoryManager theoryManager = getProlog().getTheoryManager();
        theoryManager.addClauseProvider(td);
        //
        nSolutions(90000, "largeData(E,A,V)"); // Takes only 0.1 seconds: 900000 solutions/s
    }

}
