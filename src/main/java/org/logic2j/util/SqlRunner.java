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
package org.logic2j.util;

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
 * Execute SQL statements in an IoC manner, guaranteeing proper error handling
 * and resource cleaning.
 *
 * @version $Id$
 */
public class SqlRunner {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SqlRunner.class);
  private static final boolean DEBUG_ENABLED = logger.isDebugEnabled();
  private static final Object[] EMPTY_PARAMS = new Object[0];

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
   * @throws SQLException 
   */
  public List<Object[]> query(String theSelect, Object[] theParameters) throws SQLException {
	  
    if (theParameters == null) {
      theParameters = EMPTY_PARAMS;
    }
    PreparedStatement stmt = null;
    ResultSet rs = null;
    List<Object[]> result = null;
    if (DEBUG_ENABLED) {
      logger.debug("SqlRunner SQL \"" + theSelect + '"');
      logger.debug(" parameters=" + Arrays.asList(theParameters));
    }
    try {
      Connection conn = this.dataSource.getConnection();
      stmt = this.prepareStatement(conn, theSelect);
      this.fillStatement(stmt, theParameters);
      rs = stmt.executeQuery();

      result = handle(rs);

    } catch (SQLException e) {
      if (DEBUG_ENABLED) {
        logger.debug("Caught exception \"" + e + "\", going to rethrow");
      }
      this.rethrow(e, theSelect, theParameters);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
      } catch (SQLException ignored) {
        // Quiet
      }
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException ignored) {
        // Quiet
      }
      // Should we close the connection here??? (return it to the pool in case of a pooled connection?)
    }
    return result;
  }

  //---------------------------------------------------------------------------
  // Template methods - may be overidden
  //---------------------------------------------------------------------------

  /**
   * @param theResultSet
   * @return Full result set in memory :-(
   * @throws SQLException 
   */
  private List<Object[]> handle(ResultSet theResultSet) throws SQLException {
    List<Object[]> result = new ArrayList<Object[]>();

    ResultSetMetaData meta = theResultSet.getMetaData();
    int cols = meta.getColumnCount();
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
   * @param cause The original exception that will be chained to the new 
   * exception when it's rethrown. 
   * 
   * @param sql The query that was executing when the exception happened.
   * 
   * @param params The query replacement paramaters; <code>null</code> is a 
   * valid value to pass in.
   * 
   * @throws SQLException
   */
  protected void rethrow(SQLException cause, String sql, Object[] params) throws SQLException {

    StringBuffer msg = new StringBuffer(cause.getMessage());

    msg.append(", query=\"");
    msg.append(sql);
    msg.append("\", parameters=");

    if (params == null) {
      msg.append("[]");
    } else {
      msg.append(Arrays.asList(params));
    }

    SQLException e = new SQLException(msg.toString());
    e.setNextException(cause);

    throw e;
  }
}
