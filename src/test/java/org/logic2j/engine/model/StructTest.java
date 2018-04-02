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

package org.logic2j.engine.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StructTest {

    @Test
    public void struct0() {
        Struct a1 = new Struct("f");
        assertThat(a1.getArity()).isEqualTo(0);
        assertThat(a1.getName()).isEqualTo("f");
        Struct a2 = new Struct("f");
        assertThat(a2).isNotSameAs(a1);
        assertThat(a2).isEqualTo(a1);
    }

    @Test
    public void atomAsString() {
        Object a1 = Struct.atom("f");
        assertThat(a1 instanceof String).isTrue();
        assertThat(a1).isEqualTo("f");
        Object a2 = Struct.atom("f");
        assertThat(a2).isEqualTo(a1);
    }


    @Test
    public void atomAsStruct() {
        Object a1 = Struct.atom("true");
        assertThat(a1 instanceof Struct).isTrue();
        assertThat(((Struct) a1).getName()).isEqualTo("true");
        Object a2 = Struct.atom("true");
        assertThat(a2).isNotSameAs(a1);
    }

    @Test
    public void struct2() {
        Struct a1 = new Struct("f", "a", "b");
        assertThat(a1.getArity()).isEqualTo(2);
        assertThat(a1.getName()).isEqualTo("f");
        assertThat(a1.getArg(0)).isEqualTo("a");
        assertThat(a1.getArg(1)).isEqualTo("b");
        Struct a2 = new Struct("f", "a", "b");
        assertThat(a2).isNotSameAs(a1);
        assertThat(a2).isEqualTo(a1);
        assertThat(new Struct("f", "b", "a")).isNotEqualTo(a1);
    }

}