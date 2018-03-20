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
package org.logic2j.contrib.library.pojo;

import org.junit.Assert;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.impl.EnvManager;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;


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
    public void javaNewForEnum() throws Exception {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('org.logic2j.core.api.TermAdapter$FactoryMode', 'ANY_TERM')").var("X").single();
        Assert.assertSame(TermAdapter.FactoryMode.ANY_TERM, x);
    }

    @Test
    public void javaNewWithDefaultConstructor() throws Exception {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('java.util.ArrayList')").var("X").single();
        Assert.assertTrue(x instanceof ArrayList<?>);
    }


    @Test
    public void javaNewWithConstructorAndArgs() throws Exception {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('java.lang.String', 'text')").var("X").single();
        assertEquals("text", x);
    }


    @Test(expected = PrologNonSpecificError.class)
    public void javaNewWithConstructorAndArgsFails() throws Exception {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('java.lang.String', 'arg', 'extraArg')").var("X").single();
    }

    @Test
    public void javaInstantiateWithConstructorArgs() throws Exception {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('org.logic2j.contrib.library.pojo.PojoLibraryTest$PrologInstantiatedPojo', 'toto', 1, 2.3)").var("X").single();
        Assert.assertTrue(x instanceof PrologInstantiatedPojo);
        final PrologInstantiatedPojo pojo = (PrologInstantiatedPojo) x;
        assertEquals("toto", pojo.getStr());
        assertEquals(new Integer(1), pojo.getAnInt());
        assertEquals(new Double(2.3), pojo.getaDouble());
    }

    @Test
    public void javaInstantiateWithEmptyConstructorAndInjectProperties() throws Exception {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('org.logic2j.contrib.library.pojo.PojoLibraryTest$PrologInstantiatedPojo'), " +
        "property(X, 'str', 'toto', 'w'), property(X, 'anInt', 1, 'w'), property(X, 'aDouble', 2.3, 'w')").var("X").single();
        Assert.assertTrue(x instanceof PrologInstantiatedPojo);
        final PrologInstantiatedPojo pojo = (PrologInstantiatedPojo) x;
        assertEquals("toto", pojo.getStr());
        assertEquals(new Integer(1), pojo.getAnInt());
        assertEquals(new Double(2.3), pojo.getaDouble());
    }


    @Test
    public void javaInstantiateWithJavaUnify() throws Exception {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('org.logic2j.contrib.library.pojo.PojoLibraryTest$PrologInstantiatedPojo'), " +
        "javaUnify(X, [str=toto, anInt=1, aDouble=2.3])").var("X").single();
        Assert.assertTrue(x instanceof PrologInstantiatedPojo);
        final PrologInstantiatedPojo pojo = (PrologInstantiatedPojo) x;
        assertEquals("toto", pojo.getStr());
        assertEquals(new Integer(1), pojo.getAnInt());
        assertEquals(new Double(2.3), pojo.getaDouble());
    }


    // ---------------------------------------------------------------------------
    // A sample Pojo to be injected from Prolog
    // ---------------------------------------------------------------------------

    public static class PrologInstantiatedPojo {
        private String str;
        private Integer anInt;
        private Double aDouble;

        public PrologInstantiatedPojo() {
        }

        public PrologInstantiatedPojo(String str, Integer anInt, Double aDouble) {
            this.str = str;
            this.anInt = anInt;
            this.aDouble = aDouble;
        }

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public Integer getAnInt() {
            return anInt;
        }

        public void setAnInt(Integer anInt) {
            this.anInt = anInt;
        }

        public Double getaDouble() {
            return aDouble;
        }

        public void setaDouble(Double aDouble) {
            this.aDouble = aDouble;
        }
    }

}
