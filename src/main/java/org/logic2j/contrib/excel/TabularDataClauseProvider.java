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

package org.logic2j.contrib.excel;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.engine.exception.PrologNonSpecificException;
import org.logic2j.engine.unify.UnifyContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.logic2j.engine.model.TermApiLocator.termApi;

/**
 * An implementation of ClauseProvider for TabularData.
 * See also TabularDataFactProvider for an implementation using DataFacts instead of clauses (preferred for large data sets).
 */
public class TabularDataClauseProvider implements ClauseProvider {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TabularDataClauseProvider.class);
  static final String EAVT = "eavt";
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
    final List<Object> terms = new TabularDataTermAdapter(this.prolog.getTermAdapter()).toTerms(this.tabularData, this.mode);
    for (final Object term : terms) {
      this.clauses.add(new Clause(this.prolog, term));
    }
    logger.debug("Finished to init clauses");
  }

  @Override
  public Iterable<Clause> listMatchingClauses(Object theGoal, UnifyContext currentVars) {
    final String predicateSignature = termApi().predicateSignature(theGoal);
    switch (this.mode) {
      case EAV_NAMED:
        if (!predicateSignature.equals(this.tabularData.getDataSetName() + "/3")) {
          return Collections.emptyList();
        }
        return this.clauses;
      case EAVT:
        if (!predicateSignature.equals(EAVT_4)) {
          return Collections.emptyList();
        }
        return this.clauses;
      case RECORD:
        if (!predicateSignature.equals(this.tabularData.getDataSetName() + '/' + this.tabularData.getNbColumns())) {
          return Collections.emptyList();
        }
        return this.clauses;
      default:
        throw new PrologNonSpecificException("Unknown mode " + this.mode);
    }
  }
}
