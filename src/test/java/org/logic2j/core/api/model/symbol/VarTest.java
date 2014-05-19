package org.logic2j.core.api.model.symbol;

import org.junit.Test;
import org.logic2j.core.api.model.exception.InvalidTermException;

import static org.junit.Assert.*;

public class VarTest {

    @Test
    public void constructorValid() throws Exception {
        final Var v1 = new Var("X");
        assertSame("X", v1.getName());
        assertEquals(Term.NO_INDEX, v1.getIndex());
    }

    @Test(expected = InvalidTermException.class)
    public void constructorNull() throws Exception {
        new Var((String) null);
    }

    @Test(expected = InvalidTermException.class)
    public void constructorEmpty() throws Exception {
        new Var("");
    }

    @Test(expected = InvalidTermException.class)
    public void constructorCannotInstantiateAnonymous() throws Exception {
        new Var("_");
    }


    @Test
    public void constructorWithCharSequence() throws Exception {
        final Var v1 = new Var(new StringBuilder("X"));
        assertSame("X", v1.getName());
        assertEquals(Term.NO_INDEX, v1.getIndex());
    }


    @Test
    public void idempotence() throws Exception {
        final Var v1 = new Var("X");
        assertEquals(v1, v1);
    }


    @Test
    public void equality() throws Exception {
        final Var v1 = new Var("X");
        final Var v2 = new Var("X");
        assertNotSame(v1, v2);
        assertEquals(v1, v2);
        assertEquals(v2, v1);
    }


    @Test
    public void lowerCaseIsValid() throws Exception {
        final Var v1 = new Var("lowercase");
        assertSame("lowercase", v1.getName());
        assertEquals(Term.NO_INDEX, v1.getIndex());
    }


    @Test(expected = InvalidTermException.class)
    public void cannotCloneAnonymous() throws Exception {
        new Var(Var.ANONYMOUS_VAR);
    }

    @Test
    public void isAnonymousTrue() throws Exception {
        assertTrue(Var.ANONYMOUS_VAR.isAnonymous());
    }

    @Test
    public void isAnonymousFalse() throws Exception {
        assertFalse(new Var("X").isAnonymous());
    }




}