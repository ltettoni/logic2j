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

import org.logic2j.core.api.model.var.Binding;

/**
 * Prototype implementation to manage a trail of {@link Binding}s so that they can be undone.
 * So far we are using a {@link ThreadLocal} variable to avoid having to pass a context object around - this may have a performance impact
 * although not proven.
 * TODO Explain the data structure used.
 */
public final class BindingTrail {

    public static class Milestone {
        Binding binding;
        Milestone previous;

    }

    /**
     * Keep the last Milestone of our trail in a {@link ThreadLocal} so that we don't have to pass its reference as argument
     * to all methods, everywhere. This reveals to have very little performance impact.
     */
    private static final ThreadLocal<Milestone> lastMilestone = new ThreadLocal<Milestone>() {

        @Override
        protected Milestone initialValue() {
            // Path initially undefined
            return null;
        }

    };

    /**
     * Register a valid new Milestone on our trail, but with empty content.
     * 
     * @return
     */
    public static Milestone markBeforeAddingBindings() {
        final Milestone last = lastMilestone.get();
        final Milestone newed = new Milestone();
        newed.previous = last;
        lastMilestone.set(newed);
        return newed;
    }

    /**
     * Add (remember) that a {@link Binding} was done, so that it can be undone by {@link #undoBindingsUntilPreviousMark()}
     * 
     * @param theBinding
     */
    public static void addBinding(Binding theBinding) {
        final Milestone current = lastMilestone.get();
        if (current.binding == null) {
            // We had a null on top of stack, replace by the first Binding that needs later undoing
            current.binding = theBinding;
        } else {
            theBinding.linkNext(current.binding);
            current.binding = theBinding;
        }
    }

    /**
     * Reset all bindings that have been added by {@link #addBinding(Binding)} since the last call to {@link #markBeforeAddingBindings()}.
     * 
     * @note An initial {@link #markBeforeAddingBindings()} should always be done.
     */
    public static void undoBindingsUntilPreviousMark() {
        final Milestone current = lastMilestone.get();
        undoBindingsUntilPreviousMark(current);
    }

    public static void undoBindingsUntilPreviousMark(Milestone current) {
        // Remove one level from the stack, then will process its content
        if (current != null) {
            for (Binding iter = current.binding; iter != null; iter = iter.nextToUnbind()) {
                iter.free();
                // To be really 100% safe, we could consider resetting the nextToUnbind() link to null - but this appears not necessary
            }
            lastMilestone.set(current.previous);
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
        lastMilestone.remove();
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
        final Milestone current = lastMilestone.get();
        if (current == null || current.binding == null) {
            return 0;
        }
        int counter = 0;
        for (Binding iter = current.binding; iter != null; iter = iter.nextToUnbind()) {
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
        int counter = 0;
        for (Milestone current = lastMilestone.get(); current != null; current = current.previous) {
            counter++;
        }
        return counter;
    }

}
