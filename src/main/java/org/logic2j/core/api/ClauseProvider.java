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
package org.logic2j.core.api;

import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.monadic.PoV;

/**
 * Provide {@link Clause}s (facts or rules) from various content sources to the {@link Solver} inference
 * engine. The most typical implementation is that clauses are parsed from one (or several) theories' textual content. Other implementations
 * include database back-ends, or online resources.<br/>
 * Notice the {@link Iterable} nature of the returned clauses. This allows implementors to return iterable results sets, for example from
 * database cursors. The {@link Solver} does not need all clauses in memory at once!
 * 
 * Contract: The {@link Solver} will never cache the result from {@link #listMatchingClauses(Object, org.logic2j.core.api.monadic.PoV)},
 * therefore think of caching in case of remote content.
 */
public interface ClauseProvider {

    /**
     * Provide {@link Clause}s (facts or rules) potentially matching theGoal argument, which often is a Struct with bound or unbound
     * variables.<br/>
     * All clauses that could (but may eventually not) match theGoal must be returned by this method. This implies that the match may be
     * broader than actually needed, the {@link Solver} will determine by unification if {@link Clause}s
     * returned by this method will be eligible for inference.
     * 
     * @param theGoal
     * @return An ordered {@link Iterable} of {@link Clause}s that are candidates for unifying with theGoal. Aside from performance aspects,
     *         it is not critical to return {@link Clause}s whose head would eventually not be used by the
     *         {@link Solver}.
     */
    Iterable<Clause> listMatchingClauses(Object theGoal, PoV pov);

}
