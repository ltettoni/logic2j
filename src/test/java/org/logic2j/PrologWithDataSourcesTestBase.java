package org.logic2j;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;

/**
 * Common base class for testing the Prolog engine with data sources.
 * Although it would be formally cleaner to instantiate data sources once per test case 
 * using a @Before, since we are using read-only reference databases, let's go for a slightly 
 * faster approach: one connection per test class.
 * @version $Id$
 */
public abstract class PrologWithDataSourcesTestBase extends PrologTestBase {

  private static final String ZIPCODES_DATABASE_DIR = "src/test/db/derby";
  private static final String DATABASE_USER = "APP";
  private static final String DATABASE_PWD = "APP";

  private Connection zipcodesConnection = null;

  protected DataSource derbyDataSource(String theDerbyDatabaseDir) {
    final EmbeddedDataSource ds = new EmbeddedDataSource();
    ds.setDatabaseName(theDerbyDatabaseDir);
    ds.setUser(DATABASE_USER);
    ds.setPassword(DATABASE_PWD);
    return ds;
  }

  protected DataSource zipcodesDataSource() {
    return derbyDataSource(ZIPCODES_DATABASE_DIR);
  }

  protected Connection zipcodesConnection() throws SQLException {
    if (this.zipcodesConnection == null) {
      this.zipcodesConnection = zipcodesDataSource().getConnection();
    }
    return this.zipcodesConnection;
  }

}
