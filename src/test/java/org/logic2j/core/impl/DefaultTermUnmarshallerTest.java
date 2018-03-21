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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test both the DefaultTermUnmarshaller and DefaultTermMarshaller
 */
public class DefaultTermUnmarshallerTest {
    private static final Logger logger = LoggerFactory.getLogger(DefaultTermUnmarshallerTest.class);

    public static final DefaultTermUnmarshaller UNMARSHALLER = new DefaultTermUnmarshaller();

    public static final DefaultTermMarshaller MARSHALLER = new DefaultTermMarshaller();


    @Test
    public void numbers() throws Exception {
        assertThat(UNMARSHALLER.unmarshall("2323")).isEqualTo(new Integer(2323));
        assertThat(UNMARSHALLER.unmarshall("3.14")).isEqualTo(new Double(3.14));
        assertThat(UNMARSHALLER.unmarshall("2323L")).isEqualTo(new Long(2323));
        assertThat(UNMARSHALLER.unmarshall("3.14f")).isEqualTo(new Float(3.14));
    }


    @Test
    public void basicTerms() throws Exception {
        assertThat(UNMARSHALLER.unmarshall("a")).isEqualTo("a");
        assertThat(UNMARSHALLER.unmarshall("X") instanceof Var).isTrue();
        assertThat(UNMARSHALLER.unmarshall("_")).isEqualTo(Var.ANONYMOUS_VAR);
    }

    @Test
    public void struct() throws Exception {
        assertThat(MARSHALLER.marshall(UNMARSHALLER.unmarshall("f(a)"))).isEqualTo("f(a)");
        assertThat(MARSHALLER.marshall(UNMARSHALLER.unmarshall("f(1, 3.14, a, X)"))).isEqualTo("f(1, 3.14, a, X)");
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
        assertThat(term.getArg(3)).isEqualTo(term.getArg(0));
    }
}
