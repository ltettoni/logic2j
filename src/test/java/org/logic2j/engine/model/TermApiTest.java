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
package org.logic2j.engine.model;

import org.junit.Test;
import org.logic2j.engine.exception.InvalidTermException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Low-level tests of the {@link TermApi} facade.
 */
public class TermApiTest {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TermApiTest.class);

    @Test
    public void placeholderToReproduceError() {
        //
    }


    @Test
    public void structurallyEquals() {
        // Vars are never structurally equal ...
        assertThat(new Var<Object>("X").structurallyEquals(new Var<Object>("Y"))).isFalse();
        final Var<?> x1 = new Var<Object>("X");
        final Var<?> x2 = new Var<Object>("X");
        // ... even when they have the same name
        assertThat(x1.structurallyEquals(x2)).isFalse();
        final Struct s = new Struct("s", x1, x2);
        assertThat(TermApi.structurallyEquals(s.getArg(0), s.getArg(1))).isFalse();
        // After factorization, the 2 X will be same
        final Struct s2 = TermApi.factorize(s);
        assertThat(s2).isNotSameAs(s);
        assertThat(s.structurallyEquals(s2)).isFalse();
        assertThat(TermApi.structurallyEquals(s2.getArg(0), s2.getArg(1))).isTrue();
    }


    @Test
    public void collectTerms() {
        Term term;
        //
        term = Struct.valueOf("p", "X", 2);
        logger.info("Flat terms: {}", TermApi.collectTerms(term));
        //
        term = Struct.valueOf("a", new Struct("b"), "c");
        logger.info("Flat terms: {}", TermApi.collectTerms(term));
        //
        term = new Struct(Struct.FUNCTOR_CLAUSE, new Struct("a", Struct.valueOf("p", "X", "Y")), Struct.valueOf("p", "X", "Y"));
        logger.info("Flat terms: {}", TermApi.collectTerms(term));
        //
        final Term clause = new Struct(Struct.FUNCTOR_CLAUSE, new Struct("a", Struct.valueOf("p", "X", "Y")), Struct.valueOf("p", "X", "Y"));
        logger.info("Flat terms of original {}", TermApi.collectTerms(clause));
        final Object t2 = TermApi.normalize(clause);
        logger.info("Found {} bindings", ((Struct) t2).getIndex());
        assertThat(((Struct) t2).getIndex()).isEqualTo(2);
        logger.info("Flat terms of copy     {}", TermApi.collectTerms(t2));
        assertThat(t2.toString()).isEqualTo(clause.toString());
    }

    @Test
    public void assignIndexes() {
        int nbVars;
        nbVars = TermApi.assignIndexes(new Struct("f"), 0);
        assertThat(nbVars).isEqualTo(0);
        nbVars = TermApi.assignIndexes(new Var<Object>("X"), 0);
        assertThat(nbVars).isEqualTo(1);
        nbVars = TermApi.assignIndexes(Var.ANONYMOUS_VAR, 0);
        assertThat(nbVars).isEqualTo(0);
        //
        nbVars = TermApi.assignIndexes(2L, 0);
        assertThat(nbVars).isEqualTo(0);
        nbVars = TermApi.assignIndexes(1.1, 0);
        assertThat(nbVars).isEqualTo(0);
    }

    @Test
    public void selectTerm() {
        final Object arg0 = Struct.valueOf("b", "c", "c2");
        final Object term = Struct.valueOf("a", arg0, "b2");
//        unmarshall("a(b(c,c2),b2)");
        //
        assertThat(TermApi.selectTerm(term, "", Struct.class)).isEqualTo(term);
        assertThat(TermApi.selectTerm(term, "a", Struct.class)).isEqualTo(term);
        try {
            TermApi.selectTerm(term, "a[-1]", Struct.class);
            fail("Should fail");
        } catch (final InvalidTermException e) {
            // OK
        }
        //
        try {
            TermApi.selectTerm(term, "a[0]", Struct.class);
            fail("Should fail");
        } catch (final InvalidTermException e) {
            // OK
        }
        //
        try {
            TermApi.selectTerm(term, "a[4]", Struct.class);
            fail("Should fail");
        } catch (final InvalidTermException e) {
            // OK
        }
        //
        try {
            TermApi.selectTerm(term, "z", Struct.class);
            fail("Should fail");
        } catch (final InvalidTermException e) {
            // OK
        }
        //
        final Struct sTerm = (Struct) term;
        assertThat(TermApi.selectTerm(term, "a/", Struct.class)).isEqualTo(arg0);
        assertThat(TermApi.selectTerm(term, "a[1]", Struct.class)).isEqualTo(arg0);
        assertThat(TermApi.selectTerm(term, "[1]", Struct.class)).isEqualTo(arg0);
        assertThat(TermApi.selectTerm(term, "a/b", Struct.class)).isEqualTo(arg0);
        assertThat(TermApi.selectTerm(term, "a[1]/b", Struct.class)).isEqualTo(arg0);
        assertThat(TermApi.selectTerm(term, "a[2]", String.class)).isEqualTo("b2");
        assertThat(TermApi.selectTerm(term, "a[2]/b2", String.class)).isEqualTo("b2");
        assertThat(TermApi.selectTerm(term, "a/b/c", String.class)).isEqualTo("c");
        assertThat(TermApi.selectTerm(term, "a/b[1]", String.class)).isEqualTo("c");
        assertThat(TermApi.selectTerm(term, "a/[1]", String.class)).isEqualTo("c");
        assertThat(TermApi.selectTerm(term, "a/b[2]", String.class)).isEqualTo("c2");
    }


    @Test(expected=InvalidTermException.class)
    public void functorFromSignatureFails() {
        TermApi.functorFromSignature("toto4");
    }


    @Test
    public void functorFromSignature1() {
        assertThat(TermApi.functorFromSignature("toto/4")).isEqualTo("toto");
    }



    @Test(expected=InvalidTermException.class)
    public void arityFromSignatureFails() {
        TermApi.arityFromSignature("toto4");
    }

    @Test
    public void arityFromSignature1() {
        assertThat(TermApi.arityFromSignature("toto/4")).isEqualTo(4);
    }

    @Test
    public void quoteIfNeeded() {
        assertThat(TermApi.quoteIfNeeded(null)).isNull();
        assertThat(TermApi.quoteIfNeeded("").toString()).isEqualTo("''");
        assertThat(TermApi.quoteIfNeeded(" ").toString()).isEqualTo("' '");
        assertThat(TermApi.quoteIfNeeded("ab").toString()).isEqualTo("ab");
        assertThat(TermApi.quoteIfNeeded("Ab").toString()).isEqualTo("'Ab'");
        assertThat(TermApi.quoteIfNeeded("it's").toString()).isEqualTo("'it''s'");
        assertThat(TermApi.quoteIfNeeded("a''b").toString()).isEqualTo("'a''''b'");
        assertThat(TermApi.quoteIfNeeded("'that'").toString()).isEqualTo("'''that'''");
    }
}
