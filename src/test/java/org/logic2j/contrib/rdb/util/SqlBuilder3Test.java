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

import org.junit.Test;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Column;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Criterion;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Operator;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Table;
import org.logic2j.engine.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
   *
   * @throws Exception
   */
  @Test
  public void inlistParamsPlaceholders() {
    assertThat(SqlBuilder3.inlistParamsPlaceholders(-10).toString()).isEqualTo("");
    assertThat(SqlBuilder3.inlistParamsPlaceholders(-1).toString()).isEqualTo("");
    assertThat(SqlBuilder3.inlistParamsPlaceholders(0).toString()).isEqualTo("");
    assertThat(SqlBuilder3.inlistParamsPlaceholders(1).toString()).isEqualTo("?");
    assertThat(SqlBuilder3.inlistParamsPlaceholders(4).toString()).isEqualTo("(?,?,?,?)");
  }

  /**
   * Testing internals.
   *
   * @throws Exception
   */
  @Test
  public void flattenedParams() {
    assertThat(CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(), "|")).isEqualTo("");
    assertThat(CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams((Object[]) null), "|")).isEqualTo("");
    assertThat(CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(5), "|")).isEqualTo("5");
    assertThat(CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(1, 2, 3), "|")).isEqualTo("1|2|3");
    assertThat(CollectionUtils.formatSeparated(SqlBuilder3.flattenedParams(1, new Integer[]{10, 11}, 3), "|")).isEqualTo("1|10|11|3");
  }

  @Test
  public void empty() {
    final SqlBuilder3 sb = new SqlBuilder3();
    assertThat(sb.getStatement()).isEqualTo("select * /* no_proj_defined */ from ");
  }

  @Test
  public void simple0() {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.addConjunction(sb.column(sb.table("tbl"), "id"), 12);
    assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.id=?");
    assertThat(sb.getParameters().length).isEqualTo(1);
    assertThat(sb.getParameters()[0]).isEqualTo(12);
  }

  @Test
  public void simple1() {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.addConjunction(sb.column(sb.table("tbl"), "col"), "val");
    assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.col=?");
    assertThat(sb.getParameters().length).isEqualTo(1);
    assertThat(sb.getParameters()[0]).isEqualTo("val");
  }

  @Test
  public void caseInsensitive() {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.addConjunction(sb.criterion(sb.column(sb.table("tbl"), "col"), Operator.EQ_CASE_INSENSITIVE, "val"));
    assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where lower(tbl.col)=lower(?)");
    assertThat(sb.getParameters().length).isEqualTo(1);
    assertThat(sb.getParameters()[0]).isEqualTo("val");
  }

  @Test
  public void simpleIsNull() {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.addConjunction(sb.column(sb.table("tbl"), "checknull"), null);
    assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.checknull is null");
    assertThat(sb.getParameters().length).isEqualTo(0);
  }

  @Test
  public void simpleIsNotNull() {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.addConjunction(sb.criterion(sb.column(sb.table("tbl"), "checknotnull"), Operator.NOT_EQ, null));
    assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.checknotnull is not null");
    assertThat(sb.getParameters().length).isEqualTo(0);
  }

  @Test
  public void criterionNeverEquals() {
    final SqlBuilder3 sb = new SqlBuilder3();
    sb.addConjunction(sb.criterionNeverEquals(sb.column(sb.table("tbl"), "id")));
    assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.id!=tbl.id");
    assertThat(sb.getParameters().length).isEqualTo(0);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void simple2() {
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      sb.addConjunction(sb.criterion(col, 3));
      assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.col=?");
      assertThat(sb.getParameters().length).isEqualTo(1);
      final Integer[] expectedParams = new Integer[]{3};
      assertThat(sb.getParameters()).isEqualTo(expectedParams);
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      sb.addConjunction(sb.criterion(col, new Character[]{'a', 'b', 'c'}));
      assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.col in (?,?,?)");
      assertThat(sb.getParameters().length).isEqualTo(3);
      final Character[] expectedParams = new Character[]{'a', 'b', 'c'};
      assertThat(sb.getParameters()).isEqualTo(expectedParams);
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      sb.addConjunction(sb.criterion(col, new String[]{"a", "b", "c"}));
      assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.col in (?,?,?)");
      assertThat(sb.getParameters().length).isEqualTo(3);
      final String[] expectedParams = new String[]{"a", "b", "c"};
      assertThat(sb.getParameters()).isEqualTo(expectedParams);
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      final Integer[] arr = new Integer[]{5, 6};
      sb.addConjunction(sb.criterion(col, arr));
      assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.col in (?,?)");
      assertThat(sb.getParameters().length).isEqualTo(2);
      final Integer[] expectedParams = new Integer[]{5, 6};
      assertThat(sb.getParameters()).isEqualTo(expectedParams);
    }
    {
      SqlBuilder3 sb = new SqlBuilder3();
      Column col = sb.column(sb.table("tbl"), "col");
      try {
        sb.addConjunction(sb.criterion(col, new char[]{'a', 'b', 'c'}));
        fail("no longer supported for native arrays, use vararg signature");
      } catch (Exception e) {
        // Expected
      }
      sb.addConjunction(sb.criterionVararg(col, 'a', 'b', 'c'));
      assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from tbl where tbl.col in (?,?,?)");
      assertThat(sb.getParameters().length).isEqualTo(3);
      final Character[] expectedParams = new Character[]{'a', 'b', 'c'};
      assertThat(sb.getParameters()).isEqualTo(expectedParams);
    }
  }

  @Test
  public void multipleTables() {
    SqlBuilder3 sb = new SqlBuilder3();
    Column col = sb.column(sb.table("t1"), "c1");
    sb.addConjunction(sb.criterion(col, 1));
    Column col2 = sb.column(sb.table("t2"), "c2");
    sb.addConjunction(sb.criterion(col2, 2));
    sb.addOrderBy(sb.ascending(col2));
    assertThat(sb.getSelect()).isEqualTo("select * /* no_proj_defined */ from t2, t1 where t1.c1=? and t2.c2=? order by t2.c2 asc");
  }

  @Test
  public void join() {
    SqlBuilder3 sb = new SqlBuilder3();
    Column t1c1 = sb.column(sb.table("table1", "t1"), "c1");
    sb.addConjunction(sb.criterion(t1c1, 1));
    sb.addProjection(sb.column(sb.table("table1", "t1"), "proj1"));
    Column t2c2 = sb.column(sb.table("table2", "t2"), "c2");
    sb.addConjunction(sb.criterion(t2c2, 2));
    sb.innerJoin(t1c1, t2c2);
    Column t3c3 = sb.column(sb.table("table3", "t3"), "c3");
    sb.innerJoin(t3c3, t1c1);
    assertThat(sb.getSelect()).isEqualTo(
            "select t1.proj1 from table1 t1 inner join table2 t2 on t2.c2=t1.c1 inner join table3 t3 on t3.c3=t1.c1 where t1.c1=? and t2.c2=?");
    assertThat(sb.getSelectCount()).isEqualTo(
            "select count(t1.proj1) from table1 t1 inner join table2 t2 on t2.c2=t1.c1 inner join table3 t3 on t3.c3=t1.c1 where t1.c1=? and t2.c2=?");
  }

  @Test
  public void joinSameTable() {
    SqlBuilder3 sb = new SqlBuilder3();
    Column col = sb.column(sb.table("table"), "c1");
    Column col2 = sb.column(sb.table("table", "alias"), "c2");
    sb.innerJoin(col, col2);
    assertThat(sb.getSelectCount()).isEqualTo("select count(*) from table inner join table alias on alias.c2=table.c1");
  }

  @Test
  public void joinWithExtraCriterion() {
    SqlBuilder3 sb = new SqlBuilder3();
    Column t1c1 = sb.column(sb.table("table1", "t1"), "c1");
    Table t2 = sb.table("table2", "t2");
    Column t2c2 = sb.column(t2, "c2");
    sb.addProjection(t1c1);
    final Criterion extraCriterion1 = sb.criterion(sb.column(t2, "txt"), "str");
    final Criterion extraCriterion2 = sb.criterion(sb.column(t2, "num"), 5);

    // When using an innerJoin:
    sb.innerJoin(t1c1, t2c2, extraCriterion1, extraCriterion2);
    assertThat(sb.getSelect()).isEqualTo("select t1.c1 from table1 t1 inner join table2 t2 on t2.c2=t1.c1 and t2.txt=? and t2.num=?");
  }

  @Test
  public void existsInsteadOfJoin() {
    SqlBuilder3 sb = new SqlBuilder3();
    Column t1c1 = sb.column(sb.table("table1", "t1"), "c1");
    Table t2 = sb.table("table2", "t2");
    Column t2c2 = sb.column(t2, "c2");
    sb.addProjection(t1c1);
    final Criterion extraCriterion1 = sb.criterion(sb.column(t2, "txt"), "str");
    final Criterion extraCriterion2 = sb.criterion(sb.column(t2, "num"), 5);

    sb.addConjunction(sb.exists(t1c1, t2c2, extraCriterion1, extraCriterion2));
    assertThat(sb.getSelect())
            .isEqualTo("select t1.c1 from table1 t1 where exists(select t2.c2 from table2 t2 where t2.txt=? and t2.num=? and t2.c2=t1.c1)");
  }

  @Test
  public void notExists() {
    SqlBuilder3 sb = new SqlBuilder3();
    Column t1c1 = sb.column(sb.table("table1", "t1"), "c1");
    Table t2 = sb.table("table2", "t2");
    Column t2c2 = sb.column(t2, "c2");
    sb.addProjection(t1c1);
    final Criterion extraCriterion1 = sb.criterion(sb.column(t2, "txt"), "str");
    final Criterion extraCriterion2 = sb.criterion(sb.column(t2, "num"), 5);

    sb.addConjunction(sb.notExists(t1c1, t2c2, extraCriterion1, extraCriterion2));
    assertThat(sb.getSelect())
            .isEqualTo("select t1.c1 from table1 t1 where not exists(select t2.c2 from table2 t2 where t2.txt=? and t2.num=? and t2.c2=t1.c1)");
  }

  @Test
  public void subtableUnion() {
    final SqlBuilder3 sb = new SqlBuilder3();
    boolean all = false;
    sb.tableSubUnion("sub", all, simple("t1", "c1", 12), simple("t2", "c2", "x"), simple("t3", "c3", 34));
    // logger.debug("Union query: {}", sb.describe());
    assertThat(sb.getSelect()).isEqualTo(
            "select * /* no_proj_defined */ from (select t1.c1 from t1 where t1.c1=? union select t2.c2 from t2 where t2.c2=? union select t3.c3 from t3 where t3.c3=?) sub");
    assertThat(sb.getParameters().length).isEqualTo(3);
  }

  @Test
  public void subtableUnionAll() {
    final SqlBuilder3 sb = new SqlBuilder3();
    boolean all = true;
    sb.tableSubUnion("sub", all, simple("t1", "c1", 12), simple("t2", "c2", "x"), simple("t3", "c3", 34));
    // logger.debug("Union query: {}", sb.describe());
    assertThat(sb.getSelect()).isEqualTo(
            "select * /* no_proj_defined */ from (select t1.c1 from t1 where t1.c1=? union all select t2.c2 from t2 where t2.c2=? union all select t3.c3 from t3 where t3.c3=?) sub");
    assertThat(sb.getParameters().length).isEqualTo(3);
  }

  @Test
  public void subselect() {
    SqlBuilder3 sb = new SqlBuilder3();
    Column col = sb.column(sb.table("table"), "id");
    sb.addConjunction(sb.subselect(col, "my_sub_select of ? or ?", new Integer[]{5, 6}));
    logger.debug("Union query: {}", sb.describe());
    assertThat(sb.getSelectCount()).isEqualTo("select count(*) from table where table.id in (my_sub_select of ? or ?)");
  }

  @Test
  public void apiUseCase1() {
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
    assertThat(sb.getSelectWithInlineParams()).isEqualTo(
            "select t2.tgt_id from committee t1 inner join apinav3_fw_cp t2 on t2.src_id=t1.id inner join person t3 on t3.id=t2.tgt_id where t1.id=9 and t2.tgt_id=8");
  }

  /**
   * We used to generate bogus count() SQL when more than one projection was registered...
   * Example:    select count(tbl.col1, tbl.col2) from tbl
   * instead of: select count(*) from tbl
   * Yet I'm not 100% sure it's the correct behaviour we want - but at least not bogus SQL.
   */
  @Test
  public void countMoreThanOneProjectedColumn() {
    final SqlBuilder3 sb = new SqlBuilder3();
    Table table = sb.table("tbl");
    sb.addProjection(sb.column(table, "col1"));
    sb.addProjection(sb.column(table, "col2"));
    // Old bogus SQL
    String selectCount = sb.getSelectCount();
    assertThat("select count(tbl.col1, tbl.col2) from tbl".equals(selectCount)).isFalse();
    assertThat(selectCount).isEqualTo("select count(*) from tbl");
  }
}
