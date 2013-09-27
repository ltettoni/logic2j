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
package org.logic2j.core.functional;

import static org.junit.Assert.assertEquals;

import java.awt.Button;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.Prolog;
import org.logic2j.core.api.Solver;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.solver.holder.MultipleSolutionsHolder;

/**
 * Test how Java objects can invoke and be returned from logic2j's {@link Prolog} and {@link Solver}.
 */
public class AdapterTest extends PrologTestBase {
    // private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdapterTest.class);

    public static class MyOutputObject {

    }

    public static enum MyEnum {
        V1, V2, V3
    }

    public static abstract class PrologAdapter<In, Out> {
        public abstract Iterable<Out> solve(In in);
    }

    @Ignore("Under design")
    @Test
    public void invokeWithJavaObjects() throws Exception {
        // Just convert a vararg of Object... to a Struct, and that's OK
        this.prolog.solve(invocation("goal", 123, "str", 3.14, MyEnum.V1, new Object(), new Button()));
        // ^^^static : en tout cas pas.
    }

    @Ignore("Under design")
    @Test
    public void testRetrieveJavaObjects() throws Exception {
        this.prolog.solve("X is 2+3").unique().binding("X", Integer.class);
    }

    /**
     * @param dataSetName
     * @param args
     * @return
     */
    private Term invocation(String predicateName, Object... args) {
        return Struct.valueOf(predicateName, Arrays.asList(args));
    }

    @Test
    public void plainDouble() throws Exception {
        loadTheoryFromTestResourcesDir("test-functional.pl");
        //
        MultipleSolutionsHolder solutions;
        solutions = this.prolog.solve("dbl(X)").all();
        List<Object> binding = solutions.binding("X");
        assertEquals("[1.1, 1.2, 1.3]", binding.toString());
    }
}
