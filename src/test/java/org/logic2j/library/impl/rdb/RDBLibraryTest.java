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
package org.logic2j.library.impl.rdb;

import static org.junit.Assert.assertNotNull;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.PrologTestBase;

/**
 */
@Ignore
public class RDBLibraryTest extends PrologTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RDBLibraryTest.class);
  private EmbeddedDataSource ds;

  @Override
  @Before
  public void setUp() {
    super.setUp();
    ds = new EmbeddedDataSource();
    ds.setDatabaseName("src/test/db/derby");
    ds.setUser("APP");
    ds.setPassword("APP");
    bind("zipdb", ds);
  }


  @Test
  public void ensureCanGetConnection() throws Exception {
    assertNotNull(this.ds.getConnection());
  }
}
