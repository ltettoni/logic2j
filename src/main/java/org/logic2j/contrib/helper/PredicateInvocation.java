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
package org.logic2j.contrib.helper;


import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.impl.PrologImplementation;

import java.util.Arrays;

/**
 * Bundles a predicate name, and a number of parameters,
 * and builds the Prolog goal to represent it.
 *
 * @version $Id$
 */
public class PredicateInvocation<T> {

    private Class<? extends T> desiredType;
    private final String goal; // When null, using old form predicateName + args
    private final String predicateName;
    private final String projectionVariable; // What we want to project. Normally one of the arguments.
    private final Object[] arguments;

    /**
     * Private constructor, application code should use static factories.
     *
     * @param theProjectionVariable
     * @param thePredicateName
     * @param theArguments
     */
    private PredicateInvocation(Class<? extends T> type, String theProjectionVariable, String thePredicateName, Object... theArguments) {
        super();
        this.goal = null;
        this.desiredType = type;
        this.projectionVariable = theProjectionVariable;
        this.predicateName = thePredicateName;
        this.arguments = theArguments;
    }

    /**
     * Build for a goal (many predicates)
     * @param type
     * @param theProjectionVariable
     * @param theGoalAsString
     */
    private PredicateInvocation(Class<? extends T> type, String theProjectionVariable, String theGoalAsString) {
        super();
        this.goal = theGoalAsString;
        this.desiredType = type;
        this.projectionVariable = theProjectionVariable;
        this.predicateName = null;
        this.arguments = null;
    }

    /**
     * Create a PredicateInvocation for checking a goal without obtaining results.
     *
     * @param thePredicateName
     * @param theArguments Any objects, the configured TermAdapter will be used to convert them into terms.
     * @return A PredicateInvocation that is not projecting a variable.
     */
    public static <T> PredicateInvocation<T> invocation(Class<? extends T> type, String thePredicateName, Object... theArguments) {
        final PredicateInvocation invocation = new PredicateInvocation(type, null, thePredicateName, theArguments);
        return invocation;
    }


    /**
     * Create a PredicateInvocation for projecting a single variable. This means: give me all X such that predicate(...., X, ...) yields true.
     *
     * @param theProjectionVariable
     * @param thePredicateName
     * @param theArguments          May be Object, toString() will be used
     * @return A PredicateInvocation that is projecting a variable.
     */
    public static <T> PredicateInvocation<T> projection(Class<? extends T> type, String theProjectionVariable, String thePredicateName, Object... theArguments) {
        final PredicateInvocation invocation = new PredicateInvocation(type, theProjectionVariable, thePredicateName, theArguments);
        // Normally we should (at least) warn if theProjectionVariable does not appear within arguments...
        return invocation;
    }

    public static <T> PredicateInvocation<T> projectionFromGoal(Class<? extends T> type, String theProjectionVariable, String theGoalString) {
        final PredicateInvocation invocation = new PredicateInvocation(type, theProjectionVariable, theGoalString);
        // Normally we should (at least) warn if theProjectionVariable does not appear within arguments...
        return invocation;
    }

    /**
     * @return the name of the projectionVariable
     */
    public String getProjectionVariable() {
        return this.projectionVariable;
    }

    public String getPredicateName() {
        return predicateName;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Class<? extends T> getDesiredType() {
        return desiredType;
    }

    /**
     * Convert to a term
     *
     * @return A single String atom or more likely a Struct
     */
    public Object toTerm(PrologImplementation prolog) {
        if (this.goal!=null) {
            final Object term = prolog.getTermUnmarshaller().unmarshall(this.goal);
            if (! (term instanceof Struct)) {
                throw new InvalidTermException("Expecting a Struct term, got " + this.goal);
            }
            final Struct struct = (Struct) term;
            return TermApi.normalize(struct);
        } else {
            final Object[] original = getArguments();
            if (original.length == 0) {
                return getPredicateName();
            }
            final Object[] parsed = new Object[original.length];
            for (int i = 0; i < original.length; i++) {
                if (original[i] instanceof CharSequence) {
                    parsed[i] = prolog.getTermUnmarshaller().unmarshall((CharSequence) original[i]);
                } else {
                    parsed[i] = prolog.getTermAdapter().toTerm(original[i], TermAdapter.FactoryMode.ANY_TERM);
                }
            }
            final Struct struct = new Struct(getPredicateName(), parsed);
            return TermApi.normalize(struct);
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
        sb.append('{');
        if (this.getProjectionVariable() != null) {
            sb.append(getProjectionVariable());
            sb.append('|');
        }
        if (this.goal!=null) {
            sb.append(this.goal);
        } else {
            sb.append(getPredicateName());
            sb.append(Arrays.asList(getArguments()));
        }
        sb.append('}');
        return sb.toString();
    }

}
