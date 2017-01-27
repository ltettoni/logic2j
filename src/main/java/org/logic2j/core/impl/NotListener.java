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

package org.logic2j.core.impl;/*
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

import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.listener.SolutionListenerBase;
import org.logic2j.core.api.unify.UnifyContext;

/**
 * A SolutionListener that implements the logical not.
 */
public class NotListener extends SolutionListenerBase {
    private boolean atLeastOneSolution = false;

    @Override
    public Integer onSolution(UnifyContext currentVars) {
        // Do NOT relay the solution further, just remember there was one
        this.atLeastOneSolution = true;
        // No need to seek for further solutions. Watch out this means the goal will stop evaluating on first success.
        // Fixme Should rather say the enumeration was cancelled on purpose (optimized like in AND statements)
        return Continuation.USER_ABORT;
    }

    public boolean exists() {
        return atLeastOneSolution;
    }
}
