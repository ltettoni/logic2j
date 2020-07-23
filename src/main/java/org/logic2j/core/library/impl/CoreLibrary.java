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
package org.logic2j.core.library.impl;

import static org.logic2j.engine.model.TermApiLocator.termApi;
import static org.logic2j.engine.model.TermApiLocator.termApiExt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.library.annotation.Functor;
import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.impl.NotListener;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.Solver;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.model.PrologLists;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.solver.listener.CountingSolutionListener;
import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.unify.UnifyContext;

/**
 * Provide the core primitives of the Prolog language.
 * Most is implemented in Java, but there is an associated Prolog theory at:
 * /src/main/prolog/org/logic2j/core/library/impl/core/CoreLibrary.pro
 */
@SuppressWarnings("StringEquality")
public class CoreLibrary extends LibraryBase {

  public CoreLibrary(PrologImplementation theProlog) {
    super(theProlog);
  }

  private static final ComparisonFunction COMPARE_GT = new ComparisonFunction() {
    @Override
    public boolean apply(Number val1, Number val2) {
      return val1.doubleValue() > val2.doubleValue();
    }

    @Override
    public boolean apply(CharSequence val1, CharSequence val2) {
      return val1.toString().compareTo(val2.toString()) > 0;
    }
  };

  private static final ComparisonFunction COMPARISON_LT = new ComparisonFunction() {

    @Override
    public boolean apply(Number val1, Number val2) {
      return val1.doubleValue() < val2.doubleValue();
    }

    @Override
    public boolean apply(CharSequence val1, CharSequence val2) {
      return val1.toString().compareTo(val2.toString()) < 0;
    }
  };

  private static final ComparisonFunction COMPARISON_GE = new ComparisonFunction() {

    @Override
    public boolean apply(Number val1, Number val2) {
      return val1.doubleValue() >= val2.doubleValue();
    }

    @Override
    public boolean apply(CharSequence val1, CharSequence val2) {
      return val1.toString().compareTo(val2.toString()) >= 0;
    }
  };

  private static final ComparisonFunction COMPARISON_LE = new ComparisonFunction() {

    @Override
    public boolean apply(Number val1, Number val2) {
      return val1.doubleValue() <= val2.doubleValue();
    }

    @Override
    public boolean apply(CharSequence val1, CharSequence val2) {
      return val1.toString().compareTo(val2.toString()) <= 0;
    }
  };

  private static final ComparisonFunction COMPARISON_EQ = new ComparisonFunction() {

    @Override
    public boolean apply(Number val1, Number val2) {
      return val1.doubleValue() == val2.doubleValue();
    }

    @Override
    public boolean apply(CharSequence val1, CharSequence val2) {
      return val1.toString().compareTo(val2.toString()) == 0;
    }
  };

  private static final ComparisonFunction COMPARISON_NE = new ComparisonFunction() {

    @Override
    public boolean apply(Number val1, Number val2) {
      return val1.doubleValue() != val2.doubleValue();
    }

    @Override
    public boolean apply(CharSequence val1, CharSequence val2) {
      return val1.toString().compareTo(val2.toString()) != 0;
    }
  };

  private static final AggregationFunction AGGREGATION_PLUS = (val1, val2) -> {
    if (val1 instanceof Integer && val2 instanceof Integer) {
      return val1.intValue() + val2.intValue();
    }
    return (double) (val1.intValue() + val2.intValue());
  };

  private static final AggregationFunction AGGREGATION_MINUS = (val1, val2) -> {
    if (val1 instanceof Integer && val2 instanceof Integer) {
      return val1.intValue() - val2.intValue();
    }
    return (double) (val1.intValue() - val2.intValue());
  };

  private static final AggregationFunction AGGREGRATION_TIMES = (val1, val2) -> {
    if (val1 instanceof Integer && val2 instanceof Integer) {
      return val1.intValue() * val2.intValue();
    }
    return (double) (val1.intValue() * val2.intValue());
  };

