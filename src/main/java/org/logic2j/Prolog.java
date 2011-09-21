package org.logic2j;

import org.logic2j.model.symbol.Term;
import org.logic2j.solve.SolutionHolder;
import org.logic2j.theory.TheoryManager;

/**
 * Interface for using Prolog from an application perspective.
 * TODO See if we can minimize the interface (only add if absolutely required, otherwise add to the PrologImplementor)
 * 
 */
public interface Prolog {

  /**
   * A shortcut method to create a {@link Term} by delegating instantiation to the current {@link TermFactory}.
   * @param theSource Any instance of {@link Object} that may be converted to a {@link Term}.
   * @return A valid {@link Term}, ready for unification or inference within the current {@link Prolog} engine.
   */
  public abstract Term term(Object theSource);

  /**
   * The entry point for solving a goal (this is the higer-level API, internal solving uses
   * a listener).
   * @param theGoal
   * @return A {@link SolutionHolder} that will allow the caller code to dereference 
   * solution(s) and their bindings (values of variables).
   */
  public abstract SolutionHolder solve(CharSequence theGoal);

  public abstract TermFactory getTermFactory();

  /**
   * Needed in order to manage the theories loaded into the engine.
   * @return The {@link TheoryManager} currently registered.
   */
  public abstract TheoryManager getTheoryManager();
}
