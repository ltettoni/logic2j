package org.logic2j.core.api.solver.extractor;

import org.logic2j.core.api.unify.UnifyContext;

/**
 * Created by Laurent on 29.05.2014.
 */
public interface SolutionExtractor<T> {

    T extractSolution(UnifyContext currentVars);

}
