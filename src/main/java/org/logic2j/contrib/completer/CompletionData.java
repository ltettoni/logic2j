package org.logic2j.contrib.completer;

import java.util.Collections;
import java.util.Set;

/**
 * Created by tettoni on 2015-10-11.
 */
public class CompletionData {

    public String original; // Complete input submitted

    public String originalBeforeStripped;

    public String stripped; // Only the last part where we search for completion
    public String functor;  // When processing arguments, the functor of these arguments

    /**
     * From the beginning of the predicate (functor), until before the stripped part
     */
    public String partialPredicate;

    public int argNo;

    Set<String> completions = Collections.emptySet();

    public Set<String> getCompletions() {
        return completions;
    }

    public void setCompletions(Set<String> completions) {
        this.completions = completions;
    }
}
