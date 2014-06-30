package org.logic2j.core;

import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.api.solver.listener.CountingSolutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Used in test cases to extract number of solutions and solutions to a goal.
 */
public class ExtractingSolutionListener extends CountingSolutionListener {
    private static final Logger logger = LoggerFactory.getLogger(ExtractingSolutionListener.class);

    private final Object goal;
    private final Set<Var> vars;
    private final Set<String> varNames;
    private final List<Map<String, Object>> solutions;

    public ExtractingSolutionListener(Object theGoal) {
        this.goal = theGoal;
        vars = TermApi.allVars(this.goal).keySet();
        // Here we use an expensive TreeSet but this is only for test cases - it will get the solutions ordered and will help assertions
        varNames = new TreeSet<String>();
        for (Var var : vars) {
            varNames.add(var.getName());
        }
        varNames.add(Var.WHOLE_SOLUTION_VAR_NAME); // This pseudo var means the whole solution


        solutions = new ArrayList<Map<String, Object>>();

        logger.info("Init listener for \"{}\"", theGoal);
    }

    @Override
    public Continuation onSolution(UnifyContext currentVars) {
        final Object solution = currentVars.reify(goal);
        logger.info(" solution: {}", solution);

        final Map<String, Object> solutionVars = new HashMap<String, Object>();
        solutionVars.put(Var.WHOLE_SOLUTION_VAR_NAME, solution); // The global solution
        for (Var var : vars) {
            final Object varValue = currentVars.reify(var);
            solutionVars.put(var.getName(), varValue);
        }
        this.solutions.add(solutionVars);

        return super.onSolution(currentVars);
    }

    public void report() {
        switch ((int) getCounter()) {
            case 0:
                logger.info("Solving \"{}\" yields no solution", goal);
                break;
            case 1:
                logger.info("Solving \"{}\" yields a single solution", goal);
                break;
            default:
                logger.info("Solving \"{}\" yields {} solution(s)", goal, getCounter());
                break;
        }
    }

    public Collection<Var> getVariables() {
        return this.vars;
    }

    public List<Map<String, Object>> getSolutions() {
        return this.solutions;
    }

    public List<Object> getValues(String varName) {
        if (! varNames.contains(varName)) {
            throw new IllegalArgumentException("Variable \"" + varName +"\" not defined in goal \"" + this.goal + '"');
        }
        List<Object> values = new ArrayList<Object>(this.solutions.size());
        for (Map<String, Object> solution : this.solutions) {
            values.add(solution.get(varName));
        }
        return values;
    }


    public Collection<String> getVarNames() {
        return this.varNames;
    }
}
