/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.engine.unify;

import org.logic2j.core.api.model.DataFact;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.TermApi;
import org.logic2j.engine.model.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.logic2j.engine.model.Var.strVar;

/**
 * A monad-like object that allows dereferencing variables to their effective current values,
 * or to modify variables (and return a new UnifyContext).
 */
public class UnifyContext {
  private static final Logger logger = LoggerFactory.getLogger(UnifyContext.class);
  //    static final Logger audit = LoggerFactory.getLogger("audit");

  final int currentTransaction;

  // TODO Make private - only Clause and Solver are using it yet
  public int topVarIndex;  // "top" value is one above the current max

  private final UnifyStateByLookup impl;

  UnifyContext(UnifyStateByLookup implem) {
    this(implem, 0, 0);
  }

  UnifyContext(UnifyStateByLookup implem, int currentTransaction, int topVarIndex) {
    this.impl = implem;
    this.currentTransaction = currentTransaction;
    this.topVarIndex = topVarIndex;
    //        audit.info("New at t={}", currentTransaction);
    //        audit.info("    this={}", this);
  }


  /**
   * Instantiate a new Var and assign a unique index
   *
   * @param theName
   * @return A new Var uniquely indexed
   */
  public Var createVar(String theName) {
    final Var var = strVar(theName);
    var.setIndex(topVarIndex++);
    return var;
  }

  /**
   * Bind var to ref (var will be altered in the returned UnifyContext); ref is untouched.
   * <p>
   * (private except that used from test case)
   *
   * @param var
   * @param ref
   * @return
   */
  UnifyContext bind(Var var, Object ref) {
    if (var == ref) {
      logger.debug("Not mapping {} onto itself", var);
      return this;
    }
    //        audit.info("Bind   {} -> {} at t=" + this.currentTransaction, var, ref);
    return impl.bind(this, var, ref);
  }


  /**
   * In principle one must use the recursive form reify()
   *
   * @param theVar
   * @return The dereferenced content of theVar, or theVar if it was free
   */
  private Object finalValue(Var theVar) {
    final Object dereference = this.impl.dereference(theVar, this.currentTransaction);
    return dereference;
  }

  /**
   * Resolve variables to their values.
   *
   * @param term
   * @return The dereferenced content of term, or theVar if it was free, or null if term is null
   */
  public Object reify(Object term) {
    if (term instanceof Var) {
      term = finalValue((Var) term);
      // The var might end up on a Struct, that needs recursive reification
    }
    if (term instanceof Struct) {
      //            audit.info("Reify Struct at t={}  {}", this.currentTransaction, term);
      final Struct s = (Struct) term;
      if (s.getIndex() == 0) {
        // Structure is an atom or a constant term - no need to further transform
        return term;
      }
      final Object[] args = s.getArgs();
      final int arity = args.length;
      final Object[] reifiedArgs = new Object[arity];
      for (int i = 0; i < arity; i++) {
        reifiedArgs[i] = reify(args[i]);
      }
      final Struct res = new Struct(s, reifiedArgs);
      if (s.getIndex() > 0) {
        // The original structure had variables, maybe the cloned one will still have (if those were free)
        // We need to reassign indexes. It's costly, unfortunately.
        TermApi.assignIndexes(res, 0);
      }
      //            audit.info("               yields {}", res);
      return res;
    }
    return term;
  }


  public UnifyContext unify(Object term1, Object term2) {
    //        audit.info("Unify  {}  ~  {}", term1, term2);
    if (term1 == term2) {
      return this;
    }
    if (term2 instanceof Var) {
      // Switch arguments - we prefer having term1 being the var.
      // Notice that formally, we should check  && !(term1 instanceof Var)
      // to avoid possible useless switching when unifying Var <-> Var.
      // However, the extra instanceof total costs 3% more than a useless switch.
      final Object term1held = term1;
      term1 = term2;
      term2 = term1held;
    }
    if (term1 instanceof Var) {
      // term1 is a Var: we need to check if it is bound or not
      Var var1 = (Var) term1;
      final Object final1 = finalValue(var1);
      if (!(final1 instanceof Var)) {
        // term1 is bound - unify
        return unify(final1, term2);
      }
      // Ended up with final1 being a free Var, so term1 was a free var
      var1 = (Var) final1;
      // free Var var1 need to be bound
      if (term2 instanceof Var) {
        // Binding two vars
        final Var var2 = (Var) term2;
        // Link one to two (should we link to the final or the initial value???)
        // Now do the binding of two vars
        return bind(var1, var2);
      } else {
        // Do the binding of one var to a literal
        return bind(var1, term2);
      }
    } else if (term1 instanceof Struct) {
      // Case of Struct <-> Var: already taken care of by switching, see above
      if (!(term2 instanceof Struct)) {
        // Not unified - we can only unify 2 Struct
        return null;
      }
      final Struct s1 = (Struct) term1;
      final Struct s2 = (Struct) term2;
      // The two Struct must have compatible signatures (functor and arity)
      //noinspection StringEquality
      if (s1.getPredicateSignature() != s2.getPredicateSignature()) {
        return null;
      }
      // Now we will unify all arguments, stopping at the first that do not match
      final Object[] s1Args = s1.getArgs();
      final Object[] s2Args = s2.getArgs();
      final int arity = s1Args.length;
      UnifyContext runningMonad = this;
      for (int i = 0; i < arity; i++) {
        runningMonad = runningMonad.unify(s1Args[i], s2Args[i]);
        if (runningMonad == null) {
          // Struct sub-element not unified - fail the whole unification
          return null;
        }
      }
      // All matched, return the latest monad
      return runningMonad;
    } else {
      return term1.equals(term2) ? this : null;
    }
  }


  /**
   * Unify against DataFact
   *
   * @param term1
   * @param dataFact
   * @return
   */
  public UnifyContext unify(Object term1, DataFact dataFact) {
    if (!(term1 instanceof Struct)) {
      // Only Struct could match a DataFact
      return null;
    }
    final Struct struct = (Struct) term1;
    final Object[] dataFactElements = dataFact.elements;
    if (struct.getName() != dataFactElements[0]) {// Names are {@link String#intern()}alized so OK to check by reference
      // Functor must match
      return null;
    }
    final int arity = struct.getArity();
    if (arity != dataFactElements.length - 1) {
      // Arity must match as well
      return null;
    }
    final Object[] structArgs = struct.getArgs();
    UnifyContext runningMonad = this;
    // Unify all dataFactElements
    for (int i = 0; i < arity; i++) {
      final Object structArg = structArgs[i];
      final Object dataFactElement = dataFactElements[1 + i];
      runningMonad = runningMonad.unify(structArg, dataFactElement);
      if (runningMonad == null) {
        // Struct sub-dataFactElement not unified - fail the whole unification
        return null;
      }
    }
    return runningMonad;
  }

  @Override
  public String toString() {
    return "vars#" + this.currentTransaction + impl.toString();
  }

}