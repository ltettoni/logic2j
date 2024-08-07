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
package org.logic2j.contrib.library.pojo;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.logic2j.contrib.library.OptionsString;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.library.annotation.Functor;
import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.exception.PrologNonSpecificException;
import org.logic2j.engine.model.PrologLists;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.solver.Continuation;
import org.logic2j.engine.unify.UnifyContext;

/**
 * Relate Java POJOs to Prolog variables: used to instantiate Java objects, access properties (setters, getters),
 * and convert Java collections to Prolog lists.
 */
public class PojoLibrary extends LibraryBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PojoLibrary.class);

  public PojoLibrary(PrologImplementation theProlog) {
    super(theProlog);
  }

  @Override
  public Object dispatch(String theMethodName, Struct<?> theGoalStruct, UnifyContext currentVars) {
    final Object result;
    // Argument methodName is {@link String#intern()}alized so OK to check by reference
    final Object[] args = theGoalStruct.getArgs();
    final int arity = theGoalStruct.getArity();
    if (theMethodName == "javaNew") {
      result = javaNew(currentVars, args);
    } else if (arity == 1) {
      result = NO_DIRECT_INVOCATION_USE_REFLECTION;
    } else if (arity == 2) {
      final Object arg0 = args[0];
      final Object arg1 = args[1];
      if (theMethodName == "bind") {
        result = bind(currentVars, arg0, arg1);
      } else {
        result = NO_DIRECT_INVOCATION_USE_REFLECTION;
      }
    } else if (arity == 3) {
      final Object arg0 = args[0];
      final Object arg1 = args[1];
      final Object arg2 = args[2];
      if (theMethodName == "property") {
        result = property(currentVars, arg0, arg1, arg2);
      } else {
        result = NO_DIRECT_INVOCATION_USE_REFLECTION;
      }
    } else if (arity == 4) {
      final Object arg0 = args[0];
      final Object arg1 = args[1];
      final Object arg2 = args[2];
      final Object arg3 = args[3];
      if (theMethodName == "property") {
        result = property(currentVars, arg0, arg1, arg2, arg3);
      } else {
        result = NO_DIRECT_INVOCATION_USE_REFLECTION;
      }
    } else {
      result = NO_DIRECT_INVOCATION_USE_REFLECTION;
    }
    return result;
  }

  /**
   * Override this method with whatever introspection framework you want.
   * Here we use BeanUtils.
   *
   * @param theInstance
   * @param theExpression
   * @return The value introspected
   */
  protected Object introspect(Object theInstance, String theExpression) {
    final Object value;
    try {
      value = PropertyUtils.getProperty(theInstance, theExpression);
    } catch (IllegalAccessException e) {
      throw new PrologNonSpecificException("Could not get property \"" + theExpression + "\" from object: " + theInstance + ": " + e);
    } catch (NoSuchMethodException e) {
      return null;
      // throw new PrologNonSpecificError("Could not get property \"" + theExpression + "\" from object: " + theInstance + ": " + e);
    } catch (InvocationTargetException e) {
      throw new PrologNonSpecificException(
              "Could not get property \"" + theExpression + "\" from object: " + theInstance + ": " + e.getTargetException());
    }
    return value;
  }


  private void inject(Object pojo, String theExpression, Object newValue) {
    try {
      PropertyUtils.setProperty(pojo, theExpression, newValue);
    } catch (IllegalAccessException | NoSuchMethodException e) {
      throw new PrologNonSpecificException("Could not set property \"" + theExpression + "\" from object: " + pojo + ": " + e);
    } catch (InvocationTargetException e) {
      throw new PrologNonSpecificException("Could not set property \"" + theExpression + "\" from object: " + pojo + ": " + e.getTargetException());
    }
  }

  @Predicate
  public int property(UnifyContext currentVars, Object thePojo, Object thePropertyName, Object theValue) {
    return property(currentVars, thePojo, thePropertyName, theValue, null);
  }


  /**
   * Extraction of values from POJO from reflection.
   * NOTE: Implementation far from complete: read-only, and requires property name to be specified (cannot extract all by backtracking a free var).
   *
   * @param currentVars
   * @param thePojo
   * @param thePropertyName
   * @param theValue
   * @param theOptions      Comma-separated list of "r" for read, "w" for write.
   * @return
   */
  @Predicate
  public int property(UnifyContext currentVars, Object thePojo, Object thePropertyName, Object theValue,
                      Object theOptions) {
    // First argument
    final Object pojo = currentVars.reify(thePojo);
    ensureBindingIsNotAFreeVar(pojo, "property/3", 0);
    // Second argument
    final Object propertyName = currentVars.reify(thePropertyName);
    ensureBindingIsNotAFreeVar(propertyName, "property/3", 1);
    // Invocation mode
    OptionsString mode = new OptionsString(currentVars, theOptions, "r");

    //
    Object currentValue = introspect(pojo, (String) propertyName);
      switch (currentValue) {
          case null -> {
              if (mode.hasOption("r")) {
                  // Null value means no solution (property does "not exist")
                  return Continuation.CONTINUE;
              }
              if (mode.hasOption("w")) {
                  // If value passed as argument is defined, will set
                  final Object newValue = currentVars.reify(theValue);
                  inject(pojo, (String) propertyName, newValue);

                  return notifySolution(currentVars);
              }
              throw new PrologNonSpecificException("Option \"" + mode + "\" is not allowed");
          }

          // Collections will send multiple individual solutions
          case Collection collection -> {
              for (Object javaElem : collection) {
                  final Object prologRepresentation = getProlog().getTermAdapter().toTerm(javaElem, FactoryMode.ATOM);
                  final int result = unifyAndNotify(currentVars, prologRepresentation, theValue);
                  if (result != Continuation.CONTINUE) {
                      return result;
                  }
              }
              return Continuation.CONTINUE;
          }
          case Object[] objects -> {
              for (Object javaElem : objects) {
                  final Object prologRepresentation = getProlog().getTermAdapter().toTerm(javaElem, FactoryMode.ATOM);
                  final int result = unifyAndNotify(currentVars, prologRepresentation, theValue);
                  if (result != Continuation.CONTINUE) {
                      return result;
                  }
              }
              return Continuation.CONTINUE;
          }
          default -> {
          }
      }
      // Convert java objects to Prolog terms
    final Object prologRepresentation = getProlog().getTermAdapter().toTerm(currentValue, FactoryMode.ATOM);
    return unifyAndNotify(currentVars, prologRepresentation, theValue);
  }


  /**
   * Get, set or check a variable.
   * If theTarget is a free Var, then:
   * If the binding has a value, unify it to the free Var (this means, gets the value)
   * If the binding has no value, do nothing (but yields a successful solution)
   * If theTarget is a bound Var, then:
   * If the binding has no value, set the bound Var's value into the binding
   * If the binding has a value unifiable to theVar, succeed without other effect
   * If the binding has a value that cannot be unified, fails
   *
   * @param currentVars
   * @param theBindingName The name to use for the binding
   * @param theTarget      Typically a Var. If a value was bound: will unify with it. If no value was bound, unify with the anonymous variable.
   * @return One solution, either theTarget is unified to a real value, or is left unchanged (unified to the anonymous var)
   */
  @Predicate
  public int bind(UnifyContext currentVars, Object theBindingName, Object theTarget) {
    final Object nameTerm = currentVars.reify(theBindingName);
    ensureBindingIsNotAFreeVar(nameTerm, "bind/2", 0);

    final String name = nameTerm.toString();
    final Object bindingValue = this.getProlog().getTermAdapter().getVariable(name);
    final boolean bindingIsDefined = bindingValue != null;

    final Object targetTerm = currentVars.reify(theTarget);
    final boolean targetIsFree = targetTerm instanceof Var;

    // Implement the logic as per the spec defined in comment above
    final int result;
    if (targetIsFree) {
      if (bindingIsDefined) {
        // Getting value
        result = unifyAndNotify(currentVars, bindingValue, theTarget);
      } else {
        // Nothing to unify but escalate a solution
        result = notifySolution(currentVars);
      }
    } else {
      if (bindingIsDefined) {
        // Try to unify, will succeed or not
        result = unifyAndNotify(currentVars, bindingValue, theTarget);
      } else {
        // Set the value (and return successful solution)
        this.getProlog().getTermAdapter().setVariable(name, targetTerm);
        result = notifySolution(currentVars);
      }
    }
    return result;
  }

  /**
   * Instantiate Java object calling one of its constructors.
   * For example: X is javaNew('java.lang.String', 'text')
   *
   * @param currentVars
   * @param args        First argument is the fully qualified class name, then remaining arguments are passed to constructor.
   * @return Java invocation of constructor
   */
  @Functor
  public Object javaNew(UnifyContext currentVars, Object... args) {
    // More generic instantiation than the TermFactory
    final Object className = currentVars.reify(args[0]);
    ensureBindingIsNotAFreeVar(className, "javaNew", 0);
    try {
      final Class<?> aClass = Class.forName(className.toString());
      if (Enum.class.isAssignableFrom(aClass)) {
        final String enumName = currentVars.reify(args[1]).toString();

        final Enum[] enumConstants = ((Class<Enum<?>>) aClass).getEnumConstants();
        for (Enum c : enumConstants) {
          if (c.name().equals(enumName)) {
            return c;
          }
        }
        throw new IllegalArgumentException("Enum class " + aClass + ": no such enum value " + enumName);
      } else {
        // Regular Pojo
        try {
          final int nbArgs = args.length - 1;
          if (nbArgs == 0) {
            return aClass.newInstance();
          } else {
            // Collect arguments and their types
            final Object[] constructorArgs = new Object[nbArgs];
            final Class<?>[] constructorClasses = new Class<?>[nbArgs];
            for (int i = 1; i <= nbArgs; i++) {
              constructorArgs[i - 1] = currentVars.reify(args[i]);
              constructorClasses[i - 1] = constructorArgs[i - 1].getClass();
            }
            // Instantiation - this is very far from being robust !
            return aClass.getConstructor(constructorClasses).newInstance(constructorArgs);
          }
        } catch (InstantiationException e) {
          throw new PrologNonSpecificException(this + " could not create instance of " + aClass + ", args=" + Arrays.asList(args) + " : " + e);
        } catch (IllegalAccessException e) {
          throw new PrologNonSpecificException(this + " could not create instance of " + aClass + ", args=" + Arrays.asList(args) + " : " + e);
        } catch (NoSuchMethodException e) {
          throw new PrologNonSpecificException(this + " could not create instance of " + aClass + ", args=" + Arrays.asList(args) + " : " + e);
        } catch (InvocationTargetException e) {
          throw new PrologNonSpecificException(this + " could not create instance of " + aClass + " constructor failed with: " + e.getTargetException());
        }
      }
    } catch (ClassNotFoundException e) {
      throw new InvalidTermException("Cannot instantiate term of class \"" + className + "\": " + e);
    }
  }

  /**
   * Unify Prolog list to Java list, in two directions.
   *
   * @param currentVars
   * @param prologList
   * @param javaList
   * @return
   */
  @Predicate
  public int javaList(UnifyContext currentVars, Object prologList, Object javaList) {
    final Object pList = currentVars.reify(prologList);
    final Object jList = currentVars.reify(javaList);
    if (javaList instanceof Var<?>) {
      // Prolog to Java
      if (!PrologLists.isList(pList)) {
        // No solution
        return Continuation.CONTINUE;
      }
      final List<Object> elements = new ArrayList<>();
      PrologLists.javaListFromPList(((Struct<?>) pList), elements, Object.class);
      return unifyAndNotify(currentVars, elements, jList);
    } else {
      if (!(jList instanceof List<?>)) {
        // No solution
        return Continuation.CONTINUE;
      }
      final Struct<?> elements = PrologLists.createPList((List) jList);
      return unifyAndNotify(currentVars, elements, pList);
    }
  }

}
