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
    public void numbers() throws Exception {
        assertEquals(new Integer(2323), UNMARSHALLER.unmarshall("2323"));
        assertEquals(new Double(3.14), UNMARSHALLER.unmarshall("3.14"));
        assertEquals(new Long(2323), UNMARSHALLER.unmarshall("2323L"));
        assertEquals(new Float(3.14), UNMARSHALLER.unmarshall("3.14f"));
    }


    @Test
    public void basicTerms() throws Exception {
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
