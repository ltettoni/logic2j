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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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
  public static final String OPERATOR_NOT = "not";
  public static final String OPERATOR_AND = "and";
  public static final String OPERATOR_OR = "or";

  /**
   * One of the constants {@link #SELECT}, {@link #UPDATE} or {@link #DELETE}.
   */
  private String instruction = SELECT; // The default statement is a query
  private String sql = null; // The text SQL
  private List<Object> parameters = new ArrayList<Object>();

  private boolean inlineParams = false; // When true, will inline all parameters instead of using placehoder '?'
  private boolean distinct = false; // Generate "select distinct ..." or "count(distinct ...)"

  public Set<Table> tables = new LinkedHashSet<Table>(); // All tables registered, unique!
  private List<BaseCriterion> conjunctions = new ArrayList<BaseCriterion>();
  private List<Column> projections = new ArrayList<Column>();
  private List<ColumnOrder> orders = new ArrayList<ColumnOrder>();

  private List<Join> join = new ArrayList<Join>();

  public SqlBuilder3() {
    // Nothing
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  /**
   * @return The SQL statement.
   */
  public String getSql() {
    ensureInitialized();
    return this.sql;
  }

  private void ensureInitialized() {
    if (this.sql == null) {
      throw new IllegalStateException("SqlBuilder not initialized - one of the generate*() methods needs to be called first");
    }
  }

  //---------------------------------------------------------------------------
  // Methods
  //---------------------------------------------------------------------------

  public void generateSelect() {
    ensureSelect();
    this.parameters.clear(); // This is quite ugly - while generating (not while registering) we do addParameter() so we need to clear them first!
    final StringBuilder sb = new StringBuilder(300);
    sb.append(this.instruction);
    sb.append(' ');
    if (isDistinct()) {
      sb.append("distinct ");
    }
    if (isSelect()) {
      if (!this.projections.isEmpty()) {
        sb.append(generateProjections());
        sb.append(' ');
      } else {
        sb.append('*');
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
    this.sql = sb.toString();
    substitutePlaceholdersIfAsked();
  }

  public void generateSelectCount() {
    ensureSelect();
    this.parameters.clear(); // This is quite ugly - while generating (not while registering) we do addParameter() so we need to clear them first!
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
    this.sql = sb.toString();
    substitutePlaceholdersIfAsked();
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
  }

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

  private Object conjunctions() {
    final List<String> fragments = new ArrayList<String>();
    for (BaseCriterion conj : this.conjunctions) {
      fragments.add(conj.sql());
    }
    return CollectionUtils.formatSeparated(fragments, " and ");
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

  private void ensureSelect() {
    if (!isSelect()) {
      throw new UnsupportedOperationException("Cannot generate \"select\" on non-select SqlBuilder3");
    }
  }

  private void substitutePlaceholdersIfAsked() {
    if (isInlineParams()) {
      this.sql = substituteArgPlaceholders(this.sql, this.parameters);
      this.parameters.clear();
    }
  }

  /**
   * Generate several "?" parameter placeholders for scalar or vectorial (inlist) parameters.
   * @param theNumber 0 for no parameter, 1 for a scalar parameter, >1 for vectorial parameters,
   * this implies the INlist operator.
   * @return "" when theNumber=0, "?" when theNumber=1, otherwise "(?,?,?,?...?)" with
   * as many question marks as argument theNumber
   */
  static StringBuilder inlistParamsPlaceholders(int theNumber) {
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
  static Object[] flattenedParams(Object... theParams) {
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

  private String substituteArgPlaceholders(final String selectWithParams, final List<Object> theParams) {
    final StringBuilder sb = new StringBuilder();
    final int len = selectWithParams.length();
    int argN = 0;
    for (int i = 0; i < len; i++) {
      final char c = selectWithParams.charAt(i);
      if (c == '?') {
        sb.append(objectToSqlLiteral(theParams.get(argN)));
        argN++;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * @param theObject
   * @return A sql literal for the object. Beuarhk.
   */
  private Object objectToSqlLiteral(Object theObject) {
    if (theObject instanceof CharSequence) {
      return '\'' + String.valueOf(theObject) + '\'';
    }
    return String.valueOf(theObject);
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
    final StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
    sb.append('{');
    if (this.sql != null) {
      sb.append("sql=\"");
      sb.append(this.sql);
      sb.append("\"");
    } else {
      sb.append("no-sql-yet");
    }
    sb.append('}');
    return sb.toString();
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

  boolean isSelect() {
    return SELECT.equals(this.instruction);
  }

  public boolean isDistinct() {
    return this.distinct;
  }

  public void setDistinct(boolean theDistinct) {
    this.distinct = theDistinct;
  }

  /**
   * @return the inlineParams
   */
  public boolean isInlineParams() {
    return this.inlineParams;
  }

  /**
   * @param theInlineParams the inlineParams to set
   */
  public void setInlineParams(boolean theInlineParams) {
    this.inlineParams = theInlineParams;
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

  //---------------------------------------------------------------------------
  // Factories
  //---------------------------------------------------------------------------

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

  /**
   * @param theAlias
   * @param optionUnionAll
   * @param theSubQueries
   * @return A union subtable.
   */
  public Table tableSubUnion(String theAlias, boolean optionUnionAll, SqlBuilder3... theSubQueries) {
    Table table = new Table(theSubQueries, optionUnionAll, theAlias);
    this.tables.add(table);
    return table;
  }

  public Column column(Table theTable, String theColumnName) {
    return new Column(theTable, theColumnName);
  }

  public Criterion criterion(Column theColumn, Object... theScalarOrListValue) {
    return criterion(theColumn, OPERATOR_EQ_OR_IN, theScalarOrListValue);
  }

  public Criterion criterion(Column theColumn, String theOperator, Object... theScalarOrListValue) {
    final String effectiveOperator;
    if (theOperator == null || OPERATOR_EQ_OR_IN.equals(theOperator)) {
      effectiveOperator = equalityOrInOperator(theScalarOrListValue);
    } else if (OPERATOR_NOT_EQ_NOR_IN.equals(theOperator)) {
      effectiveOperator = notEqualityOrInOperator(theScalarOrListValue);
    } else {
      effectiveOperator = theOperator;
    }
    return new Criterion(theColumn, effectiveOperator, theScalarOrListValue);
  }

  public BaseCriterion not(BaseCriterion theCriterion) {
    return new LogicalExpression(OPERATOR_NOT, theCriterion);
  }
  
  public BaseCriterion and(BaseCriterion... theCriteria) {
    return new LogicalExpression(OPERATOR_AND, theCriteria);
  }

  public BaseCriterion or(BaseCriterion... theCriteria) {
    return new LogicalExpression(OPERATOR_OR, theCriteria);
  }

  
  public SqlBuilder3 addConjunction(Column theColumn, Object... theScalarOrListValue) {
    this.conjunctions.add(criterion(theColumn, theScalarOrListValue));
    return this;
  }

  public SqlBuilder3 addConjunction(BaseCriterion theCriterion) {
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
   * @version $Id: SqlBuilder3.java,v 1.7 2011-10-14 22:53:46 tettoni Exp $
   */
  public class Table {

    public String table;
    public String alias; // Never null, == to table when was not specified
    private int aliasCounter = 0;
    private SqlBuilder3[] subQueries = null;
    private boolean optionUnionAll;

    public Table(String theTableAndAlias) {
      this(theTableAndAlias, theTableAndAlias);
    }

    Table(String theTable, String theAlias) {
      super();
      this.table = theTable;
      this.alias = theAlias;
    }

    /**
     * Table based on sub queries.
     * @param theSubQueries
     * @param theOptionUnionAll 
     * @param theAlias
     */
    public Table(SqlBuilder3[] theSubQueries, boolean theOptionUnionAll, String theAlias) {
      if (theAlias == null || theAlias.length() == 0) {
        throw new IllegalArgumentException("Subtable alias name is required");
      }
      if (theSubQueries == null || theSubQueries.length == 0) {
        throw new IllegalArgumentException("At least one subtable required");
      }
      this.table = "sub_" + theAlias;
      this.alias = theAlias;
      this.subQueries = theSubQueries;
      this.optionUnionAll = theOptionUnionAll;
    }

    public boolean exactlyEquals(Table that) {
      return this.table.equals(that.table) && this.alias.equals(that.alias);
    }

    public synchronized Table createWithAutoAlias(String theTableName, String theAliasPrefix) {
      final String alia = theAliasPrefix + (++this.aliasCounter);
      return new Table(theTableName, alia);
    }

    public String declaration() {
      if (this.subQueries != null) {
        final StringBuilder subTable = new StringBuilder();
        subTable.append('(');
        int counter = 0;
        for (SqlBuilder3 sub : this.subQueries) {
          if (!sub.isSelect()) {
            throw new IllegalArgumentException("Only subtables corresponding to select statement allowed, not " + sub.toString());
          }
          if (counter > 0) {
            subTable.append(" union ");
            if (this.optionUnionAll) {
              subTable.append("all ");
            }
          }
          sub.generateSelect();
          subTable.append(sub.getSql());
          SqlBuilder3.this.addParameter(sub.getParameters());
          counter++;
        }
        subTable.append(')');
        subTable.append(' ');
        subTable.append(this.alias);
        return subTable.toString();
      }
      if (this.alias != null && !this.alias.equalsIgnoreCase(this.table)) {
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

  public abstract class BaseCriterion {
    public String operator;
    public abstract String sql();
  }
  
  
  public class LogicalExpression extends BaseCriterion {
    public List<BaseCriterion> members = new ArrayList<BaseCriterion>();

    /**
     * @param theOperator
     * @param theCriterion
     */
    LogicalExpression(String theOperator, BaseCriterion... theCriterion) {
      this.operator = theOperator;
      this.members.clear();
      this.members.addAll(Arrays.asList(theCriterion));
    }

    @Override
    public String sql() {
      if (OPERATOR_NOT.equalsIgnoreCase(this.operator)) {
        return OPERATOR_NOT + '(' + this.members.get(0).sql() + ')'; 
      }
      final List<String> fragments = new ArrayList<String>();
      for (BaseCriterion element : this.members) {
        fragments.add(element.sql());
      }
      return '(' + CollectionUtils.formatSeparated(fragments, " " + this.operator + " ") + ')';
    }
  }
  
  public class Criterion extends BaseCriterion {

    private Column column;
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

    @Override
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

  /**
   * @return Multiline description for debugging.
   */
  public String describe() {
    final StringBuilder sb = new StringBuilder(this.toString());
    sb.append('\n');
    sb.append(" projections: ");
    sb.append(this.projections);
    sb.append('\n');
    sb.append(" tables     : ");
    sb.append(this.tables);
    sb.append('\n');
    sb.append(" joins      : ");
    sb.append(this.join);
    sb.append('\n');
    sb.append(" criteria   : ");
    sb.append(this.conjunctions);
    sb.append('\n');
    sb.append(" parameters : ");
    sb.append(this.parameters);
    return sb.toString();
  }

}
