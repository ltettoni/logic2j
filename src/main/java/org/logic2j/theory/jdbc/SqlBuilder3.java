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
package org.logic2j.theory.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.logic2j.util.CollectionMap;
import org.logic2j.util.CollectionUtils;

// TODO: possibility to inject parameter values at a later stage, when the structure of the SqlBuilder is already created (factorized)
// TODO: possibility to have a Column on the RHS of a Criterion (e.g. Column1 > Column2, not only Column1>123)
// TODO: cannot handle subqueries
// TODO: cannot express AST expressions (OR, NOT)
public class SqlBuilder3 {
  // public static final String INSERT = "insert"; Not yet supported
  public static final String SELECT = "select";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";

  public static final String OPERATOR_EQ_OR_IN = "=";
  public static final String OPERATOR_NOT_EQ_NOR_IN = "!=";

  /**
   * One of the constants {@link #SELECT}, {@link #UPDATE} or {@link #DELETE}.
   */
  private String instruction = SELECT; // The default statement is a query
  private List<Object> parameters = new ArrayList<Object>();
  private boolean distinct = false; // Generate "select distinct ..." or "count(distinct ...)"

  public Set<Table> tables = new LinkedHashSet<Table>(); // All tables registered, unique!
  private List<Criterion> conjunctions = new ArrayList<Criterion>();
  private List<Column> projections = new ArrayList<Column>();
  private List<ColumnOrder> orders = new ArrayList<ColumnOrder>();

  private List<Join> join = new ArrayList<Join>();

  public SqlBuilder3() {
    // Nothing
  }

  /**
   * @return The SQL statement.
   */
  public String getStatement() {
    this.parameters.clear();
    StringBuilder sb = new StringBuilder(300);
    sb.append(this.instruction);
    sb.append(' ');
    if (isDistinct()) {
      sb.append("distinct ");
    }
    if (isSelect()) {
      if (!this.projections.isEmpty()) {
        sb.append(generateProjections());
        sb.append(' ');
      }
      allTables(sb);
    } else if (UPDATE.equals(this.instruction)) {
      sb.append(" table ");
      sb.append(getSingleTable());
      sb.append(" set ");
      sb.append(generateProjections());
    } else if (DELETE.equals(this.instruction)) {
      sb.append(" from ");
      sb.append(getSingleTable());
    }
    if (!this.conjunctions.isEmpty()) {
      sb.append(" where ");
      sb.append(conjunctions());
    }
    if (isSelect() && !this.orders.isEmpty()) {
      sb.append(" order by ");
      sb.append(orderByClause());
    }
    return sb.toString();
  }

  private void allTables(StringBuilder sb) {
    final CollectionMap<Table, Table> map = new CollectionMap<SqlBuilder3.Table, SqlBuilder3.Table>();
    final CollectionMap<Table, Join> mapJoin = new CollectionMap<SqlBuilder3.Table, SqlBuilder3.Join>();
    // Register joined tables
    for (Join j : this.join) {
      Table left = j.leftColumn.getTable();
      Table right = j.rightColumn.getTable();
      if (map.containsKey(left)) {
        map.add(left, right);
        mapJoin.add(left, j);
      } else if (map.containsKey(right)) {
        map.add(right, left);
        mapJoin.add(right, j);
      } else {
        map.add(left, right);
        mapJoin.add(left, j);
      }
    }
    // Now for all non-joined tables, only add them if not already listed with the joined ones.
    for (Table tbl : this.tables) {
      if (!map.contains(tbl)) {
        map.getOrCreate(tbl);
      }
    }

    sb.append("from ");
    // All the tables that do not need a join
    List<String> tableTokens = new ArrayList<String>();
    for (Entry<Table, Collection<Table>> entry : map.entrySet()) {
      if (entry.getValue().isEmpty()) {
        tableTokens.add(entry.getKey().declaration());
      }
    }

    // Now all those that need a join
    for (Entry<Table, Collection<Join>> entry : mapJoin.entrySet()) {
      List<String> joinTokens = new ArrayList<String>();
      joinTokens.add(entry.getKey().declaration());
      for (Join jn : entry.getValue()) {
        joinTokens.add(jn.generate(entry.getKey()));
      }
      tableTokens.add(CollectionUtils.formatSeparated(joinTokens, " "));
    }

    sb.append(CollectionUtils.formatSeparated(tableTokens, ", "));
    //    final Set<Table> unjoinedTables = unjoinedTables();
    //    sb.append(tables(unjoinedTables));
    //    if (!this.joinInfo.isEmpty()) {
    //      sb.append(' ');
    //      sb.append(joinClauses(unjoinedTables));
    //    }
  }

