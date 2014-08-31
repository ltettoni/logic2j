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

import org.junit.Assert;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.impl.EnvManager;


public class PojoLibraryTest extends PrologTestBase {

    @Test
    public void bind() {
        loadLibrary(new PojoLibrary(this.prolog));
        uniqueSolution("bind('thread.name', X)");
        bind("name", "value");
        //
        uniqueSolution("bind('thread.name', X), X=value");
        noSolutions("bind('thread.name', X), X=some_other");
        noSolutions("bind('thread.name', a)");
        noSolutions("bind('thread.name', name)");
        uniqueSolution("bind('thread.name', value)");
    }

    /**
     * Helper method for PojoLibrary-related test cases: bind a Java object by name.
     *
     * @param theKey
     * @param theValue
     */
    protected void bind(String theKey, Object theValue) {
        EnvManager.setThreadVariable(theKey, theValue);
    }

    // Testing of the "property" predicate is done where we can assert objects into the theory,
    // this is in DynamicClauseProviderTest


    @Test
    public void javaNew() throws Exception {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('org.logic2j.core.api.TermAdapter$FactoryMode', 'ANY_TERM')").var("X").single();
        Assert.assertSame(TermAdapter.FactoryMode.ANY_TERM, x);
    }
}
