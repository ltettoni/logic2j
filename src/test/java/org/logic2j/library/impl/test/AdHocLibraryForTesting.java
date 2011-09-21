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
