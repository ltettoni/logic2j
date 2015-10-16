package org.logic2j.contrib.completer;

import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.extractor.SingleVarExtractor;
import org.logic2j.core.api.solver.listener.SingleVarSolutionListener;
import org.logic2j.core.impl.PrologImplementation;
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
    private static final String COMPLETION_VAR = "CompleterVar";
    private static final int MAX_FETCH = 100;

    private final PrologImplementation prolog;

    public Completer(PrologImplementation prolog) {
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

                    int end = pos+1;
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
        final Set<String> signatures = new TreeSet<String>();
        // From all loaded clause providers
        for (final ClauseProvider cp : this.prolog.getTheoryManager().getClauseProviders()) {
            for (final Clause clause : cp.listMatchingClauses(new Var<Object>("unused"), null)) {
                final String predicateKey = clause.getPredicateKey();
                if (ACCEPTABLE.matcher(predicateKey).matches() && predicateKey.startsWith(partialInput.toString())) {
                    signatures.add(predicateKey);
                } else {
                    logger.debug("Signature not retained for completion: {}", predicateKey);
                }
            }
        }
        // From libraries (TBD)

        logger.debug("Distinct signatures: {}", signatures);
        return signatures;
    }

    public CompletionData complete(CharSequence partialInput) {
        final CompletionData completionData = strip(partialInput.toString());
        final Set<String> completions = new TreeSet<String>();
        if (completionData.functor!=null) {
            // Find arity
            final Set<String> signatures = allSignatures(completionData.functor);
            if (signatures.isEmpty()) {
                return completionData;
            }
            // find same predicate
            for (String signature: signatures) {
                if (TermApi.functorFromSignature(signature).equals(completionData.functor)) {
                    int arity = TermApi.arityFromSignature(signature);

                    final int commaCount = commaCount(completionData.partialPredicate);
                    String goal = buildGoal(completionData.partialPredicate, arity);
                    logger.info("Going to execute: {}", goal);
                    Object goalObj = prolog.getTermUnmarshaller().unmarshall(goal);

                    SingleVarExtractor<Object> stringSingleVarExtractor = new SingleVarExtractor<Object>(goalObj, COMPLETION_VAR, Object.class);
                    SingleVarSolutionListener<Object> listener = new SingleVarSolutionListener<Object>(stringSingleVarExtractor);
                    listener.setMaxFetch(MAX_FETCH);

                    this.prolog.getSolver().solveGoal(goalObj, listener);
                    boolean hasVar = false;
                    final String termination = (arity > commaCount+1) ? ", " : ")";
                    for (Object sol : listener.getResults()) {
                        String compl;
                        if (sol instanceof Var<?>) {
                            hasVar = true;
                        } else {
                            compl = String.valueOf(sol);
                            completions.add(compl + termination);
                        }
                    }
                    if (hasVar) {
                        completions.add("X" + termination);
                        completions.add("_" + termination);
                    }
                }
            }

        } else {

            for (String signature : allSignatures(completionData.stripped)) {
                final String functor = TermApi.functorFromSignature(signature);
                final String fragment = functor + '(';
                if (fragment.startsWith(completionData.stripped)) {
                    completions.add(fragment);
                }
            }
        }
        completionData.setCompletions(completions);
        return completionData;
    }

    private String buildGoal(String partialPredicate, int arity) {
        final int commaCount = commaCount(partialPredicate);
        StringBuilder sb = new StringBuilder(partialPredicate);
        sb.append(COMPLETION_VAR);
        for (int i=arity-1; i>commaCount; i--) {
            sb.append(", _");
        }
        sb.append(')');
        return sb.toString();
    }

    private int commaCount(String partialPredicate) {
        int commaCount = 0;
        for (char c : partialPredicate.toCharArray()) {
            if (c==',') {
                commaCount++;
            }
        }
        return commaCount;
    }
}
