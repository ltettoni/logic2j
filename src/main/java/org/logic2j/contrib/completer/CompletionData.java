package org.logic2j.contrib.completer;

import java.util.Collections;
import java.util.Set;

/**
 * Created by tettoni on 2015-10-11.
 */
public class CompletionData {

    public String original;
    public String stripped;
    public String functor;
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
