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

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.logic2j.model.BaseTermVisitor;
import org.logic2j.model.InvalidTermException;
import org.logic2j.model.TermVisitor;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.symbol.Var;

/**
 * Variables associated to a Struct.
 * TODO Improve performance: instantiation of {@link VarBindings} from a Struct in a theory.
 * Find a better way than runtime instantiation.
 */
public class VarBindings {
  private static final TermApi TERM_API = new TermApi();

  private final Term referer; // The Term, mostly a Struct, whose Var indexes refer to this VarBindings
  private Binding[] bindings;

  /**
   * Determine how free (unbound) variables will be represented in resulting bindings
   * returned by {@link VarBindings#explicitBindings(FreeVarBehaviour)}.
   *
   */
  public enum FreeVarBehaviour {
    /**
     * Free variables will not be included in result bindings. Asking the value of 
     * a variable that is not bound to a literal Term is likely to throw an Exception.
     */
    SKIPPED,

    /**
     * Free variables will be represented by the presence of a Map {@link Entry} with the
     * variable name as the key, and a null value. You are required to use {@link Map#containsKey(Object)}
     * to identify this case, since {@link Map#get(Object)} returning null won't allow you to distinguish between
     * a free variable or asking the value of an undefined variable.
     */
    NULL_ENTRY,

    /**
     * 
     */
    FREE_NOT_SELF,

    /**
     * Free variables will be reported with their terminal value, this means a free X
     * will be represented by a mapping "X"->"X". A variable X bound to a free variable Y will
     * be represented as "X"->"Y".
     */
    FREE
  }

  /**
   * Create VarBindings for a given Term, ususally a Struct.
   * @param theTerm
   */
  public VarBindings(Term theTerm) {
    this.referer = theTerm;
    final short index = theTerm.getIndex();
    if (index == Term.NO_INDEX) {
      throw new InvalidTermException("Cannot create VarBindings for uninitialized term " + theTerm);
    }
    final int nbVars;
    if (theTerm instanceof Var) {
      if (((Var) theTerm).isAnonymous()) {
        nbVars = 0;
      } else {
        nbVars = 1;
      }
    } else {
      nbVars = index;
    }
    this.bindings = new Binding[nbVars];
    for (int i = 0; i < nbVars; i++) {
      final int varIndex = i;
      this.bindings[varIndex] = new Binding();
      final TermVisitor<Void> setRefToVar = new BaseTermVisitor<Void>() {

        @SuppressWarnings("synthetic-access")
        @Override
        public Void visit(Var theVar) {
          if (theVar.getIndex() == varIndex) {
            VarBindings.this.bindings[varIndex].setVar(theVar);
          }
          return null;
        }

      };
      theTerm.accept(setRefToVar);
    }
  }

  /**
   * Copy (cloning) constructor, faster than the one that builds by traversing a term.
   * @param theOriginal
   */
  public VarBindings(VarBindings theOriginal) {
    this.referer = theOriginal.referer;
    int nbVars = theOriginal.bindings.length;
    this.bindings = new Binding[nbVars];
    // All bindings need cloning
    for (int i = 0; i < nbVars; i++) {
      this.bindings[i] = theOriginal.bindings[i].cloneIt();
    }
  }

  /**
   * @return true when this bindings contains no elements (usually, a Struct has no variables, so this
   * {@link VarBindings} is empty).
   */
  public boolean isEmpty() {
    return this.bindings.length == 0;
  }

  /**
   * @param theVariableName
   * @param theBehaviour
   * @return The explicit substituted binding for a given variable.
   */
  public Term explicitBinding(String theVariableName, FreeVarBehaviour theBehaviour) {
    final Var var = this.referer.findVar(theVariableName);
    if (var == null) {
      throw new InvalidTermException("No variable named \"" + theVariableName + "\" in " + this.referer);
    }
    final Term substitute = TERM_API.substitute(var, this, null);
    return substitute;
  }

