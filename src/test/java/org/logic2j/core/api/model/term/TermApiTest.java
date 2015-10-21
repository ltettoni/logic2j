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
package org.logic2j.core.api.model.term;

import org.junit.Test;
import org.logic2j.core.api.model.exception.InvalidTermException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Low-level tests of the {@link org.logic2j.core.api.model.term.TermApi} facade.
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
        assertFalse(new Var<Object>("X").structurallyEquals(new Var<Object>("Y")));
        final Var<?> x1 = new Var<Object>("X");
        final Var<?> x2 = new Var<Object>("X");
        // ... even when they have the same name
        assertFalse(x1.structurallyEquals(x2));
        final Struct s = new Struct("s", x1, x2);
        assertFalse(TermApi.structurallyEquals(s.getArg(0), s.getArg(1)));
        // After factorization, the 2 X will be same
        final Struct s2 = (Struct) TermApi.factorize(s);
        assertNotSame(s, s2);
        assertFalse(s.structurallyEquals(s2));
        assertTrue(TermApi.structurallyEquals(s2.getArg(0), s2.getArg(1)));
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
        assertEquals(2, ((Struct) t2).getIndex());
        logger.info("Flat terms of copy     {}", TermApi.collectTerms(t2));
        assertEquals(clause.toString(), t2.toString());
    }

    @Test
    public void assignIndexes() {
        int nbVars;
        nbVars = TermApi.assignIndexes(new Struct("f"), 0);
        assertEquals(0, nbVars);
        nbVars = TermApi.assignIndexes(new Var<Object>("X"), 0);
        assertEquals(1, nbVars);
        nbVars = TermApi.assignIndexes(Var.ANONYMOUS_VAR, 0);
        assertEquals(0, nbVars);
        //
        nbVars = TermApi.assignIndexes(Long.valueOf(2), 0);
        assertEquals(0, nbVars);
        nbVars = TermApi.assignIndexes(Double.valueOf(1.1), 0);
        assertEquals(0, nbVars);
    }

    @Test
    public void selectTerm() {
        final Object arg0 = Struct.valueOf("b", "c", "c2");
        final Object term = Struct.valueOf("a", arg0, "b2");
//        unmarshall("a(b(c,c2),b2)");
        //
        assertSame(term, TermApi.selectTerm(term, "", Struct.class));
        assertSame(term, TermApi.selectTerm(term, "a", Struct.class));
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
        assertSame(arg0, TermApi.selectTerm(term, "a/", Struct.class));
        assertSame(arg0, TermApi.selectTerm(term, "a[1]", Struct.class));
        assertSame(arg0, TermApi.selectTerm(term, "[1]", Struct.class));
        assertSame(arg0, TermApi.selectTerm(term, "a/b", Struct.class));
        assertSame(arg0, TermApi.selectTerm(term, "a[1]/b", Struct.class));
        assertSame("b2", TermApi.selectTerm(term, "a[2]", String.class));
        assertSame("b2", TermApi.selectTerm(term, "a[2]/b2", String.class));
        assertSame("c", TermApi.selectTerm(term, "a/b/c", String.class));
        assertSame("c", TermApi.selectTerm(term, "a/b[1]", String.class));
        assertSame("c", TermApi.selectTerm(term, "a/[1]", String.class));
        assertSame("c2", TermApi.selectTerm(term, "a/b[2]", String.class));
    }


    @Test(expected=InvalidTermException.class)
    public void functorFromSignatureFails() throws Exception {
        TermApi.functorFromSignature("toto4");
    }


    @Test
    public void functorFromSignature1() throws Exception {
        assertEquals("toto", TermApi.functorFromSignature("toto/4"));
    }



    @Test(expected=InvalidTermException.class)
    public void arityFromSignatureFails() throws Exception {
        TermApi.arityFromSignature("toto4");
    }

    @Test
    public void arityFromSignature1() throws Exception {
        assertEquals(4, TermApi.arityFromSignature("toto/4"));
    }

    @Test
    public void quoteIfNeeded() throws Exception {
        assertNull(TermApi.quoteIfNeeded(null));
        assertEquals("''", TermApi.quoteIfNeeded("").toString());
        assertEquals("' '", TermApi.quoteIfNeeded(" ").toString());
        assertEquals("ab", TermApi.quoteIfNeeded("ab").toString());
        assertEquals("'Ab'", TermApi.quoteIfNeeded("Ab").toString());
        assertEquals("'it''s'", TermApi.quoteIfNeeded("it's").toString());
        assertEquals("'a''''b'", TermApi.quoteIfNeeded("a''b").toString());
        assertEquals("'''that'''", TermApi.quoteIfNeeded("'that'").toString());
    }
}
