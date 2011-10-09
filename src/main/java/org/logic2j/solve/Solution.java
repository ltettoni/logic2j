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

import java.util.Map;

import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.TermApi;
import org.logic2j.model.var.Bindings;
import org.logic2j.model.var.Bindings.FreeVarBehaviour;

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
   * @param theBindings
   */
  public Solution(Term theGoal, Bindings theBindings) {
    this.solution = TERM_API.substitute(theGoal, theBindings, null);
    this.bindings = theBindings.explicitBindings(FreeVarBehaviour.NULL_ENTRY);
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
