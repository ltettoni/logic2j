package org.logic2j.contrib.completer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
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
        assertThat(data.getCompletions().size(), is(1));
        assertThat(data.getCompletions(), contains("member("));
    }


    @Test
    public void member_lpar() {
        CompletionData data = complete("member(");
        assertThat(data.getCompletions(), emptyCollectionOf(String.class));
    }


    @Test
    public void mem() {
        CompletionData data = complete("mem");
        assertThat(data.getCompletions().size(), is(1));
        assertThat(data.getCompletions(), contains("member("));
    }


    @Test
    public void a() {
        CompletionData data = complete("a");
        assertThat(data.getCompletions(), contains("a(", "ab(", "ac(", "append("));
    }


    @Ignore("so far library predicates are not returned.")
    @Test
    public void writ() {
        CompletionData data = complete("writ");
        // logger.info("CompleterContext: {}", CompleterContextHolder.instance.get());
        assertThat(data.getCompletions(), contains("write("));
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
        assertThat(data.getCompletions().size(), is(1));
        assertThat(data.getCompletions(), contains("a, takeout("));
    }

    /**
     * When the predicate is defined, we know the arity and can start evaluating candidates.
     */
    @Test
    public void a_comma_b_lpar() {
        CompletionData data = complete("a, b(");
        assertThat(data.getCompletions(), hasItem("a, b(are)"));
        assertThat(data.getCompletions(), hasItem("a, b('These')"));
        assertThat(data.getCompletions(), hasItem("a, b('special''s')"));
    }

    /**
     * When an undefined predicate is referenced - we can't guess event the arity - so no completion.
     */
    @Test
    public void a_comma_undef_lpar() {
        CompletionData data = complete("a, undef(");
        assertThat(data.getCompletions().size(), is(0));
    }


    @Test
    public void a_lpar_value_rpar() throws Exception {
        CompletionData data = complete("a(1)");
        assertThat(data.getCompletions().size(), is(0));
    }


    @Test
    public void a_lpar_value_rpar_comma() throws Exception {
        CompletionData data = complete("a(1),");
        assertAllPredicatesAreCompleted("a(1),", data);
    }


    private void assertAllPredicatesAreCompleted(String prefix, CompletionData data) {
        assertThat(data.getCompletions().size(), greaterThanOrEqualTo(10));
        assertThat(data.getCompletions().size(), lessThan(20));
        assertThat(data.getCompletions(), hasItem(prefix + "a("));
        assertThat(data.getCompletions(), hasItem(prefix + "ab("));
        assertThat(data.getCompletions(), hasItem(prefix + "ac("));
        assertThat(data.getCompletions(), hasItem(prefix + "b("));
        assertThat(data.getCompletions(), hasItem(prefix + "member("));
        assertThat(data.getCompletions(), hasItem(prefix + "takeout("));
    }


    @Test
    public void a_lpar_value_rpar_comma_ab() throws Exception {
        CompletionData data = complete("a(1), ab");
        assertThat(data.getCompletions().size(), is(1));
        assertThat(data.getCompletions(), hasItem("a(1), ab("));
    }


    @Test
    public void a_lpar_value_rpar_comma_ab_lpar() throws Exception {
        CompletionData data = complete("a(1), ab(");
        assertThat(data.getCompletions().size(), is(6));
        assertThat(data.getCompletions(), hasItem("a(1), ab(1, "));
        assertThat(data.getCompletions(), hasItem("a(1), ab(2, "));
        assertThat(data.getCompletions(), hasItem("a(1), ab(3, "));
        assertThat(data.getCompletions(), hasItem("a(1), ab(4, "));
        assertThat(data.getCompletions(), hasItem("a(1), ab(5, "));
        assertThat(data.getCompletions(), hasItem("a(1), ab(6, "));
    }


    @Test
    public void strip1() {
        assertThat(Completer.strip("").stripped, is(""));
        assertThat(Completer.strip("a").stripped, is("a"));
        assertThat(Completer.strip(" a").stripped, is("a"));
        assertThat(Completer.strip(" a(").stripped, is(""));
    }


    @Test
    public void strip2() {
        assertThat(Completer.strip("a").functor, nullValue());
        assertThat(Completer.strip(" f( ab").partialPredicate, is("f( "));
        assertThat(Completer.strip(" f( ab").functor, is("f"));
        assertThat(Completer.strip(" f(").partialPredicate, is("f("));
        assertThat(Completer.strip(" f( ab, cd, ef").partialPredicate, is("f( ab, cd, "));
        assertThat(Completer.strip(" f( ab, cd, ef").functor, is("f"));
    }


    @Test
    public void strip3() {
        assertThat(Completer.strip("a(1)").stripped, is(""));
        assertThat(Completer.strip("a(1)").functor, nullValue());
        assertThat(Completer.strip("a(1) ").stripped, is(""));
        assertThat(Completer.strip("a(1) ").functor, nullValue());
        assertThat(Completer.strip("a(1) , ").stripped, is(""));
        assertThat(Completer.strip("a(1) , ").functor, nullValue());
    }



    @Test
    public void txt_lpar() {
        CompletionData data = complete("txt(");
        assertThat(data.getCompletions().size(), is(3));
        assertThat(data.getCompletions(), hasItem("txt('One')"));
        assertThat(data.getCompletions(), hasItem("txt('Once')"));
        assertThat(data.getCompletions(), hasItem("txt('Two')"));
    }

    @Test
    public void txt_lpar_quote() {
        CompletionData data = complete("txt('");
        assertThat(data.getCompletions().size(), is(3));
        assertThat(data.getCompletions(), hasItem("txt('One')"));
        assertThat(data.getCompletions(), hasItem("txt('Once')"));
        assertThat(data.getCompletions(), hasItem("txt('Two')"));
    }

    @Test
    public void txt_lpar_quote_O() {
        CompletionData data = complete("txt('O");
        assertThat(data.getCompletions().size(), is(2));
        assertThat(data.getCompletions(), hasItem("txt('One')"));
        assertThat(data.getCompletions(), hasItem("txt('Once')"));
    }


    @Test
    public void txt_lpar_quote_Z() {
        CompletionData data = complete("txt('Z");
        assertThat(data.getCompletions().size(), is(0));
    }


    @Test
    public void acceptPredicateKey() {
        assertTrue(Completer.acceptPredicateKey("pred/3"));
        assertFalse(Completer.acceptPredicateKey("pred_/3"));
        assertFalse(Completer.acceptPredicateKey("pred_1/3"));
        assertFalse(Completer.acceptPredicateKey("pred_22/3"));
    }

}
