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
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.model.Continuation;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.monadic.PoV;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class PojoLibrary extends LibraryBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PojoLibrary.class);

    private static final ThreadLocal<Map<String, Object>> threadLocalBindings = new ThreadLocal<Map<String, Object>>() {

        @Override
        protected Map<String, Object> initialValue() {
            return new TreeMap<String, Object>();
        }

    };

    public PojoLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Override
    public Object dispatch(String theMethodName, Struct theGoalStruct, PoV pov, SolutionListener theListener) {
        final Object result;
        // Argument methodName is {@link String#intern()}alized so OK to check by reference
        final int arity = theGoalStruct.getArity();
        if (arity == 2) {
            final Object arg0 = theGoalStruct.getArg(0);
            final Object arg1 = theGoalStruct.getArg(1);
            if (theMethodName == "bind") {
                result = bind(theListener, pov, arg0, arg1);
            } else {
                result = NO_DIRECT_INVOCATION_USE_REFLECTION;
            }
        } else if (arity == 3) {
            final Object arg0 = theGoalStruct.getArg(0);
            final Object arg1 = theGoalStruct.getArg(1);
            final Object arg2 = theGoalStruct.getArg(2);
            if (theMethodName == "property") {
                result = property(theListener, pov, arg0, arg1, arg2);
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
     * 
     * @param theInstance
     * @param theExpression
     * @return The value introspected
     */
    protected Object introspect(Object theInstance, String theExpression) {
        Object value;
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
    public Continuation bind(final SolutionListener theListener, PoV pov, Object theBindingName, Object theTarget) {
        final Object nameTerm = pov.reify(theBindingName);
        ensureBindingIsNotAFreeVar(nameTerm, "bind/2");

        final String name = nameTerm.toString();
        final Object instance = extract(name);
        final Object instanceTerm = createConstantTerm(instance);
        return unifyInternal(theListener, pov, instanceTerm, theTarget);
    }

    @Primitive
    public Continuation property(final SolutionListener theListener, PoV pov, Object thePojo, Object thePropertyName, Object theValue) {
        // First argument
        final Object pojo = pov.reify(thePojo);
        ensureBindingIsNotAFreeVar(pojo, "property/3");
        // Second argument
        final Object propertyName = pov.reify(thePropertyName);
        ensureBindingIsNotAFreeVar(propertyName, "property/3");
        //
        Object javaValue = introspect(pojo, (String)propertyName);
        if (javaValue == null) {
            logger.debug("Property {} value is null or does not exist", propertyName);
            return Continuation.CONTINUE;
        }
        if (javaValue instanceof Collection<?>) {
            // Convert collection to a Prolog list
            javaValue = getProlog().getTermAdapter().term(javaValue, FactoryMode.ATOM);
        }
        return unifyInternal(theListener, pov, javaValue, theValue);
    }

    /**
     * A utility method to emulate calling the bind/2 predicate from Java.
     * 
     * @param theKey
     * @param theValue
     */
    public static void bind(String theKey, Object theValue) {
        threadLocalBindings.get().put(theKey, theValue);
    }

    public static Object extract(String theKey) {
        return threadLocalBindings.get().get(theKey);
    }

}
