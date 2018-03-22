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

package org.logic2j.contrib.completer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class CompleterTest extends PrologTestBase {
    private static final Logger logger = LoggerFactory.getLogger(CompleterTest.class);

    @Override
    protected PrologReferenceImplementation.InitLevel initLevel() {
        return PrologReferenceImplementation.InitLevel.L2_BASE_LIBRARIES;
    }


    @Before
    public void loadTheory() {
        loadTheoryFromTestResourcesDir("test-completer.pro");
    }


    private CompletionData complete(CharSequence partialInput) {
        final Completer completer = new Completer(getProlog());
        return completer.complete(partialInput);
    }


    @Test
    public void member() {
        CompletionData data = complete("member");
        assertThat(data.getCompletions().size()).isEqualTo(1);
        assertThat(data.getCompletions()).containsExactly("member(");
    }


    @Test
    public void member_lpar() {
        CompletionData data = complete("member(");
        assertThat(data.getCompletions()).isEmpty();
    }


    @Test
    public void mem() {
        CompletionData data = complete("mem");
        assertThat(data.getCompletions().size()).isEqualTo(1);
        assertThat(data.getCompletions()).containsExactly("member(");
    }


    @Test
    public void a() {
        CompletionData data = complete("a");
        assertThat(data.getCompletions()).containsExactly("a(", "ab(", "ac(", "append(");
    }


    @Ignore("so far library predicates are not returned.")
    @Test
    public void writ() {
        CompletionData data = complete("writ");
        // logger.info("CompleterContext: {}", CompleterContextHolder.instance.get());
        assertThat(data.getCompletions()).containsExactly("write(");
    }


    @Test
    public void empty() {
        CompletionData data = complete("");
        assertAllPredicatesAreCompleted("", data);
    }

    @Test
    public void a_comma() {
        CompletionData data = complete("a, ");
        assertAllPredicatesAreCompleted("a, ", data);
    }

    @Test
    public void a_comma_ta() {
        CompletionData data = complete("a, ta");
        assertThat(data.getCompletions().size()).isEqualTo(1);
        assertThat(data.getCompletions()).containsExactly("a, takeout(");
    }

    /**
     * When the predicate is defined, we know the arity and can start evaluating candidates.
     */
    @Test
    public void a_comma_b_lpar() {
        CompletionData data = complete("a, b(");
        assertThat(data.getCompletions()).contains("a, b(are)");
        assertThat(data.getCompletions()).contains("a, b('These')");
        assertThat(data.getCompletions()).contains("a, b('special''s')");
    }

    /**
     * When an undefined predicate is referenced - we can't guess event the arity - so no completion.
     */
    @Test
    public void a_comma_undef_lpar() {
        CompletionData data = complete("a, undef(");
        assertThat(data.getCompletions().size()).isEqualTo(0);
    }


    @Test
    public void a_lpar_value_rpar() {
        CompletionData data = complete("a(1)");
        assertThat(data.getCompletions().size()).isEqualTo(0);
    }


    @Test
    public void a_lpar_value_rpar_comma() {
        CompletionData data = complete("a(1),");
        assertAllPredicatesAreCompleted("a(1),", data);
    }


    private void assertAllPredicatesAreCompleted(String prefix, CompletionData data) {
        assertThat(data.getCompletions().size()).isGreaterThanOrEqualTo(10);
        assertThat(data.getCompletions().size()).isLessThan(20);
        assertThat(data.getCompletions()).contains(prefix + "a(");
        assertThat(data.getCompletions()).contains(prefix + "ab(");
        assertThat(data.getCompletions()).contains(prefix + "ac(");
        assertThat(data.getCompletions()).contains(prefix + "b(");
        assertThat(data.getCompletions()).contains(prefix + "member(");
        assertThat(data.getCompletions()).contains(prefix + "takeout(");
    }


    @Test
    public void a_lpar_value_rpar_comma_ab() {
        CompletionData data = complete("a(1), ab");
        assertThat(data.getCompletions().size()).isEqualTo(1);
        assertThat(data.getCompletions()).contains("a(1), ab(");
    }


    @Test
    public void a_lpar_value_rpar_comma_ab_lpar() {
        CompletionData data = complete("a(1), ab(");
        assertThat(data.getCompletions().size()).isEqualTo(6);
        assertThat(data.getCompletions()).contains("a(1), ab(1, ");
        assertThat(data.getCompletions()).contains("a(1), ab(2, ");
        assertThat(data.getCompletions()).contains("a(1), ab(3, ");
        assertThat(data.getCompletions()).contains("a(1), ab(4, ");
        assertThat(data.getCompletions()).contains("a(1), ab(5, ");
        assertThat(data.getCompletions()).contains("a(1), ab(6, ");
    }


    @Test
    public void strip1() {
        assertThat(Completer.strip("").stripped).isEqualTo("");
        assertThat(Completer.strip("a").stripped).isEqualTo("a");
        assertThat(Completer.strip(" a").stripped).isEqualTo("a");
        assertThat(Completer.strip(" a(").stripped).isEqualTo("");
    }


    @Test
    public void strip2() {
        assertThat(Completer.strip("a").functor).isNull();
        assertThat(Completer.strip(" f( ab").partialPredicate).isEqualTo("f( ");
        assertThat(Completer.strip(" f( ab").functor).isEqualTo("f");
        assertThat(Completer.strip(" f(").partialPredicate).isEqualTo("f(");
        assertThat(Completer.strip(" f( ab, cd, ef").partialPredicate).isEqualTo("f( ab, cd, ");
        assertThat(Completer.strip(" f( ab, cd, ef").functor).isEqualTo("f");
    }


    @Test
    public void strip3() {
        assertThat(Completer.strip("a(1)").stripped).isEqualTo("");
        assertThat(Completer.strip("a(1)").functor).isNull();
        assertThat(Completer.strip("a(1) ").stripped).isEqualTo("");
        assertThat(Completer.strip("a(1) ").functor).isNull();
        assertThat(Completer.strip("a(1) , ").stripped).isEqualTo("");
        assertThat(Completer.strip("a(1) , ").functor).isNull();
    }



    @Test
    public void txt_lpar() {
        CompletionData data = complete("txt(");
        assertThat(data.getCompletions().size()).isEqualTo(3);
        assertThat(data.getCompletions()).contains("txt('One')");
        assertThat(data.getCompletions()).contains("txt('Once')");
        assertThat(data.getCompletions()).contains("txt('Two')");
    }

    @Test
    public void txt_lpar_quote() {
        CompletionData data = complete("txt('");
        assertThat(data.getCompletions().size()).isEqualTo(3);
        assertThat(data.getCompletions()).contains("txt('One')");
        assertThat(data.getCompletions()).contains("txt('Once')");
        assertThat(data.getCompletions()).contains("txt('Two')");
    }

    @Test
    public void txt_lpar_quote_O() {
        CompletionData data = complete("txt('O");
        assertThat(data.getCompletions().size()).isEqualTo(2);
        assertThat(data.getCompletions()).contains("txt('One')");
        assertThat(data.getCompletions()).contains("txt('Once')");
    }


    @Test
    public void txt_lpar_quote_Z() {
        CompletionData data = complete("txt('Z");
        assertThat(data.getCompletions().size()).isEqualTo(0);
    }


    @Test
    public void acceptPredicateKey() {
        assertThat(Completer.acceptPredicateKey("pred/3")).isTrue();
        assertThat(Completer.acceptPredicateKey("pred_/3")).isFalse();
        assertThat(Completer.acceptPredicateKey("pred_1/3")).isFalse();
        assertThat(Completer.acceptPredicateKey("pred_22/3")).isFalse();
    }

}
