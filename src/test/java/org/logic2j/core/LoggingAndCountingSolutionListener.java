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
    private static final Logger logger = LoggerFactory.getLogger(LoggingAndCountingSolutionListener.class);

    private final Object theGoal;

    public LoggingAndCountingSolutionListener(Object theGoal) {
        this.theGoal = theGoal;
        logger.info("Init listener for \"{}\"", theGoal);
    }

    @Override
    public Continuation onSolution(PoV thePoV) {
        logger.info(" solution: {}", thePoV.reify(theGoal));
        return super.onSolution(thePoV);
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
