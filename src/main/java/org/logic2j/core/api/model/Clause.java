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
package org.logic2j.core.api.model;

import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.TermApi;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.unify.UnifyContext;
import org.logic2j.core.impl.PrologImplementation;

import java.util.*;

/**
 * Represents a fact or a rule in a Theory; this is described by "content" Object.
 * This class provides extra features for efficient lookup
 * and matching by {@link org.logic2j.core.impl.theory.TheoryManager}s.
 * Simple facts may be represented in two manners:
 * <ol>
 * <li>An Object, or a Struct with any functor different from ':-' (this is recommended and more optimal)</li>
 * <li>A Struct with functor ':-' and "true" as body (this is less optimal because this is actually a rule)</li>
 * <li>A Struct with functor ':-' and a real body</li>
 * </ol>
 */
public class Clause {

  private final Object content; // Immutable, not null

  /**
   * All Vars in the clause in an array, indexed by each Var's index (ie Var with getIndex()=N will be located
   * in array element N).
   */
  private final Var[] indexedVars;

  private Object head;
  private Object body;

  /**
   * A number of clones of this Clause, to avoid many cloning during inference.
   */
  private TreeMap<Integer, Clause> cache;

  /**
   * Make a Term (must be a Struct) read for inference, it will normalize it.
   *
   * @param theProlog     Required to normalize theClauseTerm according to the current libraries.
   * @param theClauseTerm
   */
  public Clause(PrologImplementation theProlog, Object theClauseTerm) {
    // if (!(theClauseTerm instanceof Struct)) {
    // throw new InvalidTermException("Need a Struct to build a clause, not " + theClauseTerm);
    // }
    // Any Clause must be normalized otherwise we won't be able to infer on it!
    this.content = TermApi.normalize(theClauseTerm, theProlog.getLibraryManager().wholeContent());
    // Store indexedVars into an array, indexed by the var's index
    final Var[] distinctVars = TermApi.distinctVars(content);
    this.indexedVars = new Var[distinctVars.length];
    for (Var distinctVar : distinctVars) {
      this.indexedVars[distinctVar.getIndex()] = distinctVar;
    }
    initDenormalizedFields();
  }


  private Clause(Clause original, Struct cloned, Var[] clonedVars) {
    this.content = cloned;
    this.indexedVars = clonedVars;
    this.cache = null; // That one should never be modified - we are on a clone
    initDenormalizedFields();
  }


  private void initDenormalizedFields() {
    if (!(this.content instanceof Struct)) {
      // A single Object (typically a String) is a fact.
      this.head = this.content;
      this.body = null;
      return;
    }
    final Struct struct = (Struct) this.content;
    if (Struct.FUNCTOR_CLAUSE != struct.getName()) { // Names are {@link String#intern()}alized so OK to check by reference
      this.head = this.content;
      this.body = null;
      return;
    }
    // We know it's a Struct with the clause functor, arity must be 2, check the body
    final Object[] clauseArgs = struct.getArgs();
    final Object body = clauseArgs[1];
    if (Struct.ATOM_TRUE.equals(body)) {
      this.head = clauseArgs[0];
      this.body = null;
      return;
    }
    // The body is more complex, it's certainly a rule
    this.head = clauseArgs[0];
    this.body = body;
  }

  /**
   * Extract head and body of this clause, into the target array passed as argument.
   *
   * @param currentVars
   * @param clauseHeadAndBody Function return values in this Object[2], that's not really Java-ish but efficient... :-(
   */
  public void headAndBodyForSubgoal(UnifyContext currentVars, Object[] clauseHeadAndBody) {
    final Clause clonedClause;
    if (needCloning()) {
      clonedClause = cloned(currentVars);
    } else {
      clonedClause = this;
    }
    clauseHeadAndBody[0] = clonedClause.head;
    clauseHeadAndBody[1] = clonedClause.body; // Will be null for facts
  }

