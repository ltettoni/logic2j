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

package org.logic2j.core.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.model.var.Bindings;

/**
 */
public class FinalVarTermMarshallerTest extends PrologTestBase {

    @Test
    public void xBoundToY() {
        Object term = getProlog().getTermUnmarshaller().unmarshall("f(X, Y)");
        Bindings bindings = new Bindings(term);
        bindings.getBinding((short) 0).bindTo(bindings.getBinding((short) 1).getVar(), bindings);
        CharSequence formatted = new FinalVarTermMarshaller(getProlog(), bindings).marshall(term);
        assertEquals("f(Y, Y)", formatted);
    }

    @Test
    public void yBoundToX() {
        Object term = getProlog().getTermUnmarshaller().unmarshall("f(X, Y)");
        Bindings bindings = new Bindings(term);
        bindings.getBinding((short) 1).bindTo(bindings.getBinding((short) 0).getVar(), bindings);
        CharSequence formatted = new FinalVarTermMarshaller(getProlog(), bindings).marshall(term);
        assertEquals("f(X, X)", formatted);
    }
}
