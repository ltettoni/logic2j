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

import org.logic2j.core.solver.listener.SolutionListener;
import org.logic2j.core.util.ReportUtils;

/**
 * One stack-frame to keep track of the goal solving state, and manage the "cut" to prune solution trees.<br/>
 * Implements logic and data for:
 * <ul>
 * <li>implementing "cut" (programmatic search tree pruning)</li>
 * <li>user cancellation (see return value from {@link SolutionListener#onSolution()}</li>
 * </ul>
 * This class manages BOTH individual stack-frames as well as the complete stack involved when solving one goal.<br/>
 * s
 * <p/>
 * 
 * The default constructor instantiates the whole trailing bindings stack with one root frame, and returns it. The constructor for other
 * stack-frames is lighter, it shares most of its parent, only redefines new node for local management of the cut.
 */
public final class GoalFrame {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GoalFrame.class);

    /**
     * Index not yet defined. Indices are 0-based.
     */
    private static final int UNDEF_INDEX = -2;

    private final GoalFrame parent; // Our root stack-frame. The boundary condition is parent==this

    // Management of the "cut" goal requires breaking pure recursion, to do that we need to track
    // the position (index=0,1,2...) of this frame relative to its sibling (those frames having the same parent).
    // When a "cut" is executed somewhere down the chain of ',' operators:
    // - the parent's cutIndex will be assignd to the child index where the cut happened
    // (e.g. for "a,b,c,!,d,e", the cutIndex of the parent ',' will be assigned to 2, the index of goal "c")
    // - then upon backtracking, all goals having the same parent whose childIndex is lower or equal
    // than the cutIndex will abort generating solutions any longer.
    private int nbChildren;
    private final int childIndex;
    private int cutIndex;

    // Becomes true when user requested to cancel solutions
    private boolean userCanceled;

    /**
     * Create a new full stack with a default size, and its initial (root) stack-frame. This constructor is called only once when a goal
     * needs to be solved.
     */
    public GoalFrame() {
        this.parent = this; // Boundary condition: loops on itself
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

        // State bindings dedicated to "cut"
        this.nbChildren = 0; // No children yet
        this.childIndex = theParent.nbChildren; // Our index within our parent goal: 0,1,...
        this.cutIndex = UNDEF_INDEX;
        this.userCanceled = false;
        theParent.nbChildren++;
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
        final StringBuilder sb = new StringBuilder(ReportUtils.shortDescription(this));
        sb.append('{');
        /*
        final int size = this.trailingBindings.size();
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
        */
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