  /**
   * @param theBehaviour How to represent free (non-ground) variables
   * @return All variable bindings resolved, with behaviour as specified for free vars.
   */
  public Map<String, Term> explicitBindings(FreeVarBehaviour theBehaviour) {
    final Map<String, Term> result = new TreeMap<String, Term>();
    final IdentityHashMap<Binding, Var> bindingToVar = new IdentityHashMap<Binding, Var>();
    for (Binding binding : this.bindings) {
      Binding finalBinding = binding;
      while (finalBinding.isVar()) {
        finalBinding = finalBinding.getLink();
      }
      bindingToVar.put(finalBinding, binding.getVar());
    }
    for (Binding binding : this.bindings) {
      if (binding.getVar() == null) {
        throw new IllegalStateException("VarBindings not properly initialized: Binding " + binding
            + " does not refer to Var (null)");
      }
      final String varName = binding.getVar().getName();
      while (binding.isVar()) {
        binding = binding.getLink();
      }
      final Term boundTerm = binding.getTerm();
      switch (binding.getType()) {
        case LIT:
          if (varName == null) {
            throw new IllegalStateException("Cannot assign null (undefined) var, not all variables of " + this
                + " are referenced from Term " + this.referer + ", binding " + binding + " can't be assigned a variable name");
          }
          final Term substitute = TERM_API.substitute(boundTerm, binding.getLiteralBindings(), bindingToVar);
          result.put(varName, substitute);
          break;
        case FREE:
          switch (theBehaviour) {
            case SKIPPED:
              // Nothing added to the resulting bindings: no Map entry (no key, no value)
              break;
            case NULL_ENTRY:
              // Add one entry with null value
              result.put(varName, null);
              break;
            case FREE_NOT_SELF:
              if (binding.getVar() == null) {
                throw new IllegalStateException(
                    "Oops the binding of a variable does not refer to it's Term var, so we don't have the name of the variable!");
              }
              if (!binding.getVar().getName().equals(varName)) {
                // Avoid reporting "X=null" for free variables or "X=X" as a binding...
                result.put(varName, binding.getVar());
              }
              break;
            case FREE:
              if (binding.getVar() == null) {
                throw new IllegalStateException(
                    "Oops the binding of a variable does not refer to it's Term var, so we don't have the name of the variable!");
              }
              result.put(varName, binding.getVar());
              break;
          }
          //          if (boundTerm != null) {
          ////          if (boundTerm != null && !((Var) boundTerm).getName().equals(varName)) {
          //            // Avoid reporting "X=null" for free variables or "X=X" as a binding...
          //            result.put(varName, boundTerm);
          //          }
          break;
        case VAR:
          throw new IllegalStateException("Should not happen");
      }
    }
    return result;
  }

  /**
   * Find the local bindings corresponding to one of the variables of the
   * Struct referred to by this VarBindings.
   * @param theVar
   * @return null when not found
   */
  public VarBindings findBindings(Var theVar) {
    int index = 0;
    for (Binding binding : this.bindings) {
      if (binding.getVar() == theVar && index == theVar.getIndex()) {
        return this;
      }
      index++;
    }
    // Not found: search deeper through bindings
    for (Binding binding : this.bindings) {
      if (binding.getType() == BindingType.LIT) {
        VarBindings foundDeeper = binding.getLiteralBindings().findBindings(theVar);
        if (foundDeeper != null) {
          return foundDeeper;
        }
      }
    }
    // Not found
    return null;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return this.getClass().getSimpleName() + "(empty)";
    }
    return this.getClass().getSimpleName() + Arrays.asList(this.bindings);
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  /**
   * @return The number of bindings.
   */
  public int nbBindings() {
    return this.bindings.length;
  }

  /**
   * @param theIndex
   * @return The {@link Binding} at theIndex
   */
  public Binding getBinding(short theIndex) {
    return this.bindings[theIndex];
  }

}
