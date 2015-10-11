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
package org.logic2j.core.impl.theory;

import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    private final HashMap<String, List<Clause>> clauses = new HashMap<String, List<Clause>>();

    private Object initializationGoal = null;

    /**
     * Add one {@link Clause}.
     * 
     * @param theClause
     */
    public synchronized void add(Clause theClause) {
        final String clauseFamilyKey = theClause.getPredicateKey();
        List<Clause> family = this.clauses.get(clauseFamilyKey);
        if (family == null) {
            // No Clause yet defined in this family, create one
            family = new ArrayList<Clause>();
            this.clauses.put(clauseFamilyKey, family);
        }
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
            List<Clause> family = this.clauses.get(clauseFamilyKey);
            if (family == null) {
                // No Clause yet defined in this family, create one
                family = new ArrayList<Clause>();
                this.clauses.put(clauseFamilyKey, family);
            }
            final List<Clause> clausesToAdd = extraEntry.getValue();
            family.addAll(clausesToAdd);
        }
        if (theContentToAddToThis.getInitializationGoal()!=null) {
            if (this.getInitializationGoal()!=null) {
                logger.warn("Overwriting initialization goal \"{}\" with \"{}\"", this.getInitializationGoal(), theContentToAddToThis.getInitializationGoal());
            }
            this.setInitializationGoal(theContentToAddToThis.getInitializationGoal());
        }
    }

    /**
     * Retrieve clauses matching theGoalTerm (by predicate's head name and arity).
     * 
     * @param theGoalTerm
     * @return An Iterable for a foreach() loop.
     */
    public Iterable<Clause> find(Object theGoalTerm) {
        if (theGoalTerm instanceof Var<?>) {
            final ArrayList<Clause> result = new ArrayList<>();
            for (List<Clause> cl: this.clauses.values()) {
                result.addAll(cl);
            }
            return result;
        }

        final String clauseFamilyKey = TermApi.predicateSignature(theGoalTerm);
        final List<Clause> family = this.clauses.get(clauseFamilyKey);
        if (family == null) {
            // Predicate not registered in this theory clauses, return empty, it's not a failure condition
            return Collections.emptyList();
        }
        return family;
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
