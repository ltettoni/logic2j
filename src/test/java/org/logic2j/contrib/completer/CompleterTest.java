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
        assertThat(data.getCompletions(), hasItem("member(X, "));
        assertThat(data.getCompletions(), hasItem("member(_, "));
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
        assertAllPredicatesAreCompleted(data);
    }

    @Test
    public void a_comma() {
        CompletionData data = complete("a, ");
        assertAllPredicatesAreCompleted(data);
    }

    @Test
    public void a_comma_ta() {
        CompletionData data = complete("a, ta");
        assertThat(data.getCompletions().size(), is(1));
        assertThat(data.getCompletions(), contains("takeout("));
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
        assertAllPredicatesAreCompleted(data);
    }


    private void assertAllPredicatesAreCompleted(CompletionData data) {
        assertThat(data.getCompletions().size(), greaterThanOrEqualTo(10));
        assertThat(data.getCompletions().size(), lessThan(20));
        assertThat(data.getCompletions(), hasItem("a("));
        assertThat(data.getCompletions(), hasItem("ab("));
        assertThat(data.getCompletions(), hasItem("ac("));
        assertThat(data.getCompletions(), hasItem("b("));
        assertThat(data.getCompletions(), hasItem("member("));
        assertThat(data.getCompletions(), hasItem("takeout("));
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
}
