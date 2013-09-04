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

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;
import org.logic2j.core.impl.theory.DefaultTheoryManager;
import org.logic2j.core.impl.theory.TheoryContent;
import org.logic2j.core.impl.theory.TheoryManager;

public class TheoryTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TheoryTest.class);

    /**
     * Use a bare {@link PrologImplementation} for testing the {@link TheoryManager} and how theories are loaded.
     */
    @Override
    protected InitLevel initLevel() {
        return InitLevel.L0_BARE;
    }

    /**
     * This is making sure all test theories are loadable (no syntax issue, etc).
     */
    @Test
    public void loadAllTestTheories() throws IOException {
        final File[] allTheoryFilesFromTestResourceDir = allTheoryFilesFromTestResourceDir();
        for (final File theory : allTheoryFilesFromTestResourceDir) {
            final TheoryManager theoryManager = new DefaultTheoryManager(this.prolog);
            final TheoryContent content = theoryManager.load(theory);
            logger.info("Loaded library: {}", content);
        }
    }

}
