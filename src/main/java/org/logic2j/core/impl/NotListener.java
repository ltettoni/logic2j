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
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.solver.listener.SolutionListenerBase;
import org.logic2j.core.api.unify.UnifyContext;

/**
 * A SolutionListener that implements the logical not.
 */
public class NotListener extends SolutionListenerBase {
    boolean found = false;

    @Override
    public Continuation onSolution(UnifyContext currentVars) {
        // Do NOT relay the solution further, just remember there was one
        this.found = true;
        return Continuation.USER_ABORT; // No need to seek for further solutions
    }

    public boolean hasSolution() {
        return found;
    }
}