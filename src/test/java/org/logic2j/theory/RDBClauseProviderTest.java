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
package org.logic2j.theory;

import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.logic2j.PrologWithDataSourcesTestBase;
import org.logic2j.library.impl.rdb.RDBBase;
import org.logic2j.model.symbol.Struct;

public class RDBClauseProviderTest extends PrologWithDataSourcesTestBase {
  private RDBClauseProvider provider;

  @Override
  @Before
  public void setUp() {
    super.setUp();
    this.provider = new RDBClauseProvider(getProlog(), zipcodesDataSource());
  }

  @Test
  public void test_getConnection() throws SQLException {
    assertNotNull(zipcodesConnection());
  }

  @Test
  public void listMatchingClauses() {
    assertNotNull(this.provider);
    final Struct theGoal = new Struct("zip_code", "Zip", "City");
    this.provider.listMatchingClauses(theGoal, null);
  }

  @Test
  public void listMatchingClausesWithSpecialTransformer() {
    assertNotNull(this.provider);
    final Struct theGoal = new Struct("zip_code", "Zip", "City");
    this.provider.setTermFactory(new RDBBase.AllStringsAsAtoms(getProlog()));
    this.provider.listMatchingClauses(theGoal,null);
  }

  @Test
  public void matchClausesFromProlog() {
    getProlog().getClauseProviders().add(this.provider);
    // Matching all
    assertNSolutions(79991, "zip_code(_, _)");
    assertNSolutions(79991, "zip_code(X, _)");
    assertNSolutions(79991, "zip_code(_, Y)");
    assertNSolutions(79991, "zip_code(X, Y)");
    // Match on first argument
    assertNSolutions(0, "zip_code('90008', dummy)");
    assertNSolutions(4, "zip_code('90008', _)");
    assertNSolutions(4, "zip_code('90008', Y)");
    assertNSolutions(4, "Z='90008', Y=dummy, zip_code(Z, _)");
    assertNoSolution("Y=dummy, zip_code('90008', Y)");
    assertNoSolution("Y=dummy, Z=other, zip_code('90008', Y)");
    assertNSolutions(4, "Z=dummy, zip_code('90008', Y)");
    assertNoSolution("zip_code('90008', Y), Y=dummy");
    // Match on second argument
    assertNSolutions(102, "zip_code(_, 'LOS ANGELES')");
    assertNSolutions(102, "zip_code(X, 'LOS ANGELES')");
    assertNoSolution("X=dummy, zip_code(X, 'LOS ANGELES')");
    assertNoSolution("zip_code(X, 'LOS ANGELES'), X=dummy");
    // Match on both arguments
    assertNSolutions(1, "zip_code('90008', 'LOS ANGELES')");
    // Match on list testing
    assertNSolutions(0, "zip_code(['90008',dummy], Y)");
    assertNoSolution("Y=[dummy,'LOS ANGELES'], zip_code('90008', Y)");
    // NO matches
    assertNoSolution("zip_code('00000', 'UNDEFINED')");
    assertNoSolution("zip_code('90008', 'UNDEFINED')");
    assertNoSolution("zip_code('00000', 'LOS ANGELES')");
    assertNoSolution("zip_code(X, X)");
  }

}