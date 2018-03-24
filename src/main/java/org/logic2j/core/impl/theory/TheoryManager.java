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
package org.logic2j.core.impl.theory;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.DataFactProvider;
import org.logic2j.core.api.model.Clause;
import org.logic2j.engine.solver.Solver;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * The API to manage theories (lists of Prolog {@link Clause}s (facts or rules) expressed as text.
 * The {@link TheoryManager} also implements
 * {@link ClauseProvider} since it provides sequences of {@link Clause}s to
 * the {@link Solver} inference engine.
 * Provides methods for:
 * <ul>
 * <li>Loading theory files, classloadable resources or URLs</li>
 * <li>(future)Asserting and retracting {@link Clause}s</li>
 * </ul>
 */
public interface TheoryManager extends ClauseProvider {

  // ---------------------------------------------------------------------------
  // Load Theories from various sources into a TheoryContent representation
  // ---------------------------------------------------------------------------

  /**
   * Load the {@link TheoryContent} from a File defining a theory;
   * this only loads and return the content, use {@link #addTheory(TheoryContent)} to make it
   * available to the {@link org.logic2j.core.impl.PrologImplementation}.
   *
   * @param theFile
   * @return The content of the theory
   * @throws java.io.IOException
   */
  TheoryContent load(File theFile) throws IOException;

  /**
   * Load from a URL.
   *
   * @param theTheory
   * @return The content of the theory
   */
  TheoryContent load(URL theTheory);

  /**
   * Load from a classloadable resource.
   *
   * @param theClassloadableResourceOrUrl
   * @return The content of the theory
   */
  TheoryContent load(String theClassloadableResourceOrUrl);

  /**
   * @return All clause providers, in same order as when registered.
   * TODO The actual ordering of ClauseProviders may not always be required: it is only
   * important when the same predicate is available from several providers (rare). Could we in certain cases use
   * multi-threaded access to all clause providers?
   */
  Iterable<ClauseProvider> getClauseProviders();

  boolean hasDataFactProviders();

  Iterable<DataFactProvider> getDataFactProviders();

  /**
   * @param theNewProvider
   */
  void addClauseProvider(ClauseProvider theNewProvider);

  /**
   * @param theNewProvider
   */
  void addDataFactProvider(DataFactProvider theNewProvider);

  // ---------------------------------------------------------------------------
  // Alter the current Prolog instance with content from theories
  // ---------------------------------------------------------------------------

  /**
   * @param theContent To be merged into this and made available to the {@link org.logic2j.core.impl.PrologImplementation}.
   */
  void addTheory(TheoryContent theContent);

}
