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
package org.logic2j.theory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.logic2j.ClauseProvider;
import org.logic2j.PrologImplementor;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.library.impl.rdb.RDBBase;
import org.logic2j.model.Clause;
import org.logic2j.model.exception.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.Bindings;
import org.logic2j.util.SqlBuilder3;
import org.logic2j.util.SqlBuilder3.Table;
import org.logic2j.util.SqlRunner;

/**
 * List {@link Clause}s (facts, never rules) from relational database tables or views accessed 
 * from the JDBC {@link DataSource} API.
 * When trying to solve the goal "zipcode_city(94101, City)" which yields City='SAN FRANCISCO', 
 * this class expects a database table or view such as "PRED_ZIPCODE_CITY(INTEGER ARG_0, VARCHAR ARG_1)".
 */
public class RDBClauseProvider extends RDBBase implements ClauseProvider {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RDBClauseProvider.class);

  /**
   * The target database is supposed to implement tables, or (more realistically) views
   * that start with the following name. The rest of the table or view name will be the
   * predicate being listed.
   */
  //private static final String PREDICATE_TABLE_OR_VIEW_HEADER = "pred_";
  //private static final String PREDICATE_COLUMN_HEADER = "arg_";
  private HashMap<String, String[]> nameMapper = new HashMap<String, String[]>();
  private String prefix;

  public RDBClauseProvider(PrologImplementor theProlog, DataSource theDataSource, String prefix) {
    super(theProlog, theDataSource);
    this.prefix=prefix;
  }

  public RDBClauseProvider(PrologImplementor theProlog, DataSource theDataSource) {
	    super(theProlog, theDataSource);
	    this.prefix = "";
	  }
  
  public void saveTableInfo(String tableName, String[] fieldName){
	  this.nameMapper.put(tableName, fieldName);
  }
  
  private final String[] readTableInfo(String tableName){
	  return this.nameMapper.get(tableName);
  }

  
  @Override
  public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings) {
    String predicateName = theGoal.getName();
    SqlBuilder3 builder = new SqlBuilder3();
    builder.setInstruction(SqlBuilder3.SELECT);
    String tableName = tableName(theGoal);
    Table table = builder.table(tableName);
    String[] columnName = this.readTableInfo(tableName);
    
    
    for (int i = 0; i < theGoal.getArity(); i++) {
      builder.addProjection(builder.column(table, columnName[i]));
    }
    
    for (int i = 0; i < theGoal.getArity(); i++) {
    	Term t = theGoal.getArg(i);
    	if (t instanceof Var && theGoalBindings!=null) {
    		t = (new TermApi()).substitute(theGoal.getArg(i), theGoalBindings, null);
    	}
    	if (t instanceof Struct && (t.isAtom() || t.isList())){
   	    	if (t.isAtom()){
    			builder.addConjunction(builder.criterion(builder.column(table, columnName[i]), SqlBuilder3.OPERATOR_EQ_OR_IN, ((Struct)t).getName()));
    		}
   	    	else if(t.isList()){
   	    		addConjunctionList(builder, table, i, ((Struct)t).javaListFromPList(new ArrayList<Struct>(), Struct.class));
   	    	}
    	}
    	//Here we check if there is any bindings (theGoalBindings) that we can unify with the Term theGoal.getArg(i) which is a variable.
    }
    List<Clause> clauses = queryForClauses(builder, predicateName);
    return clauses;
  }

  
  protected void addConjunctionList(SqlBuilder3 builder, Table table, int columnNumber, ArrayList<Struct> structList){
	  Object[] listValues = new Object[structList.size()];
	  for (int i = 0; i < structList.size(); i++){
		  listValues[i] = structList.get(i).getName();
	  }
	  builder.addConjunction(builder.criterion(builder.column(table, this.readTableInfo(table.table)[columnNumber]), listValues));
  }
  

  protected List<Clause> queryForClauses(SqlBuilder3 builder, String predicateName) {

    List<Clause> clauses = new ArrayList<Clause>();
    List<Object[]> rows;
    try {
      builder.generateSelect();
      rows = new SqlRunner(getDataSource()).query(builder.getSql(), builder.getParameters());
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
    return theGoal.getName().substring(this.prefix.length());
  }

}
