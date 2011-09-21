package org.logic2j.model;

import org.logic2j.Prolog;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.theory.TheoryManager;

/**
 * A Clause is a {@link Struct} representing a fact or a rule in a theory; 
 * it has extra features for efficient lookup and matching by {@link TheoryManager}s.
 * We implement by composition (wrapping a {@link Struct}), not not derivation.
 * Facts may be represented in two manners:
 * <ol>
 * <li>A Struct with any functor different from :- (this is recommended and more optimal)</li>
 * <li>A Struct with functor :- and "true" as body (this is less optimal)</li>
 * </ol>
 *
 */
public class Clause {

  /**
   * The {@link Struct} that represents the 
   * content of either a rule (when it has a body) or a fact 
   * (does not have a body - or has "true" as body), see description of {@link #isFact()}.
   */
  private Struct content;

  /**
   * An empty {@link VarBindings} reserved for unifying this clause with goals.
   */
  private VarBindings vars;

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
    this.vars = new VarBindings(this.content);
  }

  /**
   * Copy constructor.
   * @param theOriginal
   */
  public Clause(Clause theOriginal) {
    this.content = (Struct) theOriginal.content.cloneIt();
    this.vars = new VarBindings(theOriginal.getVars());
  }

  /**
   * Use this method to determine if the {@link Clause} is a fact, before calling
   * {@link #getBody()} that would return "true" and entering a sub-goal demonstration.
   * 
   * @return True if the clause is a fact: if the {@link Struct} does not have ":-" as functor, or if 
   * the body is "true".
   */
  public boolean isFact() {
    // TODO Cache this value
    if (Struct.FUNCTOR_CLAUSE != this.content.roName) {
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
    return Struct.FUNCTOR_CLAUSE == this.content.roName;
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
   * @return The key indicating the predicate of this clause
   */
  public String getPredicateKey() {
    final Struct head = getHead();
    return head.getPredicateIndicator();
  }

  /**
   * @return the vars
   */
  public VarBindings getVars() {
    return this.vars;
  }

  //---------------------------------------------------------------------------
  // Core
  //---------------------------------------------------------------------------

  @Override
  public String toString() {
    return this.content.toString();
  }

}
