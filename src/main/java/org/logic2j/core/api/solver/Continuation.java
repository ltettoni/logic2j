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

package org.logic2j.core.api.solver;

/**
 * Codes that the application or library returns to specify the behaviour that the inference engine should take after
 * a solution was found, via
 * {@link org.logic2j.core.api.solver.listener.SolutionListener#onSolution(org.logic2j.core.api.unify.UnifyContext)}.
 *
 * NOTE: Only those two possible values should be returned, see documentation of SolutionListener.
 *
 * @author tettoni
 */
public interface Continuation {
    /**
     * Value that {@link org.logic2j.core.api.solver.listener.SolutionListener#onSolution(org.logic2j.core.api.unify.UnifyContext)}
     * must return for the inference engine to continue solving (search for alternate solutions).
     */
    public static Integer CONTINUE  = Integer.valueOf(0);
    /**
     * Value that {@link org.logic2j.core.api.solver.listener.SolutionListener#onSolution(org.logic2j.core.api.unify.UnifyContext)}
     * must return for the inference engine to stop solving (ie. means caller requests abort).
     */
    public static Integer USER_ABORT = Integer.valueOf(-1);

}
