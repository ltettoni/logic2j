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

import static org.assertj.core.api.Assertions.assertThat;

public class VarTest {

    @Test
    public void constructorValid() throws Exception {
        final Var<?> v1 = new Var<Object>("X");
        assertThat(v1.getName()).isEqualTo("X");
        assertThat(v1.getIndex()).isEqualTo(Term.NO_INDEX);
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
        assertThat(v1.getName()).isEqualTo("X");
        assertThat(v1.getIndex()).isEqualTo(Term.NO_INDEX);
    }


    @Test
    public void idempotence() throws Exception {
        final Var<?> v1 = new Var<Object>("X");
        assertThat(v1).isEqualTo(v1);
    }


    @Test
    public void equality() throws Exception {
        final Var<?> v1 = new Var<Object>("X");
        final Var<?> v2 = new Var<Object>("X");
        assertThat(v2).isNotSameAs(v1);
        assertThat(v2).isEqualTo(v1);
        assertThat(v1).isEqualTo(v2);
    }


    @Test
    public void lowerCaseIsValid() throws Exception {
        final Var<?> v1 = new Var<Object>("lowercase");
        assertThat(v1.getName()).isEqualTo("lowercase");
        assertThat(v1.getIndex()).isEqualTo(Term.NO_INDEX);
    }


    @Test(expected = InvalidTermException.class)
    public void cannotCloneAnonymous() throws Exception {
        Var.copy(Var.ANONYMOUS_VAR);
    }

    @Test
    public void isAnonymousTrue() throws Exception {
        assertThat(Var.ANONYMOUS_VAR.isAnonymous()).isTrue();
    }

    @Test
    public void isAnonymousFalse() throws Exception {
        assertThat(new Var<Object>("X").isAnonymous()).isFalse();
    }




}