  public String getSelectCount() {
    if (!isSelect()) {
      throw new UnsupportedOperationException("Cannot generate \"select\" on non-select SqlBuilder3");
    }
    this.parameters.clear();
    StringBuilder sb = new StringBuilder(300);
    sb.append(this.instruction);
    sb.append(" count(");
    if (isDistinct()) {
      sb.append("distinct ");
    }
    if (!this.projections.isEmpty()) {
      sb.append(generateProjections());
    } else {
      sb.append("*");
    }
    sb.append(") ");
    allTables(sb);
    if (!this.conjunctions.isEmpty()) {
      sb.append(" where ");
      sb.append(conjunctions());
    }
    return sb.toString();
  }

  //  private Set<Table> unjoinedTables() {
  //    final LinkedList<Table> unjoinedTables = new LinkedList<Table>(this.tables);
  //    for (Iterator<Table> iterator = unjoinedTables.descendingIterator(); iterator.hasNext();) {
  //      Table elem = iterator.next();
  //      // Search if this table is referenced in any of the joins
  //      boolean found = false;
  //      for (Join ji : this.joinInfo) {
  //        if (ji.refersTable(elem)) {
  //          found = true;
  //          break;
  //        }
  //      }
  //      if (found && unjoinedTables.size() > 1) {
  //        iterator.remove();
  //      }
  //    }
  //    return new LinkedHashSet<Table>(unjoinedTables);
  //  }

  private CharSequence orderByClause() {
    final List<String> fragments = new ArrayList<String>();
    for (ColumnOrder order : this.orders) {
      fragments.add(order.toString());
    }
    return CollectionUtils.formatSeparated(fragments, ", ");
  }

  private CharSequence generateProjections() {
    final List<String> fragments = new ArrayList<String>();
    for (Column proj : this.projections) {
      fragments.add(proj.toString());
    }
    return CollectionUtils.formatSeparated(fragments, ", ");
  }

  private String getSingleTable() {
    if (this.tables.size() != 1) {
      throw new IllegalStateException("Expecting only one table, got: " + this.tables);
    }
    return this.tables.iterator().next().getTable();
  }

  //  private CharSequence tables(Set<Table> unjoinedTables) {
  //    final List<String> fragments = new ArrayList<String>();
  //    for (Table table : unjoinedTables) {
  //      fragments.add(table.declaration());
  //    }
  //    return CollectionUtils.formatSeparated(fragments, ", ");
  //  }

  //  private CharSequence joinClauses(Set<Table> theUnjoinedTables) {
  //    List<String> fragments = new ArrayList<String>();
  //    for (Join jInfo : this.joinInfo) {
  //      fragments.add(jInfo.generate(theUnjoinedTables));
  //    }
  //    return CollectionUtils.formatSeparated(fragments, " ");
  //  }

  private Object conjunctions() {
    final List<String> fragments = new ArrayList<String>();
    for (Criterion conj : this.conjunctions) {
      fragments.add(conj.sql());
    }
    return CollectionUtils.formatSeparated(fragments, " and ");
  }

  /**
   * @return The resulting SQL SELECT (only) statement.
   */
  public String getSelect() {
    if (!isSelect()) {
      throw new UnsupportedOperationException("Cannot generate \"select\" on non-select SqlBuilder3");
    }
    return getStatement();
  }

  /**
   * @param theProjections
   * @return The resulting SQL SELECT statement for counting on the specified projection(s).
   */
  //  public String getCount(String... theProjections) {
  //    if (!isSelect()) {
  //      throw new UnsupportedOperationException("Cannot generate \"select count\" on non-select SqlBuilder3");
  //    }
  //    if (theProjections == null || theProjections.length == 0) {
  //      throw new IllegalArgumentException("Projection for counting must not be null or empty");
  //    }
  //    String projPrefix = getAlias();
  //    return getStatement(
  //        true,
  //        "count(" + (isDistinct() ? "distinct " : "")
  //            + CollectionUtils.formatSeparated(prefixedProjections(projPrefix, theProjections), ", ") + ")");
  //  }

  /**
   * @param theParameter
   * @return The placeholder, "?" for a scalar, or "(?,?,?...)" for an array or collection.
   */
  public String addParameter(Object... theParameter) {
    final Object[] flattenedParams = flattenedParams(theParameter);
    final String result = inlistParamsPlaceholders(flattenedParams.length).toString();
    this.parameters.addAll(Arrays.asList(flattenedParams));
    return result;
  }

