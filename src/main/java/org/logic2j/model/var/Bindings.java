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
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.symbol.Var;

/**
 * Store the actual values of all variables of a {@link Term}, as a list of {@link Binding}s, 
 * one per {@link Var}iable found within it.<br/>
 * Usually the {@link Term} is a {@link Struct} that represents a goal to be demonstrated or 
 * unified. The Term referring to this object is called the "referrer".<br/>
 * 
 * TODO Improve performance: instantiation of {@link #Bindings(Term)}. 
 * Find a better way than runtime instantiation.
 */
public class Bindings {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Bindings.class);
  private static boolean isDebug = logger.isDebugEnabled();

  private static final TermApi TERM_API = new TermApi();

  /**
   * The Term, usually a {@link Struct}, whose {@link Var}iables refer to this Bindings
   * through their indexes.
   */
  private final Term referrer;

  /**
   * All {@link Binding}s, one per instance of {@link Var}iable.
   * There are as many bindings as the distinct number of variables in 
   * the referrer Term, i.e. the length of bindings equals the maximum
   * of all indexes in all {@link Var}s of the referrer, plus one.
   * See also {@link Var#getIndex()}.
   * This array is never null, but may be empty (length=0) when the
   * referrer Term does not contain any {@link Var}iable.
   */
  private Binding[] bindings;

  /**
   * Determine how free (unbound) variables will be represented in resulting bindings
   * returned by {@link Bindings#explicitBindings(FreeVarBehaviour)}.
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
     * TBD
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
   * {@link TermVisitor} used to assign a reference to the original {@link Var}iable into a {@link Binding}.
   */
  private static class SetVarInBindingVisitor extends BaseTermVisitor<Var> {

    private Binding binding;
    private int index;

    SetVarInBindingVisitor(Binding theBinding, int theIndex) {
      this.binding = theBinding;
      this.index = theIndex;
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public Var visit(Var theVar) {
      if (theVar.getIndex() == index) {
        binding.setVar(theVar);
        // Returning non-null will stop useless recursion
        return theVar;
      }
      return null;
    }

  }

  /**
   * Instantiate a Bindings to hold all variables of a given {@link Term}, named
   * further the "referrer", which is ususally a {@link Struct}.
   * @param theReferrer The Term whose {@link Var}iables refer to this object.
   * @see Bindings#getReferrer() to further access theTerm
   */
  public Bindings(Term theReferrer) {
    // Check arguments
    //
    // Note: this constructor should be called only with Var or Struct as arguments, but we don't check this. Should we?
    final short index = theReferrer.getIndex();
    if (index == Term.NO_INDEX) {
      throw new InvalidTermException("Cannot create Bindings for uninitialized Term " + theReferrer);
    }
    this.referrer = theReferrer;
    // Determine number of distinct variables
    final int nbVars;
    if (theReferrer instanceof Var) {
      if (((Var) theReferrer).isAnonymous()) {
        nbVars = 0;
      } else {
        nbVars = 1;
      }
    } else {
      // Will be a Struct; in that case the index is the number of bindings
      nbVars = index;
    }
    //
    // Allocate and initialize all bindings
    //
    this.bindings = new Binding[nbVars];
    for (int i = 0; i < nbVars; i++) {
      final int varIndex = i; // Need a final var for visitor subclass
      final Binding binding = new Binding();
      this.bindings[varIndex] = binding;
      // Assign Binding.var field 
      // TODO This is costly see https://github.com/ltettoni/logic2j/issues/26
      theReferrer.accept(new SetVarInBindingVisitor(binding, varIndex));
    }
  }

  /**
   * Copy (cloning) constructor, used for efficiency since the original one
   * needs to a complete traversal of the term.<br/>
   * The referrer of the new Bindings is the same as theOriginal.
   * @param theOriginal The one to clone from, remains intact.
   */
  public Bindings(Bindings theOriginal) {
    this.referrer = theOriginal.referrer;
    int nbVars = theOriginal.bindings.length;
    this.bindings = new Binding[nbVars];
    // All bindings need cloning
    for (int i = 0; i < nbVars; i++) {
      this.bindings[i] = theOriginal.bindings[i].cloneIt();
    }
  }

  /**
   * @return true when this bindings contains no elements (usually, a Struct has no variables, so this
   * {@link Bindings} is empty).
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
    final Var var = this.referrer.findVar(theVariableName);
    if (var == null) {
      throw new InvalidTermException("No variable named \"" + theVariableName + "\" in " + this.referrer);
    }
    final Term substitute = TERM_API.substitute(var, this, null);
    return substitute;
  }

  /**
   * @param theBehaviour How to represent free (non-ground) variables
   * @return All variable bindings resolved, with behaviour as specified for free bindings.
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
        throw new IllegalStateException("Bindings not properly initialized: Binding " + binding
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
                + " are referenced from Term " + this.referrer + ", binding " + binding + " can't be assigned a variable name");
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
   * Struct referred to by this Bindings.
   * @param theVar
   * @return null when not found
   */
  public Bindings findBindings(Var theVar) {
    int index = 0;
    for (Binding binding : this.bindings) {
      // FIXEM: dubious use of == instead of static equality
      if (binding.getVar() == theVar && index == theVar.getIndex()) {
        return this;
      }
      index++;
    }
    // Not found: search deeper through bindings
    for (Binding binding : this.bindings) {
      if (binding.getType() == BindingType.LIT) {
        Bindings foundDeeper = binding.getLiteralBindings().findBindings(theVar);
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
    final String address = isDebug ? ('@' + Integer.toHexString(super.hashCode())) : "";
    if (isEmpty()) {
      return this.getClass().getSimpleName() + address + "(empty)";
    }
    return this.getClass().getSimpleName() + address + Arrays.asList(this.bindings);
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

  /**
   * @return The Term whose variable values are held in this structure, 
   * actually the one that was provided to the consturctor {@link #Bindings(Term)}.
   */
  public Term getReferrer() {
    return this.referrer;
  }
  
}
