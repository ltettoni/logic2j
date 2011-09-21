package org.logic2j.solve;

import java.util.Map;

import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.var.VarBindings;
import org.logic2j.model.var.VarBindings.FreeVarBehaviour;

/**
 * Describes one of the solution(s) to a goal; this includes the resolved {@link Term} (with all
 * bound variables substituted to their actual values - only free variables remaining), and
 * all variable bindings exposed.
 * 
 */
public class Solution {
  private static final TermApi TERM_API = new TermApi();

  // The resolved goal with all bound variables resovled to their bound terms
  // If any variable remains they are free.
  private final Term solution;

  // The bindings, per variable
  private final Map<String, Term> bindings;

  /**
   * Build a solution for the current variable bindings. This will
   * calculate the substituted value of bound variables, i.e. "denormalize" the result
   * and store all bindings as explicit denormalized terms.
   * @param theGoal
   * @param theVars
   */
  public Solution(Term theGoal, VarBindings theVars) {
    this.solution = TERM_API.substitute(theGoal, theVars, null);
    this.bindings = theVars.explicitBindings(FreeVarBehaviour.NULL_ENTRY);
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  /**
   * @return the solution
   */
  public Term getSolution() {
    return this.solution;
  }

  /**
   * @return the bindings
   */
  public Map<String, Term> getBindings() {
    return this.bindings;
  }

  //---------------------------------------------------------------------------
  // Methods
  //---------------------------------------------------------------------------

  /**
   * Obtain the binding for a particular variable name.
   */
  public Term getBinding(String theVariableName) {
    if (this.bindings == null) {
      throw new IllegalArgumentException("No bindings");
    }
    if (!this.bindings.containsKey(theVariableName)) {
      throw new IllegalArgumentException("No variable named \"" + theVariableName + "\" or variable is not bound");
    }
    return this.bindings.get(theVariableName);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + ':' + getSolution() + ", " + getBindings();
  }

}
