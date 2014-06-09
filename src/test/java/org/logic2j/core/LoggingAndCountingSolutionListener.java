package org.logic2j.core;

import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Document me!
 */
public class LoggingAndCountingSolutionListener extends CountingSolutionListener {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAndCountingSolutionListener.class);

    private final Object theGoal;

    public LoggingAndCountingSolutionListener(Object theGoal) {
        this.theGoal = theGoal;
        logger.info("Init listener for \"{}\"", theGoal);
    }

    @Override
    public Continuation onSolution(UnifyContext currentVars) {
        if (logger.isInfoEnabled()) {
            final Object value = currentVars.reify(theGoal);
            logger.info(" solution: {}", value);
        }
        return super.onSolution(currentVars);
    }

    public void report() {
        switch ((int)getCounter()) {
            case 0:
                logger.info("Solving \"{}\" yields no solution", theGoal);
                break;
            case 1:
                logger.info("Solving \"{}\" yields a single solution", theGoal);
                break;
            default:
                logger.info("Solving \"{}\" yields {} solution(s)", theGoal, getCounter());
                break;
        }
    }

}
