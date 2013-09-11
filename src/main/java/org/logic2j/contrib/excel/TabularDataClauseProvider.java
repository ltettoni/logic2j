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

import java.io.Serializable;
import java.util.ArrayList;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;

public class TabularDataClauseProvider implements ClauseProvider {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TabularDataClauseProvider.class);
    private static final String EAVT = "eavt";
    private static final String EAVT_4 = EAVT + "/4";

    /**
     * Describe how tabular data must be asserted into clauses.
     */
    public static enum AssertionMode {
        /**
         * Data is asserted as "named triples". For a dataset called myData, assertions will be such as:
         * myData(entityIdentifier, propertyName, propertyValue).
         */
        EAV_NAMED,
        /**
         * Data is asserted as "quads". The predicate is always "eavt" (entity, attribute, value, transaction).
         * The "transaction" identifier is the dataset name. For example:
         * eavt(entityIdentifier, propertyName, propertyValue, myData).
         */
        EAVT,
        /**
         * Data is asserted as full records with one argument per column. The order matters. This is the least
         * flexible format since changes to the data (adding or removing or reordering columns) will change the assertions.
         * myData(valueOfColumn1, valueOfColumn2, valueOfColumn3, ..., valueOfColumnN).
         */
        RECORD
    }

    private final PrologImplementation prolog;
    private final TabularData data;
    private final AssertionMode mode;

    private final ArrayList<Clause> clauses = new ArrayList<Clause>();

    public TabularDataClauseProvider(PrologImplementation prolog, TabularData data, AssertionMode mode) {
        this.prolog = prolog;
        this.data = data;
        this.mode = mode;
        initClauses();
    }

    private void initClauses() {
        logger.debug("Starting to init clauses");
        final TermAdapter termAdapter = this.prolog.getTermAdapter();
        final int nbRows = this.data.getNbRows();
        final int nbColumns = this.data.getNbColumns();
        for (int r = 0; r < nbRows; r++) {
            try {
                final Serializable[] row = this.data.getData()[r];
                switch (mode) {
                case EAV_NAMED: {
                    if (this.data.getPrimaryKeyColumn() < 0) {
                        throw new PrologNonSpecificError("Exposing tabular data with mode EAV requires the entities have a unique identifier, specify the 'primaryKeyColumn' attribute");
                    }
                    final String identifier = row[this.data.getPrimaryKeyColumn()].toString();
                    for (int c = 0; c < nbColumns; c++) {
                        if (c != this.data.getPrimaryKeyColumn()) {
                            final String property = this.data.getColumnNames()[c];
                            final Serializable value = row[c];
                            final Term theClauseTerm = termAdapter.term(this.data.getDataSetName(), FactoryMode.ATOM, identifier, property, value);
                            final Clause clause = new Clause(this.prolog, theClauseTerm);
                            clauses.add(clause);
                        }
                    }
                    break;
                }
                case EAVT: {
                    if (this.data.getPrimaryKeyColumn() < 0) {
                        throw new PrologNonSpecificError("Exposing tabular data with mode EAV requires the entities have a unique identifier, specify the 'primaryKeyColumn' attribute");
                    }
                    final String identifier = row[this.data.getPrimaryKeyColumn()].toString();
                    for (int c = 0; c < nbColumns; c++) {
                        if (c != this.data.getPrimaryKeyColumn()) {
                            final String property = this.data.getColumnNames()[c];
                            final Serializable value = row[c];
                            final Term theClauseTerm = termAdapter.term(EAVT, FactoryMode.ATOM, identifier, property, value, this.data.getDataSetName());
                            final Clause clause = new Clause(this.prolog, theClauseTerm);
                            clauses.add(clause);
                        }
                    }
                    break;
                }
                case RECORD: {
                    final Term theClauseTerm = termAdapter.term(this.data.getDataSetName(), FactoryMode.ATOM, (Object[]) row);
                    final Clause clause = new Clause(this.prolog, theClauseTerm);
                    clauses.add(clause);
                    break;
                }
                default:
                    throw new PrologNonSpecificError("Unknown mode " + this.mode);
                }
            } catch (Exception e) {
                throw new PrologNonSpecificError("Could not initClauses on row=" + r, e);
            }
        }
        logger.debug("Finished to init clauses");
    }

    @Override
    public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings) {
        final String predicateSignature = theGoal.getPredicateSignature();
        switch (mode) {
        case EAV_NAMED:
            if (!predicateSignature.equals(data.getDataSetName() + "/3")) {
                return null;
            }
            return clauses;
        case EAVT:
            if (!predicateSignature.equals(EAVT_4)) {
                return null;
            }
            return clauses;
        case RECORD:
            if (!predicateSignature.equals(data.getDataSetName() + '/' + data.getNbColumns())) {
                return null;
            }
            return clauses;
        default:
            throw new PrologNonSpecificError("Unknown mode " + this.mode);
        }
    }
}
