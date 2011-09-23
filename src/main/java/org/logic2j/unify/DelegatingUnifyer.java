package org.logic2j.unify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import org.logic2j.model.InvalidTermException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.TNumber;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.symbol.Var;
import org.logic2j.model.var.Binding;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.GoalFrame;

/**
 * Unify by delegating to individual methods with exact signatures on {@link Term} subclasses.
 *
 */
public class DelegatingUnifyer implements Unifyer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DelegatingUnifyer.class);

  static {
    final Map<String, Method> methodMap = new TreeMap<String, Method>();

    for (Method method : DelegatingUnifyer.class.getMethods()) {
      if ("unify".equals(method.getName())) {
        Class<?>[] parameterTypes = method.getParameterTypes();

        String key1 = classKey(parameterTypes[0]);
        String key2 = classKey(parameterTypes[2]);
        String key = key1 + '-' + key2;
        methodMap.put(key, method);
      }
    }
    logger.info("MethodMap: {}", methodMap);
  }

  @Override
  public boolean unify(Term term1, VarBindings vars1, Term term2, VarBindings vars2, GoalFrame theGoalFrame) {
    for (Method method : this.getClass().getMethods()) {
      if ("unify".equals(method.getName())) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes[0].isAssignableFrom(term1.getClass()) && parameterTypes[1].isAssignableFrom(term2.getClass())) {
          try {
            theGoalFrame.markForNextBindings();
            boolean unified = (Boolean) method.invoke(this, new Object[] { term1, term2, vars1, vars2, theGoalFrame });
            if (!unified) {
              deunify(theGoalFrame);
            }
            return unified;
          } catch (InvocationTargetException e) {
            throw new InvalidTermException("Could not determine or invoke unification method: " + e.getTargetException());
          } catch (Exception e) {
            throw new InvalidTermException("Could not determine or invoke unification method: " + e);
          }
        }
      }
    }
    throw new InvalidTermException("Could not determine or invoke unification method");
  }

  /**
   * @param theClass
   */
  private static String classKey(Class<?> theClass) {
    int level = 0;
    Class<?> c = theClass;
    while (c != Object.class) {
      level++;
      c = c.getSuperclass();
    }
    return level + "_" + theClass.getName();
  }

  public boolean unify(Struct s1, Struct s2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    if (!(s1.roName == s2.roName && s1.roArity == s2.roArity)) {
      return false;
    }
    int arity1 = s1.roArity;
    for (int i = 0; i < arity1; i++) {
      if (!unify(s1.getArg(i), vars1, s2.getArg(i), vars2, theGoalFrame)) {
        return false;
      }
    }
    return true;
  }

  public boolean unify(Struct term1, TNumber term2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    return false;
  }

  public boolean unify(Struct term1, Var term2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    // Second term is var, we prefer have it first
    return unify(term2, term1, vars2, vars1, theGoalFrame);
  }

  public boolean unify(TNumber term1, Struct term2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    return false;
  }

  public boolean unify(TNumber term1, TNumber term2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    return term1.equals(term2);
  }

  public boolean unify(TNumber term1, Var term2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    // Second term is var, we prefer have it first
    return unify(term2, term1, vars2, vars1, theGoalFrame);
  }

  public boolean unify(Var term1, Struct term2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    return unifyVarToWhatever(term1, term2, vars1, vars2, theGoalFrame);
  }

  public boolean unify(Var term1, TNumber term2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    return unifyVarToWhatever(term1, term2, vars1, vars2, theGoalFrame);
  }

  public boolean unify(Var term1, Var term2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    return unifyVarToWhatever(term1, term2, vars1, vars2, theGoalFrame);
  }

  private boolean unifyVarToWhatever(Var var1, Term term2, VarBindings vars1, VarBindings vars2, GoalFrame theGoalFrame) {
    // Variable: 
    // - when anonymous, unifies
    // - when free, bind it
    // - when bound, follow VARs until end of chain
    if (var1.isAnonymous()) {
      return true;
    }
    Binding binding1 = var1.derefToBinding(vars1);
    while (binding1.isVar()) {
      // Loop on bound variables
      binding1 = binding1.getLink();
    }
    // Followed chain to the end until we hit either a FREE or LITERAL binding
    if (binding1.isFree()) {
      // Should not bind to an anonymous variable
      if ((term2 instanceof Var) && ((Var) term2).isAnonymous()) {
        return true;
      }
      // Bind the free var
      binding1.bindTo(term2, vars2, theGoalFrame);
      return true;
    } else if (binding1.isLiteral()) {
      // We have followed term1 to end up with a literal. It may either unify or not depending if
      // term2 is a Var or the same literal. To simplify implementation we recurse with the constant
      // part as term2
      return unify(term2, vars2, binding1.getTerm(), binding1.getLiteralBindings(), theGoalFrame);
    } else {
      throw new IllegalStateException("Internal error, unexpected binding type for " + binding1);
    }
  }

  @Override
  public void deunify(GoalFrame theGoalFrame) {
    theGoalFrame.clearBindingsToMark();
  }
}