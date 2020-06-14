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
package org.logic2j.contrib.rdb.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

/**
 * Execute SQL statements in an IoC manner, guaranteeing proper error handling and resource cleaning.
 * Highly inspired by Jakarta Commons "dbutils".
 *
 * @version $Id$
 */
public class SqlRunner {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SqlRunner.class);
  private static final boolean DEBUG_ENABLED = logger.isDebugEnabled();
  // private static final Object[] EMPTY_PARAMS = new Object[0];

  private final DataSource dataSource;

  /**
   * @param theDataSource
   */
  public SqlRunner(DataSource theDataSource) {
    this.dataSource = theDataSource;
  }

  /**
   * @param theSelect
   * @param theParameters
   * @return The result (all data)
   * @throws java.sql.SQLException
   */
  public List<Object[]> query(String theSelect, Object[] theParameters) throws SQLException {
    List<Object[]> result = null;
    if (DEBUG_ENABLED) {
      logger.debug("SqlRunner SQL \"" + theSelect + '"');
      logger.debug(" parameters=" + Arrays.asList(theParameters));
    }
    try (final Connection conn = this.dataSource.getConnection(); //
         final PreparedStatement stmt = this.prepareStatement(conn, theSelect)) {
      this.fillStatement(stmt, theParameters);
      try (final ResultSet rs = stmt.executeQuery()) {
        result = handle(rs);
      } catch (final SQLException e) {
        if (DEBUG_ENABLED) {
          logger.debug("Caught exception \"" + e + "\", going to rethrow");
        }
        this.rethrow(e, theSelect, theParameters);
      }
    }
    return result;
  }

  // ---------------------------------------------------------------------------
  // Template methods - may be overridden
  // ---------------------------------------------------------------------------

  /**
   * @param theResultSet
   * @return Full result set in memory :-(
   * @throws java.sql.SQLException
   */
  private List<Object[]> handle(ResultSet theResultSet) throws SQLException {
    final List<Object[]> result = new ArrayList<>();

    final ResultSetMetaData meta = theResultSet.getMetaData();
    final int cols = meta.getColumnCount();
    while (theResultSet.next()) {
      result.add(toArray(theResultSet, cols));
    }

    return result;
  }

  /**
   * @param theResultSet
   * @param theCols
   * @return One row as array
   */
  protected Object[] toArray(ResultSet theResultSet, int theCols) throws SQLException {
    final Object[] result = new Object[theCols];
    for (int i = 0; i < theCols; i++) {
      result[i] = theResultSet.getObject(i + 1);
    }
    return result;
  }

  protected PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
    return conn.prepareStatement(sql);
  }

  protected void fillStatement(PreparedStatement stmt, Object[] params) throws SQLException {
    if (params == null) {
      return;
    }
    for (int i = 0; i < params.length; i++) {
      if (params[i] != null) {
        stmt.setObject(i + 1, params[i]);
      } else {
        stmt.setNull(i + 1, Types.OTHER);
      }
    }
  }

  /**
   * Throws a new exception with a more informative error message.
   *
   * @param cause  The original exception that will be chained to the new exception when it's rethrown.
   * @param sql    The query that was executing when the exception happened.
   * @param params The query replacement parameters; <code>null</code> is a valid value to pass in.
   * @throws java.sql.SQLException
   */
  protected void rethrow(SQLException cause, String sql, Object[] params) throws SQLException {

    final StringBuilder msg = new StringBuilder(cause.getMessage());

    msg.append(", query=\"");
    msg.append(sql);
    msg.append("\", parameters=");

    if (params == null) {
      msg.append("[]");
    } else {
      msg.append(Arrays.asList(params));
    }

    final SQLException e = new SQLException(msg.toString());
    e.setNextException(cause);

    throw e;
  }
}
