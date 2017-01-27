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
     * @param theListener
     * @param currentVars
     * @return
     */
    Object dispatch(String theMethodName, Struct theGoalStruct, SolutionListener theListener, UnifyContext currentVars);

}
