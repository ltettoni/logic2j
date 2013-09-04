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

import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.var.Bindings;

/**
 * Functionality to unify terms together. Various implementations possible.
 */
public interface Unifier {

    /**
     * Unify terms together, term1 with its bindings, and term2 with its bindings.
     * It is perfectly legal to call with theBindings1==theBindings2.
     * 
     * @param term1
     * @param theBindings1
     * @param term2
     * @param theBindings2
     * @return true if term1 and term2 could be unified together.
     */
    boolean unify(Term term1, Bindings theBindings1, Term term2, Bindings theBindings2);

    /**
     * Deunify to the last unification that returned true.
     */
    void deunify();

}
