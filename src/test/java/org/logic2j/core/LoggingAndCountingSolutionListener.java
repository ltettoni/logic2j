package org.logic2j.core;

import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Laurent on 25.05.2014.
 */
public class LoggingAndCountingSolutionListener extends CountingSolutionListener {
    private final Object theGoal;

    public LoggingAndCountingSolutionListener(Object theGoal) {
        this.theGoal = theGoal;
    }

    private static final Logger logger = LoggerFactory.getLogger(LoggingAndCountingSolutionListener.class);

    @Override
    public Continuation onSolution(PoV thePoV) {
        logger.info("Solution: {}", thePoV.reify(theGoal));
        return super.onSolution(thePoV);
    }

    public void report() {
        logger.info("Solving \"{}\" yields {} solutions", theGoal, getCounter());
    }

}
