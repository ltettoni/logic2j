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

import java.util.*;

/**
 * Storage of the content of a theory: an ordered collection of {@link Clause}s, with some indexing and structuring added for performance.
 */
public class TheoryContent {

    /**
     * The data structure to hold our clauses: lists of {@link Clause}s by predicate key. Key: unique key for all clauses whose predicate
     * head is a family, see {@link Clause#getPredicateKey()}. Value: ordered list of very very very immutable {@link Clause}s.
     */
    private final HashMap<String, List<Clause>> content = new HashMap<String, List<Clause>>();

    /**
     * Add one {@link Clause}.
     * 
     * @param theClause
     */
    public void add(Clause theClause) {
        final String clauseFamilyKey = theClause.getPredicateKey();
        List<Clause> family = this.content.get(clauseFamilyKey);
        if (family == null) {
            // No Clause yet defined in this family, create one
            family = new ArrayList<Clause>();
            this.content.put(clauseFamilyKey, family);
        }
        family.add(theClause);
    }

    /**
     * Add all {@link Clause}s contained in theExtraContent. Watch out, references are added, Clauses are NOT copied, because of their
     * immutable nature, they can be shared.
     * 
     * @param theExtraContent
     */
    public void addAll(TheoryContent theExtraContent) {
        for (final Map.Entry<String, List<Clause>> extraEntry : theExtraContent.content.entrySet()) {
            final String clauseFamilyKey = extraEntry.getKey();
            final List<Clause> clausesToAdd = extraEntry.getValue();
            if (this.content.containsKey(clauseFamilyKey)) {
                this.content.get(clauseFamilyKey).addAll(clausesToAdd);
            } else {
                this.content.put(clauseFamilyKey, clausesToAdd);
            }
        }
    }

    /**
     * Retrieve clauses matching theGoalTerm (by predicate's head name and arity).
     * 
     * @param theGoalTerm
     * @return An iterable for a foreach() loop.
     */
    public Iterable<Clause> find(Object theGoalTerm) {
        final String key = TermApi.getPredicateSignature(theGoalTerm);
        final List<Clause> list = this.content.get(key);
        if (list == null) {
            // Predicate not registered in this theory content, return empty, it's not a failure condition
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + this.content + ')';
    }
}
