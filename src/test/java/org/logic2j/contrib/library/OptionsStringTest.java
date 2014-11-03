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