package org.logic2j.theory;

import java.io.File;
import java.io.IOException;

import org.logic2j.ClauseProvider;
import org.logic2j.model.prim.PLibrary;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.solve.GoalSolver;

/**
 * An API to manage theories (lists of Prolog facts and clauses expressed as text
 * and parsed into {@link Term}s).
 * The {@link TheoryManager} is an implementation of a {@link ClauseProvider} since it
 * provides sequences of clauses to the {@link GoalSolver} inference engine.
 *
 */
public interface TheoryManager extends ClauseProvider {

  //---------------------------------------------------------------------------
  // Load Theories from various sources into a TheoryContent representation
  //---------------------------------------------------------------------------

  public abstract TheoryContent load(PLibrary theLibrary);

  public abstract TheoryContent load(File theFile) throws IOException;

  //---------------------------------------------------------------------------
  // Alter the current Prolog instance with content from theories
  //---------------------------------------------------------------------------

  /**
   * @param theContent to set - will replace any previously defined content.
   */
  public abstract void setTheory(TheoryContent theContent);

  /**
   * @param theContent to add
   */
  public abstract void addTheory(TheoryContent theContent);

  public abstract void assertZ(Struct theClause, boolean theB, String theName, boolean theB2);

  //  //---------------------------------------------------------------------------
  //  // Access content of theories currently loaded
  //  //---------------------------------------------------------------------------
  //
  //  public abstract Iterable<Clause> listMatchingClauses(Struct theGoalTerm);

}
