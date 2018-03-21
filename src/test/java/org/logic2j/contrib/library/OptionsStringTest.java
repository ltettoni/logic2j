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
package org.logic2j.contrib.library;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionsStringTest {


    @Test
    public void testConstructor() {
        new OptionsString("");
        new OptionsString("  ");
        new OptionsString(null);
    }


    @Test
    public void testHasOption() {
        final OptionsString optionsString = new OptionsString("a, b,  c,");
        assertThat(optionsString.hasOption("a")).isTrue();
        assertThat(optionsString.hasOption("b")).isTrue();
        assertThat(optionsString.hasOption("c")).isTrue();
        assertThat(optionsString.hasOption("d")).isFalse();
    }


    @Test
    public void testAssertValidOptions() {
        final OptionsString optionsString = new OptionsString("a, b,  c,");
        optionsString.assertValidOptions(new String[]{"a", "b", "c", "d"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssertValidOptions_noB() {
        final OptionsString optionsString = new OptionsString("a, b,  c,");
        optionsString.assertValidOptions(new String[]{"a", "c", "d"});
    }
}