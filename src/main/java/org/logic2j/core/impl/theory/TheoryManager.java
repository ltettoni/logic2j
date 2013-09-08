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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.Solver;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.impl.PrologImplementation;

/**
 * The API to manage theories (lists of Prolog {@link Clause}s (facts or rules) expressed as text. The {@link TheoryManager} also implements
 * {@link ClauseProvider} since it provides sequences of clauses to the {@link Solver} inference engine.
 * Provide methods for:
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
     * {@link #addTheory(TheoryContent)} to make it available to the {@link PrologImplementation}.
     * 
     * @param theFile
     * @return The content of the theory from theFile.
     * @throws IOException
     */
    TheoryContent load(File theFile) throws IOException;

    /**
     * @param theTheory
     * @return
     */
    TheoryContent load(URL theTheory);

    /**
     * @return All clause providers, in same order as when registered.
     *         TODO But the actual ordering may not be always needed, it's only
     *         important when the same predicate is available from several providers (not frequent). Could we in certain cases use
     *         multi-threaded access to all clause providers?
     */
    // TODO See if we can get rid of this and implement proper indexing of Goal -> List<Clause>
    Iterable<ClauseProvider> getClauseProviders();

    /**
     * @param theNewProvider
     */
    void addClauseProvider(ClauseProvider theNewProvider);

    // ---------------------------------------------------------------------------
    // Alter the current Prolog instance with content from theories
    // ---------------------------------------------------------------------------

    /**
     * @param theContent To be merged into this and made available to the {@link PrologImplementation}.
     */
    void addTheory(TheoryContent theContent);

}
