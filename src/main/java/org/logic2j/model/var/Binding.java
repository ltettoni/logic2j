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
package org.logic2j.model.var;

import org.logic2j.model.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.Var;
import org.logic2j.solve.GoalFrame;

/**
 * Define the effective value of a variable, it can be either free,
 * bound to a final term, or unified to another variable (either free, bound, or chaining).
 * 
 * The properties of a {@link Binding} depend on its type according to the following table:
 * <pre>
 * Value of fields depending on the BindingType
 * -----------------------------------------------------------------------------------------------------
 * type     literalVarBindings            term           link
 * -----------------------------------------------------------------------------------------------------
 * FREE     null                          null(*)        null
 * LIT      vars of the literal term      ref to term    null
 * VAR      null                          null           ref to a Binding
 *                                                       representing the bound var
 *
 * (*) In case of a variable, there is a method in VarBindings that post-assigns the "term" 
 *     member to point to the variable, this allows retrieving its name for reporting 
 *     bindings to the application code.
 * </pre>
 * 
 */
public class Binding implements Cloneable {

  // See description of fields in this class' Javadoc, and on getters.

  private BindingType type;
  private Term term;
  private VarBindings literalVarBindings;
  private Binding link;

  // Ref to the variable associated to this binding in the "owning" Struct. 
  // Note that this is the ref to the last instance of the var in case there are more than one.
  private Var var;

  /**
   * New binding, for a (yet) free variable.
   */
  public Binding() {
    super();
    this.type = BindingType.FREE;
  }

  /**
   * Create one "fake" binding to a literal.
   * @param theLiteral
   * @param theLiteralBindings
   * @return This is used to return a pair (Term, VarBindings) where needed.
   */
  public static Binding createLiteralBinding(Term theLiteral, VarBindings theLiteralBindings) {
    final Binding binding = new Binding();
    binding.type = BindingType.LIT;
    binding.link = null;
    binding.term = theLiteral;
    binding.literalVarBindings = theLiteralBindings;
    binding.var = null;
    return binding;
  }

  public Binding cloneIt() {
    try {
      return (Binding) this.clone();
    } catch (CloneNotSupportedException e) {
      throw new InvalidTermException("Failed cloning: " + e);
    }
  }

  /**
   * Bind a variable to a {@link Term}, may be a literal or another variable.
   * @param theTerm
   * @param theGoalFrame 
   */
  public void bindTo(Term theTerm, VarBindings theFrame, GoalFrame theGoalFrame) {
    if (!isFree()) {
      throw new IllegalStateException("Should not bind a non-free Binding!");
    }
    if (theTerm instanceof Var && !((Var) theTerm).isAnonymous()) {
      // Bind Var -> Var, see description at top of class
      final Var other = (Var) theTerm;
      final Binding targetBinding = other.derefToBinding(theFrame);
      if (targetBinding == this) {
        return;
      }
      this.type = BindingType.VAR;
      this.term = null;
      this.literalVarBindings = null;
      this.link = targetBinding;
    } else {
      this.type = BindingType.LIT;
      this.term = theTerm;
      this.literalVarBindings = theFrame;
      this.link = null;
    }
    // Remember (if asked for)
    if (theGoalFrame != null) {
      theGoalFrame.addBinding(this);
    }
  }

  public boolean isVar() {
    return this.type == BindingType.VAR;
  }

  public boolean isLiteral() {
    return this.type == BindingType.LIT;
  }

  /**
   * Free the binding, i.e. revert a possibly bound variable to the {@value BindingType#FREE} state.
   */
  public void free() {
    this.type = BindingType.FREE;
    // The following is not functionally necessary
    this.term = null;
    this.literalVarBindings = null;
    this.link = null;
  }

  @Override
  public String toString() {
    final String str;
    switch (this.type) {
      case LIT:
        str = "->" + this.term.toString();
        break;
      case VAR:
        str = "->" + this.link;
        break;
      default:
        str = "";
        break;
    }
    return this.type + str;
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public BindingType getType() {
    return this.type;
  }

  /**
   * When {@link #getType()} is {@link BindingType#LIT}, the {@link #getTerm()} is a literal 
   * {@link Struct}, and this represents the {@link VarBindings} storing the content of those variables.
   * @return The {@link VarBindings} associated to the Term from {@link #getTerm()}.
   */
  public VarBindings getLiteralVarBindings() {
    return this.literalVarBindings;
  }

  /**
   * Reference to a bound term: for {@link BindingType#LIT}, this is the literal,
   * for {@link BindingType#VAR}, it refers to the {@link Term} of subclass {@link Var}.
   */
  public Term getTerm() {
    return this.term;
  }

  public Binding getLink() {
    return this.link;
  }

  public Var getVar() {
    return this.var;
  }

  public void setVar(Var theVar) {
    this.var = theVar;
  }

  public boolean isFree() {
    return this.type == BindingType.FREE;
  }
}
