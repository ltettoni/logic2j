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
package org.logic2j.core.impl;

import org.logic2j.core.api.*;
import org.logic2j.engine.solver.Solver;

/**
 * An interface that Prolog implementations must provide; this goes beyond the lighter facade interface {@link Prolog} intended for client
 * use. This one exposes accessors to the internal state of the effective implementation.
 */
public interface PrologImplementation extends Prolog {

  // ---------------------------------------------------------------------------
  // Accessors to the sub-features of the Prolog engine
  // ---------------------------------------------------------------------------

  /**
   * @return The implementation for managing libraries.
   */
  LibraryManager getLibraryManager();

  /**
   * @return The implementation of inference logic.
   */
  Solver getSolver();

  /**
   * @return The implementation for managing operators.
   */
  OperatorManager getOperatorManager();

  /**
   * @return Marshalling
   */
  TermMarshaller getTermMarshaller();

  TermUnmarshaller getTermUnmarshaller();

  void setTermAdapter(TermAdapter termAdapter);

}
