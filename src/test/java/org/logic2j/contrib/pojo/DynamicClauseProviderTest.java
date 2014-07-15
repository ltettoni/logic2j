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
        // Make sure the next assertion would return an index of zero
        final int index = this.dynamic.assertClause("one");
        assertEquals(0, index);
        this.dynamic.retractAll();
    }

    @Test
    public void assertSingleAtomFact() {
        noSolutions("aYetUnknownFact");
        // Assert
        final Object theFact = "aYetUnknownFact";
        final int index = this.dynamic.assertClause(theFact); // Note: we use another string!
        assertEquals(0, index);
        uniqueSolution("" + "aYetUnknownFact"); // Add empty string to make another reference
        noSolutions("aStillUnknownFact");
        // Retract
        this.dynamic.retractFactAt(index); // Note: each time we use another string!
        noSolutions("" + "aYetUnknownFact");
        noSolutions("aStillUnknownFact");
    }

    @Test
    public void assertStructFact() {
        noSolutions("zz(X)");
        // Assert
        final Struct fact1 = new Struct("zz", 11);
        final int index1 = this.dynamic.assertClause(fact1);
        assertEquals(0, index1);
        assertEquals(11, uniqueSolution("zz(Q)").var("Q").unique());
        // Assert
        final Struct fact2 = new Struct("zz", 22);
        final int index2 = this.dynamic.assertClause(fact2);
        assertEquals(1, index2);
        assertEquals(Arrays.asList(new Integer[] { 11, 22 }), nSolutions(2, "zz(Q)").var("Q").list());
        // Retract
        this.dynamic.retractFactAt(index1);
        assertEquals(22, uniqueSolution("zz(Q)").var("Q").unique());
        this.dynamic.retractFactAt(index2);
        noSolutions("zz(X)");
    }

    @Test
    public void assertObjectFact() {
        noSolutions("zz(X)");
        // Assert
        final Object awtRectangle = new Rectangle(1, 2, 3, 4);
        final Struct fact1 = new Struct("eav", "context", "shape", awtRectangle);
        final int index1 = this.dynamic.assertClause(fact1);
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
        this.dynamic.assertClause(fact1);
        assertEquals(new Rectangle(1, 2, 3, 4), uniqueSolution("eav(_,_,R)").var("R").unique());
        assertEquals(3.0, uniqueSolution("eav(_,_,R), property(R, width, W)").var("W").unique());
        uniqueSolution("eav(_,_,R), property(R, height, 4.0)");
        noSolutions("eav(_,_,R), property(R, height, 3.0)");
    }

    @Test
    public void retractToIndex() throws Exception {
        noSolutions("iter");
        // Assert first range
        for (int i=0; i<50; i++) {
            this.dynamic.assertClause("iter");
        }
        nSolutions(50, "iter");
        final int middleIndex = this.dynamic.assertClause("iter");
        nSolutions(51, "iter");
        // Assert second range
        for (int i=0; i<50; i++) {
            this.dynamic.assertClause("iter");
        }
        nSolutions(101, "iter");
        // Retract to middle
        this.dynamic.retractToBeforeIndex(middleIndex);
        nSolutions(50, "iter");
        this.dynamic.retractToBeforeIndex(5);
        nSolutions(5, "iter");
        this.dynamic.retractToBeforeIndex(0);
        noSolutions("iter");
    }
}
