package org.logic2j.library.impl.pojo;

import java.util.Map;
import java.util.TreeMap;

import org.logic2j.PrologImplementor;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;

/**
 */
public class PojoLibrary extends LibraryBase {
  private static final ThreadLocal<Map<String, Object>> threadLocalBindings = new ThreadLocal<Map<String, Object>>() {

    @Override
    protected Map<String, Object> initialValue() {
      return new TreeMap<String, Object>();
    }

  };

  public PojoLibrary(PrologImplementor theProlog) {
    super(theProlog);
  }

  @Primitive
  public void bind(final SolutionListener theListener, GoalFrame theGoalFrame, VarBindings vars, Term theBindingName, Term theTarget) {
    final Struct t1 = resolve(theBindingName, vars, Struct.class);
    final String name = t1.getName();
    final Object instance = extract(name);
    final Term instanceTerm = createConstantTerm(instance);
    final boolean unified = unify(instanceTerm, vars, theTarget, vars, theGoalFrame);
    notifyIfUnified(unified, theGoalFrame, theListener);
  }

  /**
   * A utility method to emulate calling the bind/2 predicate from Java.
   * @param theKey
   * @param theValue
   */
  public static void bind(String theKey, Object theValue) {
    threadLocalBindings.get().put(theKey, theValue);
  }

  public static <T> T extract(String theKey) {
    return (T) threadLocalBindings.get().get(theKey);
  }

}
