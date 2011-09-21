package org.logic2j;

import org.logic2j.model.Clause;
import org.logic2j.model.symbol.Struct;
import org.logic2j.solve.GoalSolver;

/**
 * Provide clauses from various sources to the {@link GoalSolver} inference engine.
 * The most typical implementation is that clauses are parsed and normalized 
 * from one or several theories' textual content. 
 * Yet other implementations are possible.
 * 
 */
public interface ClauseProvider {

  /**
   * List clauses (facts or rules) potentially matching the specified goal.
   * @param theGoal
   * @return An ordered list of {@link Clause}s that are candidates for unifying with theGoal.
   */
  public Iterable<Clause> listMatchingClauses(Struct theGoal);

}
