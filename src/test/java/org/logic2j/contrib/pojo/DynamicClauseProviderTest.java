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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.logic2j.contrib.library.pojo.PojoLibrary;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.model.term.Struct;

import java.awt.*;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class DynamicClauseProviderTest extends PrologTestBase {

    private DynamicClauseProvider dynamic;

    @Before
    public void init() {
        this.dynamic = new DynamicClauseProvider(getProlog());
        getProlog().getTheoryManager().addClauseProvider(this.dynamic);
        getProlog().getLibraryManager().loadLibrary(new PojoLibrary(getProlog()));
    }

    @After
    public void retractAll() {
        this.dynamic.retractAll();
    }

    @Test
    public void assertSingleAtomFact() {
        noSolutions("aYetUnknownFact");
        // Assert
        final Struct theFact = new Struct("aYetUnknownFact");
        final int index = this.dynamic.assertFact(theFact); // Note: we use another string!
        assertEquals(0, index);
        uniqueSolution("" + "aYetUnknownFact");
        noSolutions("aStillUnknownFact");
        // Retract
        this.dynamic.retractFactAt(index); // Note: we use another string!
        noSolutions("" + "aYetUnknownFact");
        noSolutions("aStillUnknownFact");
    }

    @Test
    public void assertStructFact() {
        noSolutions("zz(X)");
        // Assert
        final Struct fact1 = new Struct("zz", 1);
        final int index1 = this.dynamic.assertFact(fact1);
        assertEquals(0, index1);
        assertEquals(1, uniqueSolution("zz(X)").var("X").unique());
        // Assert
        final Struct fact2 = new Struct("zz", 2);
        final int index2 = this.dynamic.assertFact(fact2);
        assertEquals(1, index2);
        assertEquals(Arrays.asList(new Integer[] { 1, 2 }), nSolutions(2, "zz(X)").var("X").list());
        // Retract
        this.dynamic.retractFactAt(index1);
        assertEquals(2, uniqueSolution("zz(X)").var("X").unique());
        this.dynamic.retractFactAt(index2);
        noSolutions("zz(X)");
    }

    @Test
    public void assertObjectFact() {
        noSolutions("zz(X)");
        // Assert
        final Object awtRectangle = new Rectangle(1, 2, 3, 4);
        final Struct fact1 = new Struct("eav", "context", "shape", awtRectangle);
        final int index1 = this.dynamic.assertFact(fact1);
        assertEquals(0, index1);
        assertSame(awtRectangle, uniqueSolution("eav(_, _, X)").var("X").unique());
        assertEquals(new Rectangle(1, 2, 3, 4), uniqueSolution("eav(_,_,X)").var("X").unique());
        // Retract
        this.dynamic.retractFactAt(index1);
        noSolutions("eav(_,_,X)");
    }

    @Test
    public void assertObjectFactProperties() {
        // Assert
        final Object awtRectangle = new Rectangle(1, 2, 3, 4);
        final Struct fact1 = new Struct("eav", "context", "shape", awtRectangle);
        this.dynamic.assertFact(fact1);
        assertEquals(new Rectangle(1, 2, 3, 4), uniqueSolution("eav(_,_,R)").var("R").unique());
        assertEquals(3.0, uniqueSolution("eav(_,_,R), property(R, width, W)").var("W").unique());
        uniqueSolution("eav(_,_,R), property(R, height, 4.0)");
        noSolutions("eav(_,_,R), property(R, height, 3.0)");
    }
}
