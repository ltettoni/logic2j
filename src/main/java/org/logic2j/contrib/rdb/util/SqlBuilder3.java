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

import org.logic2j.engine.util.CollectionUtils;

import java.util.*;

/**
 * Generate the lexical part of SQL and array of arguments based on higher-levels components of a query.
 * <p>
 * TODO: possibility to inject parameter valus at a later stage, when the structure of the SqlBuilder is already created (factorized)
 * TODO: cannot express AST expressions (OR, NOT)
 * TODO: no aggregations (min, max, sum, count)
 *
 * @author tettoni
 */
public class SqlBuilder3 {
  private static final String DEFAULT_TABLE_ALIAS = "t";

  // public static final String INSERT = "insert"; Not yet supported
  public static final String SELECT = "select";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";

  /**
   * One of the constants {@link #SELECT}, {@link #UPDATE} or {@link #DELETE}.
   */
  private String instruction = SELECT; // The default statement is a query
  List<Object> parameters = new ArrayList<>();
  private boolean distinct = false; // Generate "select distinct ..." or "count(distinct ...)"

  private Set<Table> tables = new LinkedHashSet<>(); // All tables registered, unique!
  private List<Criterion> conjunctions = new ArrayList<>();
  private List<Column> projections = new ArrayList<>();
  private List<ColumnOrder> orders = new ArrayList<>();
  private List<Join> join = new ArrayList<>();
  private int aliasCounter = 0;

  public SqlBuilder3() {
    // Nothing
  }

  public SqlBuilder3(SqlBuilder3 original) {
    this.parameters = new ArrayList<>(original.parameters);
    this.instruction = original.instruction;
    this.distinct = original.distinct;
    this.aliasCounter = original.aliasCounter;
    this.tables = new LinkedHashSet<>(original.tables);
    this.conjunctions = new ArrayList<>(original.conjunctions);
    this.projections = new ArrayList<>(original.projections);
    this.orders = new ArrayList<>(original.orders);
    this.join = new ArrayList<>(original.join);
  }

  /**
   * @return The SQL statement.
   */
  public String getStatement() {
    this.parameters.clear(); // This is quite ugly - while generating (not while registering) we do addParameter() so we need to clear them first!
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
      } else {
        // This is questionable. Should we return all, or a fixed column "noproj" and values "1", or rownum?
        sb.append('*');
        sb.append(" /* no_proj_defined */ ");
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
    final HashSet<Table> classicTables = new HashSet<>();
    final HashSet<Table> joinedTables = new HashSet<>();

    final List<String> classicFragments = new ArrayList<>();
    final List<String> joinFragments = new ArrayList<>();

    // Analyze joined tables
    for (Join jn : this.join) {
      final Table leftSideTable = jn.leftColumn.getTable();
      final Table rightSideTable = jn.rightColumn.getTable();
      final boolean leftAlreadyFound = joinedTables.contains(leftSideTable) || classicTables.contains(leftSideTable);
      final boolean rightAlreadyFound = joinedTables.contains(rightSideTable) || classicTables.contains(rightSideTable);
      if (leftAlreadyFound && rightAlreadyFound) {
        // Won'd add a join, just register a conjunction
        throw new UnsupportedOperationException("Sorry at the moment can't re-join on already-declared tables");
      } else if (leftAlreadyFound && !rightAlreadyFound) {
        joinedTables.add(rightSideTable);
        joinFragments.add(jn.generate(leftSideTable));
      } else if (!leftAlreadyFound && rightAlreadyFound) {
        joinedTables.add(leftSideTable);
        joinFragments.add(jn.generate(rightSideTable));
      } else if (!leftAlreadyFound && !rightAlreadyFound) {
        classicTables.add(leftSideTable);
        joinedTables.add(rightSideTable);
        classicFragments.add(leftSideTable.declaration());
        joinFragments.add(jn.generate(leftSideTable));
      } else {
        throw new UnsupportedOperationException("Program bug!"); // Can never happen if above tests cover all
      }
    }

    // If any table exists as defined in this.table, but has not yet been generated, it's time to add it now.
    for (Table tbl : this.tables) {
      if (!(classicTables.contains(tbl) || joinedTables.contains(tbl))) {
        classicTables.add(tbl);
        classicFragments.add(0, tbl.declaration()); // It's necessary to prepend, otherwise table aliases can't be used in joins.
      }
    }
    // Generate SQL
    sb.append("from ");
    sb.append(CollectionUtils.formatSeparated(classicFragments, ", "));
    if (!joinFragments.isEmpty()) {
      sb.append(' ');
      sb.append(CollectionUtils.formatSeparated(joinFragments, " "));
    }
  }

