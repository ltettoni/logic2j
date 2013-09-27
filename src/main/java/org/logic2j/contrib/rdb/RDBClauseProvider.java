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
package org.logic2j.contrib.rdb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.logic2j.contrib.rdb.util.SqlBuilder3;
import org.logic2j.contrib.rdb.util.SqlBuilder3.Table;
import org.logic2j.contrib.rdb.util.SqlRunner;
import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;

/**
 * List {@link Clause}s (facts, never rules) from relational database tables or views accessed from the JDBC {@link DataSource} API. When
 * trying to solve the goal "zipcode_city(94101, City)" which yields City='SAN FRANCISCO', this class expects a database table or view such
 * as "PRED_ZIPCODE_CITY(INTEGER ARG_0, VARCHAR ARG_1)".
 */
public class RDBClauseProvider extends RDBBase implements ClauseProvider {

    /**
     * The target database is supposed to implement tables, or (more realistically) views that start with the following name. The rest of
     * the table or view name will be the predicate being listed.
     */
    // private static final String PREDICATE_TABLE_OR_VIEW_HEADER = "pred_";
    // private static final String PREDICATE_COLUMN_HEADER = "arg_";
    private final HashMap<String, String[]> nameMapper = new HashMap<String, String[]>();
    private final String prefix;

    public RDBClauseProvider(PrologImplementation theProlog, DataSource theDataSource, String prefix) {
        super(theProlog, theDataSource);
        this.prefix = prefix;
    }

    public RDBClauseProvider(PrologImplementation theProlog, DataSource theDataSource) {
        super(theProlog, theDataSource);
        this.prefix = "";
    }

    public void saveTableInfo(String tableName, String[] fieldName) {
        this.nameMapper.put(tableName, fieldName);
    }

    private final String[] readTableInfo(String tableName) {
        return this.nameMapper.get(tableName);
    }

    // ---------------------------------------------------------------------------
    // Implementation of ClauseProvider
    // ---------------------------------------------------------------------------

    @Override
    public Iterable<Clause> listMatchingClauses(Object theGoal, Bindings theGoalBindings) {
        if (!(theGoal instanceof Struct)) {
            throw new InvalidTermException("Need a Struct term instead of " + theGoal);
        }
        final Struct goalStruct = (Struct) theGoal;
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
            if (t instanceof Var && theGoalBindings != null) {
                t = TermApi.substitute(goalStruct.getArg(i), theGoalBindings, null);
            }
            final boolean isAtom = TermApi.isAtom(t);
            if (t instanceof Struct && (isAtom || TermApi.isList(t))) {
                if (isAtom) {
                    builder.addConjunction(builder.criterion(builder.column(table, columnName[i]), SqlBuilder3.OPERATOR_EQ_OR_IN, ((Struct) t).getName()));
                } else if (TermApi.isList(t)) {
                    addConjunctionList(builder, table, i, ((Struct) t).javaListFromPList(new ArrayList<Struct>(), Struct.class));
                }
            }
            // Here we check if there is any bindings (theGoalBindings) that we can unify with the Term theGoal.getArg(i) which is a
            // variable.
        }
        final List<Clause> clauses = queryForClauses(builder, predicateName);
        return clauses;
    }

    protected void addConjunctionList(SqlBuilder3 builder, Table table, int columnNumber, ArrayList<Struct> structList) {
        final Object[] listValues = new Object[structList.size()];
        for (int i = 0; i < structList.size(); i++) {
            listValues[i] = structList.get(i).getName();
        }
        builder.addConjunction(builder.criterion(builder.column(table, this.readTableInfo(table.table)[columnNumber]), listValues));
    }

    protected List<Clause> queryForClauses(SqlBuilder3 builder, String predicateName) {

        final List<Clause> clauses = new ArrayList<Clause>();
        List<Object[]> rows;
        try {
            builder.generateSelect();
            rows = new SqlRunner(getDataSource()).query(builder.getSql(), builder.getParameters());
            for (final Object[] row : rows) {
                final Object[] args = new Object[row.length];
                for (int i = 0; i < row.length; i++) {
                    final Object object = row[i];
                    args[i] = getTermAdapter().term(object, FactoryMode.ANY_TERM);
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
