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

import junit.framework.TestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OptionsStringTest {


    @Test
    public void testConstructor() throws Exception {
        new OptionsString("");
        new OptionsString("  ");
        new OptionsString(null);
    }


    @Test
    public void testHasOption() throws Exception {
        final OptionsString optionsString = new OptionsString("a, b,  c,");
        assertTrue(optionsString.hasOption("a"));
        assertTrue(optionsString.hasOption("b"));
        assertTrue(optionsString.hasOption("c"));
        assertFalse(optionsString.hasOption("d"));
    }


    @Test
    public void testAssertValidOptions() throws Exception {
        final OptionsString optionsString = new OptionsString("a, b,  c,");
        optionsString.assertValidOptions(new String[]{"a", "b", "c", "d"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssertValidOptions_noB() throws Exception {
        final OptionsString optionsString = new OptionsString("a, b,  c,");
        optionsString.assertValidOptions(new String[]{"a", "c", "d"});
    }
}