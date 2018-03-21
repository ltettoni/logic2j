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

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.engine.exception.PrologNonSpecificError;
import org.logic2j.core.impl.EnvManager;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


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
    public void javaNewForEnum() {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('org.logic2j.core.api.TermAdapter$FactoryMode', 'ANY_TERM')").var("X").single();
        assertThat(x).isEqualTo(TermAdapter.FactoryMode.ANY_TERM);
    }

    @Test
    public void javaNewWithDefaultConstructor() {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('java.util.ArrayList')").var("X").single();
        assertThat(x instanceof ArrayList<?>).isTrue();
    }


    @Test
    public void javaNewWithConstructorAndArgs() {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('java.lang.String', 'text')").var("X").single();
        assertThat(x).isEqualTo("text");
    }


    @Test(expected = PrologNonSpecificError.class)
    public void javaNewWithConstructorAndArgsFails() {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('java.lang.String', 'arg', 'extraArg')").var("X").single();
    }

    @Test
    public void javaInstantiateWithConstructorArgs() {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('org.logic2j.contrib.library.pojo.PojoLibraryTest$PrologInstantiatedPojo', 'toto', 1, 2.3)").var("X").single();
        assertThat(x instanceof PrologInstantiatedPojo).isTrue();
        final PrologInstantiatedPojo pojo = (PrologInstantiatedPojo) x;
        assertThat(pojo.getStr()).isEqualTo("toto");
        assertThat(pojo.getAnInt()).isEqualTo(new Integer(1));
        assertThat(pojo.getaDouble()).isEqualTo(new Double(2.3));
    }

    @Test
    public void javaInstantiateWithEmptyConstructorAndInjectProperties() {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('org.logic2j.contrib.library.pojo.PojoLibraryTest$PrologInstantiatedPojo'), " +
        "property(X, 'str', 'toto', 'w'), property(X, 'anInt', 1, 'w'), property(X, 'aDouble', 2.3, 'w')").var("X").single();
        assertThat(x instanceof PrologInstantiatedPojo).isTrue();
        final PrologInstantiatedPojo pojo = (PrologInstantiatedPojo) x;
        assertThat(pojo.getStr()).isEqualTo("toto");
        assertThat(pojo.getAnInt()).isEqualTo(new Integer(1));
        assertThat(pojo.getaDouble()).isEqualTo(new Double(2.3));
    }


    @Test
    public void javaInstantiateWithJavaUnify() {
        loadLibrary(new PojoLibrary(this.prolog));
        final Object x = uniqueSolution("X is javaNew('org.logic2j.contrib.library.pojo.PojoLibraryTest$PrologInstantiatedPojo'), " +
        "javaUnify(X, [str=toto, anInt=1, aDouble=2.3])").var("X").single();
        assertThat(x instanceof PrologInstantiatedPojo).isTrue();
        final PrologInstantiatedPojo pojo = (PrologInstantiatedPojo) x;
        assertThat(pojo.getStr()).isEqualTo("toto");
        assertThat(pojo.getAnInt()).isEqualTo(new Integer(1));
        assertThat(pojo.getaDouble()).isEqualTo(new Double(2.3));
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
