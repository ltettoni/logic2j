package org.logic2j.theory.jdbc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.theory.jdbc.SqlBuilder3;
import org.logic2j.theory.jdbc.SqlBuilder3.Column;
import org.logic2j.util.CollectionUtils;

/**
 */
public class SqlBuilder3Test {

  @Test
  public void test_inlistParamsPlaceholders() throws Exception {
    assertEquals("", SqlBuilder3.inlistParamsPlaceholders(-10).toString());
    assertEquals("", SqlBuilder3.inlistParamsPlaceholders(-1).toString());
    assertEquals("", SqlBuilder3.inlistParamsPlaceholders(0).toString());
    assertEquals("?", SqlBuilder3.inlistParamsPlaceholders(1).toString());
    assertEquals("(?,?,?,?)", SqlBuilder3.inlistParamsPlaceholders(4).toString());
  }

  @Test
  public void test_flattenedParams() throws Exception {
    assertEquals("", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(), "|"));
    assertEquals("", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams((Object[]) null), "|"));
    assertEquals("5", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(5), "|"));
    assertEquals("1|2|3", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(1, 2, 3), "|"));
    assertEquals("1|10|11|3", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(1, new Integer[] { 10, 11 }, 3), "|"));
  }

  @Test
  public void test_getStatement_empty() throws Exception {
    final SqlBuilder3 sb = new SqlBuilder3();
    assertEquals("select from ", sb.getStatement());
  }

  @Test
  public void test1() throws Exception {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.addConjunction(sb.column(sb.table("tbl"), "col"), "val");
    assertEquals("select from tbl where tbl.col=?", sb.getSelect());
    assertEquals(1, sb.getParameters().length);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void test2() throws Exception {
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      sb.addConjunction(sb.criterion(col, 3));
      assertEquals("select from tbl where tbl.col=?", sb.getSelect());
      assertEquals(1, sb.getParameters().length);
      final Integer[] expectedParams = new Integer[] { 3 };
      assertEquals(expectedParams, sb.getParameters());
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      sb.addConjunction(sb.criterion(col, 'a', 'b', 'c'));
      assertEquals("select from tbl where tbl.col in (?,?,?)", sb.getSelect());
      assertEquals(3, sb.getParameters().length);
      final Character[] expectedParams = new Character[] { 'a', 'b', 'c' };
      assertEquals(expectedParams, sb.getParameters());
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      final Integer[] arr = new Integer[] { 5, 6 };
      sb.addConjunction(sb.criterion(col, (Object[]) arr));
      assertEquals("select from tbl where tbl.col in (?,?)", sb.getSelect());
      assertEquals(2, sb.getParameters().length);
      final Integer[] expectedParams = new Integer[] { 5, 6 };
      assertEquals(expectedParams, sb.getParameters());
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      final Integer[] arr = new Integer[] { 5, 6 };
      sb.addConjunction(sb.criterion(col, arr, 4, arr));
      assertEquals("select from tbl where tbl.col in (?,?,?,?,?)", sb.getSelect());
      assertEquals(5, sb.getParameters().length);
      final Integer[] expectedParams = new Integer[] { 5, 6, 4, 5, 6 };
      assertEquals(expectedParams, sb.getParameters());
    }
  }

  @Test
  public void test_multi_tables() throws Exception {
    SqlBuilder3 sb = new SqlBuilder3();
    Column col = sb.column(sb.table("t1"), "c1");
    sb.addConjunction(sb.criterion(col, 1));
    Column col2 = sb.column(sb.table("t2"), "c2");
    sb.addConjunction(sb.criterion(col2, 2));
    sb.addOrderBy(sb.ascending(col2));
    assertEquals("select from t1, t2 where t1.c1=? and t2.c2=? order by t2.c2 asc", sb.getSelect());
  }

  @Test
  public void test_join() throws Exception {
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
  public void test_joinsametable() throws Exception {
    SqlBuilder3 sb = new SqlBuilder3();
    Column col = sb.column(sb.table("table"), "c1");
    Column col2 = sb.column(sb.table("table", "alias"), "c2");
    sb.innerJoin(col, col2);
    assertEquals("select count(*) from table inner join table alias on alias.c2=table.c1", sb.getSelectCount());
  }
}