  private static final AggregationFunction AGGREGATION_NEGATE = (val1, val2) -> {
    if (val1 instanceof Integer && val2 instanceof Integer) {
      return -val1.intValue();
    }
    return (double) -val1.intValue();
  };


  @Override
  public Object dispatch(String theMethodName, Struct<?> theGoalStruct, UnifyContext currentVars) {
    final Object result;
    // Argument methodName is {@link String#intern()}alized so OK to check by reference
    final Object[] goalStructArgs = theGoalStruct.getArgs();
    final int arity = goalStructArgs.length;
    if (arity == 0) {
      if (theMethodName == "fail") {
        result = fail(currentVars);
      } else if (theMethodName == "trueFunctor") {
        result = trueFunctor(currentVars);
      } else {
        result = NO_DIRECT_INVOCATION_USE_REFLECTION;
      }
    } else if (arity == 1) {
      final Object arg0 = goalStructArgs[0];
      if (theMethodName == "not") {
        result = not(currentVars, arg0);
      } else if (theMethodName == "atom") {
        result = atom(currentVars, arg0);
      } else if (theMethodName == "atomic") {
        result = atomic(currentVars, arg0);
      } else if (theMethodName == "var") {
        result = var(currentVars, arg0);
      } else if (theMethodName == "exists") {
        result = exists(currentVars, arg0);
      } else {
        result = NO_DIRECT_INVOCATION_USE_REFLECTION;
      }
    } else if (arity == 2) {
      final Object arg0 = goalStructArgs[0];
      final Object arg1 = goalStructArgs[1];
      if (theMethodName == "unify") {
        result = unify(currentVars, arg0, arg1);
      } else if (theMethodName == "expression_greater_equal_than") {
        result = expression_greater_equal_than(currentVars, arg0, arg1);
      } else if (theMethodName == "expression_greater_than") {
        result = expression_greater_than(currentVars, arg0, arg1);
      } else if (theMethodName == "expression_lower_than") {
        result = expression_lower_than(currentVars, arg0, arg1);
      } else if (theMethodName == "expression_lower_equal_than") {
        result = expression_lower_equal_than(currentVars, arg0, arg1);
      } else if (theMethodName == "expression_equals") {
        result = expression_equals(currentVars, arg0, arg1);
      } else if (theMethodName == "is") {
        result = is(currentVars, arg0, arg1);
      } else if (theMethodName == "plus") {
        result = plus(currentVars, arg0, arg1);
      } else if (theMethodName == "minus") {
        result = minus(currentVars, arg0, arg1);
      } else if (theMethodName == "multiply") {
        result = multiply(currentVars, arg0, arg1);
      } else if (theMethodName == "notUnify") {
        result = notUnify(currentVars, arg0, arg1);
      } else if (theMethodName == "clause") {
        result = clause(currentVars, arg0, arg1);
      } else if (theMethodName == "predicate2PList") {
        result = predicate2PList(currentVars, arg0, arg1);
      } else if (theMethodName == "atom_length") {
        result = atom_length(currentVars, arg0, arg1);
      } else if (theMethodName == "length") {
        result = length(currentVars, arg0, arg1);
      } else if (theMethodName == "count") {
        result = count(currentVars, arg0, arg1);
      } else {
        result = NO_DIRECT_INVOCATION_USE_REFLECTION;
      }
    } else if (arity == 3) {
      final Object arg0 = goalStructArgs[0];
      final Object arg1 = goalStructArgs[1];
      final Object arg2 = goalStructArgs[2];
      if (theMethodName == "findall") {
        result = findall(currentVars, arg0, arg1, arg2);
      } else if (theMethodName == "distinct") {
        result = distinct(currentVars, arg0, arg1, arg2);
      } else {
        result = NO_DIRECT_INVOCATION_USE_REFLECTION;
      }
    } else {
      result = NO_DIRECT_INVOCATION_USE_REFLECTION;
    }
    return result;
  }

