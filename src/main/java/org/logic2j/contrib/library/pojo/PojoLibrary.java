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
package org.logic2j.contrib.library.pojo;

import org.apache.commons.beanutils.PropertyUtils;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.library.Primitive;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Var;
import org.logic2j.core.api.solver.Continuation;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.EnvManager;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class PojoLibrary extends LibraryBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PojoLibrary.class);

    public PojoLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, UnifyContext currentVars, SolutionListener theListener) {
        final Object result;
        // Argument methodName is {@link String#intern()}alized so OK to check by reference
        final int arity = theGoalStruct.getArity();
        if (arity == 2) {
            final Object arg0 = theGoalStruct.getArg(0);
            final Object arg1 = theGoalStruct.getArg(1);
            if (theMethodName == "bind") {
                result = bind(theListener, currentVars, arg0, arg1);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 3) {
            final Object arg0 = theGoalStruct.getArg(0);
            final Object arg1 = theGoalStruct.getArg(1);
            final Object arg2 = theGoalStruct.getArg(2);
            if (theMethodName == "property") {
                result = property(theListener, currentVars, arg0, arg1, arg2);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else {
            result = NO_DIRECT_INVOCATION_USE_REFLECTION;
        }
        return result;
    }

    /**
     * Override this method with whatever introspection framework you want.
     * Here we use BeanUtils.
     *
     * @param theInstance
     * @param theExpression
     * @return The value introspected
     */
    protected Object introspect(Object theInstance, String theExpression) {
        final Object value;
        try {
            value = PropertyUtils.getProperty(theInstance, theExpression);
        } catch (IllegalAccessException e) {
            // No solution
            return null;
        } catch (InvocationTargetException e) {
            // No solution
            return null;
        } catch (NoSuchMethodException e) {
            // No solution
            return null;
        }
        return value;
    }


    @Primitive
    public Integer property(final SolutionListener theListener, UnifyContext currentVars, Object thePojo, Object thePropertyName, Object theValue) {
        // First argument
        final Object pojo = currentVars.reify(thePojo);
        ensureBindingIsNotAFreeVar(pojo, "property/3", 0);
        // Second argument
        final Object propertyName = currentVars.reify(thePropertyName);
        ensureBindingIsNotAFreeVar(propertyName, "property/3", 1);
        //
        Object javaValue = introspect(pojo, (String) propertyName);
        if (javaValue == null) {
            logger.debug("Property {} value is null or does not exist", propertyName);
            // No solution returned to the application
            // Yet continue inference
            return Continuation.CONTINUE;
        }
        // Collections will send individual solutions
        if (javaValue instanceof Collection) {
            for (Object javaElem : (Collection) javaValue) {
                final Object prologRepresentation = getProlog().getTermAdapter().toTerm(javaElem, FactoryMode.ATOM);
                final Integer result = unifyAndNotify(theListener, currentVars, prologRepresentation, theValue);
                if (result != Continuation.CONTINUE) {
                    return result;
                }
            }
            return Continuation.CONTINUE;
        }
        if (javaValue instanceof Object[]) {
            for (Object javaElem : (Object[]) javaValue) {
                final Object prologRepresentation = getProlog().getTermAdapter().toTerm(javaElem, FactoryMode.ATOM);
                final Integer result = unifyAndNotify(theListener, currentVars, prologRepresentation, theValue);
                if (result != Continuation.CONTINUE) {
                    return result;
                }
            }
            return Continuation.CONTINUE;
        }
        // Convert java objects to Prolog terms
        final Object prologRepresentation = getProlog().getTermAdapter().toTerm(javaValue, FactoryMode.ATOM);
        return unifyAndNotify(theListener, currentVars, prologRepresentation, theValue);
    }

    /**
     * Get, set or check a variable.
     * If theTarget is a free Var
     *   If the binding has a value, unify it to the free Var (this means, gets the value)
     *   If the binding has no value, do nothing (but yields a successful solution)
     * If theTarget is a bound Var
     *   If the binding has no value, set the bound Var's value into the binding
     *   If the binding has a value unifiable to theVar, succeed without other effect
     *   If the binding has a value that cannot be unified, fails
     *
     * @param theListener
     * @param currentVars
     * @param theBindingName The name to use for the binding
     * @param theTarget      Typically a Var. If a value was bound: will unify with it. If no value was bound, unify with the anonymous variable.
     * @return One solution, either theTarget is unified to a real value, or is left unchanged (unified to the anonymous var)
     */
    @Primitive
    public Integer bind(final SolutionListener theListener, UnifyContext currentVars, Object theBindingName, Object theTarget) {
        final Object nameTerm = currentVars.reify(theBindingName);
        ensureBindingIsNotAFreeVar(nameTerm, "bind/2", 0);

        final String name = nameTerm.toString();
        final Object bindingValue = this.getProlog().getTermAdapter().getVariable(name);
        final boolean bindingIsDefined = bindingValue != null;

        final Object targetTerm = currentVars.reify(theTarget);
        final boolean targetIsFree = targetTerm instanceof Var;

        // Implement the logic as per the spec defined in comment above
        final Integer result;
        if (targetIsFree) {
            if (bindingIsDefined) {
                // Getting value
                result = unifyAndNotify(theListener, currentVars, bindingValue, theTarget);
            } else {
                // Nothing to unify but escalate a solution
                result = notifySolution(theListener, currentVars);
            }
        } else {
            if (bindingIsDefined) {
                // Try to unify, will succeed or not
                result = unifyAndNotify(theListener, currentVars, bindingValue, theTarget);
            } else {
                // Set the value (and return successful solution)
                this.getProlog().getTermAdapter().setVariable(name, targetTerm);
                result = notifySolution(theListener, currentVars);
            }
        }
        return result;
    }

    /**
     * A utility method to emulate calling the bind/2 predicate from Java.
     *
     * @param theVariableName
     * @param theValue
     */
    public void bind(String theVariableName, Object theValue) {
        this.getProlog().getTermAdapter().setVariable(theVariableName, theValue);
    }

}
