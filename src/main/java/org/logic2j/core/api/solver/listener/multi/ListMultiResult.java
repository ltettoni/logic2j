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

    final Var<?> var;

    final List<Integer> values;

    final UnifyContext currentVars;

    final Iterator<Integer> iter;

    public ListMultiResult(UnifyContext currentVars, Var<?> theVar, List<Integer> values) {
        this.var = theVar;
        this.values = values;
        this.currentVars = currentVars;
        this.iter = this.values.iterator();
    }


    public ListMultiResult(UnifyContext currentVars, MultiResult multiLHS, MultiResult multiRHS) {
        if (! (multiLHS instanceof ListMultiResult)) {
            throw new UnsupportedOperationException("Left argument must be instanceof ListMultiResult");
        }
        if (! (multiRHS instanceof ListMultiResult)) {
            throw new UnsupportedOperationException("Right argument must be instanceof ListMultiResult");
        }
        final ListMultiResult left = (ListMultiResult)multiLHS;
        final ListMultiResult right = (ListMultiResult)multiRHS;
        if (left.var != right.var) {
            throw new UnsupportedOperationException("Must have same var to combine");
        }
        this.var = left.var;
        this.values = new ArrayList<Integer>(left.values);
        this.values.retainAll(right.values);
        this.currentVars = currentVars;
        this.iter = this.values.iterator();
    }


    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public UnifyContext next() {
        final Integer next = iter.next();
        final UnifyContext after = currentVars.unify(var, next);
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
