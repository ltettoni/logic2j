package org.logic2j.core.library.impl;

import org.junit.Test;
import org.logic2j.core.api.Prolog;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Demonstrate simple use cases for Logic2j.
 */
public class UseCaseTest {

    @Test
    public void instantiateViaFactory() throws Exception {
        final Prolog prolog = new PrologBuilder().createInstance();
        assertNotNull(prolog);
    }


    @Test
    public void configureDefaultLibs() throws Exception {
        assertFalse(new PrologBuilder().withoutLibraries(false).isNoLibraries());
        assertTrue(new PrologBuilder().withoutLibraries(true).isNoLibraries());
        assertFalse(new PrologBuilder().withCoreLibraries(false).isCoreLibraries());
        assertTrue(new PrologBuilder().withCoreLibraries(true).isCoreLibraries());
    }

    @Test
    public void obtainDefaultPrologAndSolve() throws Exception {
        final Prolog prolog = new PrologBuilder().createInstance();
        assertEquals(4, prolog.solve("member(X, [a,b,c,d])").count());
    }


    @Test
    public void loadTheoryAndSolve() throws Exception {
        final File th1 = new File("src/test/resources/queens.pro");
        final File th2 = new File("src/test/resources/hanoi.pro");
        final Prolog prolog = new PrologBuilder().withTheory(th1, th2).createInstance();
        assertEquals(2, prolog.solve("queens(4, _)").count());
    }


    @Test
    public void solve() throws Exception {
        final Prolog prolog = new PrologBuilder()
        .withTheory(new File("src/test/resources/queens.pro"))
        .createInstance();
        final List<Object> objectList = prolog.solve("queens(4, Q)").var("Q").list();
        fail("test something here!");
    }
}