  @Predicate(name = Struct.FUNCTOR_TRUE)
  // We can't name the method "true" it's a Java reserved word...
  public int trueFunctor(UnifyContext currentVars) {
    return notifySolution(currentVars);
  }

  @Predicate
  public int fail(@SuppressWarnings("unused") UnifyContext currentVars) {
    // Do not propagate a solution - that's all
    return Continuation.CONTINUE;
  }

  @Predicate
  public int var(UnifyContext currentVars, Object t1) {
    int continuation = Continuation.CONTINUE;
    if (t1 instanceof Var<?>) {
      final Var<?> var = (Var<?>) t1;
      if (var == Var.anon()) {
        notifySolution(currentVars);
      } else {
        final Object value = currentVars.reify(t1);
        if (value instanceof Var<?>) {
          continuation = notifySolution(currentVars);
        }
      }
    }
    return continuation;
  }

  @Predicate
  public int atom(UnifyContext currentVars, Object theTerm) {
    final Object value = currentVars.reify(theTerm);
    if (termApi().isAtom(value)) {
      return notifySolution(currentVars);
    }
    return Continuation.CONTINUE;
  }

  @Predicate
  public int atomic(UnifyContext currentVars, Object theTerm) {
    final Object value = currentVars.reify(theTerm);
    if (termApi().isAtomic(value)) {
      return notifySolution(currentVars);
    }
    return Continuation.CONTINUE;
  }

  @Predicate
  public int number(UnifyContext currentVars, Object theTerm) {
    final Object value = currentVars.reify(theTerm);
    ensureBindingIsNotAFreeVar(value, "number/1", 0);
    if (value instanceof Number) {
      return notifySolution(currentVars);
    }
    return Continuation.CONTINUE;
  }

  @Predicate(name = "=")
  public int unify(UnifyContext currentVars, Object t1, Object t2) {
    return unifyAndNotify(currentVars, t1, t2);
  }

  /**
   * Not unifyable
   * @param currentVars
   * @param t1
   * @param t2
   * @return success if t1 and t2 cannot be unified
   */
  @Predicate(name = "\\=")
  public int notUnify(UnifyContext currentVars, Object t1, Object t2) {
    final UnifyContext after = currentVars.unify(t1, t2);
    if (after == null) {
      // Not unified
      return notifySolution(currentVars);
    }
    // Unified
    return Continuation.CONTINUE;
  }

  // Surprisingly enough the operator \+ means "not provable".
  @Predicate(synonyms = "\\+")
  public int not(UnifyContext currentVars, Object theGoal) {

    final NotListener callListener = new NotListener();

    Solver solver = getProlog().getSolver();
    solver.solveGoal(theGoal, currentVars.withListener(callListener));
    final int continuation;
    if (callListener.exists()) {
      continuation = Continuation.CONTINUE;
    } else {
      // Not found - notify a solution (that's the purpose of not/1 !)
      continuation = notifySolution(currentVars);
    }
    return continuation;
  }

  @Predicate
  public int atom_length(UnifyContext currentVars, Object theAtom, Object theLength) {
    final Object value = currentVars.reify(theAtom);
    ensureBindingIsNotAFreeVar(value, "atom_length/2", 0);
    final String atomText = value.toString();
    final Long atomLength = (long) atomText.length();
    return unify(currentVars, atomLength, theLength);
  }