  //---------------------------------------------------------------------------
  // Utility functions
  //---------------------------------------------------------------------------

  /**
   * Generate several "?" parameter placeholders for scalar or vectorial (inlist) parameters.
   * @param theNumber 0 for no parameter, 1 for a scalar parameter, >1 for vectorial parameters,
   * this implies the INlist operator.
   * @return "" when theNumber=0, "?" when theNumber=1, otherwise "(?,?,?,?...?)" with
   * as many question marks as argument theNumber
   */
  public static StringBuilder inlistParamsPlaceholders(int theNumber) {
    final StringBuilder sb = new StringBuilder(1000);
    if (theNumber <= 0) {
      // return empty
    } else if (theNumber == 1) {
      sb.append('?');
    } else {
      sb.append('(');
      for (int i = theNumber; i > 0; i--) {
        sb.append('?');
        if (i > 1) {
          sb.append(',');
        }
      }
      sb.append(')');
    }
    return sb;
  }

  /**
   * Flatten out parameters of arrays and collections: in case one element is itself
   * an array or collection, all its first level elements will be added to the returned collection.
   * @param theParams
   * @return An array of theParams, where elements of arrays or collections are flatted
   * out (only the first level). May be empty but never null.
   */
  public static Object[] flattenedParams(Object... theParams) {
    if (theParams == null) {
      return new Object[0];
    }
    final ArrayList<Object> sqlParams = new ArrayList<Object>();
    // Flatten out collections and arrays
    for (Object param : theParams) {
      if (param instanceof Object[]) {
        for (Object p : (Object[]) param) {
          sqlParams.add(p);
        }
      } else if (param instanceof Collection<?>) {
        for (Object p : (Collection<?>) param) {
          sqlParams.add(p);
        }
      } else {
        // Scalar: one single element
        sqlParams.add(param);
      }
      // TODO handle the case of enums - but by name or position???
      // If param was null nothing is added
    }
    final Object[] array = sqlParams.toArray(new Object[] {});
    return array;
  }

  //---------------------------------------------------------------------------
  // Utility methods
  //---------------------------------------------------------------------------

  /**
   * @param theScalarOrListValue
   * @return "=" or " in " depending on the class of theScalarOrListValue
   */
  private String equalityOrInOperator(Object[] theScalarOrListValue) {
    if (theScalarOrListValue.length == 1) {
      if (theScalarOrListValue[0] instanceof Collection<?>) {
        int size = ((Collection<?>) theScalarOrListValue[0]).size();
        if (size > 1) {
          return " in ";
        }
      }
      return OPERATOR_EQ_OR_IN;
    } else if (theScalarOrListValue.length > 1) {
      return " in ";
    } else {
      return null;
    }
  }

  private String notEqualityOrInOperator(Object[] theScalarOrListValue) {
    if (theScalarOrListValue.length == 1) {
      if (theScalarOrListValue[0] instanceof Collection<?>) {
        int size = ((Collection<?>) theScalarOrListValue[0]).size();
        if (size > 1) {
          return " not in ";
        }
      }
      return OPERATOR_NOT_EQ_NOR_IN;
    } else if (theScalarOrListValue.length > 1) {
      return " not in ";
    } else {
      return null;
    }
  }

  //---------------------------------------------------------------------------
  // Core
  //---------------------------------------------------------------------------

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "(\"" + getStatement() + "\")";
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  /**
   * @param theInstruction one of {@link #SELECT}, {@link #UPDATE}, {@link #DELETE}.
   * @return this
   */
  public SqlBuilder3 setInstruction(String theInstruction) {
    this.instruction = theInstruction;
    return this;
  }

  public Object[] getParameters() {
    return this.parameters.toArray(new Object[] {});
  }

  public void setParameters(Object... theParameters) {
    this.parameters = new ArrayList<Object>(theParameters != null ? Arrays.asList(theParameters) : Collections.emptyList());
  }

  private boolean isSelect() {
    return SELECT.equals(this.instruction);
  }

  public boolean isDistinct() {
    return this.distinct;
  }

  public void setDistinct(boolean theDistinct) {
    this.distinct = theDistinct;
  }

  /**
   * @return The number of projected columns
   */
  public int getNbProjections() {
    return this.projections.size();
  }

