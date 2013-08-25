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
package org.logic2j.core.library.mgmt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.logic2j.core.library.PLibrary;
import org.logic2j.core.model.exception.InvalidTermException;
import org.logic2j.core.model.exception.RecursionException;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.listener.SolutionListener;
import org.logic2j.core.solver.listener.SolutionListenerBase;

/**
 * Describe a primitive, either a:
 * <ul>
 * <li>Directive</li>
 * <li>Predicate</li>
 * <li>Functor</li>
 * </ul>
 * 
 * @note Strangely, this class has ivoke() features so it's not only a description!
 */
public class PrimitiveInfo {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrimitiveInfo.class);
    private static final boolean debug = logger.isDebugEnabled();

    public static enum PrimitiveType {
        /**
         * Not yet implemented.
         */
        DIRECTIVE,
        /**
         * A predicate implemented as a Java method, will produce solution(s) or not via the {@link SolutionListener} interface.
         */
        PREDICATE,
        /**
         * A functor yields a result, such as +(2,3).
         */
        FUNCTOR
    }

    private final PrimitiveType type;
    private final String name;
    private final PLibrary library; // The library instance on which the method will be invoked (they are not static methods)
    private final Method method; // The method that implements the primitive's logic
    private final boolean varargs;

    public PrimitiveInfo(PrimitiveType theType, PLibrary theLibrary, String theName, Method theMethod, boolean theVarargs) {
        super();
        this.type = theType;
        this.library = theLibrary;
        this.name = theName;
        this.method = theMethod;
        this.varargs = theVarargs;
    }

    /**
     * @param theGoalStruct
     * @param theGoalVars
     * @param theGoalStruct
     */
    public Object invoke(Struct theGoalStruct, Bindings theGoalVars, SolutionListener theListener) {
        if (debug) {
            logger.debug("PRIMITIVE > invocation of {}", this);
        }
        final int arity = theGoalStruct.getArity();
        final int nbargs = isVarargs() ? 3 : (2 + arity);
        final Object[] args = new Object[nbargs];
        int i = 0;
        args[i++] = theListener;
        args[i++] = theGoalVars;
        if (isVarargs()) {
            // All arguments as an array
            final Term[] varargArray = new Term[arity];
            int j = 0;
            while (j < arity) {
                varargArray[j] = theGoalStruct.getArg(j);
                j++;
            }
            args[i++] = varargArray;
        } else {
            // Regular argument passing
            int j = 0;
            while (j < arity) {
                args[i++] = theGoalStruct.getArg(j++);
            }
        }
        try {
            final Object result = getMethod().invoke(getLibrary(), args);
            if (debug) {
                logger.debug("PRIMITIVE < result={}", result);
            }
            return result;
        } catch (final IllegalArgumentException e) {
            throw e;
        } catch (final IllegalAccessException e) {
            throw new InvalidTermException("Could not access method " + getMethod(), e);
        } catch (final InvocationTargetException e) {
            final Throwable targetException = e.getTargetException();
            if (targetException instanceof RecursionException) {
                // If we already have trouble in recursivity, don't add further exceptions - just rethrow.
                throw (RecursionException) targetException;
            }
            if (targetException instanceof StackOverflowError) {
                final RecursionException recursionException = new RecursionException("Stack overflow while executing primitive " + this);
                recursionException.stackOverflow = true;
                throw recursionException;
            }
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            }
            throw new InvalidTermException("Primitive threw an exception: " + this + ": " + targetException, targetException);
        }
    }

    private static final SolutionListenerBase NO_LISTENER = null;

    public Term invokeFunctor(Struct theGoalStruct, Bindings theGoalVars) {
        return (Term) invoke(theGoalStruct, theGoalVars, NO_LISTENER);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public PrimitiveType getType() {
        return this.type;
    }

    public PLibrary getLibrary() {
        return this.library;
    }

    public Method getMethod() {
        return this.method;
    }

    public String getName() {
        return this.name;
    }

    public boolean isVarargs() {
        return this.varargs;
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{lib=" + getLibrary() + ", type=" + getType() + ", name=" + getName() + ", method=" + getMethod().getName() + '}';
    }

}
