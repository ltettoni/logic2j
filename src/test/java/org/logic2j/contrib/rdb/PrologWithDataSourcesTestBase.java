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
package org.logic2j.contrib.rdb;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.logic2j.core.PrologTestBase;

/**
 * Common base class for testing the Prolog engine with data sources. Although it would be formally cleaner to instantiate data sources once
 * per test case (using a @Before), let's go for a slightly faster approach: one connection per test class, since we are only reading from our
 * reference databases.
 */
public abstract class PrologWithDataSourcesTestBase extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrologWithDataSourcesTestBase.class);

  private static final String SRC_TEST_DB = "src/test/resources/db";
  private static final String DERBY_VERSION_STRING = "v10.13.1.1";
  private static final String ZIPCODES_DERBY_DIR = SRC_TEST_DB + "/zipcodes1/derby-" + DERBY_VERSION_STRING;
  private static final String DERBY_USER = "APP"; // "APP" is a good default in Derby, see doc
  private static final String DERBY_PWD = "APP"; // "APP" is a good default in Derby, see doc

  private Connection zipcodesConnection = null;

  /**
   * @param theDerbyDatabaseDir Relative path to the derby binary directory, usually under "src/test/db/NAME"
   * @return A new Derby EmbeddedDataSource
   */
  protected DataSource derbyDataSource(String theDerbyDatabaseDir) {
    final EmbeddedDataSource ds = new EmbeddedDataSource();
    ds.setDatabaseName(theDerbyDatabaseDir);
    ds.setUser(DERBY_USER);
    ds.setPassword(DERBY_PWD);
    return ds;
  }

  /**
   * @return A {@link javax.sql.DataSource} to the "zipcodes" reference database.
   */
  protected DataSource zipcodesDataSource() {
    return derbyDataSource(ZIPCODES_DERBY_DIR);
  }

  /**
   * @return A (previously obtained and reused) {@link java.sql.Connection} to the "zipcodes" reference database.
   * @throws java.sql.SQLException
   */
  protected Connection zipcodesConnection() throws SQLException {
    if (this.zipcodesConnection == null) {
      this.zipcodesConnection = zipcodesDataSource().getConnection();
      logger.debug("Instantiated new connection to zipcodes DB");
    }
    return this.zipcodesConnection;
  }

}
