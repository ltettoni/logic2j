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

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.monadic.UnifyContext;
import org.logic2j.core.impl.PrologImplementation;

import java.util.ArrayList;
import java.util.List;

public class TabularDataClauseProvider implements ClauseProvider {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TabularDataClauseProvider.class);
    private static final String EAVT = "eavt";
    private static final String EAVT_4 = EAVT + "/4";

    private final PrologImplementation prolog;
    private final TabularData tabularData;
    private final TermAdapter.AssertionMode mode;

    private final ArrayList<Clause> clauses = new ArrayList<Clause>();

    public TabularDataClauseProvider(PrologImplementation theProlog, TabularData theTabularData, TermAdapter.AssertionMode theMode) {
        this.prolog = theProlog;
        this.tabularData = theTabularData;
        this.mode = theMode;
        initClauses();
    }

    private void initClauses() {
        logger.debug("Starting to init clauses");
        final List<Object> terms = new TabularDataTermAdapter(this.prolog.getTermAdapter()).terms(this.tabularData, this.mode);
        for (final Object term : terms) {
            this.clauses.add(new Clause(this.prolog, (Struct)term));
        }
        logger.debug("Finished to init clauses");
    }

    @Override
    public Iterable<Clause> listMatchingClauses(Object theGoal, UnifyContext currentVars) {
        final String predicateSignature = TermApi.getPredicateSignature(theGoal);
        switch (this.mode) {
        case EAV_NAMED:
            if (!predicateSignature.equals(this.tabularData.getDataSetName() + "/3")) {
                return null;
            }
            return this.clauses;
        case EAVT:
            if (!predicateSignature.equals(EAVT_4)) {
                return null;
            }
            return this.clauses;
        case RECORD:
            if (!predicateSignature.equals(this.tabularData.getDataSetName() + '/' + this.tabularData.getNbColumns())) {
                return null;
            }
            return this.clauses;
        default:
            throw new PrologNonSpecificError("Unknown mode " + this.mode);
        }
    }
}
