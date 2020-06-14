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
package org.logic2j.core.api.library;

import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.exception.RecursionException;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.solver.listener.SolutionListener;
import org.logic2j.engine.unify.UnifyContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;

/**
 * Describe a {@link org.logic2j.core.api.library.annotation.Predicate}, which is either a:
 * <ul>
 * <li>Directive - not yet supported</li>
 * <li>Predicate - a "function" in Prolog, will produce solutions</li>
 * <li>Functor - evaluates to a result, for example the addition of two numbers</li>
 * </ul>
 *
 * @note Strangely, this class has invoke() features so it's not only a description!
 */
public class PrimitiveInfo {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrimitiveInfo.class);
  private static final boolean isDebug = logger.isDebugEnabled();

  // Not functional - only used to warn once per JVM that inefficient invocation is in place
  private static final HashSet<String> methodNeedingReflectiveInvocation = new HashSet<>();


  public enum PrimitiveType {
    /**
     * Not yet implemented.
     */
    DIRECTIVE,
    /**
     * A predicate implemented as a Java method, will produce solution(s) through a {@link SolutionListener} interface.
     */
    PREDICATE,
    /**
     * A functor yields a result, such as +(2,3).
     */
    FUNCTOR
  }


  private final PrimitiveType type;
  private final String name;
  private final String methodName;
  private final PLibrary library; // The library instance on which the method will be invoked (they are not static methods)
  private final Method method; // The method that implements the primitive's logic
  private final boolean isVarargs;

  public PrimitiveInfo(PrimitiveType theType, PLibrary theLibrary, String theName, Method theMethod, boolean theVarargs) {
    super();
    this.type = theType;
    this.library = theLibrary;
    this.name = theName;
    this.method = theMethod;
    this.methodName = theMethod.getName().intern();
    this.isVarargs = theVarargs;
  }

  public Object invoke(Struct theGoalStruct, UnifyContext currentVars) {
    final Object result = this.library.dispatch(this.methodName, theGoalStruct, currentVars);
    if (result != PLibrary.NO_DIRECT_INVOCATION_USE_REFLECTION) {
      return result;
    }
    // We did not do a direct invocation - we will have to rely on reflection
    if (!methodNeedingReflectiveInvocation.contains(this.methodName)) {
      logger.warn("Invocation of library primitive \"{}\" for goal \"{}\" uses reflection - consider implementing {}.dispatch()", this.methodName,
              theGoalStruct, this.library);
      // Avoid multiple logging
      methodNeedingReflectiveInvocation.add(this.methodName);
    }
    try {
      if (isDebug) {
        logger.debug("PRIMITIVE > invocation of {}", this);
      }
      return invokeReflective(theGoalStruct, currentVars);
    } catch (final IllegalArgumentException e) {
      throw e;
    } catch (final IllegalAccessException e) {
      throw new InvalidTermException("Could not access method " + this.method, e);
    } catch (final InvocationTargetException e) {
      final Throwable targetException = e.getTargetException();
      if (targetException instanceof RecursionException) {
        // If we already have trouble in recursion, don't add further exceptions - just rethrow.
        throw (RecursionException) targetException;
      }
      if (targetException instanceof StackOverflowError) {
        final RecursionException recursionException = new RecursionException("Stack overflow while executing primitive " + this);
        throw recursionException;
      }
      if (targetException instanceof RuntimeException) {
        throw (RuntimeException) targetException;
      }
      throw new InvalidTermException("Primitive threw an exception: " + this + ": " + targetException, targetException);
    } catch (final Throwable e) {
      throw new InvalidTermException("Invocation of method " + this.method + " went really bad: " + e, e);
    }
  }

  /**
   * @param theGoalStruct
   * @param currentVars
   * @throws java.lang.reflect.InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  private Object invokeReflective(Struct theGoalStruct, UnifyContext currentVars)
          throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    final int arity = theGoalStruct.getArity();
    final int nbArgs = this.isVarargs ? 2 : (1 + arity);
    final Object[] args = new Object[nbArgs];
    int i = 0;
    args[i++] = currentVars;
    if (this.isVarargs) {
      // All arguments as an array
      final Object[] varargArray = new Object[arity];
      int j = 0;
      while (j < arity) {
        varargArray[j] = theGoalStruct.getArg(j);
        j++;
      }
      args[i] = varargArray;
    } else {
      // Regular argument passing
      int j = 0;
      while (j < arity) {
        args[i++] = theGoalStruct.getArg(j++);
      }
    }
    final Object result = this.method.invoke(this.library, args);
    if (isDebug) {
      logger.debug("PRIMITIVE < result={}", result);
    }
    return result;

  }

  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public PrimitiveType getType() {
    return this.type;
  }

  // ---------------------------------------------------------------------------
  // Methods of java.lang.Object
  // ---------------------------------------------------------------------------

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{lib=" + this.library + ", type=" + getType() + ", name=" + this.name + ", method=" + this.method
            .getName() + '}';
  }

}
