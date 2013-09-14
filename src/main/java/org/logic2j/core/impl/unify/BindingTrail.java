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

package org.logic2j.core.impl.unify;

import java.util.ArrayList;
import java.util.Stack;

import org.logic2j.core.api.model.var.Binding;

/**
 * Prototype implementation to manage a trail of {@link Binding}s so that they can be undone.
 * So far we are using a {@link ThreadLocal} variable to avoid having to pass a context object around - this may have a performance impact
 * although not proven.
 */
public final class BindingTrail {

    private static final ThreadLocal<Stack<ArrayList<Binding>>> stackOfBindings = new ThreadLocal<Stack<ArrayList<Binding>>>() {

        @Override
        protected Stack<ArrayList<Binding>> initialValue() {
            return new Stack<ArrayList<Binding>>();
        }

    };

    public static Stack<ArrayList<Binding>> markBeforeAddingBindings() {
        final Stack<ArrayList<Binding>> stack = stackOfBindings.get();
        // Lazy instantiation of a new ArrayList<Binding>() on top of stack
        stack.push(new ArrayList<Binding>());
        // stack.push(null);
        return stack;
    }

    public static Stack<ArrayList<Binding>> markBeforeAddingBindingsLazy() {
        final Stack<ArrayList<Binding>> stack = stackOfBindings.get();
        // Lazy instantiation of a new ArrayList<Binding>() on top of stack
        // stack.push(new ArrayList<Binding>());
        stack.push(null);
        return stack;
    }

    /**
     * Add (remember) that a {@link Binding} was done, so that it can be undone by {@link #undoBindingsUntilPreviousMark()}
     * 
     * @param theBinding
     */
    public static void addBinding(Binding theBinding) {
        final Stack<ArrayList<Binding>> stack = stackOfBindings.get();
        ArrayList<Binding> top = stack.peek();
        if (top == null) {
            // If we had a lazy collection - let's instantiate now
            top = new ArrayList<Binding>();
            stack.pop();
            stack.push(top);
        }
        top.add(theBinding);
    }

    /**
     * Reset all bindings that have been added by {@link #addBinding(Binding)} since the last call to {@link #markBeforeAddingBindings()}.
     * 
     * @note An initial {@link #markBeforeAddingBindings()} should always be done.
     */
    public static void undoBindingsUntilPreviousMark() {
        final Stack<ArrayList<Binding>> stack = stackOfBindings.get();
        // Remove one level from the stack, then will process its content
        final ArrayList<Binding> bindings = stack.pop();
        // Process all bindings to undo
        if (bindings != null) {
            for (int i = bindings.size() - 1; i >= 0; i--) {
                final Binding toUnbind = bindings.get(i);
                toUnbind.free();
            }
        }
    }

    /**
     * @param stack
     */
    public static void undoBindingsUntilPreviousMark(Stack<ArrayList<Binding>> stack) {
        // Remove one level from the stack, then will process its content
        final ArrayList<Binding> bindings = stack.pop();
        // Process all bindings to undo
        if (bindings != null) {
            for (int i = bindings.size() - 1; i >= 0; i--) {
                final Binding toUnbind = bindings.get(i);
                toUnbind.free();
            }
        }
    }

    /**
     * This method is part of white-box testing, it should not be needed in principle.
     * Use package scope there's a class in the test tree that is in the same package.
     * 
     * @deprecated Use only from test cases
     */
    @Deprecated
    // Use only from test cases
    static void reset() {
        stackOfBindings.remove();
        // Any subsequent access such as get() will invoke method initialValue()
    }

    /**
     * This method is part of white-box testing, it should not be needed in principle.
     * Use package scope there's a class in the test tree that is in the same package.
     * 
     * @return The number of bindings that would be deunified.
     * @deprecated To be used only from test cases for low-level white-box unit testing of unification.
     */
    @Deprecated
    static int nbBindings() {
        final Stack<ArrayList<Binding>> stack = stackOfBindings.get();
        if (stack.isEmpty()) {
            return 0;
        }
        ArrayList<Binding> bindings = stack.peek();
        if (bindings == null) {
            return 0;
        }
        return bindings.size();
    }

    /**
     * This method is part of white-box testing, it should not be needed in principle.
     * Use package scope there's a class in the test tree that is in the same package.
     * 
     * @deprecated Use only from test cases
     * @return The current stack size
     */
    @Deprecated
    static int size() {
        final Stack<ArrayList<Binding>> stack = stackOfBindings.get();
        return stack.size();
    }

}
