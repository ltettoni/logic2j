package org.logic2j.theory.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.logic2j.ClauseProvider;
import org.logic2j.PrologImplementor;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.library.impl.rdb.RDBBase;
import org.logic2j.model.Clause;
import org.logic2j.model.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.theory.jdbc.SqlBuilder3.Table;
import org.logic2j.util.SqlRunner;

/**
 * List {@link Clause}s (facts, no rules) from tables accessed from the JDBC {@link DataSource} API.
 *
 */
public class DBClauseProvider extends RDBBase implements ClauseProvider {

  public DBClauseProvider(PrologImplementor theProlog, DataSource theDataSource) {
    super(theProlog, theDataSource);
  }

  @Override
  public Iterable<Clause> listMatchingClauses(Struct theGoal) {
    String predicateName = theGoal.getName();
    SqlBuilder3 builder = new SqlBuilder3();
    builder.setInstruction(SqlBuilder3.SELECT);
    Table table = builder.table(tableName(theGoal));
    for (int i = 0; i < theGoal.getArity(); i++) {
      builder.addProjection(builder.column(table, "arg_" + i));
    }
    List<Clause> clauses = queryForClauses(builder, predicateName);
    return clauses;
  }

  protected List<Clause> queryForClauses(SqlBuilder3 builder, String predicateName) {

    // TODO Should use a RowProcessor that directly creates clauses?
    List<Clause> clauses = new ArrayList<Clause>();
    List<Object[]> rows;
    try {
      rows = new SqlRunner(getDataSource()).query(builder.getSelect(), builder.getParameters());
      for (Object[] row : rows) {
        Term[] args = new Term[row.length];
        for (int i = 0; i < row.length; i++) {
          Object object = row[i];
          args[i] = getTermFactory().create(object, FactoryMode.ANY_TERM);
        }
        final Clause cl = new Clause(getProlog(), new Struct(predicateName, args));
        clauses.add(cl);
      }
    } catch (SQLException e) {
      throw new InvalidTermException("Exception not handled: " + e, e);
    }
    return clauses;
  }

  //---------------------------------------------------------------------------
  // Methods
  //---------------------------------------------------------------------------

  private String tableName(Struct theGoal) {
    return "pred_" + theGoal.getName();
  }

}
