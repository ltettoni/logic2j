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
package org.logic2j.core.api.library;

import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.unify.UnifyContext;

/**
 * A library of Prolog primitives implemented in Java, each as a method of the class.
 * Sometimes {@link PLibrary}es combine Java and a theory of Prolog rules and facts, associated
 * as a classloadable resource. Low-level predicates are implemented in Java and
 * high-level or facade predicates are implemented in Prolog.
 */
public interface PLibrary {

    static final String NO_DIRECT_INVOCATION_USE_REFLECTION = "no-direct-invocation-use-reflection";

    /**
     * The dispatcher allow direct invocation of the primitives, without the need for reflection,
     * for performance reasons.
     *
     * @param theMethodName
     * @param theGoalStruct
     * @param currentVars
     * @param theListener
     * @return
     */
    Object dispatch(String theMethodName, Struct theGoalStruct, UnifyContext currentVars, SolutionListener theListener);

}
