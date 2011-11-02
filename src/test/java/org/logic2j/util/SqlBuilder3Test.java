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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.util.SqlBuilder3.Column;
import org.logic2j.util.SqlBuilder3.Table;

public class SqlBuilder3Test {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SqlBuilder3Test.class);
  
  private SqlBuilder3 simple(String tbl, String col, Object value) {
    SqlBuilder3 sb = new SqlBuilder3();
    Table table = sb.table(tbl);
    Column column = sb.column(table, col);
    sb.addProjection(column);
    if (value!=null) {
      sb.addConjunction(sb.criterion(column, value));
    }
    return sb;
  }
  
  /**
   * Testing internals.
   * @throws Exception
   */
  @Test
  public void inlistParamsPlaceholders() throws Exception {
    assertEquals("", SqlBuilder3.inlistParamsPlaceholders(-10).toString());
    assertEquals("", SqlBuilder3.inlistParamsPlaceholders(-1).toString());
    assertEquals("", SqlBuilder3.inlistParamsPlaceholders(0).toString());
    assertEquals("?", SqlBuilder3.inlistParamsPlaceholders(1).toString());
    assertEquals("(?,?,?,?)", SqlBuilder3.inlistParamsPlaceholders(4).toString());
  }

  /**
   * Testing internals.
   * @throws Exception
   */
  @Test
  public void flattenedParams() throws Exception {
    assertEquals("", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(), "|"));
    assertEquals("", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams((Object[]) null), "|"));
    assertEquals("5", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(5), "|"));
    assertEquals("1|2|3", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(1, 2, 3), "|"));
    assertEquals("1|10|11|3", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(1, new Integer[] { 10, 11 }, 3), "|"));
  }

  @Test
  public void empty() throws Exception {
    final SqlBuilder3 sb = new SqlBuilder3();
    assertEquals("select * from ", sb.getStatement());
  }

  @Test
  public void simple0() throws Exception {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.addConjunction(sb.column(sb.table("tbl"), "id"), 12);
    assertEquals("select * from tbl where tbl.id=?", sb.getSelect());
    assertEquals(1, sb.getParameters().length);
  }
  
  @Test
  public void simple1() throws Exception {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.addConjunction(sb.column(sb.table("tbl"), "col"), "val");
    assertEquals("select * from tbl where tbl.col=?", sb.getSelect());
    assertEquals(1, sb.getParameters().length);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void simple2() throws Exception {
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      sb.addConjunction(sb.criterion(col, 3));
      assertEquals("select * from tbl where tbl.col=?", sb.getSelect());
      assertEquals(1, sb.getParameters().length);
      final Integer[] expectedParams = new Integer[] { 3 };
      assertEquals(expectedParams, sb.getParameters());
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      sb.addConjunction(sb.criterion(col, 'a', 'b', 'c'));
      assertEquals("select * from tbl where tbl.col in (?,?,?)", sb.getSelect());
      assertEquals(3, sb.getParameters().length);
      final Character[] expectedParams = new Character[] { 'a', 'b', 'c' };
      assertEquals(expectedParams, sb.getParameters());
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      final Integer[] arr = new Integer[] { 5, 6 };
      sb.addConjunction(sb.criterion(col, (Object[]) arr));
      assertEquals("select * from tbl where tbl.col in (?,?)", sb.getSelect());
      assertEquals(2, sb.getParameters().length);
      final Integer[] expectedParams = new Integer[] { 5, 6 };
      assertEquals(expectedParams, sb.getParameters());
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      final Integer[] arr = new Integer[] { 5, 6 };
      sb.addConjunction(sb.criterion(col, arr, 4, arr));
      assertEquals("select * from tbl where tbl.col in (?,?,?,?,?)", sb.getSelect());
      assertEquals(5, sb.getParameters().length);
      final Integer[] expectedParams = new Integer[] { 5, 6, 4, 5, 6 };
      assertEquals(expectedParams, sb.getParameters());
    }
  }
 
  
  @Test
  public void multipleTables() throws Exception {
    SqlBuilder3 sb = new SqlBuilder3();
    Column col = sb.column(sb.table("t1"), "c1");
    sb.addConjunction(sb.criterion(col, 1));
    Column col2 = sb.column(sb.table("t2"), "c2");
    sb.addConjunction(sb.criterion(col2, 2));
    sb.addOrderBy(sb.ascending(col2));
    assertEquals("select * from t1, t2 where t1.c1=? and t2.c2=? order by t2.c2 asc", sb.getSelect());
  }

  @Test
  public void join() throws Exception {
    SqlBuilder3 sb = new SqlBuilder3();
    Column col = sb.column(sb.table("table1", "t1"), "c1");
    sb.addConjunction(sb.criterion(col, 1));
    sb.addProjection(sb.column(sb.table("table1", "t1"), "proj1"));
    Column col2 = sb.column(sb.table("table2", "t2"), "c2");
    sb.addConjunction(sb.criterion(col2, 2));
    sb.innerJoin(col, col2);
    sb.innerJoin(sb.column(sb.table("table3", "t3"), "c3"), col);
    assertEquals(
        "select t1.proj1 from table1 t1 inner join table2 t2 on t2.c2=t1.c1 inner join table3 t3 on t3.c3=t1.c1 where t1.c1=? and t2.c2=?",
        sb.getSelect());
    assertEquals(
        "select count(t1.proj1) from table1 t1 inner join table2 t2 on t2.c2=t1.c1 inner join table3 t3 on t3.c3=t1.c1 where t1.c1=? and t2.c2=?",
        sb.getSelectCount());
  }

  @Test
  public void joinSameTable() throws Exception {
    SqlBuilder3 sb = new SqlBuilder3();
    Column col = sb.column(sb.table("table"), "c1");
    Column col2 = sb.column(sb.table("table", "alias"), "c2");
    sb.innerJoin(col, col2);
    assertEquals("select count(*) from table inner join table alias on alias.c2=table.c1", sb.getSelectCount());
  }
  
  @Test
  public void subtable() throws Exception {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.tableSubUnion("sub", false, simple("t1", "c1", 12), simple("t2", "c2", "x"), simple("t3", "c3", 34));
    logger.info("Union query: {}", sb.describe());
    assertEquals("select * from (select t1.c1 from t1 where t1.c1=? union select t2.c2 from t2 where t2.c2=? union select t3.c3 from t3 where t3.c3=?) sub", sb.getSelect());
    assertEquals(3, sb.getParameters().length);
  }
  
}

