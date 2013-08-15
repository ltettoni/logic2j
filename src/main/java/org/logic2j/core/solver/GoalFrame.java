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
package org.logic2j.core.solver;

import java.util.ArrayList;
import java.util.Stack;

import org.logic2j.core.model.var.Binding;
import org.logic2j.core.solver.listener.SolutionListener;
import org.logic2j.core.util.ReportUtils;

/**
 * One stack-frame to keep track of the goal solving state, and manage the "cut" to prune solution trees.<br/>
 * Implements logic and data for:
 * <ul>
 * <li>remembering which variables have been bound by unification, in order to deunify before next solutions: {@link #trailingBindings}</li>
 * <li>implementing "cut" (programmatic search tree pruning)</li>
 * <li>user cancellation (see return value from {@link SolutionListener#onSolution()}</li>
 * </ul>
 * This class manages BOTH individual stack-frames as well as the complete stack involved when solving one goal.<br/>
 * s
 * <p/>
 * TODO: Why an ArrayList of Binding for trailingBindings and a Stack of integers??? Why not just a LinkedList?
 * 
 * The default constructor instantiates the whole trailing bindings stack with one root frame, and returns it. The constructor for other
 * stack-frames is lighter, it shares most of its parent, only redefines new node for local management of the cut.
 */
public final class GoalFrame {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GoalFrame.class);

    /**
     * Initial stack capacity: number of variable bindings that can be hold before the stack is automatically grown.
     */
    private static final int INITIAL_SIZE = 100;

    /**
     * Index not yet defined. Indices are 0-based.
     */
    private static final int UNDEF_INDEX = -2;

    /**
     * Remember {@link Binding}s to be de-unified.
     */
    private final ArrayList<Binding> trailingBindings;

    private final GoalFrame parent; // Our root stack-frame. The boundary condition is parent==this

    /**
     * A stack of indexes into {@link #trailingBindings} to restore bindings between successive calls to unification. Each unification must
     * call {@link #markBeforeAddingBindings()} before starting, then {@link #addBinding(Binding)}, and finally
     * {@link #undoBindingsUntilPreviousMark()}.
     */
    // TODO see if we can simplify using just a Stack of Binding
    private final Stack<Integer> bindingMarkBetweenUnify;

    // Management of the "cut" goal requires breaking pure recursion, to do that we need to track
    // the position (index=0,1,2...) of this frame relative to its sibling (those frames having the same parent).
    // When a "cut" is executed somewhere down the chain of ',' operators:
    // - the parent's cutIndex will be assignd to the child index where the cut happened
    // (e.g. for "a,b,c,!,d,e", the cutIndex of the parent ',' will be assigned to 2, the index of goal "c")
    // - then upon backtracking, all goals having the same parent whose childIndex is lower or equal
    // than the cutIndex will abort generating solutions any longer.
    private int nbChildren;
    private int childIndex;
    private int cutIndex;

    // Becomes true when user requested to cancel solutions
    private boolean userCanceled;

    /**
     * Create a new full stack with a default size, and its initial (root) stack-frame. This constructor is called only once when a goal
     * needs to be solved.
     */
    public GoalFrame() {
        this.parent = this; // Boundary condition: loops on itself
        this.trailingBindings = new ArrayList<Binding>(INITIAL_SIZE);
        this.bindingMarkBetweenUnify = new Stack<Integer>();
        this.bindingMarkBetweenUnify.ensureCapacity(INITIAL_SIZE);
        this.bindingMarkBetweenUnify.push(0); // Initial condition: assumed a mark at 0
        // State bindings dedicated to "cut"
        this.nbChildren = 0;
        this.childIndex = UNDEF_INDEX;
        this.cutIndex = UNDEF_INDEX;
        this.userCanceled = false;
    }

    /**
     * Add a new stack-frame as a child of an existing "parent".
     * 
     * @param theParent
     */
    public GoalFrame(GoalFrame theParent) {
        this.parent = theParent;
        // Our parent defined the data structures to hold our inference state, we will
        // share the same structures since they are not directly related to the management
        // of cut and user cancellation, but related to inference which is not altered
        // by goal solving boundaries
        this.trailingBindings = theParent.trailingBindings;
        this.bindingMarkBetweenUnify = theParent.bindingMarkBetweenUnify;
        // State bindings dedicated to "cut"
        this.nbChildren = 0; // No children yet
        this.childIndex = theParent.nbChildren; // Our index within our parent goal: 0,1,...
        this.cutIndex = UNDEF_INDEX;
        this.userCanceled = false;
        theParent.nbChildren++;
    }

    // ---------------------------------------------------------------------------
    // Management of the unification / deunification stack
    // ---------------------------------------------------------------------------

    public void markBeforeAddingBindings() {
        final int upperWatermark = this.trailingBindings.size();
        this.bindingMarkBetweenUnify.push(upperWatermark);
    }

    /**
     * Add (remember) that a {@link Binding} was done, so that it can be undone by {@link #undoBindingsUntilPreviousMark()}
     * 
     * @param theBinding
     */
    public void addBinding(Binding theBinding) {
        this.trailingBindings.add(theBinding);
    }

    /**
     * Reset all bindings that have been added by {@link #addBinding(Binding)} since the last call to {@link #markBeforeAddingBindings()}.
     * 
     * @note An initial {@link #markBeforeAddingBindings()} should always be done.
     */
    public void undoBindingsUntilPreviousMark() {
        int indexOfPreviousMark = this.bindingMarkBetweenUnify.pop();
        for (int i = this.trailingBindings.size() - 1; i >= indexOfPreviousMark; i--) {
            final Binding toUnbind = this.trailingBindings.remove(i);
            // TODO Is it efficient to remove() from an ArrayList?, see https://github.com/ltettoni/logic2j/issues/9
            toUnbind.free();
        }
    }

    /**
     * @return The number of bindings that would be deunified.
     * @deprecated Bo be used only from test cases for low-level white-box unit testing of unification.
     */
    @Deprecated
    public Object nbBindings() {
        return this.trailingBindings.size() - this.bindingMarkBetweenUnify.peek();
    }

    // ---------------------------------------------------------------------------
    // User cancellation request and status
    // ---------------------------------------------------------------------------

    public void raiseUserCanceled() {
        this.userCanceled = true;
    }

    public boolean isUserCanceled() {
        return this.userCanceled;
    }

    // ---------------------------------------------------------------------------
    // Management of the cut (!)
    // ---------------------------------------------------------------------------

    public void signalCut() {
        this.cutIndex = this.nbChildren - 1;
        logger.debug("!!! Executed CUT on goalFrame={}, setting cutIndex={}", this, this.cutIndex);
    }

    public boolean isCut() {
        return this.cutIndex != UNDEF_INDEX;
    }

    /**
     * @return TBD
     */
    public boolean hasCutInSiblingSubsequentGoal() {
        return this.parent.cutIndex >= this.childIndex;
    }

    // ---------------------------------------------------------------------------
    // Override top Object methods
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(ReportUtils.shortDescription(this));
        sb.append('{');
        int size = this.trailingBindings.size();
        int i = this.bindingMarkBetweenUnify.peek();
        sb.append(i);
        sb.append(':');
        while (i < size) {
            sb.append(this.trailingBindings.get(i));
            sb.append(' ');
            i++;
            sb.append(i);
            sb.append(':');
        }
        sb.append("(top) ");
        sb.append(" #chld=");
        sb.append(this.nbChildren);
        sb.append(", childIx=");
        sb.append(this.childIndex);
        sb.append(", cutIx=");
        sb.append(this.cutIndex);
        sb.append('}');
        return sb.toString();
    }

}