  private Clause cloned(UnifyContext currentVars) {
    if (this.cache == null) {
      this.cache = new TreeMap<Integer, Clause>();
      //            logger.warn("Instantiating Clause cache for {}", this.content);
    }
    final Map.Entry<Integer, Clause> ceilingEntry = this.cache.ceilingEntry(currentVars.topVarIndex(0));
    if (ceilingEntry == null) {
      //            logger.warn("Cloning {}", this);
      // No such entry: create and insert
      final Clause clonedClause = cloneClauseAndRemapIndexes(this, currentVars);
      final int initialVarIndex = clonedClause.indexedVars[0].getIndex(); // There MUST be at least one var otherwise we would not be cloning
      this.cache.put(initialVarIndex, clonedClause);
      return clonedClause;
    }
    final Clause reused = ceilingEntry.getValue();
    int desiredTop = reused.indexedVars[reused.indexedVars.length - 1].getIndex() + 1;
    currentVars.topVarIndex(desiredTop - currentVars.topVarIndex(0));
    //        logger.warn("Reusing cloned clause {}", reused);
    return reused;
  }



  private Clause cloneClauseAndRemapIndexes(Clause theClause, UnifyContext currentVars) {
    //            audit.info("Clone  {}  (base={})", content, this.topVarIndex);
    final Var[] originalVars = theClause.indexedVars;
    final int nbVars = originalVars.length;
    // Allocate the new indexedVars by cloning the original ones. Index is preserved meaning that
    // when we traverse the original structure and find a Var of index N, we can replace it
    // in the cloned structure by the clonedVar[N]
    final Var[] clonedVars = new Var[nbVars];
    for (int i = 0; i < nbVars; i++) {
      clonedVars[i] = Var.copy(originalVars[i]);
    }
    assert theClause.content instanceof Struct;
    final Struct cloned = cloneStruct((Struct) theClause.content, clonedVars);
    // Now reindex the cloned indexedVars
    for (int i = 0; i < nbVars; i++) {
      clonedVars[i].setIndex(clonedVars[i].getIndex() + currentVars.topVarIndex(0));
    }
    // And increment the highest Var index accordingly
    currentVars.topVarIndex(nbVars);

    //        if (currentVars.topVarIndex > ProfilingInfo.max1) {
    //            ProfilingInfo.max1 = currentVars.topVarIndex;
    //        }
    //    audit.info("Cloned {}  (base={})", cloned, this.topVarIndex);
    return new Clause(theClause, cloned, clonedVars);
  }

  private Struct cloneStruct(Struct theStruct, Var[] clonedVars) {
    final Object[] args = theStruct.getArgs();
    final int arity = args.length;
    final Object[] clonedArgs = new Object[arity];
    for (int i = 0; i < arity; i++) {
      final Object arg = args[i];
      if (arg instanceof Struct) {
        final Struct recursedClonedElement = cloneStruct((Struct) arg, clonedVars);
        clonedArgs[i] = recursedClonedElement;
      } else if (arg instanceof Var && arg != Var.anon()) {
        final int originalVarIndex = ((Var) arg).getIndex();
        clonedArgs[i] = clonedVars[originalVarIndex];
      } else {
        clonedArgs[i] = arg;
      }
    }
    final Struct struct = theStruct.cloneWithNewArguments(clonedArgs);
    return struct;
  }

  // ---------------------------------------------------------------------------
  // Methods to denormalize immutable fields
  // ---------------------------------------------------------------------------

  /**
   * @return true only if this Clause's content is a Struct which holds variables.
   */
  private boolean needCloning() {
    if (!(this.content instanceof Struct)) {
      return false;
    }
    final Struct cs = (Struct) this.content;
    return cs.getIndex() > 0;
  }

  /**
   * @return The key that uniquely identifies the family of the {@link Clause}'s head predicate.
   */
  public String getPredicateKey() {
    return TermApi.predicateSignature(this.head);
  }

  // ---------------------------------------------------------------------------
  // Methods of java.lang.Object
  // ---------------------------------------------------------------------------

  @Override
  public String toString() {
    return this.content.toString();
  }

}