  /**
   * Check existence of subGoal. Without binding its variables, and stopping any solving after the first solution
   * of subGoal is proven. This is a most efficient implementation but may have dubious effects - maybe to study
   * a little in details...
   *
   * @param currentVars
   * @param theGoal
   * @return
   */
  @Predicate
  public int exists(UnifyContext currentVars, final Object theGoal) {
    final CountingSolutionListener listenerForSubGoal = new CountingSolutionListener() {
      @Override
      public int onSolution(UnifyContext currentVars) {
        super.onSolution(currentVars);
        // Upon the first solution found, notify the engine to stop generating
        return Continuation.USER_ABORT;
      }
    };
    // Now solve the target sub goal
    final Object effectiveGoal = currentVars.reify(theGoal);
    getProlog().getSolver().solveGoal(effectiveGoal, currentVars.withListener(listenerForSubGoal));

    // And unify with result
    final int counted = listenerForSubGoal.count();
    // Note: won't ever be greater than one due to our listener that stops generation
    if (counted > 0) {
      return notifySolution(currentVars);
    }
    return Continuation.CONTINUE;
  }

  private void collectReifiedResults(UnifyContext currentVars, final Object theTemplate, Object theGoal, final Collection<Object> javaResults) {
    final SolutionListener listenerForSubGoal = vars -> {
      final Object templateReified = vars.reify(theTemplate);
      javaResults.add(templateReified);
      return Continuation.CONTINUE;
    };
    // Now solve the target sub goal
    final Object effectiveGoal = currentVars.reify(theGoal);
    getProlog().getSolver().solveGoal(effectiveGoal, currentVars.withListener(listenerForSubGoal));
  }


  @Predicate
  public int count(UnifyContext currentVars, final Object theGoal, final Object theNumber) {
    final CountingSolutionListener listenerForSubGoal = new CountingSolutionListener();
    // Now solve the target sub goal
    final Object effectiveGoal = currentVars.reify(theGoal);
    getProlog().getSolver().solveGoal(effectiveGoal, currentVars.withListener(listenerForSubGoal));

    // And unify with result of counting (as Integer)
    final Integer counted = listenerForSubGoal.count();
    return unify(currentVars, theNumber, counted);
  }

  @Predicate
  public int findall(UnifyContext currentVars, final Object theTemplate, final Object theGoal,
                     final Object theResult) {
    final ArrayList<Object> allReifiedResults = new ArrayList<>(100); // Our internal collection of results
    collectReifiedResults(currentVars, theTemplate, theGoal, allReifiedResults);

    // Convert all results into a prolog list structure
    // Note on var indexes: all variables present in the projection term will be
    // copied into the resulting plist, so there's no need to reindex.
    // However, the root level Struct that makes up the list does contain a bogus
    // index value but -1.
    final Struct<?> plist = PrologLists.createPList(allReifiedResults);

    // And unify with result
    return unify(currentVars, theResult, plist);
  }

  @Predicate
  public int distinct(UnifyContext currentVars, final Object theTemplate, final Object theGoal,
                      final Object theResult) {
    final LinkedHashSet<Object> distinctReifiedResults = new LinkedHashSet<>(100); // Our internal collection of results
    collectReifiedResults(currentVars, theTemplate, theGoal, distinctReifiedResults);

    // Convert all results into a prolog list structure
    // Note on var indexes: all variables present in the projection term will be
    // copied into the resulting plist, so there's no need to reindex.
    // However, the root level Struct that makes up the list does contain a bogus
    // index value but -1.
    final Struct<?> plist = PrologLists.createPList(distinctReifiedResults);

    // And unify with result
    return unify(currentVars, theResult, plist);
  }

  /**
   * @param currentVars
   * @param theList
   * @param theLength
   * @return Length of a prolog list
   */
  @Predicate
  public int length(UnifyContext currentVars, Object theList, Object theLength) {
    final Object value = currentVars.reify(theList);
    ensureBindingIsNotAFreeVar(value, "length/2", 0);
    if (!PrologLists.isList(value)) {
      throw new InvalidTermException("A Prolog list is required for length/2,  was " + value);
    }
    final ArrayList<Object> javalist = PrologLists.javaListFromPList(((Struct<?>) value), new ArrayList<>(), Object.class);
    final Integer listLength = javalist.size();
    return unify(currentVars, listLength, theLength);
  }

