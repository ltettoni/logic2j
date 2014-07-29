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

package org.logic2j.contrib.pojo;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.PrologImplementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allow dynamic assertion / retraction of facts or rules.
 * This {@link org.logic2j.core.api.ClauseProvider} will help you solving goals against data contexts that evolve over time.
 * You can easily add or retract facts or rules made available to the {@link org.logic2j.core.impl.Solver}.
 */
public class DynamicClauseProvider implements ClauseProvider {

  private final PrologImplementation prolog;
  private List<Clause> clauses = Collections.synchronizedList(new ArrayList<Clause>());

  public DynamicClauseProvider(PrologImplementation theProlog) {
    this.prolog = theProlog;
  }

  /**
   * Watch out will return all clauses not only those potentially matching.
   * @param theGoal
   * @param currentVars
   * @return This implementation will also return clauses whose head don't match theGoal.
   */
  @Override
  public Iterable<Clause> listMatchingClauses(Object theGoal, UnifyContext currentVars) {
    final List<Clause> clauses = new ArrayList<Clause>(this.clauses.size());
    for (final Clause cl : this.clauses) {
      if (cl != null) {
        clauses.add(cl);
      }
    }
    return clauses;
  }

  /**
   * Add a fact or a rule at the end of this ClauseProvider.
   * @note The name "assert" has to do with Prolog's assert, not Java's!
   * @param theClauseStruct There's no parsing, just a plain Object; if this has to be a Prolog fact it's likely
   *                that you have to pass a Struct.
   * @return An index corresponding to the sequence number in which the fact or rule was asserted, that you may
   * use for retracting to before this assertion.
   */
  public int assertClause(Object theClauseStruct) {
    final Clause clause = new Clause(this.prolog, theClauseStruct);
    this.clauses.add(clause);
    return this.clauses.size() - 1;
  }

  /**
   * Retract only the assertion that returned theIndex
   * @param theIndex
   */
  public void retractFactAt(int theIndex) {
    this.clauses.set(theIndex, null);
  }

  /**
   * Retract all clauses.
   */
  public void retractAll() {
    this.clauses.clear();
  }

  /**
   * Retract to before the assertion that returned theIndex
   * @param indexToRetractTo
   */
  public void retractToBeforeIndex(int indexToRetractTo) {
      if (indexToRetractTo<clauses.size()) {
          clauses = Collections.synchronizedList(clauses.subList(0, indexToRetractTo));
      }
  }
}
