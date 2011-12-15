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
package org.logic2j.model;

import org.logic2j.Prolog;
import org.logic2j.model.exception.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.Bindings;
import org.logic2j.theory.TheoryManager;

/**
 * A Clause is a {@link Struct} representing a fact or a rule in a theory; 
 * it has extra features for efficient lookup and matching by {@link TheoryManager}s.
 * We implement by composition (by wrapping a {@link Struct}), not by derivation.
 * Facts may be represented in two manners:
 * <ol>
 * <li>A Struct with any functor different from ':-' (this is recommended and more optimal)</li>
 * <li>A Struct with functor ':-' and "true" as body (this is less optimal because this is actually a rule)</li>
 * </ol>
 */
public class Clause {

  /**
   * The {@link Struct} that represents the 
   * content of either a rule (when it has a body) or a fact 
   * (does not have a body - or has "true" as body), see description of {@link #isFact()}.
   */
  private final Struct content; // Immutable, not null

  /**
   * An empty {@link Bindings} reserved for unifying this clause with goals.
   */
  private final Bindings bindings; // Immutable, not null

  /**
   * Normalize theClauseTerm to be ready for inference.
   * @param theProlog 
   * @param theClauseTerm
   */
  public Clause(Prolog theProlog, Term theClauseTerm) {
    if (!(theClauseTerm instanceof Struct)) {
      throw new InvalidTermException("Need a Struct to build a clause, not " + theClauseTerm);
    }
    // Any Clause must be normalized otherwise we won't be able to infer on it!
    // Since we don't create via the TermFactory we have to do it here.
    this.content = (Struct) theProlog.getTermFactory().normalize(theClauseTerm);
    this.bindings = new Bindings(this.content);
  }

  /**
   * Copy constructor. Will clone the clause's content and the current {@link Bindings}.
   * @param theOriginal
   */
  public Clause(Clause theOriginal) {
    this.content = theOriginal.content.cloneIt(); // TODO LT: Why do we need cloning the content in a structure-sharing design???
    // Clone the block of variables
    this.bindings = new Bindings(theOriginal.getBindings());
  }

  /**
   * Use this method to determine if the {@link Clause} is a fact, before calling
   * {@link #getBody()} that would return "true" and entering a sub-goal demonstration.
   * 
   * @return True if the clause is a fact: if the {@link Struct} does not have ":-" as functor, or if 
   * the body is "true".
   */
  public boolean isFact() {
    // TODO Cache this value, see https://github.com/ltettoni/logic2j/issues/16
    if (Struct.FUNCTOR_CLAUSE != this.content.getName()) {
      return true;
    }
    // We know it's a clause functor, arity must be 2, check the body
    final Term body = this.content.getRHS();
    if (Struct.ATOM_TRUE.equals(body)) {
      return true;
    }
    // The body is more complex, it's certainly a rule
    return false;
  }

  private boolean isWithClauseFunctor() {
    return Struct.FUNCTOR_CLAUSE == this.content.getName();
  }

  /**
   * Obtain the head of the clause: for facts, this is the underlying {@link Struct}; for 
   * rules, this is the first argument to the clause functor.
   * @return The clause's head as a {@link Term}, normally a {@link Struct}.
   */
  public Struct getHead() {
    if (isWithClauseFunctor()) {
      return (Struct) this.content.getLHS();
    }
    return this.content;
  }

  /**
   * @return The clause's body as a {@link Term}, normally a {@link Struct}.
   */
  public Term getBody() {
    if (isWithClauseFunctor()) {
      return this.content.getRHS();
    }
    return Struct.ATOM_TRUE;
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  /**
   * @return The key that uniquely identifies the family of the {@link Clause}'s head predicate.
   */
  public String getPredicateKey() {
    final Struct head = getHead();
    return head.getPredicateIndicator();
  }

  /**
   * @return the bindings
   */
  public Bindings getBindings() {
    return this.bindings;
  }

  //---------------------------------------------------------------------------
  // Core java.lang.Object methods
  //---------------------------------------------------------------------------

  @Override
  public String toString() {
    return this.content.toString();
  }

}
