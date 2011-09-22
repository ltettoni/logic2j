package org.logic2j.library.impl.core;

import java.util.ArrayList;

import org.logic2j.ClauseProvider;
import org.logic2j.PrologImplementor;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.Clause;
import org.logic2j.model.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.TDouble;
import org.logic2j.model.symbol.TLong;
import org.logic2j.model.symbol.TNumber;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.Binding;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;
import org.logic2j.solve.ioc.SolutionListenerBase;

/**
 * 
 */
public class CoreLibrary extends LibraryBase {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoreLibrary.class);

  public CoreLibrary(PrologImplementor theProlog) {
    super(theProlog);
  }

  @Primitive(name = Struct.FUNCTOR_TRUE)
  // We can't name the method "true" it's a Java reserved word...
  public void trueFunctor(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars) {
    notifySolution(theGoalFrame, theListener);
  }

  @Primitive
  public void fail(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars) {
    // Nothing
  }

  @Primitive
  public void var(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1) {
    if (t1 instanceof Var) {
      Var var = (Var) t1;
      if (var.isAnonymous()) {
        notifySolution(theGoalFrame, theListener);
      } else {
        Binding binding = var.derefToBinding(vars);
        while (binding.isVar()) {
          binding = binding.getLink();
        }
        if (!binding.isLiteral()) {
          // Not ending on a literal, we end up on a free var!
          notifySolution(theGoalFrame, theListener);
        }
      }
    }
  }

  @Primitive(name = Struct.FUNCTOR_CUT)
  public void cut(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars) {
    // This is a complex behaviour - read on DefaultGoalSolver
    theGoalFrame.signalCut();
    notifySolution(theGoalFrame, theListener);
  }

  @Primitive(name = "=")
  public void unify(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1, Term t2) {
    final boolean unified = unify(t1, vars, t2, vars, theGoalFrame);
    notifyIfUnified(unified, theGoalFrame, theListener);
  }

  @Primitive(name = "\\=")
  public void notUnify(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1, Term t2) {
    final boolean unified = unify(t1, vars, t2, vars, theGoalFrame);
    if (!unified) {
      notifySolution(theGoalFrame, theListener);
    }
    if (unified) {
      deunify(theGoalFrame);
    }
  }

  /**
   * A possible yet ineffective implementation of call/1. We much prefer have the solver taking care of calls immediately
   * @param theGoalFrame
   * @param vars
   * @param t1
   */
  @Primitive
  public void call(final SolutionListener theListener, final GoalFrame theGoalFrame, VarBindings vars, Term t1) {
    // Resolve vars wherever possible
    final Term target = resolveNonVar(t1, vars, "call");
    final SolutionListenerBase callListener = new SolutionListenerBase() {
      @SuppressWarnings("synthetic-access")
      // does not affect performance at all
      @Override
      public boolean onSolution() {
        notifySolution(theGoalFrame, theListener);
        return true;
      }
    };
    // TODO Need more testing of call/1, quite unsure if this way of doing is reliable
    getProlog().getSolver().solveGoalRecursive(target, vars, theGoalFrame, callListener);
  }

  @Primitive(synonyms = "\\+")
  public void not(final SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term theGoal) {
    // Resolve vars wherever possible
    final Term target = resolveNonVar(theGoal, vars, "not");
    final class AdHocListener implements SolutionListener {
      public boolean found = false;

      @Override
      public boolean onSolution() {
        // Do NOT relay the solution further, just note there was one
        this.found = true;
        return false; // No need to find further solutions
      }
    }
    final AdHocListener callListener = new AdHocListener();
    getProlog().getSolver().solveGoalRecursive(target, vars, theGoalFrame, callListener);
    if (!callListener.found) {
      theListener.onSolution();
    }
  }

  @Primitive
  public void findall(SolutionListener theListener, GoalFrame theGoalFrame, final VarBindings vars, final Term projection,
      final Term goal, final Term result) {
    // Our internal collection of results
    final ArrayList<Term> resultList = new ArrayList<Term>();

    // Define a listener to collect all solutions for the goal specified
    final SolutionListenerBase solutionListener = new SolutionListenerBase() {

      @Override
      public boolean onSolution() {
        // Calculate the substituted goal value (resolve vars)
        @SuppressWarnings("synthetic-access")
        final Term substitute = resolve(projection, vars, Term.class);
        // And add as extra solution
        resultList.add(substitute);
        return true;
      }

    };

    // Now solve the target goal, this may find several values of course
    getProlog().getSolver().solveGoalRecursive(goal, vars, new GoalFrame(), solutionListener);

    // Convert all results into a prolog list structure
    // Note on var indexes: all variables present in the projection term will be 
    // copied into the resulting plist, so there's no need to reindex.
    // However, the root level Struct that makes up the list does contain a bogus
    // index value but -1.
    final Struct plist = Struct.createPList(resultList);

    // And unify with result
    final boolean unified = unify(result, vars, plist, vars, theGoalFrame);
    notifyIfUnified(unified, theGoalFrame, theListener);
  }

  @Primitive
  public void clause(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term theHead, Term theBody) {
    final Binding dereferencedBinding = dereferencedBinding(theHead, vars);
    final Struct realHead = (Struct) dereferencedBinding.getTerm();
    for (ClauseProvider cp : getProlog().getClauseProviders()) {
      for (Clause clause : cp.listMatchingClauses(realHead)) {
        // Clone the clause so that we can unify against its bindings
        final Clause clauseToUnify = new Clause(clause);
        final boolean headUnified = unify(clauseToUnify.getHead(), clauseToUnify.getVars(), realHead,
            dereferencedBinding.getLiteralBindings(), theGoalFrame);
        if (headUnified) {
          final boolean bodyUnified = unify(clauseToUnify.getBody(), clauseToUnify.getVars(), theBody, vars, theGoalFrame);
          if (bodyUnified) {
            notifySolution(theGoalFrame, theListener);
            deunify(theGoalFrame);
          }
          deunify(theGoalFrame);
        }
      }
    }
  }

  @Primitive(name = "=..")
  public void predicate2PList(final SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term theStruct,
      Term theList) {
    final Term ts = resolve(theStruct, vars, Term.class);
    if (ts instanceof Struct) {
      Struct struct = (Struct) ts;
      ArrayList<Term> elems = new ArrayList<Term>();
      elems.add(new Struct(struct.getName()));
      int arity = struct.getArity();
      for (int i = 0; i < arity; i++) {
        elems.add(struct.getArg(i));
      }
      Struct plist = Struct.createPList(elems);
      final boolean unified = unify(plist, vars, theList, vars, theGoalFrame);
      notifyIfUnified(unified, theGoalFrame, theListener);
    } else if (ts instanceof Var) {
      final Term lst = resolve(theList, vars, Term.class);
      if (!lst.isList()) {
        throw new InvalidTermException("Second argument to =.. must be a List was " + lst);
      }
      Struct lst2 = (Struct) lst;
      Struct flattened = lst2.predicateFromPList();
      final boolean unified = unify(theStruct, vars, flattened, vars, theGoalFrame);
      notifyIfUnified(unified, theGoalFrame, theListener);
    }
  }

  @Primitive
  public void is(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1, Term t2) {
    final Term evaluated = evaluateFunctor(vars, t2);
    if (evaluated == null) {
      return;
    }
    final boolean unified = unify(t1, vars, evaluated, vars, theGoalFrame);
    notifyIfUnified(unified, theGoalFrame, theListener);
  }

  @Primitive(name = ">")
  public void expression_greater_than(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1, Term t2) {
    t1 = evaluateFunctor(vars, t1);
    t2 = evaluateFunctor(vars, t2);
    if (t1 instanceof TNumber && t2 instanceof TNumber) {
      final TNumber val0n = (TNumber) t1;
      final TNumber val1n = (TNumber) t2;
      if (val0n.longValue() > val1n.longValue()) {
        notifySolution(theGoalFrame, theListener);
      }
    }
  }

  @Primitive(name = "<")
  public void expression_lower_than(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1, Term t2) {
    t1 = evaluateFunctor(vars, t1);
    t2 = evaluateFunctor(vars, t2);
    if (t1 instanceof TNumber && t2 instanceof TNumber) {
      final TNumber val0n = (TNumber) t1;
      final TNumber val1n = (TNumber) t2;
      if (val0n.longValue() < val1n.longValue()) {
        notifySolution(theGoalFrame, theListener);
      }
    }
  }

  @Primitive(name = "+")
  public Term plus(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1, Term t2) {
    t1 = evaluateFunctor(vars, t1);
    t2 = evaluateFunctor(vars, t2);
    if (t1 instanceof TNumber && t2 instanceof TNumber) {
      final TNumber val0n = (TNumber) t1;
      final TNumber val1n = (TNumber) t2;
      if (val0n instanceof TLong && val1n instanceof TLong) {
        return createTLong(val0n.longValue() + val1n.longValue());
      }
      return new TDouble(val0n.doubleValue() + val1n.doubleValue());
    }
    throw new InvalidTermException("Could not add because 2 terms are not Numbers: " + t1 + " and " + t2);
  }

  /**
   * @param theGoalFrame
   * @param vars
   * @param t1
   * @param t2
   * @return Binary minus (subtract)
   */
  @Primitive(name = "-")
  public Term minus(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1, Term t2) {
    t1 = evaluateFunctor(vars, t1);
    t2 = evaluateFunctor(vars, t2);
    if (t1 instanceof TNumber && t2 instanceof TNumber) {
      final TNumber val0n = (TNumber) t1;
      final TNumber val1n = (TNumber) t2;
      if (val0n instanceof TLong && val1n instanceof TLong) {
        return createTLong(val0n.longValue() - val1n.longValue());
      }
      return new TDouble(val0n.doubleValue() - val1n.doubleValue());
    }
    throw new InvalidTermException("Could not subtract because 2 terms are not Numbers: " + t1 + " and " + t2);
  }

  /**
   * @param theGoalFrame
   * @param vars
   * @param t1
   * @param t2
   * @return Binary multiply
   */
  @Primitive(name = "*")
  public Term multiply(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1, Term t2) {
    t1 = evaluateFunctor(vars, t1);
    t2 = evaluateFunctor(vars, t2);
    if (t1 instanceof TNumber && t2 instanceof TNumber) {
      final TNumber val0n = (TNumber) t1;
      final TNumber val1n = (TNumber) t2;
      if (val0n instanceof TLong && val1n instanceof TLong) {
        return createTLong(val0n.longValue() * val1n.longValue());
      }
      return new TDouble(val0n.doubleValue() * val1n.doubleValue());
    }
    throw new InvalidTermException("Could not multiply because 2 terms are not Numbers: " + t1 + " and " + t2);
  }

  /**
   * @param theGoalFrame
   * @param vars
   * @param t1
   * @return Unary minus (negate)
   */
  @Primitive(name = "-")
  public Term minus(SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term t1) {
    t1 = evaluateFunctor(vars, t1);
    if (t1 instanceof TNumber) {
      TNumber val0n = (TNumber) t1;
      if (val0n instanceof TDouble) {
        return new TDouble(val0n.doubleValue() * -1);
      } else if (val0n instanceof TLong) {
        return new TLong(val0n.longValue() * -1);
      }
    }
    throw new InvalidTermException("Could not negate because argument " + t1 + " is not TNumber but " + t1.getClass());
  }

  private TNumber createTLong(long num) {
    return new TLong(num);
  }

}
