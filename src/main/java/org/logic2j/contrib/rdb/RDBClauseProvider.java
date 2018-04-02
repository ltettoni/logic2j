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
package org.logic2j.contrib.rdb;

import org.logic2j.contrib.rdb.util.SqlBuilder3;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Table;
import org.logic2j.contrib.rdb.util.SqlRunner;
import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.model.PrologLists;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.TermApi;
import org.logic2j.engine.unify.UnifyContext;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * List {@link org.logic2j.core.api.model.Clause}s (facts, never rules) from relational database tables or views accessed from the JDBC {@link javax.sql.DataSource} API. When
 * trying to solve the goal "zipcode_city(94101, City)" which yields City='SAN FRANCISCO', this class expects a database table or view such
 * as "PRED_ZIPCODE_CITY(INTEGER ARG_0, VARCHAR ARG_1)".
 */
public class RDBClauseProvider extends RDBBase implements ClauseProvider {

  /**
   * The target database is supposed to implement tables, or (more realistically) views that start with the following name. The rest of
   * the table or view name will be the predicate being listed.
   */
  private final HashMap<String, String[]> nameMapper = new HashMap<String, String[]>();
  private final String prefix;

  public RDBClauseProvider(PrologImplementation theProlog, DataSource theDataSource, String thePrefix) {
    super(theProlog, theDataSource);
    this.prefix = thePrefix;
  }

  public RDBClauseProvider(PrologImplementation theProlog, DataSource theDataSource) {
    super(theProlog, theDataSource);
    this.prefix = "";
  }

  public void saveTableInfo(String tableName, String[] fieldName) {
    this.nameMapper.put(tableName, fieldName);
  }

  private String[] readTableInfo(String tableName) {
    return this.nameMapper.get(tableName);
  }

  // ---------------------------------------------------------------------------
  // Implementation of ClauseProvider
  // ---------------------------------------------------------------------------

  @Override
  public Iterable<Clause> listMatchingClauses(Object theGoal, UnifyContext currentVars) {
    if (!(theGoal instanceof Struct)) {
      throw new InvalidTermException("Need a Struct term instead of " + theGoal);
    }
    final Struct goalStruct = (Struct) currentVars.reify(theGoal);
    final String predicateName = goalStruct.getName();
    final SqlBuilder3 builder = new SqlBuilder3();
    builder.setInstruction(SqlBuilder3.SELECT);
    final String tableName = tableName(goalStruct);
    final Table table = builder.table(tableName);
    final String[] columnName = this.readTableInfo(tableName);

    for (int i = 0; i < goalStruct.getArity(); i++) {
      builder.addProjection(builder.column(table, columnName[i]));
    }

    for (int i = 0; i < goalStruct.getArity(); i++) {
      Object t = goalStruct.getArg(i);
      final boolean isAtom = TermApi.isAtom(t);
      if (t instanceof Struct && (isAtom || TermApi.isList(t))) {
        if (isAtom) {
          builder.addConjunction(builder.criterion(builder.column(table, columnName[i]), SqlBuilder3.Operator.EQ, ((Struct) t).getName()));
        } else if (TermApi.isList(t)) {
          addConjunctionList(builder, table, i, PrologLists.javaListFromPList(((Struct) t), new ArrayList<Object>(), Object.class));
        }
      }
      // Here we check if there is any bindings (theGoalBindings) that we can unify with the Term theGoal.getArg(i) which is a
      // variable.
    }
    final List<Clause> clauses = queryForClauses(builder, predicateName);
    return clauses;
  }

  protected void addConjunctionList(SqlBuilder3 builder, Table table, int columnNumber, ArrayList<Object> structList) {
    final Object[] listValues = new Object[structList.size()];
    for (int i = 0; i < structList.size(); i++) {
      listValues[i] = structList.get(i).toString();
    }
    builder.addConjunction(builder.criterion(builder.column(table, this.readTableInfo(table.table)[columnNumber]), listValues));
  }

  protected List<Clause> queryForClauses(SqlBuilder3 builder, String predicateName) {

    final List<Clause> clauses = new ArrayList<Clause>();
    List<Object[]> rows;
    try {
      builder.getSelect();
      rows = new SqlRunner(getDataSource()).query(builder.getStatement(), builder.getParameters());
      for (final Object[] row : rows) {
        final Object[] args = new Object[row.length];
        for (int i = 0; i < row.length; i++) {
          final Object object = row[i];
          args[i] = getTermAdapter().toTerm(object, FactoryMode.ANY_TERM);
        }
        final Clause cl = new Clause(getProlog(), new Struct(predicateName, args));
        clauses.add(cl);
      }
    } catch (final SQLException e) {
      throw new InvalidTermException("Exception not handled: " + e, e);
    }
    return clauses;
  }

  // ---------------------------------------------------------------------------
  // Methods
  // ---------------------------------------------------------------------------

  private String tableName(Struct theGoal) {
    return theGoal.getName().substring(this.prefix.length());
  }

}
