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
package org.logic2j.theory;

import java.io.File;
import java.io.IOException;

import org.logic2j.ClauseProvider;
import org.logic2j.PrologImplementor;
import org.logic2j.library.PLibrary;
import org.logic2j.model.Clause;
import org.logic2j.model.symbol.Struct;
import org.logic2j.solve.GoalSolver;

/**
 * An API to manage theories (lists of Prolog {@link Clause}s (facts or rules) 
 * expressed as text.
 * The {@link TheoryManager} is an implementation of a {@link ClauseProvider} since it
 * also provides sequences of clauses to the {@link GoalSolver} inference engine.
 */
public interface TheoryManager extends ClauseProvider {

  //---------------------------------------------------------------------------
  // Load Theories from various sources into a TheoryContent representation
  //---------------------------------------------------------------------------

  /**
   * Load the Prolog content associated to a {@link PLibrary}.
   * @param theLibrary
   * @return The content of the theory associated to theLibrary
   */
  public abstract TheoryContent load(PLibrary theLibrary);

  /**
   * Convenience method to load a File representing a Theory.
   * @param theFile
   * @return The content of the theory from theFile.
   * @throws IOException
   */
  public abstract TheoryContent load(File theFile) throws IOException;

  //---------------------------------------------------------------------------
  // Alter the current Prolog instance with content from theories
  //---------------------------------------------------------------------------

  /**
   * @param theContent to set - will replace any previously defined content.
   */
  public abstract void setTheory(TheoryContent theContent);

  /**
   * @param theContent To be added to the {@link PrologImplementor} engine associated.
   */
  public abstract void addTheory(TheoryContent theContent);

  public abstract void assertZ(Struct theClause, boolean theB, String theName, boolean theB2);

}