  @Predicate
  public int clause(UnifyContext currentVars, Object theHead, Object theBody) {
    final Object headValue = currentVars.reify(theHead);
    final Object[] clauseHeadAndBody = new Object[2];
    for (final ClauseProvider cp : getProlog().getTheoryManager().getClauseProviders()) {
      for (final Clause clause : cp.listMatchingClauses(headValue, currentVars)) {
        // Clone the clause so that we can unify against its bindings
        clause.headAndBodyForSubgoal(currentVars, clauseHeadAndBody);
        final Object clauseHead = clauseHeadAndBody[0];
        final UnifyContext varsAfterHeadUnified = currentVars.unify(headValue, clauseHead);
        final boolean headUnified = varsAfterHeadUnified != null;
        if (headUnified) {
          // Determine body
          final boolean isRule = clauseHeadAndBody[1] != null;
          final Object clauseBody = isRule ? clauseHeadAndBody[1] : Struct.ATOM_TRUE;
          // Unify Body
          final UnifyContext varsAfterBodyUnified = varsAfterHeadUnified.unify(clauseBody, theBody);
          if (varsAfterBodyUnified != null) {
            final int continuation = notifySolution(varsAfterBodyUnified);
            if (continuation != Continuation.CONTINUE) {
              return continuation;
            }
          }
        }
      }
    }
    return Continuation.CONTINUE;
  }

  @Predicate(name = "=..")
  public int predicate2PList(UnifyContext currentVars, Object thePredicate, Object theList) {
    final Object predicateValue = currentVars.reify(thePredicate);
    if (predicateValue instanceof Var<?>) {
      // thePredicate is still free, going ot match from theList
      final Object listValue = currentVars.reify(theList);
      ensureBindingIsNotAFreeVar(listValue, "=../2", 1);
      final Struct<?> lst2 = (Struct<?>) listValue;
      final Struct<?> flattened = PrologLists.predicateFromPList(lst2);

      return unify(currentVars, predicateValue, flattened);
    } else {
      // thePredicate is bound
      if (predicateValue instanceof Struct) {
        final Struct<?> struct = (Struct<?>) predicateValue;
        final int arity = struct.getArity();
        final ArrayList<Object> elems = new ArrayList<>(1 + arity);
        elems.add(struct.getName());
        for (Object arg : struct.getArgs()) {
          elems.add(arg);
        }
        final Struct<?> plist = PrologLists.createPList(elems);
        return unify(currentVars, plist, theList);
      }
    }
    return Continuation.CONTINUE;
  }

  @Predicate
  public int is(UnifyContext currentVars, Object t1, Object t2) {
    final Object evaluated = termApiExt().evaluate(t2, currentVars);
    if (evaluated == null) {
      // No solution
      return Continuation.CONTINUE;
    }
    return unify(currentVars, t1, evaluated);
  }

  // ---------------------------------------------------------------------------
  // Binary numeric predicates
  // ---------------------------------------------------------------------------

  /**
   * Compare numbers or chars.
   */
  private interface ComparisonFunction {
    boolean apply(Number val1, Number val2);

    boolean apply(CharSequence str1, CharSequence str2);
  }

  /**
   * For all binary predicates that compare numeric values.
   *
   * @param currentVars
   * @param t1
   * @param t2
   * @param theEvaluationFunction
   * @return The {@link Continuation} as returned by the solution notified.
   */
  private int binaryComparisonPredicate(UnifyContext currentVars, Object t1, Object t2,
                                        ComparisonFunction theEvaluationFunction) {
    final Object effectiveT1 = termApiExt().evaluate(t1, currentVars);
    final Object effectiveT2 = termApiExt().evaluate(t2, currentVars);
    int continuation = Continuation.CONTINUE;
    if (effectiveT1 instanceof Number && effectiveT2 instanceof Number) {
      final boolean condition = theEvaluationFunction.apply((Number) effectiveT1, (Number) effectiveT2);
      if (condition) {
        continuation = notifySolution(currentVars);
      }
      return continuation;
    }
    if (effectiveT1 instanceof CharSequence && effectiveT2 instanceof CharSequence) {
      final boolean condition = theEvaluationFunction.apply((CharSequence) effectiveT1, (CharSequence) effectiveT2);
      if (condition) {
        continuation = notifySolution(currentVars);
      }
      return continuation;
    }
    return continuation;
  }


