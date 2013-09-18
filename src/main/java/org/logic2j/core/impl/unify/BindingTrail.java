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

import java.util.Arrays;

import org.logic2j.core.api.model.var.Binding;

/**
 * Prototype implementation to manage a trail of {@link Binding}s so that they can be undone.
 * So far we are using a {@link ThreadLocal} variable to avoid having to pass a context object around - this may have a performance impact
 * although not proven.
 * TODO Explain the data structure used.
 */
public final class BindingTrail {

    /**
     * The number of elements in the {@link BindingTrail} to allocate at one time.
     * 1000 looks like a reasonable value, actually solves all test cases in one chunk.
     */
    private static final int BINDING_TRAIL_CHUNK = 1000;

    public static class StepInfo {
        /**
         * Our "stack", will auto-grow but never shrink.
         */
        private Binding[] bindingStack;
        private int size;
        private int top;

    }

    /**
     * Keep the last StepInfo of our trail in a {@link ThreadLocal} so that we don't have to pass its reference as argument
     * to all methods, everywhere. This reveals to have very little performance impact.
     */
    private static final ThreadLocal<StepInfo> stepInfoOfThisThread = new ThreadLocal<StepInfo>() {

        @Override
        protected StepInfo initialValue() {
            final StepInfo si = new StepInfo();
            si.size = BINDING_TRAIL_CHUNK;
            si.bindingStack = new Binding[si.size];
            si.top = -1;
            return si;
        }

    };

    /**
     * Register a valid new StepInfo on our trail, but with empty content.
     * 
     * @return
     */
    public static StepInfo markBeforeAddingBindings() {
        final StepInfo current = stepInfoOfThisThread.get();
        final int top = ++current.top;
        if (top >= current.size) {
            // OOps, need to reallocate more stack size
            final int newSize = current.size + BINDING_TRAIL_CHUNK;
            current.bindingStack = Arrays.copyOf(current.bindingStack, newSize);
            current.size = newSize;
        }
        current.bindingStack[top] = null;
        return current;
    }

    /**
     * Add (remember) that a {@link Binding} was done, so that it can be undone by {@link #undoBindingsUntilPreviousMark()}
     * 
     * @param theBinding
     */
    public static void addBinding(Binding theBinding) {
        final StepInfo current = stepInfoOfThisThread.get();
        addBinding(current, theBinding);
    }

    /**
     * @param stepInfo
     * @param binding1
     */
    public static void addBinding(StepInfo stepInfo, Binding theBinding) {
        final int top = stepInfo.top;
        final Binding topBinding = stepInfo.bindingStack[top];
        if (topBinding != null) {
            // We had an occupied on top of stack, replace by the first Binding that needs later undoing
            theBinding.linkNext(topBinding);
        }
        stepInfo.bindingStack[top] = theBinding;
    }

    /**
     * Reset all bindings that have been added by {@link #addBinding(Binding)} since the last call to {@link #markBeforeAddingBindings()}.
     * 
     * @note An initial {@link #markBeforeAddingBindings()} should always be done.
     */
    public static void undoBindingsUntilPreviousMark() {
        final StepInfo current = stepInfoOfThisThread.get();
        undoBindingsUntilPreviousMark(current);
    }

    public static void undoBindingsUntilPreviousMark(StepInfo current) {
        // Remove one level from the stack, then will process its content
        if (current.top >= 0) {
            for (Binding iter = current.bindingStack[current.top]; iter != null; iter = iter.nextToUnbind()) {
                iter.free();
                // To be really 100% safe, we could consider resetting the nextToUnbind() link to null - but this appears not necessary
            }
            current.top--;
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
        stepInfoOfThisThread.remove();
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
        final StepInfo current = stepInfoOfThisThread.get();
        if (current.top < 0 || current.bindingStack[current.top] == null) {
            return 0;
        }
        int counter = 0;
        for (Binding iter = current.bindingStack[current.top]; iter != null; iter = iter.nextToUnbind()) {
            counter++;
        }
        return counter;
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
        final StepInfo current = stepInfoOfThisThread.get();
        return current.top + 1;
    }

}
