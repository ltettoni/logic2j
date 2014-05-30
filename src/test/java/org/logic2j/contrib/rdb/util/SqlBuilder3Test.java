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
package org.logic2j.contrib.rdb.util;

import org.junit.Test;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Column;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Criterion;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Operator;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Table;
import org.logic2j.core.impl.util.CollectionUtils;

import static org.junit.Assert.*;

public class SqlBuilder3Test {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SqlBuilder3Test.class);

    public static SqlBuilder3 simple(String tbl, String col, Object value) {
      final SqlBuilder3 sb = new SqlBuilder3();
      final Table table = sb.table(tbl);
      final Column column = sb.column(table, col);
      sb.addProjection(column);
      if (value != null) {
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
      assertEquals("1|10|11|3", CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(1, new Integer[]{10, 11}, 3), "|"));
    }

    @Test
    public void empty() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      assertEquals("select * /* no_proj_defined */ from ", sb.getStatement());
    }

    @Test
    public void simple0() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      sb.addConjunction(sb.column(sb.table("tbl"), "id"), 12);
      assertEquals("select * /* no_proj_defined */ from tbl where tbl.id=?", sb.getSelect());
      assertEquals(1, sb.getParameters().length);
      assertEquals(12, sb.getParameters()[0]);
    }

    @Test
    public void simple1() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      sb.addConjunction(sb.column(sb.table("tbl"), "col"), "val");
      assertEquals("select * /* no_proj_defined */ from tbl where tbl.col=?", sb.getSelect());
      assertEquals(1, sb.getParameters().length);
      assertEquals("val", sb.getParameters()[0]);
    }

    @Test
    public void caseInsensitive() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      sb.addConjunction(sb.criterion(sb.column(sb.table("tbl"), "col"), Operator.EQ_CASE_INSENSITIVE, "val"));
      assertEquals("select * /* no_proj_defined */ from tbl where lower(tbl.col)=lower(?)", sb.getSelect());
      assertEquals(1, sb.getParameters().length);
      assertEquals("val", sb.getParameters()[0]);
    }

    @Test
    public void simpleIsNull() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      sb.addConjunction(sb.column(sb.table("tbl"), "checknull"), null);
      assertEquals("select * /* no_proj_defined */ from tbl where tbl.checknull is null", sb.getSelect());
      assertEquals(0, sb.getParameters().length);
    }
    
    @Test
    public void simpleIsNotNull() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      sb.addConjunction(sb.criterion(sb.column(sb.table("tbl"), "checknotnull"), Operator.NOT_EQ, null));
      assertEquals("select * /* no_proj_defined */ from tbl where tbl.checknotnull is not null", sb.getSelect());
      assertEquals(0, sb.getParameters().length);
    }

    @Test
    public void criterionNeverEquals() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      sb.addConjunction(sb.criterionNeverEquals(sb.column(sb.table("tbl"), "id")));
      assertEquals("select * /* no_proj_defined */ from tbl where tbl.id!=tbl.id", sb.getSelect());
      assertEquals(0, sb.getParameters().length);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void simple2() throws Exception {
      {
        SqlBuilder3 sb = new SqlBuilder3();
        Column col = sb.column(sb.table("tbl"), "col");
        sb.addConjunction(sb.criterion(col, 3));
        assertEquals("select * /* no_proj_defined */ from tbl where tbl.col=?", sb.getSelect());
        assertEquals(1, sb.getParameters().length);
        final Integer[] expectedParams = new Integer[] { 3 };
        assertEquals(expectedParams, sb.getParameters());
      }
      {
        SqlBuilder3 sb = new SqlBuilder3();
        Column col = sb.column(sb.table("tbl"), "col");
        sb.addConjunction(sb.criterion(col, new Character[] { 'a', 'b', 'c' }));
        assertEquals("select * /* no_proj_defined */ from tbl where tbl.col in (?,?,?)", sb.getSelect());
        assertEquals(3, sb.getParameters().length);
        final Character[] expectedParams = new Character[] { 'a', 'b', 'c' };
        assertEquals(expectedParams, sb.getParameters());
      }
      {
        SqlBuilder3 sb = new SqlBuilder3();
        Column col = sb.column(sb.table("tbl"), "col");
        sb.addConjunction(sb.criterion(col, new String[] { "a", "b", "c" }));
        assertEquals("select * /* no_proj_defined */ from tbl where tbl.col in (?,?,?)", sb.getSelect());
        assertEquals(3, sb.getParameters().length);
        final String[] expectedParams = new String[] { "a", "b", "c" };
        assertEquals(expectedParams, sb.getParameters());
      }
      {
        SqlBuilder3 sb = new SqlBuilder3();
        Column col = sb.column(sb.table("tbl"), "col");
        final Integer[] arr = new Integer[] { 5, 6 };
        sb.addConjunction(sb.criterion(col, arr));
        assertEquals("select * /* no_proj_defined */ from tbl where tbl.col in (?,?)", sb.getSelect());
        assertEquals(2, sb.getParameters().length);
        final Integer[] expectedParams = new Integer[] { 5, 6 };
        assertEquals(expectedParams, sb.getParameters());
      }
      {
        SqlBuilder3 sb = new SqlBuilder3();
        Column col = sb.column(sb.table("tbl"), "col");
        try {
          sb.addConjunction(sb.criterion(col, new char[] { 'a', 'b', 'c' }));
          fail("no longer supported for native arrays, use vararg signature");
        } catch (Exception e) {
          // Expected
        }
        sb.addConjunction(sb.criterionVararg(col, 'a', 'b', 'c'));
        assertEquals("select * /* no_proj_defined */ from tbl where tbl.col in (?,?,?)", sb.getSelect());
        assertEquals(3, sb.getParameters().length);
        final Character[] expectedParams = new Character[] { 'a', 'b', 'c' };
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
      assertEquals("select * /* no_proj_defined */ from t2, t1 where t1.c1=? and t2.c2=? order by t2.c2 asc", sb.getSelect());
    }

    @Test
    public void join() throws Exception {
      SqlBuilder3 sb = new SqlBuilder3();
      Column t1c1 = sb.column(sb.table("table1", "t1"), "c1");
      sb.addConjunction(sb.criterion(t1c1, 1));
      sb.addProjection(sb.column(sb.table("table1", "t1"), "proj1"));
      Column t2c2 = sb.column(sb.table("table2", "t2"), "c2");
      sb.addConjunction(sb.criterion(t2c2, 2));
      sb.innerJoin(t1c1, t2c2);
      Column t3c3 = sb.column(sb.table("table3", "t3"), "c3");
      sb.innerJoin(t3c3, t1c1);
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
    public void joinWithExtraCriterion() throws Exception {
      SqlBuilder3 sb = new SqlBuilder3();
      Column t1c1 = sb.column(sb.table("table1", "t1"), "c1");
      Table t2 = sb.table("table2", "t2");
      Column t2c2 = sb.column(t2, "c2");
      sb.addProjection(t1c1);
      final Criterion extraCriterion1 = sb.criterion(sb.column(t2, "txt"), "str");
      final Criterion extraCriterion2 = sb.criterion(sb.column(t2, "num"), 5);

      // When using an innerJoin:
      sb.innerJoin(t1c1, t2c2, extraCriterion1, extraCriterion2);
      assertEquals(
          "select t1.c1 from table1 t1 inner join table2 t2 on t2.c2=t1.c1 and t2.txt=? and t2.num=?",
          sb.getSelect());
    }

    @Test
    public void existsInsteadOfJoin() throws Exception {
      SqlBuilder3 sb = new SqlBuilder3();
      Column t1c1 = sb.column(sb.table("table1", "t1"), "c1");
      Table t2 = sb.table("table2", "t2");
      Column t2c2 = sb.column(t2, "c2");
      sb.addProjection(t1c1);
      final Criterion extraCriterion1 = sb.criterion(sb.column(t2, "txt"), "str");
      final Criterion extraCriterion2 = sb.criterion(sb.column(t2, "num"), 5);

      sb.addConjunction(sb.exists(t1c1, t2c2, extraCriterion1, extraCriterion2));
      assertEquals(
          "select t1.c1 from table1 t1 where exists(select t2.c2 from table2 t2 where t2.txt=? and t2.num=? and t2.c2=t1.c1)",
          sb.getSelect());
    }

    @Test
    public void notExists() throws Exception {
      SqlBuilder3 sb = new SqlBuilder3();
      Column t1c1 = sb.column(sb.table("table1", "t1"), "c1");
      Table t2 = sb.table("table2", "t2");
      Column t2c2 = sb.column(t2, "c2");
      sb.addProjection(t1c1);
      final Criterion extraCriterion1 = sb.criterion(sb.column(t2, "txt"), "str");
      final Criterion extraCriterion2 = sb.criterion(sb.column(t2, "num"), 5);

      sb.addConjunction(sb.notExists(t1c1, t2c2, extraCriterion1, extraCriterion2));
      assertEquals(
          "select t1.c1 from table1 t1 where not exists(select t2.c2 from table2 t2 where t2.txt=? and t2.num=? and t2.c2=t1.c1)",
          sb.getSelect());
    }

    @Test
    public void subtableUnion() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      boolean all = false;
      sb.tableSubUnion("sub", all, simple("t1", "c1", 12), simple("t2", "c2", "x"), simple("t3", "c3", 34));
      // logger.debug("Union query: {}", sb.describe());
      assertEquals(
          "select * /* no_proj_defined */ from (select t1.c1 from t1 where t1.c1=? union select t2.c2 from t2 where t2.c2=? union select t3.c3 from t3 where t3.c3=?) sub",
          sb.getSelect());
      assertEquals(3, sb.getParameters().length);
    }

    @Test
    public void subtableUnionAll() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      boolean all = true;
      sb.tableSubUnion("sub", all, simple("t1", "c1", 12), simple("t2", "c2", "x"), simple("t3", "c3", 34));
      // logger.debug("Union query: {}", sb.describe());
      assertEquals(
          "select * /* no_proj_defined */ from (select t1.c1 from t1 where t1.c1=? union all select t2.c2 from t2 where t2.c2=? union all select t3.c3 from t3 where t3.c3=?) sub",
          sb.getSelect());
      assertEquals(3, sb.getParameters().length);
    }

    @Test
    public void subselect() throws Exception {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("table"), "id");
      sb.addConjunction(sb.subselect(col, "my_sub_select of ? or ?", new Integer[] { 5, 6 }));
      logger.debug("Union query: {}", sb.describe());
      assertEquals("select count(*) from table where table.id in (my_sub_select of ? or ?)", sb.getSelectCount());
    }

    @Test
    public void apiUseCase1() throws Exception {
      SqlBuilder3 sb = new SqlBuilder3();
      Column committeeId = sb.tableWithAutoAlias("committee").column("id");
      Table nav = sb.tableWithAutoAlias("apinav3_fw_cp");
      Column src = sb.column(nav, "src_id");
      Column tgt = sb.column(nav, "tgt_id");
      sb.addProjection(tgt);
      sb.addConjunction(sb.criterion(committeeId, 9));
      sb.addConjunction(sb.criterion(tgt, 8));
      sb.innerJoin(committeeId, src);
      sb.innerJoin(tgt, sb.column(sb.tableWithAutoAlias("person"), "id"));
      logger.debug("apiUseCase1: {}", sb.describe());
      assertEquals(
          "select t2.tgt_id from committee t1 inner join apinav3_fw_cp t2 on t2.src_id=t1.id inner join person t3 on t3.id=t2.tgt_id where t1.id=9 and t2.tgt_id=8",
          sb.getSelectWithInlineParams());
    }

    /**
     * We used to generate bogus count() SQL when more than one projection was registered...
     * Example:    select count(tbl.col1, tbl.col2) from tbl
     * instead of: select count(*) from tbl
     * Yet I'm not 100% sure it's the correct behaviour we want - but at least not bogus SQL.
     */
    @Test
    public void countMoreThanOneProjectedColumn() throws Exception {
      final SqlBuilder3 sb = new SqlBuilder3();
      Table table = sb.table("tbl");
      sb.addProjection(sb.column(table, "col1"));
      sb.addProjection(sb.column(table, "col2"));
      // Old bogus SQL
      String selectCount = sb.getSelectCount();
      assertFalse("select count(tbl.col1, tbl.col2) from tbl".equals(selectCount));
      assertEquals("select count(*) from tbl", selectCount);
    }
}
