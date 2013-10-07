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

package org.logic2j.contrib.library.fnct;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;

public class FunctionLibraryTest extends PrologTestBase {

    @Test
    public void mapBottomUp() {
        loadLibrary(new FunctionLibrary(this.prolog));
        loadTheoryFromTestResourcesDir("mapping.pl");
        //
        // Mapped struct
        assertEquals("f(one, 2)", assertOneSolution("mapBottomUp(map, f(1,2), X)").binding("X").toString());

        //
        // Free vars and anonymous should not be mapped
        // assertOneSolution("mapBottomUp(map, _, anything)");
        // assertOneSolution("mapBottomUp(map, Free, Free)");

        //
        // Mapped atoms
        // assertOneSolution("mapBottomUp(map, 1, one)");
        // assertNoSolution("mapBottomUp(map, 1, other)");
        // assertEquals("one", assertOneSolution("IN=1, mapBottomUp(map, IN, X)").binding("X"));
        //
        // Unmapped atoms
        // assertOneSolution("mapBottomUp(map, 2, 2)");
        // assertNoSolution("mapBottomUp(map, 2, other)");
        // assertEquals(2L, assertOneSolution("IN=2, mapBottomUp(map, IN, X)").binding("X"));
    }
}
