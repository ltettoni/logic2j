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

package org.logic2j.contrib.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.awt.Rectangle;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.logic2j.contrib.library.pojo.PojoLibrary;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.model.symbol.Struct;

public class DynamicClauseProviderTest extends PrologTestBase {

    private DynamicClauseProvider dynamic;

    @Before
    public void init() {
        dynamic = new DynamicClauseProvider(getProlog());
        getProlog().getTheoryManager().addClauseProvider(dynamic);
        getProlog().getLibraryManager().loadLibrary(new PojoLibrary(getProlog()));
    }

    @After
    public void reset() {
        dynamic.retractAll();
    }

    @Test
    public void assertSingleAtomFact() {
        assertNoSolution("aYetUnknownFact");
        // Assert
        final String theFact = "aYetUnknownFact";
        int index = dynamic.assertFact(theFact); // Note: we use another string!
        assertEquals(0, index);
        assertOneSolution("" + "aYetUnknownFact");
        assertNoSolution("aStillUnknownFact");
        // Retract
        dynamic.retractFactAt(index); // Note: we use another string!
        assertNoSolution("" + "aYetUnknownFact");
        assertNoSolution("aStillUnknownFact");
    }

    @Test
    public void assertStructFact() {
        assertNoSolution("zz(X)");
        // Assert
        Struct fact1 = new Struct("zz", 1);
        int index1 = dynamic.assertFact(fact1);
        assertEquals(0, index1);
        assertEquals(1, assertOneSolution("zz(X)").binding("X"));
        // Assert
        Struct fact2 = new Struct("zz", 2);
        int index2 = dynamic.assertFact(fact2);
        assertEquals(1, index2);
        assertEquals(Arrays.asList(new Integer[] { 1, 2 }), assertNSolutions(2, "zz(X)").binding("X"));
        // Retract
        dynamic.retractFactAt(index1);
        assertEquals(2, assertOneSolution("zz(X)").binding("X"));
        dynamic.retractFactAt(index2);
        assertNoSolution("zz(X)");
    }

    @Test
    public void assertObjectFact() {
        assertNoSolution("zz(X)");
        // Assert
        Object awtRectangle = new Rectangle(1, 2, 3, 4);
        Struct fact1 = new Struct("eav", "context", "shape", awtRectangle);
        int index1 = dynamic.assertFact(fact1);
        assertEquals(0, index1);
        assertSame(awtRectangle, assertOneSolution("eav(_, _, X)").binding("X"));
        assertEquals(new Rectangle(1, 2, 3, 4), assertOneSolution("eav(_,_,X)").binding("X"));
        // Retract
        dynamic.retractFactAt(index1);
        assertNoSolution("eav(_,_,X)");
    }

    @Test
    public void assertObjectFactProperties() {
        // Assert
        Object awtRectangle = new Rectangle(1, 2, 3, 4);
        Struct fact1 = new Struct("eav", "context", "shape", awtRectangle);
        dynamic.assertFact(fact1);
        assertEquals(new Rectangle(1, 2, 3, 4), assertOneSolution("eav(_,_,R)").binding("R"));
        assertEquals(3.0, assertOneSolution("eav(_,_,R), property(R, width, W)").binding("W"));
        assertOneSolution("eav(_,_,R), property(R, height, 4.0)");
        assertNoSolution("eav(_,_,R), property(R, height, 3.0)");
    }
}
