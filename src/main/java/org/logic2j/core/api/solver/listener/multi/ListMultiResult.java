package org.logic2j.core.api.solver.listener.multi;/*
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

import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.unify.UnifyContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListMultiResult implements MultiResult {

    final List<Long> values;

    final UnifyContext currentVars;

    final Object theVar;

    final Iterator<Long> iter;

    public ListMultiResult(UnifyContext currentVars, Object theVar, List<Long> values) {
        this.values = values;
        this.currentVars = currentVars;
        this.theVar = theVar;
        this.iter = this.values.iterator();
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public UnifyContext next() {
        final Long next = iter.next();
        final UnifyContext after = currentVars.unify(theVar, next);
        return after;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove item from this iterator");
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + this.values;
    }
}
