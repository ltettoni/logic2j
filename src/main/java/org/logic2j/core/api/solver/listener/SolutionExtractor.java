package org.logic2j.core.api.solver.listener;

import org.logic2j.core.api.monadic.PoV;

/**
 * Created by Laurent on 29.05.2014.
 */
public interface SolutionExtractor<T> {

    T extractSolution(PoV pov);

}
