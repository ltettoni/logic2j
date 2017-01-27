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

package org.logic2j.core.library.impl;

import org.junit.Test;
import org.logic2j.contrib.helper.FluentPrologBuilder;
import org.logic2j.core.api.Prolog;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Demonstrate simple use cases for Logic2j.
 */
public class UseCaseTest {

    @Test
    public void instantiateViaFactory() throws Exception {
        final Prolog prolog = new FluentPrologBuilder().build();
        assertNotNull(prolog);
    }


    @Test
    public void configureDefaultLibs() throws Exception {
        assertFalse(new FluentPrologBuilder().withoutLibraries(false).isNoLibraries());
        assertTrue(new FluentPrologBuilder().withoutLibraries(true).isNoLibraries());
        assertFalse(new FluentPrologBuilder().withCoreLibraries(false).isCoreLibraries());
        assertTrue(new FluentPrologBuilder().withCoreLibraries(true).isCoreLibraries());
    }

    @Test
    public void obtainDefaultPrologAndSolve() throws Exception {
        final Prolog prolog = new FluentPrologBuilder().build();
        assertEquals(4, prolog.solve("member(X, [a,b,c,d])").count());
    }


    @Test
    public void loadTheoryAndSolve() throws Exception {
        final File th1 = new File("src/test/resources/queens.pro");
        final File th2 = new File("src/test/resources/hanoi.pro");
        final Prolog prolog = new FluentPrologBuilder().withTheory(th1, th2).build();
        assertEquals(2, prolog.solve("queens(4, _)").count());
    }


    @Test
    public void solve() throws Exception {
        final Prolog prolog = new FluentPrologBuilder()
        .withTheory(new File("src/test/resources/queens.pro"))
        .build();
        final List<List> objectList = prolog.solve("queens(4, Q)").var("Q", List.class).list();
        assertEquals(2, objectList.size());
        assertEquals("[3, 1, 4, 2]", objectList.get(0).toString());
        assertEquals("[2, 4, 1, 3]", objectList.get(1).toString());
    }
}
