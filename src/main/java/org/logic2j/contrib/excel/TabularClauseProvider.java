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

package org.logic2j.contrib.excel;

import java.util.ArrayList;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;

public class TabularClauseProvider implements ClauseProvider {

    private final PrologImplementation prolog;
    private final TabularData data;

    private final ArrayList<Clause> clauses = new ArrayList<Clause>();

    /**
     * @param prolog
     * @param data
     */
    public TabularClauseProvider(PrologImplementation prolog, TabularData data) {
        super();
        this.prolog = prolog;
        this.data = data;
        initClauses();
    }

    private void initClauses() {
        final TermAdapter termAdapter = this.prolog.getTermAdapter();
        final int nbRows = this.data.nbRows();
        final int nbColumns = this.data.nbColumns();
        for (int r = 0; r < nbRows; r++) {
            final Object[] row = this.data.data[r];
            final String identifier = row[this.data.rowIdentifierColumn].toString();
            for (int c = 0; c < nbColumns; c++) {
                if (c != this.data.rowIdentifierColumn) {
                    final String property = this.data.columnNames[c];
                    final Object value = row[c];
                    final Term theClauseTerm = termAdapter.term(this.data.predicateName, FactoryMode.ATOM, identifier, property, value);
                    final Clause clause = new Clause(this.prolog, theClauseTerm);
                    clauses.add(clause);
                }
            }
        }
    }

    @Override
    public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings) {
        final String predicateSignature = theGoal.getPredicateSignature();
        if (!predicateSignature.equals(data.getPredicateSignature())) {
            return null;
        }
        return clauses;
    }
}
