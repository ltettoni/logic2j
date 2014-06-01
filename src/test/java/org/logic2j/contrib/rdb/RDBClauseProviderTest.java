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
package org.logic2j.contrib.rdb;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.unify.UnifyContext;

import java.sql.SQLException;
import java.util.Iterator;

import static org.junit.Assert.assertNotNull;

public class RDBClauseProviderTest extends PrologWithDataSourcesTestBase {
    private RDBClauseProvider provider;

    @Before
    public void initRdbClauseProvider() {
        this.provider = new RDBClauseProvider(this.prolog, zipcodesDataSource());
        assertNotNull(this.provider);
    }

    @Test
    public void getConnection() throws SQLException {
        assertNotNull(zipcodesConnection());
    }

    @Test
    public void listMatchingClauses() {
        this.provider.saveTableInfo("zip_code", new String[] { "zip_code", "city" });
        final Object goal = getProlog().getTermUnmarshaller().unmarshall("zip_code(Zip, City)");
        final UnifyContext currentVars = getProlog().getSolver().initialContext();
        final Iterator<Clause> iterator = this.provider.listMatchingClauses(goal, currentVars).iterator();
        int counter;
        for (counter = 0; iterator.hasNext(); iterator.next()) {
            counter++;
        }
        org.junit.Assert.assertEquals(79991, counter);
    }

    @Test
    public void matchClausesFromProlog() {
        this.provider.saveTableInfo("zip_code", new String[] { "zip_code", "city" });
        this.prolog.getTheoryManager().addClauseProvider(this.provider);
        // Matching all
        nSolutions(79991, "zip_code(_, _)");
        nSolutions(79991, "zip_code(X, _)");
        nSolutions(79991, "zip_code(_, Y)");
        nSolutions(79991, "zip_code(X, Y)");
        // Match on first argument
        nSolutions(0, "zip_code('90008', dummy)");
        nSolutions(4, "zip_code('90008', _)");
        nSolutions(4, "zip_code('90008', Y)");
        nSolutions(4, "Z='90008', Y=dummy, zip_code(Z, _)");
        noSolutions("Y=dummy, zip_code('90008', Y)");
        noSolutions("Y=dummy, Z=other, zip_code('90008', Y)");
        nSolutions(4, "Z=dummy, zip_code('90008', Y)");
        noSolutions("zip_code('90008', Y), Y=dummy");
        // Match on second argument
        nSolutions(102, "zip_code(_, 'LOS ANGELES')");
        nSolutions(102, "zip_code(X, 'LOS ANGELES')");
        noSolutions("X=dummy, zip_code(X, 'LOS ANGELES')");
        noSolutions("zip_code(X, 'LOS ANGELES'), X=dummy");
        // Match on both arguments
        nSolutions(1, "zip_code('90008', 'LOS ANGELES')");
        // Match on list testing
        nSolutions(0, "zip_code(['90008',dummy], Y)");
        noSolutions("Y=[dummy,'LOS ANGELES'], zip_code('90008', Y)");
        // NO matches
        noSolutions("zip_code('00000', 'UNDEFINED')");
        noSolutions("zip_code('90008', 'UNDEFINED')");
        noSolutions("zip_code('00000', 'LOS ANGELES')");
        noSolutions("zip_code(X, X)");
    }

}