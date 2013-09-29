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

import java.util.ArrayList;
import java.util.List;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.Solver;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.theory.TheoryManager;

/**
 * Allow dynamic assertion / retraction of facts or rules.
 * This {@link ClauseProvider} is designed for solving goals against different data contexts, hence allowing to easily
 * add or retract information made available to the {@link Solver}, in a more dynamic manner than using the {@link TheoryManager}.
 */
public class DynamicClauseProvider implements ClauseProvider {

    private final PrologImplementation prolog;
    private final List<Clause> clauses = new ArrayList<Clause>();

    public DynamicClauseProvider(PrologImplementation prolog) {
        this.prolog = prolog;
    }

    @Override
    public Iterable<Clause> listMatchingClauses(Object theGoal, Bindings theGoalBindings) {
        final List<Clause> nonNull = new ArrayList<Clause>(this.clauses.size());
        for (Clause cl : this.clauses) {
            if (cl != null) {
                nonNull.add(cl);
            }
        }
        return nonNull;
    }

    /**
     * @note The name "assert" has to do with Prolog's assert, not Java's!
     * @param theFact
     */
    public int assertFact(Object theFact) {
        final Clause clause = new Clause(this.prolog, theFact);
        this.clauses.add(clause);
        return this.clauses.size() - 1;
    }

    /**
     * @param theFact
     */
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
