package org.logic2j.theory;

import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.Before;
import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.rdb.RDBBase;
import org.logic2j.model.symbol.Struct;
import org.logic2j.theory.jdbc.DBClauseProvider;

public class DBClauseProviderTest extends PrologTestBase {
  private EmbeddedDataSource ds;
  private DBClauseProvider provider;

  @Override
  @Before
  public void setUp() {
    super.setUp();
    ds = new EmbeddedDataSource();
    ds.setDatabaseName("src/test/db/derby");
    ds.setUser("APP");
    ds.setPassword("APP");
    //
    this.provider = new DBClauseProvider(getProlog(), this.ds);
  }

  @Test
  public void test_getConnection() throws SQLException {
    assertNotNull(this.ds.getConnection());
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