  public Table table(String theTableName) {
    return table(theTableName, theTableName);
  }

  /**
   * Create and register new table with alias, or obtain previously registered one (with same name or alias).
   * @param theTableName
   * @param theAlias
   * @return A new or previously-registered Table.
   */
  public Table table(String theTableName, String theAlias) {
    final Table candidate = new Table(theTableName, theAlias);
    for (Table elem : this.tables) {
      if (elem.exactlyEquals(candidate)) {
        return elem;
      }
    }
    this.tables.add(candidate);
    return candidate;
  }

  public Column column(Table theTable, String theColumnName) {
    return new Column(theTable, theColumnName);
  }

  public Criterion criterion(Column theColumn, Object... theScalarOrListValue) {
    return criterion(theColumn, OPERATOR_EQ_OR_IN, theScalarOrListValue);
  }

  public Criterion criterion(Column theColumn, String theOperator, Object... theScalarOrListValue) {
    final String effectiveOperator;
    if (theOperator==null || OPERATOR_EQ_OR_IN.equals(theOperator)) {
      effectiveOperator = equalityOrInOperator(theScalarOrListValue);
    } else if (OPERATOR_NOT_EQ_NOR_IN.equals(theOperator)) {
      effectiveOperator = notEqualityOrInOperator(theScalarOrListValue);
    } else {
      effectiveOperator = theOperator;
    }
    return new Criterion(theColumn, effectiveOperator, theScalarOrListValue);
  }

  public SqlBuilder3 addConjunction(Column theColumn, Object... theScalarOrListValue) {
    this.conjunctions.add(criterion(theColumn, theScalarOrListValue));
    return this;
  }

  public SqlBuilder3 addConjunction(Criterion theCriterion) {
    this.conjunctions.add(theCriterion);
    return this;
  }

  /**
   * @param theColumn
   */
  public SqlBuilder3 addProjection(Column theColumn) {
    this.projections.add(theColumn);
    return this;
  }

  /**
   * @param theColumnOrder
   */
  public void addOrderBy(ColumnOrder theColumnOrder) {
    this.orders.add(theColumnOrder);
  }

  public ColumnOrder ascending(Column theColumn) {
    return new ColumnOrder(theColumn, "asc");
  }

  public void innerJoin(Column theLeftColumn, Column theRightColumn) {
    this.join.add(new Join(theLeftColumn, theRightColumn, "inner"));
  }

  /**
   * Describe references to a table or view, possibly with an alias.
   *
   */
  public class Table {

    public String table;
    public String alias; // Never null, == to table when was not specified
    private int aliasCounter = 0;

    public Table(String theTableAndAlias) {
      this(theTableAndAlias, theTableAndAlias);
    }

    Table(String theTable, String theAlias) {
      super();
      this.table = theTable;
      this.alias = theAlias;
    }

    public boolean exactlyEquals(Table that) {
      return this.table.equals(that.table) && this.alias.equals(that.alias);
    }

    public synchronized Table createWithAutoAlias(String theTableName, String theAliasPrefix) {
      final String alia = theAliasPrefix + (++this.aliasCounter);
      return new Table(theTableName, alia);
    }

    public String declaration() {
      if (!this.alias.equalsIgnoreCase(this.table)) {
        return this.table + ' ' + this.alias;
      }
      return this.table;
    }

    /**
     * @return The string "table alias" when alias is not null nor empty.
     */
    public String sql() {
      if (!this.alias.equalsIgnoreCase(this.table)) {
        return this.alias;
      }
      return this.table;
    }

    public String getTable() {
      return this.table;
    }

    public String getAlias() {
      return this.alias;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((this.alias == null) ? 0 : this.alias.hashCode());
      result = prime * result + ((this.table == null) ? 0 : this.table.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Table other = (Table) obj;
      if (!getOuterType().equals(other.getOuterType())) {
        return false;
      }
      if (this.alias == null) {
        if (other.alias != null) {
          return false;
        }
      } else if (!this.alias.equals(other.alias)) {
        return false;
      }
      if (this.table == null) {
        if (other.table != null) {
          return false;
        }
      } else if (!this.table.equals(other.table)) {
        return false;
      }
      return true;
    }

    //    @Override
    //    public boolean equals(Object obj) {
    //      if (this == obj) {
    //        return true;
    //      }
    //      if (obj == null) {
    //        return false;
    //      }
    //      if (getClass() != obj.getClass()) {
    //        return false;
    //      }
    //      Table that = (Table) obj;
    //      String thisKey = this.distinctKey();
    //      String thatKey = that.distinctKey();
    //      if (thisKey.contains(thatKey) || thatKey.contains(thisKey)) {
    //        return true;
    //      }
    //      return false;
    //      //      return thisKey.equals(thatKey);
    //    }
    //
    //    private String distinctKey() {
    //      if (this.alias.equals(this.table)) {
    //        return "#" + this.table + "#";
    //      }
    //      return "#" + ((this.table.compareTo(this.alias) > 0) ? (this.table + "#" + this.alias) : (this.alias + "#" + this.table))
    //          + "#";
    //    }

    @Override
    public String toString() {
      return getAlias();
    }

    private SqlBuilder3 getOuterType() {
      return SqlBuilder3.this;
    }
  }

