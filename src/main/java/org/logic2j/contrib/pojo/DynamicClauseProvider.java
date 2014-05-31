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
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.impl.PrologImplementation;

import java.util.ArrayList;
import java.util.List;

/**
 * Allow dynamic assertion / retraction of facts or rules.
 * This {@link org.logic2j.core.api.ClauseProvider} is designed for solving goals against different data contexts, hence allowing to easily
 * add or retract information made available to the {@link org.logic2j.core.api.Solver}, in a more dynamic manner than using the {@link org.logic2j.core.impl.theory.TheoryManager}.
 */
public class DynamicClauseProvider implements ClauseProvider {

    private final PrologImplementation prolog;
    private final List<Clause> clauses = new ArrayList<Clause>();

    public DynamicClauseProvider(PrologImplementation theProlog) {
        this.prolog = theProlog;
    }

    @Override
    public Iterable<Clause> listMatchingClauses(Object theGoal, PoV pov) {
        final List<Clause> nonNull = new ArrayList<Clause>(this.clauses.size());
        for (final Clause cl : this.clauses) {
            if (cl != null) {
                nonNull.add(cl);
            }
        }
        return nonNull;
    }

    /**
     * @note The name "assert" has to do with Prolog's assert, not Java's!
     * @param theFact There's not parsing, just a plain Object
     */
    public int assertFact(Object theFact) {
        final Clause clause = new Clause(this.prolog, theFact);
        this.clauses.add(clause);
        return this.clauses.size() - 1;
    }

    public void retractFactAt(int theIndex) {
        this.clauses.set(theIndex, null);
    }

    /**
     * Retract all facts
     */
    public void retractAll() {
        this.clauses.clear();
    }
}
