package org.logic2j.unify;

import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.GoalFrame;

/**
 * Service to unify terms together. Various implementations possible.
 *
 */
public interface Unifyer {

  public boolean unify(Term term1, VarBindings vars1, Term term2, VarBindings vars2, GoalFrame theGoalFrame);

  public void deunify(GoalFrame theGoalFrame);

}
