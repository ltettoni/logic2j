/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.logic2j.core.api.solver.holder;

import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.api.monadic.StateEngineByLookup;
import org.logic2j.core.api.solver.listener.SolutionListenerBase;

import java.util.*;

/**
 * A relay object to provide access to the results of all the solutions to a goal.
 */
public class MultipleSolutionsHolder {

    private final SolutionHolder solutionHolder;

    MultipleSolutionsHolder(SolutionHolder theSolutionHolder) {
        this.solutionHolder = theSolutionHolder;
    }

    private Integer lowest = null; // TODO to be documented
    private Integer highest = null; // TODO to be documented

    /**
     * Start solving but indicating you are not interested by the actual solutions nor their bindings,
     * only the number of solutions.
     * 
     * @return The number of solutions
     */
    public int number() {
        final SolutionListenerBase listener = new SolutionListenerBase();
        this.solutionHolder.prolog.getSolver().solveGoal(this.solutionHolder.term, new StateEngineByLookup().emptyPoV(), listener);
        final int counter = listener.getCounter();
        checkBounds(counter);
        return counter;
    }

    private void checkBounds(int counter) {
        if (this.lowest != null && counter < this.lowest) {
            throw new PrologNonSpecificError("Number of solutions was expected to be at least " + this.lowest + " but was " + counter);
        }
        if (this.highest != null && counter > this.highest) {
            throw new PrologNonSpecificError("Number of solutions was expected to be at most " + this.highest + " but was " + counter);
        }
    }

    /**
     * Solves the goal and extract, for every solution, the value for a given variable by name.
     * 
     * @param theVariableName
     */
    public List<Object> binding(final String theVariableName) {
    throw new UnsupportedOperationException("Feature not yet implemented");
//        final List<Object> results = new ArrayList<Object>();
//
//        final Object originalTerm = this.solutionHolder.term;
//        final SolutionListener listener = new SolutionListenerBase() {
//
//            @Override
//            public Continuation onSolution(PoV theReifier) {
//                final Object term = originalTerm;
//                final Var var = TermApi.findVar(term, theVariableName);
//                if (var == null) {
//                    throw new InvalidTermException("No variable named \"" + theVariableName + "\" in " + term);
//                }
//                final Object substituted = theReifier.reify(var);
//                results.add(substituted);
//                return super.onSolution();
//            }
//        };
//        this.solutionHolder.prolog.getSolver().solveGoal(originalTerm, new StateEngineByLookup().emptyPoV(), listener);
//        final int size = results.size();
//        checkBounds(size);
//        return results;
    }

    /**
     * Solves the goal and extract, for every solution, all bindings for all variables.
     * 
     * @result An ordered list of bindings
     */
    public List<Map<String, Object>> bindings() {
        final List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        final Object originalTerm = this.solutionHolder.term;
        final SolutionListener listener = new SolutionListenerBase() {

            @Override
            public Continuation onSolution(PoV theReifier) {
                final Object term = originalTerm;

                final Map<String, Object> explicitBindings = new TreeMap<String, Object>();
                final IdentityHashMap<Var, String> vars = TermApi.allVars(term);
                for (Map.Entry<Var, String> entry : vars.entrySet()) {
                    String varName = entry.getValue();
                    Object boundValue = theReifier.finalValue(entry.getKey());
                    explicitBindings.put(varName, boundValue);
                }
                results.add(explicitBindings);
                return super.onSolution(theReifier);
            }
        };
        this.solutionHolder.prolog.getSolver().solveGoal(originalTerm, new StateEngineByLookup().emptyPoV(), listener);
        final int size = results.size();
        checkBounds(size);
        return results;
    }

    /**
     * Set internal bounds to make sure the next goal to solve has exactly the specified number of solutions.
     * 
     * @param theExpectedExactNumber
     * @return this
     */
    public MultipleSolutionsHolder ensureNumber(int theExpectedExactNumber) {
        this.lowest = this.highest = theExpectedExactNumber;
        return this;
    }

    /**
     * Set internal bounds to make sure the next goal to solve has a number of solutions between the specified bounds, inclusive.
     * 
     * @param thePermissibleLowest
     * @param thePermissibleHighest
     * @return this
     */
    public MultipleSolutionsHolder ensureRange(int thePermissibleLowest, int thePermissibleHighest) {
        this.lowest = thePermissibleLowest;
        this.highest = thePermissibleHighest;
        return this;
    }

}