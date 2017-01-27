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
package org.logic2j.core;

import org.junit.Test;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.term.Struct;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Testing the TermAdapter: how Java objects can invoke and be returned
 * from logic2j's {@link org.logic2j.core.api.Prolog} and {@link org.logic2j.core.impl.Solver}.
 */
public class AdapterTest extends PrologTestBase {
    // private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdapterTest.class);

    public static class MyOutputObject {
        // Empty
    }

    public static enum MyEnum {
        V1, V2, V3
    }

    public static abstract class PrologAdapter<In, Out> {
        public abstract Iterable<Out> solve(In in);
    }

    @Test
    public void retrieveJavaObjects() {
        assertEquals(5, this.prolog.solve("X is 2+3").var("X", Integer.class).unique().intValue());
    }

    @Test
    public void plainDouble() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        //
        final List<Object> binding = this.prolog.solve("dbl(X)").var("X").list();
        assertEquals("[1.1, 1.2, 1.3]", binding.toString());
    }

    @Test
    public void javaEnum() {
        final Struct term = this.prolog.getTermAdapter().toStruct("=", TermAdapter.FactoryMode.ANY_TERM, "X", MyEnum.V2);
        final Object binding = this.prolog.solve(term).var("X").unique();
        assertSame(MyEnum.V2, binding);
    }
}
