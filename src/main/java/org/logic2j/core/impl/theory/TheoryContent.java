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
package org.logic2j.core.impl.theory;

import static org.logic2j.engine.model.TermApiLocator.termApi;

import java.util.*;

import org.logic2j.core.api.model.Clause;
import org.logic2j.engine.model.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage of the clauses of a theory: an ordered collection of {@link Clause}s, with some indexing and structuring added for performance.
 */
public class TheoryContent {
  private static final Logger logger = LoggerFactory.getLogger(TheoryContent.class);

  /**
   * The data structure to hold our clauses: lists of {@link Clause}s by predicate key.
   * Key: unique key for all clauses whose predicate
   * head makes a family, see {@link Clause#getPredicateKey()}.
   * Value: ordered list of very very very immutable {@link Clause}s.
   */
  private final HashMap<String, List<Clause>> clauses = new HashMap<>();

  private Object initializationGoal = null;

  /**
   * Add one {@link Clause}.
   *
   * @param theClause
   */
  public synchronized void add(Clause theClause) {
    final String clauseFamilyKey = theClause.getPredicateKey();
    final List<Clause> family = this.clauses.computeIfAbsent(clauseFamilyKey, key -> new ArrayList<>());
    family.add(theClause);
  }

  /**
   * Add all {@link Clause}s contained in theContentToAddToThis.
   * Watch out, references to Clauses are added, NOT copied, because of their
   * immutable nature, they can be shared.
   *
   * @param theContentToAddToThis
   */
  public synchronized void addAll(TheoryContent theContentToAddToThis) {
    for (final Map.Entry<String, List<Clause>> extraEntry : theContentToAddToThis.clauses.entrySet()) {
      final String clauseFamilyKey = extraEntry.getKey();
      final List<Clause> family = this.clauses.computeIfAbsent(clauseFamilyKey, key -> new ArrayList<>());
      final List<Clause> clausesToAdd = extraEntry.getValue();
      family.addAll(clausesToAdd);
    }
    if (theContentToAddToThis.getInitializationGoal() != null) {
      if (this.getInitializationGoal() != null) {
        logger
                .warn("Overwriting initialization goal \"{}\" with \"{}\"", this.getInitializationGoal(), theContentToAddToThis.getInitializationGoal());
      }
      this.setInitializationGoal(theContentToAddToThis.getInitializationGoal());
    }
  }

  /**
   * Retrieve clauses matching theGoalTerm (by predicate's head name and arity).
   *
   * @param theGoalTerm
   * @return An Iterable for a foreach() loop, never null.
   */
  public Iterable<Clause> find(Object theGoalTerm) {
    if (theGoalTerm instanceof Var<?>) {
      final ArrayList<Clause> result = new ArrayList<>();
      for (List<Clause> cl : this.clauses.values()) {
        result.addAll(cl);
      }
      return result;
    }

    final String clauseFamilyKey = termApi().predicateSignature(theGoalTerm);
    final List<Clause> family = this.clauses.get(clauseFamilyKey);
      // Predicate not registered in this theory clauses, return empty, it's not a failure condition
      return Objects.requireNonNullElse(family, Collections.emptyList());
  }

  public Object getInitializationGoal() {
    return initializationGoal;
  }

  public void setInitializationGoal(Object initializationGoal) {
    this.initializationGoal = initializationGoal;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + '(' + this.clauses + ')';
  }
}
