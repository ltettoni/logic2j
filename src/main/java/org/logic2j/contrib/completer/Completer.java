package org.logic2j.contrib.completer;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.Prolog;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.term.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Created by tettoni on 2015-10-11.
 */
public class Completer {
    private static final Logger logger = LoggerFactory.getLogger(Completer.class);

    private final Prolog prolog;

    public Completer(Prolog prolog) {
        this.prolog = prolog;
    }

    /**
     * Avoid predicates such as '->', ';', etc
     */
    private static final Pattern ACCEPTABLE = Pattern.compile("[\\w']+/\\d+");


    static CompletionData strip(String str) {
        final CompletionData result = new CompletionData();
        result.original = str;
        result.stripped = str;
        result.partialPredicate = null;
        result.functor = null;
        if (str.length() > 0) {
            char c = 'a';
            int pos = str.length() - 1;
            for (pos = str.length() - 1; pos >= 0; pos--) {
                c = str.charAt(pos);
                final boolean cont = Character.isJavaIdentifierPart(c) || Character.isJavaIdentifierStart(c) || Character.isDigit(c) || c == '\'';
                if (!cont) {
                    result.stripped = str.substring(pos + 1);

                    int end = pos;
                    int argNo = 0;
                    for (; pos >= 0; pos--) {
                        c = str.charAt(pos);
                        if (c==',') {
                            argNo++;
                        }
                        if (c == '(') {
                            int parenth = pos;
                            for (pos--; pos >= 0; pos--) {
                                c = str.charAt(pos);
                                if (! (Character.isJavaIdentifierPart(c) || Character.isJavaIdentifierStart(c) || Character.isDigit(c))) {
                                    break;
                                }
                            }
                            result.partialPredicate = str.substring(pos+1, end);
                            result.functor = str.substring(pos+1, parenth);
                            result.argNo = argNo;
                            return result;
                        }
                    }
                    return result;
                }
            }
            return result;
        }
        return result;
    }

    Set<String> allSignatures(CharSequence partialInput) {
        Set<String> signatures = new TreeSet<>();
        // From all loaded clause providers
        for (final ClauseProvider cp : this.prolog.getTheoryManager().getClauseProviders()) {
            for (final Clause clause : cp.listMatchingClauses(new Var<Object>("unused"), null)) {
                String predicateKey = clause.getPredicateKey();
                if (ACCEPTABLE.matcher(predicateKey).matches()) {
                    signatures.add(predicateKey);
                } else {
                    logger.debug("Signature not retained for completion: {}", predicateKey);
                }
            }
        }
        // From libraries (TBD)

        logger.info("Distinct signatures: {}", signatures);
        return signatures;
    }
}
