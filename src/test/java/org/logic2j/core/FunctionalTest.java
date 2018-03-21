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
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.solver.holder.GoalHolder;
import org.logic2j.core.impl.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional tests of the core features.
 */
public class FunctionalTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionalTest.class);

    @Test
    public void placeholderToReproduceError() {
        // Insert problematic test case here
    }

    @Test
    public void rules() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        countNSolutions(3, "a(X)");
        countNSolutions(5, "f(Q)");
        countNSolutions(9, "a(X), b(Y)", "true, a(X), b(Y)", "a(X), b(Y), true", "a(X), true, b(Y)");
        countNSolutions(27, "a(X), b(Y), c(Z)");
    }

    @Test
    public void member() {
        countOneSolution("member(a, [a,b,c])", "member(b, [a,b,c])", "member(c, [a,b,c])");
        countNoSolution("member(d, [a,b,c])");
        logger.info(CollectionUtils.format("All bindings: ", this.prolog.solve("member(X, [a,b,c])").vars().list(), 0));

        assertThat(uniqueSolution("append([1],[2,3],X)").var("X").unique().toString()).isEqualTo("[1,2,3]");

        final GoalHolder all = nSolutions(3, "append(X,Y,[1,2])");
        assertThat(all.var("X").list()).isEqualTo(termList("[]", "[1]", "[1,2]"));
        assertThat(all.var("Y").list()).isEqualTo(termList("[1,2]", "[2]", "[]"));
    }


    @Test
    public void appendWithArraySolutions() {
        final List<Object[]> list = this.prolog.solve("append(X, Y, [a,b,c,d])").varsArray().list();
        logger.info(CollectionUtils.format("All bindings as varsArray(): ", list, 0));
        assertThat(list.size()).isEqualTo(5);
        final Object[] rec0 = list.get(0);
        assertThat(rec0.length).isEqualTo(2);
        assertThat(list.get(1).length).isEqualTo(2);
        assertThat(list.get(2).length).isEqualTo(2);
        assertThat(list.get(3).length).isEqualTo(2);
        assertThat(list.get(4).length).isEqualTo(2);
        final Object rec00 = rec0[0];
        assertThat(rec00.getClass()).isEqualTo(Struct.class);
        assertThat(rec00.toString()).isEqualTo("[]");
        final Object rec01 = rec0[1];
        assertThat(rec01.getClass()).isEqualTo(Struct.class);
        assertThat(rec01.toString()).isEqualTo("[a,b,c,d]");
    }

    @Test
    public void appendWithFactorySolutions() {
        final ObjectFactory<Integer> justLog = new ObjectFactory<Integer>() {
            int counter = 0;
            @Override
            public Integer valueOf(Object[] values) {
                logger.info("ObjectFactory called with values: {}", Arrays.asList(values));
                return counter++;
            }
        };
        final List<Integer> list = this.prolog.solve("append(X, Y, [a,b,c,d])").varsToFactory(justLog).list();
        assertThat(list.size()).isEqualTo(5);
        assertThat(list.toString()).isEqualTo("[0, 1, 2, 3, 4]");
    }

    @Test
    public void sumial() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        assertThat(uniqueSolution("sumial(0, X)").intValue("X")).isEqualTo(term(0));
        assertThat(uniqueSolution("sumial(1, X)").intValue("X")).isEqualTo(term(1));
        assertThat(uniqueSolution("sumial(2, X)").intValue("X")).isEqualTo(term(3));
        assertThat(uniqueSolution("sumial(5, X)").intValue("X")).isEqualTo(term(15));
        assertThat(uniqueSolution("sumial(10, X)").intValue("X")).isEqualTo(term(55));
        assertThat(uniqueSolution("sumial(100, X)").intValue("X")).isEqualTo(term(5050));
    }

    @Test
    public void unify() {
        loadTheoryFromTestResourcesDir("test-functional.pro");
        uniqueSolution("unifyterms(X,X)");
        assertThat(uniqueSolution("unifyterms21(X,123)").intValue("X")).isEqualTo(term(123));
        assertThat(uniqueSolution("unifyterms21(123, X)").intValue("X")).isEqualTo(term(123));
        assertThat(uniqueSolution("unifyterms22(X,123)").intValue("X")).isEqualTo(term(123));
        assertThat(uniqueSolution("unifyterms22(123, X)").intValue("X")).isEqualTo(term(123));
    }


    //    /**
