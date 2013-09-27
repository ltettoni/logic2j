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
 * This class manages the deunification "trail" to undo what was previously unified while backtracking to other solutions.
 * TODO Explain the data structure used.
 */
public final class BindingTrail {

    /**
     * The number of elements in the {@link BindingTrail} to allocate at one time.
     * 1000 looks like a reasonable value, actually solves all test cases in one chunk.
     */
    private static final int BINDING_STACK_CHUNK = 1000;

    private static final int TRAIL_STACK_CHUNK = 1000;

    public static class StepInfo {

        /**
         * The "trail" is a stack implemented with a auto-extending array.
         */
        private int[] trailStack;
        private int trailSize;
        private int trailTop;

        /**
         * The "bindings" is a stack implemented with a auto-extending array.
         */
        private Binding[] bindingStack;
        private int bindingSize;
        private int bindingTop;

    }

    /**
     * Keep the last StepInfo of our trail in a {@link ThreadLocal} so that we don't have to pass its reference as argument
     * to all methods, everywhere. This reveals to have very little performance impact.
     * Being a ThreadLocal variable, we won't have any issue of concurrent access, so no synchronization needed!
     */
    private static final ThreadLocal<StepInfo> stepInfoOfThisThread = new ThreadLocal<StepInfo>() {

        @Override
        protected StepInfo initialValue() {
            final StepInfo si = new StepInfo();
            // Trail stack
            si.trailSize = TRAIL_STACK_CHUNK;
            si.trailStack = new int[si.trailSize];
            si.trailTop = -1; // Be ready for the first "markBeforeAddingBindings()"
            // Bindings stack
            si.bindingSize = BINDING_STACK_CHUNK;
            si.bindingStack = new Binding[si.bindingSize];
            si.bindingTop = -1; // We will be pushing just after this index
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
        final int top = ++current.trailTop;
        if (top >= current.trailSize) {
            // OOps, need to reallocate more stack
            current.trailSize += TRAIL_STACK_CHUNK;
            current.trailStack = Arrays.copyOf(current.trailStack, current.trailSize);
        }
        current.trailStack[top] = current.bindingTop;
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
     * @param current
     * @param binding1
     */
    public static void addBinding(StepInfo current, Binding theBinding) {
        final int top = ++current.bindingTop;
        if (top >= current.bindingSize) {
            // OOps, need to reallocate more stack
            current.bindingSize += BINDING_STACK_CHUNK;
            current.bindingStack = Arrays.copyOf(current.bindingStack, current.bindingSize);
        }
        current.bindingStack[top] = theBinding;
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
        final int freeBindingsUntil = current.trailStack[current.trailTop];
        for (int i = current.bindingTop; i > freeBindingsUntil; i--) {
            final Binding binding = current.bindingStack[i];
            binding.free();
        }
        current.bindingTop = freeBindingsUntil;
        current.trailTop--;
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
        if (current.trailTop < 0) {
            return 0;
        }
        final int bindingStartedAt = current.trailStack[current.trailTop];
        return current.bindingTop - bindingStartedAt;
    }

    /**
     * This method is part of white-box testing, it should not be needed in principle.
     * Use package scope there's a class in the test tree that is in the same package.
     * 
     * @deprecated Use only from test cases
     * @return The current stack bindingSize
     */
    @Deprecated
    static int size() {
        final StepInfo current = stepInfoOfThisThread.get();
        return current.trailTop + 1;
    }

}
