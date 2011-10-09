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
package org.logic2j.library.impl.pojo;

import java.util.Map;
import java.util.TreeMap;

import org.logic2j.PrologImplementor;
import org.logic2j.library.impl.LibraryBase;
import org.logic2j.library.mgmt.Primitive;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.Bindings;
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
  public void bind(final SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term theBindingName, Term theTarget) {
    final Struct t1 = resolve(theBindingName, theBindings, Struct.class);
    final String name = t1.getName();
    final Object instance = extract(name);
    final Term instanceTerm = createConstantTerm(instance);
    final boolean unified = unify(instanceTerm, theBindings, theTarget, theBindings, theGoalFrame);
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

  public static Object extract(String theKey) {
    return threadLocalBindings.get().get(theKey);
  }

}