  public static class Join {
    public String joinType;
    public Column leftColumn;
    public Column rightColumn;

    // Possibly some conditions to add later

    public Join(Column theLeftColumn, Column theRightColumn, String theJoinType) {
      super();
      if (theLeftColumn.getTable().exactlyEquals(theRightColumn.getTable())) {
        throw new IllegalStateException("Cannot join on the same table with same alias: " + theLeftColumn + " and "
            + theRightColumn);
      }
      this.leftColumn = theLeftColumn;
      this.rightColumn = theRightColumn;
      this.joinType = theJoinType;
    }

    /**
     * @param theReferenceTable The table on the left before "xxx join T2"
     * @return The string represetnation of the join.
     */
    public String generate(Table theReferenceTable) {
      if (!theReferenceTable.equals(this.rightColumn.getTable())) {
        return this.joinType + " join " + this.rightColumn.getTable().declaration() + " on " + this.rightColumn + OPERATOR_EQ_OR_IN
            + this.leftColumn;
      }
      if (!theReferenceTable.equals(this.leftColumn.getTable())) {
        return this.joinType + " join " + this.leftColumn.getTable().declaration() + " on " + this.leftColumn + OPERATOR_EQ_OR_IN
            + this.rightColumn;
      }
      throw new IllegalStateException("Cannot generate join clause for " + this);
    }

    public boolean refersTable(Table theElem) {
      return theElem.equals(this.leftColumn.getTable()) || theElem.equals(this.rightColumn.getTable());
    }

    @Override
    public String toString() {
      return "Join(" + this.leftColumn + ", " + this.rightColumn + ", " + this.joinType + ")";
    }
  }

  public class Column {

    private Table table;
    private String columnName;

    /**
     * @param theTable
     * @param theColumnName
     */
    public Column(Table theTable, String theColumnName) {
      this.table = theTable;
      this.columnName = theColumnName;
    }

    /**
     * @return the table
     */
    public Table getTable() {
      return this.table;
    }

    @Override
    public String toString() {
      return sql();
    }

    public String sql() {
      return this.table.sql() + "." + this.columnName;
    }
  }

  public class Criterion {

    private Column column;
    private String operator;
    private Object[] operand;

    /**
     * @param theColumn
     * @param theOperator
     * @param theOperand
     */
    public Criterion(Column theColumn, String theOperator, Object... theOperand) {
      this.column = theColumn;
      this.operator = theOperator;
      this.operand = theOperand;
    }

    @Override
    public String toString() {
      return this.column + this.operator + Arrays.asList(this.operand);
    }

    public String sql() {
      return this.column.sql() + this.operator + addParameter(this.operand);
    }

    public Column getColumn() {
      return this.column;
    }

    public String getOperator() {
      return this.operator;
    }

    public Object[] getOperand() {
      return this.operand;
    }

  }

  public class ColumnOrder {

    private Column column;
    private String direction;

    /**
     * @param theColumn
     * @param theDirection
     */
    public ColumnOrder(Column theColumn, String theDirection) {
      this.column = theColumn;
      this.direction = theDirection;
    }

    @Override
    public String toString() {
      return this.column + " " + this.direction;
    }

    public String sql() {
      return this.column.sql() + " " + this.direction;
    }
  }

  public String describe() {
    final StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
    sb.append('\n');
    sb.append(" projections: ");
    sb.append(this.projections);
    sb.append('\n');
    sb.append(" tables    : ");
    sb.append(this.tables);
    sb.append('\n');
    sb.append(" joins     : ");
    sb.append(this.join);
    sb.append('\n');
    sb.append(" criteria  : ");
    sb.append(this.conjunctions);
    return sb.toString();
  }

}
