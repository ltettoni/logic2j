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
package org.logic2j.core.impl.theory;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.DataFactProvider;
import org.logic2j.core.api.Solver;
import org.logic2j.core.api.model.Clause;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * The API to manage theories (lists of Prolog {@link Clause}s (facts or rules) expressed as text. The {@link TheoryManager} also implements
 * {@link ClauseProvider} since it provides sequences of {@link Clause}s to the {@link Solver} inference engine.
 * Provides methods for:
 * <ul>
 * <li>Loading theory files or resources</li>
 * <li>(future)Assert and retracting {@link Clause}s</li>
 * </ul>
 */
public interface TheoryManager extends ClauseProvider {

    // ---------------------------------------------------------------------------
    // Load Theories from various sources into a TheoryContent representation
    // ---------------------------------------------------------------------------

    /**
     * Convenience method to load the {@link TheoryContent} from a File defining a theory; this only loads and return the content, use
     * {@link #addTheory(TheoryContent)} to make it available to the {@link org.logic2j.core.impl.PrologImplementation}.
     *
     * @param theFile
     * @return The content of the theory from theFile.
     * @throws java.io.IOException
     */
    TheoryContent load(File theFile) throws IOException;

    /**
     * @param theTheory
     * @return The content of the theory
     */
    TheoryContent load(URL theTheory);

    TheoryContent load(String theClassloadableResourceOrUrl);

    /**
     * @return All clause providers, in same order as when registered.
     *         TODO The actual ordering of ClauseProviders may not always be required: it is only
     *         important when the same predicate is available from several providers (rare). Could we in certain cases use
     *         multi-threaded access to all clause providers?
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
