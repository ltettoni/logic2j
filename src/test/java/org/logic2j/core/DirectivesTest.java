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
package org.logic2j.core;

import org.junit.Test;
import org.logic2j.contrib.library.pojo.PojoLibrary;
import org.logic2j.core.api.LibraryManager;
import org.logic2j.core.impl.PrologReferenceImplementation.InitLevel;

import static org.junit.Assert.assertEquals;

/**
 * Test parsing and formatting.
 */
public class DirectivesTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DirectivesTest.class);

    /**
     * No need for special init for only testing parsing and formatting.
     */
    @Override
    protected InitLevel initLevel() {
        return InitLevel.L2_BASE_LIBRARIES;
    }

    @Test
    public void load() {
        getProlog().getLibraryManager().loadLibrary(new PojoLibrary(getProlog()));

        countNoSolution("directiveFileLoaded");
        loadTheoryFromTestResourcesDir("directives-set-vars.pro");
        countOneSolution("directiveFileLoaded");
    }

}