  public String getSelectCount() {
    if (!isSelect()) {
      throw new UnsupportedOperationException("Cannot generate \"select\" on non-select SqlBuilder3");
    }
    this.parameters.clear(); // This is quite ugly - while generating (not while registering) we do addParameter() so we need to clear them first!
    StringBuilder sb = new StringBuilder(300);
    sb.append(this.instruction);
    sb.append(" count(");
    if (isDistinct()) {
      sb.append("distinct ");
    }
    final int nbProjections = this.projections.size();
    if (nbProjections == 1) {
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

  private CharSequence orderByClause() {
    final List<String> fragments = new ArrayList<>();
    for (ColumnOrder order : this.orders) {
      fragments.add(order.toString());
    }
    return CollectionUtils.formatSeparated(fragments, ", ");
  }

  private CharSequence generateProjections() {
    final List<String> fragments = new ArrayList<>();
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
    final List<String> fragments = new ArrayList<>();
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
   * Add a value or values to the array {@link #parameters}.
   *
   * @param theParameters
   * @return The placeholder, "?" for a scalar, or "(?,?,?...)" for an array or collection.
   */
  public String addParameter(Object... theParameters) {
    if (theParameters.length == 1 && theParameters[0] == null) {
      return null;
    }
    final Object[] flattenedParams = flattenedParams(theParameters);
    final String result = inlistParamsPlaceholders(flattenedParams.length).toString();
    this.parameters.addAll(Arrays.asList(flattenedParams));
    return result;
  }

  //---------------------------------------------------------------------------
  // Utility functions
  //---------------------------------------------------------------------------

  /**
   * Generate several "?" parameter placeholders for scalar or vectorial (inlist) params.
   *
   * @param theNumber 0 for no parameter, 1 for a scalar parameter, >1 for vectorial params,
   *                  this implies the INlist operator.
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
   * Flatten out params of arrays and collections: in case one element is itself
   * an array or collection, all its first level elements will be added to the returned collection.
   *
   * @param theParameters
   * @return An array of theParameters, where elements of arrays or collections are flatted
   * out (only the first level). May be empty but never null.
   */
  static Object[] flattenedParams(Object... theParameters) {
    if (theParameters == null) {
      return new Object[0];
    }
    final ArrayList<Object> sqlParams = new ArrayList<>();
    // Flatten out collections and arrays
    for (Object param : theParameters) {
      if (param instanceof Object[]) {
        Collections.addAll(sqlParams, (Object[]) param);
      } else if (param instanceof Collection<?>) {
        sqlParams.addAll((Collection<?>)param);
      } else {
        // Scalar: one single element
        sqlParams.add(param);
      }
      // TODO handle the case of enums - but by name or position???
      // If param was null nothing is added
    }
    final Object[] array = sqlParams.toArray(new Object[0]);
    return array;
  }

  /**
   * An ugly methods - normally should not be used, but in some cases we have to :-(
   *
   * @return A flat SQL string
   */
  public String getSelectWithInlineParams() {
    final String selectWithParams = getSelect();
    final StringBuilder sb = substituteArgPlaceholders(selectWithParams);
    return sb.toString();
  }

  public String getSelectCountWithInlineParams() {
    final String selectWithParams = getSelectCount();
    final StringBuilder sb = substituteArgPlaceholders(selectWithParams);
    return sb.toString();
  }

  private StringBuilder substituteArgPlaceholders(final String selectWithParams) {
    final StringBuilder sb = new StringBuilder();
    final int len = selectWithParams.length();
    int argN = 0;
    for (int i = 0; i < len; i++) {
      final char c = selectWithParams.charAt(i);
      if (c == '?') {
        sb.append(objectToSqlLiteral(getParameters()[argN]));
        argN++;
      } else {
        sb.append(c);
      }
    }
    return sb;
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
   * Determine the vectorial size of a single Object, an Array or a Collection.
   *
   * @param theScalarOrListValue
   * @return 0 if null, 1 if a single object or collection or array of one, or the
   * effective size.
   */
  public static int sizeOfScalarOrVector(Object theScalarOrListValue) {
    final int size;
    if (theScalarOrListValue == null) {
      size = 0;
    } else if (theScalarOrListValue instanceof Collection<?>) {
      size = ((Collection<?>) theScalarOrListValue).size();
    } else if (theScalarOrListValue instanceof Object[]) {
      size = ((Object[]) theScalarOrListValue).length;
    } else if (theScalarOrListValue.getClass().isArray()) {
      throw new UnsupportedOperationException(
          "At the moment, cannot handle primitive type arrays using the criterion() method, use the more dangerous criterionVararg() one");
    } else {
      size = 1;
    }
    return size;
  }

  /**
   * @param theScalarOrListValue
   * @return {@link Operator#EQ} or {@link Operator#IN} depending on the class of theScalarOrListValue
   */
  private Operator equalityOrInOperator(Object theScalarOrListValue) {
    final int size = sizeOfScalarOrVector(theScalarOrListValue);
    if (size > 1) {
      return Operator.IN;
    }
    return Operator.EQ;
  }

  /**
   * @param theScalarOrListValue
   * @return {@link Operator#NOT_EQ} or {@link Operator#NOT_IN} depending on the class of theScalarOrListValue
   */
  private Operator notEqualityOrInOperator(Object theScalarOrListValue) {
    final int size = sizeOfScalarOrVector(theScalarOrListValue);
    if (size > 1) {
      return Operator.NOT_IN;
    }
    return Operator.NOT_EQ;
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
    return this.parameters.toArray(new Object[0]);
  }

  public void setParameters(Object... theParameters) {
    this.parameters = new ArrayList<>(theParameters != null ? Arrays.asList(theParameters) : Collections.emptyList());
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
   * @return The number of projected columns
   */
  public int getNbProjections() {
    return this.projections.size();
  }

  public Table table(String theTableName) {
    return table(theTableName, theTableName);
  }

  /**
   * Create and register a new table with a specified alias, or obtain previously registered one (with same name or alias).
   *
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
   * Create and register a new table with a new automatically generated alias. Cannot return an existing one.
   *
   * @param theTableName
   * @return A new.
   */
  public Table tableWithAutoAlias(String theTableName) {
    final Table candidate = createTableWithAutoAlias(theTableName);
    this.tables.add(candidate);
    return candidate;
  }

  /**
   * Create a subtable with other sub-queries (not sub-selects):
   * "select ... from ( subselect1 union subselect2 union ... union subselectN ) alias"
   *
   * @param theAlias       The alias name of the subtable
   * @param optionUnionAll When true, will use "union all", otherwise simple "union"
   * @param theSubQueries  The sub-queries tu union, at least one.
   * @return A union subtable.
   */
  public Table tableSubUnion(String theAlias, boolean optionUnionAll, SqlBuilder3... theSubQueries) {
    final Table table = new Table(theSubQueries, optionUnionAll, theAlias);
    this.tables.add(table);
    return table;
  }

  public Column column(Table theTable, String theColumnName) {
    return new Column(theTable, theColumnName);
  }

  /**
   * Vararg signature will allow type conversion
   *
   * @param theColumn
   * @param theValues Notice the compiler will autobox primary types into Objects, which is the desired feature...
   * @return The new Criterion.
   */
  public ColumnOperatorParameterCriterion criterionVararg(Column theColumn, Object... theValues) {
    return criterion(theColumn, theValues);
  }

  /**
   * s
   * Criterion for COLUMN = LITERAL VALUE.
   *
   * @param theColumn
   * @param theScalarOrListValue
   * @return The Criterion
   */
  public ColumnOperatorParameterCriterion criterion(Column theColumn, Object theScalarOrListValue) {
    return criterion(theColumn, Operator.EQ, theScalarOrListValue);
  }

  /**
   * Criterion for COLUMN <OPERATOR> LITERAL VALUE.
   *
   * @param theColumn
   * @param theOperator
   * @param theScalarOrListValue
   * @return The {@link ColumnOperatorParameterCriterion}
   */
  public ColumnOperatorParameterCriterion criterion(Column theColumn, Operator theOperator, Object theScalarOrListValue) {
    final Operator effectiveOperator;
    if (theOperator == null || Operator.EQ.equals(theOperator)) {
      effectiveOperator = equalityOrInOperator(theScalarOrListValue);
    } else if (Operator.NOT_EQ.equals(theOperator)) {
      effectiveOperator = notEqualityOrInOperator(theScalarOrListValue);
    } else {
      effectiveOperator = theOperator;
    }
    return new ColumnOperatorParameterCriterion(theColumn, effectiveOperator, theScalarOrListValue);
  }

  /**
   * Generates column != column, this is used when we need to produce no solution, as a boundary
   * condition.
   *
   * @param theColumn
   * @return A valid Criterion without parameter.
   */
  public ColumnOperatorLiteralCriterion criterionNeverEquals(Column theColumn) {
    return new ColumnOperatorLiteralCriterion(theColumn, Operator.NOT_EQ, theColumn.sql());
  }

  /**
   * Subselect of plain literal SQL without parameters.
   *
   * @param theCol
   * @param theSql
   * @return The SubSelect
   */
  public SubSelect subselect(Column theCol, String theSql) {
    return subselect(theCol, theSql, null);
  }

  public SubSelect subselect(Column theCol, String theSql, Object[] theParameters) {
    return new SubSelect(theCol, theSql, theParameters);
  }

  public SqlBuilder3 addConjunction(Column theColumn, Object theScalarOrListValue) {
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

  public void innerJoin(Column theLeftColumn, Column theRightColumn, Criterion... theExtraCritera) {
    this.join.add(new Join(theLeftColumn, theRightColumn, "inner", theExtraCritera));
  }

  private synchronized Table createTableWithAutoAlias(String theTableName) {
    return createTableWithAutoAlias(theTableName, DEFAULT_TABLE_ALIAS);
  }

  private synchronized Table createTableWithAutoAlias(String theTableName, String theAliasPrefix) {
    final String alia = theAliasPrefix + (++this.aliasCounter);
    return new Table(theTableName, alia);
  }

  /**
   * @param theString
   */
  public void rownum(String theString) {
    this.addConjunction(new SqlCriterion("rownum " + theString));
  }

  /**
   * Generate an "exists" criterion in the form "exists(select * from theSubqueryTable where theCriterion and theSubqueryJoinColumn=theParentColumn".
   *
   * @param theColumnInThisSqlBuilder
   * @param theJoinedColumnInExistsSubquery
   * @param theCriteria
   */
  public ExistsCriterion exists(Column theColumnInThisSqlBuilder, Column theJoinedColumnInExistsSubquery, Criterion... theCriteria) {
    final ExistsCriterion existsCriterion = new ExistsCriterion(theColumnInThisSqlBuilder, theJoinedColumnInExistsSubquery, true, theCriteria);
    // We have to remove the table registered in this SqlBuilder that will be used in the subquery.
    // Otherwise, we get a "select ... from parentTable, childTable where exists(criterion on child table).
    this.tables.remove(theJoinedColumnInExistsSubquery.getTable());
    return existsCriterion;
  }

  /**
   * Generate an "not exists" criterion in the form "not exists(select * from theSubqueryTable where theCriterion and theSubqueryJoinColumn=theParentColumn".
   *
   * @param theColumnInThisSqlBuilder
   * @param theJoinedColumnInExistsSubquery
   * @param theCriteria
   */
  public ExistsCriterion notExists(Column theColumnInThisSqlBuilder, Column theJoinedColumnInExistsSubquery, Criterion... theCriteria) {
    final ExistsCriterion existsCriterion = new ExistsCriterion(theColumnInThisSqlBuilder, theJoinedColumnInExistsSubquery, false, theCriteria);
    // We have to remove the table registered in this SqlBuilder that will be used in the subquery.
    // Otherwise, we get a "select ... from parentTable, childTable where exists(criterion on child table).
    this.tables.remove(theJoinedColumnInExistsSubquery.getTable());
    return existsCriterion;
  }

  /**
   * @return Multi-line description for debugging.
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
    sb.append(" params : ");
    sb.append(this.parameters);
    return sb.toString();
  }

  //---------------------------------------------------------------------------
  // Support classes
  //---------------------------------------------------------------------------


  /**
   * Describe references to a table or view, possibly with an alias.
   *
   * @version $Id: SqlBuilder3.java,v 1.7 2011-10-14 22:53:46 tettoni Exp $
   */
  public class Table {

    public final String table;
    public final String alias; // Never null, == to table when was not specified
    private final SqlBuilder3[] subQueries;
    private final boolean optionUnionAll;

    /**
     * @param theTableAndAlias The table name which will be the same alias name.
     */
    public Table(String theTableAndAlias) {
      this(theTableAndAlias, theTableAndAlias);
    }

    Table(String theTable, String theAlias) {
      super();
      this.table = theTable;
      this.alias = theAlias;
      this.subQueries = null;
      this.optionUnionAll = false;
    }

    /**
     * Table based on sub queries.
     *
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
          subTable.append(sub.getSelect());
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
        return other.table == null;
      } else
        return this.table.equals(other.table);
    }

    @Override
    public String toString() {
      return getAlias();
    }

    private SqlBuilder3 getOuterType() {
      return SqlBuilder3.this;
    }

    public Column column(String theColumnName) {
      return new Column(this, theColumnName);
    }
  }


  public static class Join {
    public final String joinType;
    public final Column leftColumn;
    public final Column rightColumn;
    public final Criterion criteria[];

    public Join(Column theLeftColumn, Column theRightColumn, String theJoinType, Criterion... theExtraCriteria) {
      super();
      if (theLeftColumn.getTable().exactlyEquals(theRightColumn.getTable())) {
        throw new IllegalStateException("Cannot join on the same table with same alias: " + theLeftColumn + " and " + theRightColumn);
      }
      this.leftColumn = theLeftColumn;
      this.rightColumn = theRightColumn;
      this.joinType = theJoinType;
      this.criteria = theExtraCriteria;
    }

    /**
     * Generate the new SQL join representation such as, for example: "inner join Table TableAlias on Conditions"
     *
     * @param theAlreadyDeclaredTable The tables that was already declared, so that if this join won't redeclare it, only
     *                                declare the other. Eg if this join is between A.id and B.id, when theAlreadyDeclaredTable contains B,
     *                                it would generate "inner join B on B.id = A.id". If theAlreadyDeclaredTable contains A, it would generate
     *                                "inner join A on A.id = B.id".
     * @return The string representation of the join.
     */
    public String generate(Table theAlreadyDeclaredTable) {
      StringBuilder otherClauses = new StringBuilder();
      if (this.criteria != null) {
        for (Criterion criterion : this.criteria) {
          otherClauses.append(" and ");
          otherClauses.append(criterion.sql());
        }
      }
      if (!theAlreadyDeclaredTable.equals(this.rightColumn.getTable())) {
        return this.joinType + " join " + this.rightColumn.getTable().declaration() + " on " + this.rightColumn + Operator.EQ.getSql()
            + this.leftColumn + otherClauses;
      }
      if (!theAlreadyDeclaredTable.equals(this.leftColumn.getTable())) {
        return this.joinType + " join " + this.leftColumn.getTable().declaration() + " on " + this.leftColumn + Operator.EQ.getSql()
            + this.rightColumn + otherClauses;
      }
      throw new IllegalStateException("Cannot generate join clause for " + this + ", internal error");
    }

    public boolean refersTable(Table theElem) {
      return theElem.equals(this.leftColumn.getTable()) || theElem.equals(this.rightColumn.getTable());
    }

    @Override
    public String toString() {
      if (this.criteria == null || this.criteria.length == 0) {
        return "Join(" + this.leftColumn + ", " + this.rightColumn + ", " + this.joinType + ")";
      }
      return "Join(" + this.leftColumn + ", " + this.rightColumn + ", " + this.joinType + ", crit=" + Arrays.asList(this.criteria) + ")";
    }
  }


  public class Column {

    private final Table table;
    private final String columnName;

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


  public interface Criterion {
    String sql();
  }


  /**
   * A {@link Criterion} whereby a {@link Column} is associated by an operator
   * to some value (not yet defined in this class).
   *
   * @version $Id$
   */
  public abstract class ColumnOperatorCriterion implements Criterion {

    private final Column column;
    private Operator operator;
    protected final BinaryOperatorFormatter formatter = new BinaryOperatorFormatter();

    /**
     * @param theColumn
     * @param theOperator
     */
    public ColumnOperatorCriterion(Column theColumn, Operator theOperator) {
      this.column = theColumn;
      this.operator = theOperator;
    }

    public Column getColumn() {
      return this.column;
    }

    public Operator getOperator() {
      return this.operator;
    }

    public void setOperator(Operator theOperator) {
      this.operator = theOperator;
    }
  }


  /**
   * A {@link Criterion} expressing a relation between two columns, typ t1.c1=t2.c2.
   *
   * @version $Id$
   */
  public class TwoColumnsCriterion extends ColumnOperatorCriterion {

    private final Column rightColumn;

    public TwoColumnsCriterion(Column theLeftColumn, Operator theOperator, Column theRightColumn) {
      super(theLeftColumn, theOperator);
      this.rightColumn = theRightColumn;
    }

    @Override
    public String sql() {
      return this.formatter.format(getColumn(), getOperator(), this.rightColumn);
    }
  }


  /**
   * A {@link Criterion} expressing that a {@link Column}'s is related by an operator to a value,
   * passed as a parameter (and a JDBC placeholder "?" will be used).
   *
   * @version $Id$
   */
  public class ColumnOperatorParameterCriterion extends ColumnOperatorCriterion {

    private final Object operand;

    /**
     * @param theColumn
     * @param theOperator
     * @param theOperand
     */
    public ColumnOperatorParameterCriterion(Column theColumn, Operator theOperator, Object theOperand) {
      super(theColumn, theOperator);
      this.operand = theOperand;
    }

    @Override
    public String toString() {
      return this.formatter.format(getColumn(), getOperator(), getOperand());
    }

    @Override
    public String sql() {
      return this.formatter.format(getColumn(), getOperator(), addParameter(getOperand()));
    }

    public Object getOperand() {
      return this.operand;
    }


  }


  /**
   * Column <operator> <immediate value>
   */
  class ColumnOperatorLiteralCriterion extends ColumnOperatorCriterion {

    private final String literalValue;

    ColumnOperatorLiteralCriterion(Column theColumn, Operator theOperator, String theImmediateValue) {
      super(theColumn, theOperator);
      this.literalValue = theImmediateValue;
    }

    public String getLiteralValue() {
      return this.literalValue;
    }

    @Override
    public String toString() {
      return getColumn() + getOperator().getSql() + getLiteralValue();
    }

    @Override
    public String sql() {
      return getColumn().sql() + getOperator().getSql() + getLiteralValue();
    }

  }


  /**
   * A criterion expressed as plain SQL.
   *
   * @version $Id$
   */
  public static class SqlCriterion implements Criterion {

    private final String plainSql;

    public SqlCriterion(String thePlainSql) {
      this.plainSql = thePlainSql;
    }

    @Override
    public String toString() {
      return sql();
    }

    @Override
    public String sql() {
      return this.plainSql;
    }

  }


  public static class ExistsCriterion implements Criterion {
    private final SqlBuilder3 subquery;
    private final boolean positiveExistence;

    public ExistsCriterion(Column theParentColumn, Column theSubqueryJoinColumn, boolean thePositiveExistence, Criterion... theCriteria) {
      this.positiveExistence = thePositiveExistence;
      SqlBuilder3 sb = new SqlBuilder3();
      sb.addProjection(theSubqueryJoinColumn);
      sb.table(theSubqueryJoinColumn.getTable().getTable(),
          theSubqueryJoinColumn.getTable().getAlias()); // We have to re-register the table in the sub-builder
      for (Criterion criterion : theCriteria) {
        sb.addConjunction(criterion);
      }
      sb.addConjunction(sb.new TwoColumnsCriterion(theSubqueryJoinColumn, Operator.EQ, theParentColumn));
      this.subquery = sb;
    }

    @Override
    public String toString() {
      return sql();
    }

    @Override
    public String sql() {
      final String optionalNegation = this.positiveExistence ? "" : "not ";
      return optionalNegation + "exists(" + this.subquery.getSelect() + ')';
    }

  }


  /**
   * A subselect expressed with a SQL query (and "?" placehoders), an a parameters array.
   * Note: the operator is always {@link Operator#IN}
   *
   * @version $Id$
   */
  public class SubSelect extends ColumnOperatorParameterCriterion {

    private final String sql;

    /**
     * @param theColumn
     * @param theSubSelectQuery
     * @param theParameters     May be null or empty
     */
    public SubSelect(Column theColumn, String theSubSelectQuery, Object[] theParameters) {
      super(theColumn, Operator.IN, theParameters);
      this.sql = theSubSelectQuery;
    }

    @Override
    public String sql() {
      if (getOperand() != null) {
        SqlBuilder3.this.parameters.addAll(Arrays.asList(getOperand()));
      }
      return getColumn().sql() + getOperator().getSql() + '(' + this.sql + ')';
    }

    @Override
    public String toString() {
      return getColumn().sql() + getOperator().getSql() + '(' + this.sql + ')';
    }
  }


  public class ColumnOrder {

    private final Column column;
    private final String direction;

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
   * Operator enum with SQL equivalent and Prolog equivalent representations.
   */
  public enum Operator {
    EQ("=", "="), NOT_EQ("!=", "\\="), IN(" in ", null), NOT_IN(" not in ", null), LE("<=", "=<"), LT("<", "<"), GE(">=", ">="), GT(">",
        ">"), EQ_CASE_INSENSITIVE("=", null), // somewhat doubtful they should be defined as operators - these are more SQL constructs
    EXISTS(" exists ", "E"), NOT_EXISTS(" not exists ", "\\E");

    private final String sql;
    private final String prolog;

    Operator(String theSql, String theProlog) {
      this.sql = theSql;
      this.prolog = theProlog;
    }

    //---------------------------------------------------------------------------
    // Accessors
    //---------------------------------------------------------------------------

    public String getSql() {
      return this.sql;
    }

    public String getProlog() {
      return this.prolog;
    }

    /**
     * @return true if this instance of Operator is either {@link #EXISTS} or {@link #NOT_EXISTS}.
     */
    public boolean isExists() {
      return this == EXISTS || this == NOT_EXISTS;
    }

    //---------------------------------------------------------------------------
    // Factories
    //---------------------------------------------------------------------------

    public static Operator valueOfSql(String theSql) {
      for (Operator elem : values()) {
        if (theSql.equals(elem.getSql())) {
          return elem;
        }
      }
      throw new IllegalArgumentException("Cannot instantiate Operator from SQL representation \"" + theSql + '"');
    }

    public static Operator valueOfProlog(String theProlog) {
      for (Operator elem : values()) {
        if (theProlog.equals(elem.getProlog())) {
          return elem;
        }
      }
      throw new IllegalArgumentException("Cannot instantiate Operator from Prolog representation \"" + theProlog + '"');
    }
  }


  public class BinaryOperatorFormatter {

    public String format(Column theColumn, Operator theOperator, Object theOperand) {
      if (theOperand == null) {
        switch (theOperator) {
          case EQ_CASE_INSENSITIVE:
          case EQ:
            return theColumn.sql() + " is null";
          case NOT_EQ:
            return theColumn.sql() + " is not null";
          default:
            throw new UnsupportedOperationException(
                "Don't know how to format SQL binary operator \"" + theOperator + "\" when the operand value is null");
        }
      }
      final String formattedOperand;
      if (theOperand instanceof Column) {
        formattedOperand = ((Column) theOperand).sql();
      } else if (theOperand instanceof Criterion) {
        formattedOperand = ((Criterion) theOperand).sql();
      } else {
        formattedOperand = String.valueOf(theOperand);
      }
      if (theOperator == Operator.EQ_CASE_INSENSITIVE) {
        return "lower(" + theColumn.sql() + ')' + theOperator.getSql() + "lower(" + formattedOperand + ')';
      }
      return theColumn.sql() + theOperator.getSql() + formattedOperand;
    }
  }

}
