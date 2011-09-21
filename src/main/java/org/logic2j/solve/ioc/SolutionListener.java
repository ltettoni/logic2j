package org.logic2j.solve.ioc;

import org.logic2j.Prolog;
import org.logic2j.solve.SolutionHolder;

/**
 * The core, lowest-level method by which the inference engine provides solutions.
 * For easier programming, consider using {@link Prolog#solve(CharSequence)} and the
 * {@link SolutionHolder}.
 * 
 */
public interface SolutionListener {

  /**
   * The inference engine notifies the caller code that a solution 
   * was demonstrated; the real content to the solution must be
   * retrieved from the goal's variables.
   * 
   * @return The caller must return true for the inference engine to
   * continue searching for other solutions, or false
   * to break (user cancellation) the search for other solutions.
   */
  public boolean onSolution();

}