  @Predicate(name = ">")
  public int expression_greater_than(UnifyContext currentVars, Object t1, Object t2) {
    return binaryComparisonPredicate(currentVars, t1, t2, COMPARE_GT);
  }

  @Predicate(name = "<")
  public int expression_lower_than(UnifyContext currentVars, Object t1, Object t2) {
    return binaryComparisonPredicate(currentVars, t1, t2, COMPARISON_LT);
  }

  @Predicate(name = ">=")
  public int expression_greater_equal_than(UnifyContext currentVars, Object t1, Object t2) {
    return binaryComparisonPredicate(currentVars, t1, t2, COMPARISON_GE);
  }

  @Predicate(name = "=<")
  public int expression_lower_equal_than(UnifyContext currentVars, Object t1, Object t2) {
    return binaryComparisonPredicate(currentVars, t1, t2, COMPARISON_LE);
  }

  @Predicate(name = "=:=")
  public int expression_equals(UnifyContext currentVars, Object t1, Object t2) {
    return binaryComparisonPredicate(currentVars, t1, t2, COMPARISON_EQ);
  }

  @Predicate(name = "=\\=")
  public int expression_not_equals(UnifyContext currentVars, Object t1, Object t2) {
    return binaryComparisonPredicate(currentVars, t1, t2, COMPARISON_NE);
  }

  // ---------------------------------------------------------------------------
  // Functors
  // ---------------------------------------------------------------------------


  private interface AggregationFunction {
    Number apply(Number val1, Number val2);
  }

  private Object binaryFunctor(UnifyContext currentVars, Object theTerm1, Object theTerm2,
                               AggregationFunction theEvaluationFunction) {
    final Object t1 = termApiExt().evaluate(theTerm1, currentVars);
    final Object t2 = termApiExt().evaluate(theTerm2, currentVars);
    if (t1 instanceof Number && t2 instanceof Number) {
      if (t1 instanceof Integer && t2 instanceof Integer) {
        return theEvaluationFunction.apply((Number) t1, (Number) t2).intValue();
      }
      return theEvaluationFunction.apply((Number) t1, (Number) t2).doubleValue();
    }
    throw new InvalidTermException("Could not apply binaryFunctor because 2 terms are not Numbers: " + t1 + " and " + t2);
  }

  @Functor(name = "+")
  public Object plus(UnifyContext currentVars, Object t1, Object t2) {
    return binaryFunctor(currentVars, t1, t2, AGGREGATION_PLUS);
  }

  /**
   * @param currentVars
   * @param t1
   * @param t2
   * @return Binary minus (subtract)
   */
  @Functor(name = "-")
  public Object minus(UnifyContext currentVars, Object t1, Object t2) {
    return binaryFunctor(currentVars, t1, t2, AGGREGATION_MINUS);
  }

  /**
   * @param t1
   * @param t2
   * @return Binary multiply
   */
  @Functor(name = "*")
  public Object multiply(UnifyContext currentVars, Object t1, Object t2) {
    return binaryFunctor(currentVars, t1, t2, AGGREGRATION_TIMES);
  }

  /**
   * @param currentVars
   * @param t1
   * @return Unary minus (negate)
   */
  @Functor(name = "-")
  public Object minus(UnifyContext currentVars, Object t1) {
    return binaryFunctor(currentVars, t1, 0L, AGGREGATION_NEGATE);
  }
}
