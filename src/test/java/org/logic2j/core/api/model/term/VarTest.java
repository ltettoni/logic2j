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

package org.logic2j.core.api.model.term;

import org.junit.Test;
import org.logic2j.core.api.model.exception.InvalidTermException;

import static org.junit.Assert.*;

public class VarTest {

    @Test
    public void constructorValid() throws Exception {
        final Var<?> v1 = new Var<Object>("X");
        assertSame("X", v1.getName());
        assertEquals(Term.NO_INDEX, v1.getIndex());
    }

    @Test(expected = InvalidTermException.class)
    public void constructorNull() throws Exception {
        new Var<Object>((String) null);
    }

    @Test(expected = InvalidTermException.class)
    public void constructorEmpty() throws Exception {
        new Var<Object>("");
    }

    @Test(expected = InvalidTermException.class)
    public void constructorCannotInstantiateAnonymous() throws Exception {
        new Var<Object>("_");
    }


    @Test
    public void constructorWithCharSequence() throws Exception {
        final Var<?> v1 = new Var<Object>(new StringBuilder("X"));
        assertSame("X", v1.getName());
        assertEquals(Term.NO_INDEX, v1.getIndex());
    }


    @Test
    public void idempotence() throws Exception {
        final Var<?> v1 = new Var<Object>("X");
        assertEquals(v1, v1);
    }


    @Test
    public void equality() throws Exception {
        final Var<?> v1 = new Var<Object>("X");
        final Var<?> v2 = new Var<Object>("X");
        assertNotSame(v1, v2);
        assertEquals(v1, v2);
        assertEquals(v2, v1);
    }


    @Test
    public void lowerCaseIsValid() throws Exception {
        final Var<?> v1 = new Var<Object>("lowercase");
        assertSame("lowercase", v1.getName());
        assertEquals(Term.NO_INDEX, v1.getIndex());
    }


    @Test(expected = InvalidTermException.class)
    public void cannotCloneAnonymous() throws Exception {
        Var.copy(Var.ANONYMOUS_VAR);
    }

    @Test
    public void isAnonymousTrue() throws Exception {
        assertTrue(Var.ANONYMOUS_VAR.isAnonymous());
    }

    @Test
    public void isAnonymousFalse() throws Exception {
        assertFalse(new Var<Object>("X").isAnonymous());
    }




}
