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
import org.logic2j.core.api.unify.UnifyContext;

/**
 * Provide {@link Clause}s (facts or rules) from various content sources to the {@link org.logic2j.core.impl.Solver}.
 * The most classic implementation is that clauses are parsed from one (or several) theories' textual content.
 * Other implementations include database back-ends, or online resources.<br/>
 * Notice the {@link Iterable} nature of the returned clauses. This allows implementers to return large
 * results sets, for example from database cursors: the {@link org.logic2j.core.impl.Solver} does not need all clauses in memory at once!
 * 
 * Contract: The {@link org.logic2j.core.impl.Solver} will never cache the result of
 * {@link #listMatchingClauses(Object, org.logic2j.core.api.unify.UnifyContext)},
 * therefore consider implementing caching if access to resources is slow.
 */
public interface ClauseProvider {

    /**
     * Provide {@link Clause}s (facts or rules) potentially matching theGoal argument, which often is a Struct with bound or unbound
     * variables.<br/>
     * All clauses that could (but may eventually not) match theGoal must be returned by this method. This implies that the match may be
     * broader than actually needed, the {@link org.logic2j.core.impl.Solver} will determine by unification if {@link Clause}s
     * returned by this method will be eligible for inference.
     *
     * @param theGoal
     * @param currentVars TODO Remove this argument it is used only once for a contrib (
     * @return An ordered {@link java.lang.Iterable} of {@link Clause}s that are candidates for unifying with theGoal.
     * Implementers may return {@link Clause}s whose head would eventually not unify hence not be used by the
     *         {@link org.logic2j.core.impl.Solver}, however for performance reasons theGoal is provided and you better
     *         return only potentially matching clauses.
     */
    Iterable<Clause> listMatchingClauses(Object theGoal, UnifyContext currentVars);

}
