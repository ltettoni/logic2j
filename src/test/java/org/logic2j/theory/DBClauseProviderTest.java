package org.logic2j.theory;

import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.Before;
import org.junit.Test;
import org.logic2j.PrologTestBase;
import org.logic2j.library.impl.rdb.RDBBase;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.theory.jdbc.DBClauseProvider;
import org.logic2j.theory.jdbc.UtilReference;

/**
 */
public class DBClauseProviderTest extends PrologTestBase {
  private EmbeddedDataSource ds;
  private DBClauseProvider provider;

  @Override
  @Before
  public void setUp() {
    super.setUp();
    this.ds = new EmbeddedDataSource();
    this.ds.setDatabaseName("C:/Soft/Java/db-derby-10.7.1.1-bin/bin/gd30");
    this.ds.setUser("APP");
    this.ds.setPassword("APP");
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
    final Struct theGoal = new Struct("country", "ID", "VAL");
    this.provider.listMatchingClauses(theGoal);
  }

  @Test
  public void test_listMatchingClauses_withSpecialTransformer() {
    assertNotNull(this.provider);
    final Struct theGoal = new Struct("country", "ID", "VAL");
    this.provider.setTermFactory(new RDBBase.AllStringsAsAtoms(getProlog()) {
      @Override
      public Term create(Object theObject, FactoryMode theMode) {
        if (theObject instanceof Number) {
          // return new TLong(((Number) theObject).longValue());
          return new Struct(UtilReference.formatReference(((Number) theObject).longValue()));
        }
        return super.create(theObject, theMode);
      }
    });
    this.provider.listMatchingClauses(theGoal);
  }

  @Test
  public void test_fromProlog() {
    getProlog().getClauseProviders().add(this.provider);
    assertNSolutions(262, "country(CountryId, CountryName)");
    //    assertEquals(term("id154"), assertOneSolution("country(CountryId, 'France')").binding("CountryId"));
    assertNSolutions(4, "country(CountryId, C), CountryId>500");
  }

}
