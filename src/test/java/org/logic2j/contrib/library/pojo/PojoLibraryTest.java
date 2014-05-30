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
package org.logic2j.contrib.library.pojo;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;

public class PojoLibraryTest extends PrologTestBase {

    @Test
    public void bind() {
        loadLibrary(new PojoLibrary(this.prolog));
        bind("name", "value");
        //
        uniqueSolution("bind(name, X), X=value");
        noSolutions("bind(name, X), X=some_other");
        noSolutions("bind(name, a)");
        noSolutions("bind(name, name)");
        uniqueSolution("bind(name, value)");
    }

    /**
     * Helper method for PojoLibrary-related test cases: bind a Java object by name.
     * 
     * @param theKey
     * @param theValue
     */
    protected void bind(String theKey, Object theValue) {
        PojoLibrary.bind(theKey, theValue);
    }

}
