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
import java.util.List;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;

public class TabularDataClauseProvider implements ClauseProvider {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TabularDataClauseProvider.class);
    private static final String EAVT = "eavt";
    private static final String EAVT_4 = EAVT + "/4";

    private final PrologImplementation prolog;
    private final TabularData tabularData;
    private final TermAdapter.AssertionMode mode;

    private final ArrayList<Clause> clauses = new ArrayList<Clause>();

    public TabularDataClauseProvider(PrologImplementation prolog, TabularData tabularData, TermAdapter.AssertionMode mode) {
        this.prolog = prolog;
        this.tabularData = tabularData;
        this.mode = mode;
        initClauses();
    }

    private void initClauses() {
        logger.debug("Starting to init clauses");
        final List<Object> terms = new TabularDataTermAdapter(this.prolog).terms(this.tabularData, mode);
        for (Object term : terms) {
            this.clauses.add(new Clause(this.prolog, term));
        }
        logger.debug("Finished to init clauses");
    }

    @Override
    public Iterable<Clause> listMatchingClauses(Object theGoal, Bindings theGoalBindings) {
        final String predicateSignature = TermApi.getPredicateSignature(theGoal);
        switch (mode) {
        case EAV_NAMED:
            if (!predicateSignature.equals(tabularData.getDataSetName() + "/3")) {
                return null;
            }
            return clauses;
        case EAVT:
            if (!predicateSignature.equals(EAVT_4)) {
                return null;
            }
            return clauses;
        case RECORD:
            if (!predicateSignature.equals(tabularData.getDataSetName() + '/' + tabularData.getNbColumns())) {
                return null;
            }
            return clauses;
        default:
            throw new PrologNonSpecificError("Unknown mode " + this.mode);
        }
    }
}
