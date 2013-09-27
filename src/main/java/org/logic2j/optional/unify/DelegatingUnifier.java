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
package org.logic2j.optional.unify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import org.logic2j.core.api.Unifier;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.Var;
import org.logic2j.core.api.model.var.Binding;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.unify.BindingTrail;

/**
 * A {@link Unifier} that uses reflecton to determine which method to invoke to unify 2 concrete {@link Term}s. The methods invoked must
 * have the exact signature unify(Term term1, Term term2, Bindings theBindings1, Bindings theBindings2) where the
 * classes of term1 and term2 are the effective final subclasses.
 */
public class DelegatingUnifier implements Unifier {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DelegatingUnifier.class);

    static {
        final Map<String, Method> methodMap = new TreeMap<String, Method>();

        for (final Method method : DelegatingUnifier.class.getMethods()) {
            if ("unify".equals(method.getName())) {
                final Class<?>[] parameterTypes = method.getParameterTypes();

                final String key1 = classKey(parameterTypes[0]);
                final String key2 = classKey(parameterTypes[2]);
                final String key = key1 + '-' + key2;
                methodMap.put(key, method);
            }
        }
        logger.info("MethodMap: {}", methodMap);
    }

    @Override
    public boolean unify(Object term1, Bindings theBindings1, Object term2, Bindings theBindings2) {
        for (final Method method : this.getClass().getMethods()) {
            if ("unify".equals(method.getName())) {
                final Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes[0].isAssignableFrom(term1.getClass()) && parameterTypes[1].isAssignableFrom(term2.getClass())) {
                    try {
                        BindingTrail.markBeforeAddingBindings();
                        final boolean unified = (Boolean) method.invoke(this, new Object[] { term1, term2, theBindings1, theBindings2 });
                        if (!unified) {
                            deunify();
                        }
                        return unified;
                    } catch (final InvocationTargetException e) {
                        throw new InvalidTermException("Could not determine or invoke unification method: " + e.getTargetException());
                    } catch (final Exception e) {
                        throw new InvalidTermException("Could not determine or invoke unification method: " + e);
                    }
                }
            }
        }
        throw new InvalidTermException("Could not determine or invoke unification method");
    }

    /**
     * @param theClass
     */
    private static String classKey(Class<?> theClass) {
        int level = 0;
        Class<?> c = theClass;
        while (c != Object.class) {
            level++;
            c = c.getSuperclass();
        }
        return level + "_" + theClass.getName();
    }

    // ---------------------------------------------------------------------------
    // Unification method called by introspection
    // ---------------------------------------------------------------------------

    // TODO The methods should be in protected visibility, we just have to make sure we can invoke them by reflection!

    public boolean unify(Struct s1, Struct s2, Bindings theBindings1, Bindings theBindings2) {
        if (!(s1.nameAndArityMatch(s2))) {
            return false;
        }
        final int arity = s1.getArity();
        for (int i = 0; i < arity; i++) {
            if (!unify(s1.getArg(i), theBindings1, s2.getArg(i), theBindings2)) {
                return false;
            }
        }
        return true;
    }

    public boolean unify(Struct term1, Var term2, Bindings theBindings1, Bindings theBindings2) {
        // Second term is var, we prefer have it first
        return unify(term2, term1, theBindings2, theBindings1);
    }

    public boolean unify(Var term1, Struct term2, Bindings theBindings1, Bindings theBindings2) {
        return unifyVarToWhatever(term1, term2, theBindings1, theBindings2);
    }

    public boolean unify(Var term1, Var term2, Bindings theBindings1, Bindings theBindings2) {
        return unifyVarToWhatever(term1, term2, theBindings1, theBindings2);
    }

    private boolean unifyVarToWhatever(Var var1, Term term2, Bindings theBindings1, Bindings theBindings2) {
        // Variable:
        // - when anonymous, unifies
        // - when free, bind it
        // - when bound, follow VARs until end of chain
        if (var1.isAnonymous()) {
            return true;
        }
        final Binding binding1 = var1.bindingWithin(theBindings1).followLinks();
        // Followed chain to the end until we hit either a FREE or LITERAL binding
        if (binding1.isFree()) {
            // Should not bind to an anonymous variable
            if ((term2 instanceof Var) && ((Var) term2).isAnonymous()) {
                return true;
            }
            // Bind the free var
            if (binding1.bindTo(term2, theBindings2)) {
                BindingTrail.addBinding(binding1);
            }
            return true;
        } else if (binding1.isLiteral()) {
            // We have followed term1 to end up with a literal. It may either unify or not depending if
            // term2 is a Var or the same literal. To simplify implementation we recurse with the constant
            // part as term2
            return unify(term2, theBindings2, binding1.getTerm(), binding1.getLiteralBindings());
        } else {
            throw new IllegalStateException("Internal error, unexpected binding type for " + binding1);
        }
    }

    @Override
    public void deunify() {
        BindingTrail.undoBindingsUntilPreviousMark();
    }
}
