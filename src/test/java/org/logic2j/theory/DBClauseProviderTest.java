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
import org.logic2j.theory.jdbc.DBClauseProvider;

public class DBClauseProviderTest extends PrologWithDataSourcesTestBase {
  private DBClauseProvider provider;

  @Override
  @Before
  public void setUp() {
    super.setUp();
    this.provider = new DBClauseProvider(getProlog(), zipcodesDataSource());
  }

  @Test
  public void test_getConnection() throws SQLException {
    assertNotNull(zipcodesConnection());
  }

  @Test
  public void test_listMatchingClauses() {
    assertNotNull(this.provider);
    final Struct theGoal = new Struct("zip_code", "Zip", "City");
    this.provider.listMatchingClauses(theGoal);
  }

  @Test
  public void test_listMatchingClauses_withSpecialTransformer() {
    assertNotNull(this.provider);
    final Struct theGoal = new Struct("zip_code", "Zip", "City");
    this.provider.setTermFactory(new RDBBase.AllStringsAsAtoms(getProlog()));
    this.provider.listMatchingClauses(theGoal);
  }

  @Test
  public void test_fromProlog() {
    getProlog().getClauseProviders().add(this.provider);
    assertNSolutions(79991, "zip_code(_, _)");
    assertNSolutions(4, "zip_code('90008', _)");
    assertNSolutions(102, "zip_code(_, 'LOS ANGELES')");
    assertNSolutions(1, "zip_code('90008', 'LOS ANGELES')");
  }

}
