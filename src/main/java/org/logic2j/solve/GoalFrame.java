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
package org.logic2j.solve;

import java.util.ArrayList;
import java.util.Stack;

import org.logic2j.model.var.Binding;
import org.logic2j.util.ReportUtils;

/**
 * One stack frame to track goal solving (both unification trailing bindings, and 
 * the "cut" across solving goals and sub-goals).
 * <ul>
 * <li>trailing variables bound by unification, in order to deunify</li>
 * <li>goal solving state to allow "cut"</li>
 * <li>user cancellation</li>
 * </ul>
 * The default constructor instantiates the whole trailing bindings stack, and 
 * returns a first node to hold inference and unificaiton state. 
 * The constructor for children is lighter, it shares most of its parent, only
 * redefines new node for local management of the cut.
 * 
 */
public final class GoalFrame {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GoalFrame.class);

  /**
   * Initial stack capacity: number of variable bindings that can be hold before
   * the stack is automatically grown.
   */
  private static final int INITIAL_SIZE = 100;

  /**
   * Index not yet defined. Indices are 0-based.
   */
  private static final int UNDEF_INDEX = -2;

  private final GoalFrame parent; // Boundary condition of top: parent==this

  private final ArrayList<Binding> trailingBindings;

  /**
   * A stack of indexes into {@link #trailingBindings} to restore bindings between
   * successive calls to unification. Each unification must call {@link #markForNextBindings()}
   * before starting, then {@link #addBinding(Binding)}, and finally {@link #clearBindingsToMark()}.
   */
  private final Stack<Integer> bindingMarkBetweenUnify;

  // Management of the "cut" goal requires breaking pure recursion, to do that we need to track
  // the position (index=0,1,2...) of this frame relative to its sibling (those frames having the same parent).
  // When a "cut" is executed somewhere down the chain of ',' operators:
  // - the parent's cutIndex will be assignd to the child index where the cut happened
  //   (e.g. for "a,b,c,!,d,e", the cutIndex of the parent ',' will be assigned to 2, the index of goal "c")
  // - then upon backgracking, all goals having the same parent whose childIndex is lower or equal
  //   than the cutIndex will stop generating solutions any longer.
  private int nbChildren;
  private int childIndex;
  private int cutIndex;

  // Becomes true when user requested to cancel solutions
  private boolean userCanceled;

  /**
   * Create a new full stack with a default size, and its root frame.
   * This constructor is called only once when a goal needs to be solved.
   */
  public GoalFrame() {
    this.parent = this; // Boundary condition: loops on itself
    this.trailingBindings = new ArrayList<Binding>(INITIAL_SIZE);
    this.bindingMarkBetweenUnify = new Stack<Integer>();
    this.bindingMarkBetweenUnify.ensureCapacity(INITIAL_SIZE);
    this.bindingMarkBetweenUnify.push(0);
    // State bindings dedicated to "cut"
    this.nbChildren = 0;
    this.childIndex = UNDEF_INDEX;
    this.cutIndex = UNDEF_INDEX;
    this.userCanceled = false;
  }

  /**
   * Add a new stack frame on top (as a child of) an existing "parent".
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

  //---------------------------------------------------------------------------
  // Management of the unification / deunification stack
  //---------------------------------------------------------------------------

  public void markForNextBindings() {
    final int upperMark = this.trailingBindings.size();
    this.bindingMarkBetweenUnify.push(upperMark);
  }

  /**
   * Add (remember) that a binding was done, so that it can be undone by {@link #clearBindingsToMark()}
   * @param theBinding
   */
  public void addBinding(Binding theBinding) {
    this.trailingBindings.add(theBinding);
  }

  /**
   * Reset all bindings that have been added by {@link #addBinding(Binding)} after the last call to
   * {@link #markForNextBindings()}
   */
  public void clearBindingsToMark() {
    int indexOfPreviousMark = this.bindingMarkBetweenUnify.pop();
    for (int i = this.trailingBindings.size() - 1; i >= indexOfPreviousMark; i--) {
      final Binding binding = this.trailingBindings.remove(i); 
      // TODO Is it efficient to remove() from an ArrayList?, see https://github.com/ltettoni/logic2j/issues/9
      binding.free();
    }
  }

  /**
   * @return The number of bindings that would be deunified. 
   * @deprecated Use only from test cases.
   */
  @Deprecated
  public Object nbBindings() {
    return this.trailingBindings.size() - this.bindingMarkBetweenUnify.peek();
  }

  //---------------------------------------------------------------------------
  // User cancellation request and status
  //---------------------------------------------------------------------------

  public void raiseUserCanceled() {
    this.userCanceled = true;
  }

  public boolean isUserCanceled() {
    return this.userCanceled;
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public boolean isCut() {
    return this.cutIndex != UNDEF_INDEX;
  }

  public void signalCut() {
    this.cutIndex = this.nbChildren - 1;
    logger.debug("!!! Executed CUT on goalFrame={}, setting cutIndex={}", this, this.cutIndex);
  }

  /**
   * @return TBD
   */
  public boolean hasCutInSiblingSubsequentGoal() {
    return this.parent.cutIndex >= this.childIndex;
  }

  //---------------------------------------------------------------------------
  // Override top Object methods
  //---------------------------------------------------------------------------

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
