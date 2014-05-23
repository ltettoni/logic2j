package org.logic2j.core.impl;

import org.junit.Test;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * Test both the DefaultTermUnmarshaller and DefaultTermMarshaller
 */
public class DefaultTermUnmarshallerTest {
private static final Logger logger = LoggerFactory.getLogger(DefaultTermUnmarshallerTest.class);
    public static final DefaultTermUnmarshaller UNMARSHALLER = new DefaultTermUnmarshaller();
    public static final DefaultTermMarshaller MARSHALLER = new DefaultTermMarshaller();

    @Test
    public void basicTerms() throws Exception {
        assertEquals(new Long(1), UNMARSHALLER.unmarshall("1"));
        assertEquals(new Double(3.14), UNMARSHALLER.unmarshall("3.14"));
        assertSame("a", UNMARSHALLER.unmarshall("a"));
        assertTrue(UNMARSHALLER.unmarshall("X") instanceof Var);
        assertSame(Var.ANONYMOUS_VAR, UNMARSHALLER.unmarshall("_"));
    }

    @Test
    public void struct() throws Exception {
        assertEquals("f(a)", MARSHALLER.marshall(UNMARSHALLER.unmarshall("f(a)")));
        assertEquals("f(1, 3.14, a, X)", MARSHALLER.marshall(UNMARSHALLER.unmarshall("f(1, 3.14, a, X)")));
    }

    @Test(expected = InvalidTermException.class)
    public void emptyString() throws Exception {
        UNMARSHALLER.unmarshall("");
    }


    @Test(expected = InvalidTermException.class)
    public void nullString() throws Exception {
        UNMARSHALLER.unmarshall(null);
    }

    @Test
    public void normalization() throws Exception {
        final Struct term = (Struct) UNMARSHALLER.unmarshall("f(a(1,2,X), Y, X, a(1,2,X))");
        logger.info("raw: {}", term);
        assertSame(term.getArg(0), term.getArg(3));
    }
}