//     * Sometimes (when?) X is bound to a term containing a unified var to another of our vars
//     */
//    @Test
//    public void relink_vars() {
//        loadTheoryFromTestResourcesDir("test-functional.pro");
//        // Below, Y must be equal to g(123,X), but does not solve to X!
//        assertEquals(term("g(123,X)"), assertOneSolution("unifyterms3(f(123,X), Y)").var("Y").unique());
//
//        // Is this really what we should have? // TuProlog returns a binding of X to an anonymous internal var
//        assertEquals(term("f(FinalVar)"), assertOneSolution("final(X)").var("X").unique());
//    }
//
//    @Test
//    public void binding_single_var_1() {
//        loadTheoryFromTestResourcesDir("test-functional.pro");
//        final MultipleSolutionsHolder assertNSolutions = assertNSolutions(6, "ab(X,Y)");
//        assertEquals("[{X=1, Y=11}, {X=2, Y=12}, {X=3, Y=13}, {X=4, Y=14}, {X=5, Y=15}, {X=6, Y=16}]", assertNSolutions.vars().list().toString());
//        assertEquals("[1, 2, 3, 4, 5, 6]", assertNSolutions.var("X").list().toString());
//        assertEquals("[11, 12, 13, 14, 15, 16]", assertNSolutions.var("Y").list().toString());
//    }
//
//    @Test
//    public void binding_single_var_2() {
//        loadTheoryFromTestResourcesDir("test-functional.pro");
//        final MultipleSolutionsHolder assertNSolutions = assertNSolutions(6, "ac(X,Y)");
//        assertEquals("[{X=1, Y=11}, {X=2, Y=twelve}, {X=3, Y=13}, {X=4, Y=fourteen}, {X=5, Y=15}, {X=6, Y=sixteen}]", assertNSolutions.vars().list().toString());
//        assertEquals("[1, 2, 3, 4, 5, 6]", assertNSolutions.var("X").list().toString());
//        assertEquals("[11, twelve, 13, fourteen, 15, sixteen]", assertNSolutions.var("Y").list().toString());
//    }
//
    @Test
    public void findall() {
        loadTheoryFromTestResourcesDir("test-functional.pro");

        assertThat(uniqueSolution("findall(1, fail, L)").toString("L")).isEqualTo("[]");
        assertThat(uniqueSolution("findall(1, true, L)").toString("L")).isEqualTo("[1]");
        assertThat(uniqueSolution("findall(1, (true;true;true), L)").toString("L")).isEqualTo("[1,1,1]");
        assertThat(uniqueSolution("findall(a(b), (true;fail;true), L)").toString("L")).isEqualTo("[a(b),a(b)]");

        assertThat(uniqueSolution("findall(X, a(X), L)").toString("L")).isEqualTo("[1,2,3]");
        assertThat(uniqueSolution("findall(b(X), a(X), L)").toString("L")).isEqualTo("[b(1),b(2),b(3)]");
        assertThat(uniqueSolution("findall(Z, a(X), L)").toString("L")).isEqualTo("[Z,Z,Z]");
        countNoSolution("findall(X, a(X), [1])");
        uniqueSolution("findall(X, a(X), [1,2,3])");
    }

    @Test
    public void findall_bindFreeVars() {
        final GoalHolder sol = uniqueSolution("findall(X, member(X,[a,B,c]), Res)");
        assertThat(sol.toString("Res")).isEqualTo("[a,B,c]");
    }


    @Test
    public void deleteList() {
        assertThat(uniqueSolution("deletelist([a,b,c,d,b], [], Res)").toString("Res")).isEqualTo("[a,b,c,d,b]");
        assertThat(uniqueSolution("deletelist([a,b,c,d,b], [a,c], Res)").toString("Res")).isEqualTo("[b,d,b]");
        assertThat(uniqueSolution("deletelist([a,b,c,d,b], [b,c], Res)").toString("Res")).isEqualTo("[a,d]");
        assertThat(uniqueSolution("deletelist([a,b,c,d,b], [z,a,b,c,d,b,f], Res)").toString("Res")).isEqualTo("[]");
    }
}
