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
package org.logic2j.core.api.model.var;

import java.util.IdentityHashMap;
import java.util.Map.Entry;

import org.logic2j.core.api.model.PartialTermVisitor;
import org.logic2j.core.api.model.TermVisitorBase;
import org.logic2j.core.api.model.exception.PrologInternalError;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.symbol.TermApi;
import org.logic2j.core.api.model.symbol.Var;

/**
 * Define the effective value of a variable, it can be either free, bound to a final term, or unified to another variable (either free,
 * bound, or chaining).
 * 
 * The properties of a {@link Binding} depend on its type according to the following table:
 * 
 * <pre>
 * Value of fields depending on the BindingType
 * -----------------------------------------------------------------------------------------------------
 * type     literalBindings               term           link                                         var
 * -----------------------------------------------------------------------------------------------------
 * FREE     null                          null(*)        null                                         the variable
 * LITERAL  bindings of the literal term  ref to term    null                                         ?
 * LINK     null                          null           ref to a Binding representing the bound var  null
 * 
 * (*) In case of a variable, there is a method in Bindings that post-assigns the "term"
 *     member to point to the variable, this allows retrieving its name for reporting
 *     bindings to the application code.
 * </pre>
 */
public class Binding {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Binding.class);
    private static final boolean isDebug = logger.isDebugEnabled();

    // See description of fields in this class' Javadoc, and on getters.

    private BindingType type;
    private Object term;
    private Bindings literalBindings;
    private Binding link;

    /**
     * Reference to the variable associated to this binding in the "owning" Struct.
     * Used for reporting (display) and extracting the "original" variable name when extracting solutions.
     */
    private Var var;

    private Binding() {
        // Just forbid instantiation from outside - use static factory methods instead
    }

    /**
     * Shallow copy constructor, used from peer class {@link Bindings}.
     * 
     * @param originalToCopy
     */
    Binding(Binding originalToCopy) {
        this.type = originalToCopy.type;
        this.term = originalToCopy.term;
        this.literalBindings = originalToCopy.literalBindings;
        this.link = originalToCopy.link;
        this.var = originalToCopy.var;
    }

    public static Binding createFreeBinding() {
        final Binding instance = new Binding();
        instance.type = BindingType.FREE;
        // The other fields will remain null by default
        return instance;
    }
    
    /**
     * Factory method to create one "fake" binding to a literal.
     * @note Maybe make it a constructor.
     * 
     * @param theLiteral
     * @param theLiteralBindings
     * @return This is used to return a pair (Term, Bindings) where needed.
     */
    // TODO assess if needed - used only once
    public static Binding createLiteralBinding(Object theLiteral, Bindings theLiteralBindings) {
        final Binding instance = new Binding();
        instance.type = BindingType.LITERAL;
        instance.term = theLiteral;
        instance.literalBindings = theLiteralBindings;
        // The other fields will remain null by default
        // instance.link = null;
        // instance.var = null;
        return instance;
    }

    /**
     * Modify this {@link Binding} by associateing it to a {@link Term}, may be a literal or another variable.
     * 
     * @param theTerm
     * @param theBindings When theTerm is a literal, here are its current value bindings
     * @return true if a binding was done, false otherwise. Caller needs to know if future un-binding will be needed.
     */
    public boolean bindTo(Object theTerm, Bindings theBindings) {
        if (!isFree()) {
            throw new PrologNonSpecificError("Cannot overwrite a non-free Binding! Was trying to bind existing " + this + " to " + theTerm + " with " + theBindings + " ");
        }
        if (theTerm instanceof Var && !((Var) theTerm).isAnonymous()) {
            // Bind Var -> Var, see description at top of class
            final Var other = (Var) theTerm;
            // We have to follow the links, otherwise we might be creating loops.
            // See test case org.logic2j.core.functional.BugRegressionTest.infiniteLoopWhenUnifying2Vars()
            // Here, in case we are binding Y to X, and X->Y, we should avoid it...
            final Binding targetBinding = other.bindingWithin(theBindings).followLinks();
            if (targetBinding == this) {
                // Don't bind onto oneself this creates a loop!
                return false;
            }
            this.type = BindingType.LINK;
            this.term = null;
            this.literalBindings = null;
            this.link = targetBinding;
        } else {
            this.type = BindingType.LITERAL;
            this.term = theTerm;
            this.literalBindings = theBindings;
            this.link = null;
        }
        // Bound OK
        return true;
    }

    /**
     * Modify this Binding's to link it to a target binding. It must be free to do that!
     * @param targetBinding
     */
    private void linkTo(Binding targetBinding) {
      if (! this.isFree()) {
          throw new PrologNonSpecificError("Cannot overwrite a non-free Binding! Was trying to link existing " + this + " to " + targetBinding);
      }
      this.type = BindingType.LINK;
      this.term = null;
      this.literalBindings = null;
      this.link = targetBinding;
    }
    
    /**
     * Free the binding, i.e. revert a possibly bound variable to the {@value BindingType#FREE} state.
     */
    public void free() {
        this.type = BindingType.FREE;
        // Resetting the following fields is not functionally necessary, we could leave previous data in, but
        // let's have the fields as documented, this will also help us while debugging to not find non-understandable garbage
        this.term = null;
        this.literalBindings = null;
        this.link = null;
    }

    /**
     * Follow chains of linked bindings, or remain on this (return this) if not a {@link BindingType#LINK}.
     * 
     * @note On problem queens(11,_) this is invoked 68M times, with 59M real steps followed, and a longest chain of 13!
     * 
     * @return The last binding of a chain, or this instance if it is not {@link BindingType#LINK}. 
     *          The result is guaranteed not null, and to satisfy
     *          either condition: {@link #isFree()} or {@link #isLiteral()} but not both.
     */
    public final Binding followLinks() {
        Binding result = this;
        // Maybe we should use isLink() and getters, but let's be efficient!
        while (result.type == BindingType.LINK) {
            result = result.link;
        }
        return result;
    }

    
    // Maybe no longer needed
    public boolean sameAs(Binding that) {
      return this==that || (this.getTerm()==that.getTerm() && this.getLiteralBindings()==that.getLiteralBindings());
    }


    public Binding substituteNew2() {
      // -> final binding if var is free
      final IdentityHashMap<Var, Binding> bindingOfOriginalVar = new IdentityHashMap<Var, Binding>();
      
      // First pass: identify vars and their final bindings
      final PartialTermVisitor<Void> registrer = new TermVisitorBase<Void>() {

        @Override
        public Void visit(Var theVar, Bindings theBindings) {
          if (theVar.isAnonymous()) {
            return null;
          }
          final Binding finalBinding = theVar.bindingWithin(theBindings).followLinks();
          if (finalBinding.isFree()) {
            // return binding with original var
            bindingOfOriginalVar.put(theVar, finalBinding);
            return null;
          }
          // Is literal - will recurse
          TermApi.accept(this, finalBinding.getTerm(), finalBinding.getLiteralBindings());
          return null;
        }

        @Override
        public Void visit(Struct theStruct, Bindings theBindings) {
          // Recurse through children
          for (int i = 0; i < theStruct.getArity(); i++) {
            TermApi.accept(this, theStruct.getArg(i), theBindings);
          }
          return null;
        }
      };
      TermApi.accept(registrer, this.term, this.literalBindings);
      
      // Allocate new vars with same names are originals but will receive new indexes
      final IdentityHashMap<Var, Var> newVarsFromOld = new IdentityHashMap<Var, Var>();
      for (Entry<Var, Binding> entry : bindingOfOriginalVar.entrySet()) {
        Var oldVar = entry.getKey();
        Var newVar = new Var(oldVar.getName());
        newVarsFromOld.put(oldVar, newVar);
      }
      
      Object copy = copy(this.term, this.literalBindings, bindingOfOriginalVar, newVarsFromOld);
      
      if (copy==this.term) {
        return this;
      }
      
      // Assign indexes
      if (copy instanceof Term) {
        if (((Term)copy).getIndex()==Term.NO_INDEX) {
          TermApi.assignIndexes(copy, 0);
        }
      }
      // Create new Bindings
      Bindings resultBindings;
      
      resultBindings = new Bindings(copy);
      
      // Bind new vars to the final bindings of original free vars
      for (Entry<Var, Binding> entry : bindingOfOriginalVar.entrySet()) {
        Var oldVar = entry.getKey();
        Binding bindingOfOldVar = entry.getValue();
        Var newVar = newVarsFromOld.get(oldVar);
        newVar.bindingWithin(resultBindings).linkTo(bindingOfOldVar);
      }
      
      return Binding.createLiteralBinding(copy, resultBindings);
    }

    /**
     * @param theTerm
     * @param theBindings
     * @param theBindingOfOriginalVar
     * @param theNewVarsFromOld 
     */
    private Object copy(Object theTerm, Bindings theBindings, IdentityHashMap<Var, Binding> theBindingOfOriginalVar, IdentityHashMap<Var, Var> theNewVarsFromOld) {
      if (theBindings.isEmpty()) {
        return theTerm;
      }
      if (theTerm instanceof Var) {
        final Var v = (Var)theTerm;
        if (v.isAnonymous()) {
          return v;
        }
        Binding finalBinding = v.bindingWithin(theBindings).followLinks();
        if (finalBinding.isFree()) {
          final Var newVar = theNewVarsFromOld.get(v);
          if (newVar == null) {
            throw new PrologInternalError("Oops");
          }
          return newVar;
        }
        // Literal: recurse
        return copy(finalBinding.term, finalBinding.literalBindings, theBindingOfOriginalVar, theNewVarsFromOld);
      }
      if (theTerm instanceof Struct) {
        final Struct struct = (Struct)theTerm;
        final Object[] substArgs = new Object[struct.getArity()]; // All arguments after substitution
        boolean anyChildWasChanged = false;
        for (int i = 0; i < struct.getArity(); i++) {
            // Recurse for all children
            substArgs[i] = copy(struct.getArg(i), theBindings, theBindingOfOriginalVar, theNewVarsFromOld);
            anyChildWasChanged |= substArgs[i] != struct.getArg(i);
        }
        final Struct substitutedOrThis;
        if (anyChildWasChanged) {
            // New cloned structure
            substitutedOrThis = new Struct(struct.getName(), substArgs);
        } else {
            // Original unchanged - same reference
            substitutedOrThis = struct;
        }
        return substitutedOrThis;
      }
      // Any other object
      return theTerm;
    }

    
    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public BindingType getType() {
        return this.type;
    }

    /**
     * Reference to a bound term: for {@link BindingType#LITERAL}, this is the literal, for {@link BindingType#LINK}, it refers to the
     * {@link Term} of subclass {@link Var}.
     */
    public Object getTerm() {
        return this.term;
    }

    /**
     * When {@link #getType()} is {@link BindingType#LITERAL}, the {@link #getTerm()} is a literal {@link Struct}, and this represents the
     * {@link Bindings} storing the content of those variables.
     * 
     * @return The {@link Bindings} associated to the Term from {@link #getTerm()}.
     */
    public Bindings getLiteralBindings() {
        return this.literalBindings;
    }

    public Var getVar() {
        return this.var;
    }

    void setVar(Var theVar) {
        this.var = theVar;
    }

    public boolean isFree() {
        return this.type == BindingType.FREE;
    }

    public boolean isLiteral() {
        return this.type == BindingType.LITERAL;
    }

    // ---------------------------------------------------------------------------
    // Core java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        switch (this.type) {
        case LITERAL:
            sb.append(String.valueOf(this.term));
            if (isDebug) {
                // Java reference (hex address) when on isDebug
                sb.append('@');
                sb.append(Integer.toHexString(this.literalBindings.hashCode()));
            }
            break;
        case LINK:
            sb.append(this.var);
            sb.append("->");
            sb.append(this.link);
            break;
        case FREE:
            sb.append(this.var);
            // Indicate the free variable with the empty set character
            sb.append(":ø");
            break;
        }
        return sb.toString();
    }


}
