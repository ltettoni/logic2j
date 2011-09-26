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
package org.logic2j.library.impl.test;

import org.logic2j.PrologImplementor;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.symbol.TLong;
import org.logic2j.model.symbol.TNumber;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;

/**
 */
public class AdHocLibraryForTesting extends LibraryBase {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdHocLibraryForTesting.class);

  public AdHocLibraryForTesting(PrologImplementor theProlog) {
    super(theProlog);
  }

  @Primitive
  public void int_range(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term theLowerBound,
      Term theIterable, Term theUpperBound) {
    final long lower = resolve(theLowerBound, vars, TNumber.class).longValue();
    final long upper = resolve(theUpperBound, vars, TNumber.class).longValue();
    for (long iter = lower; iter <= upper; iter++) {
      final TLong iterTerm = new TLong(iter);
      final boolean unified = unify(theIterable, vars, iterTerm, vars, theGoalFrame);
      notifyIfUnified(unified, theGoalFrame, theListener);
    }
  }
}
