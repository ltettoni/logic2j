package org.logic2j.library.impl.rdb;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.Before;
import org.junit.Ignore;
import org.logic2j.PrologTestBase;

/**
 */
@Ignore
public class RDBLibraryTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RDBLibraryTest.class);

  @Override
  @Before
  public void setUp() {
    super.setUp();
    final EmbeddedDataSource ds = new EmbeddedDataSource();
    ds.setDatabaseName("src/test/db/derby");
    ds.setUser("APP");
    ds.setPassword("APP");
    bind("zipdb", ds);
  }

}
