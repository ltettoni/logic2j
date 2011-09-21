package org.logic2j.model.prim;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.logic2j.model.InvalidTermException;
import org.logic2j.model.RecursionException;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;
import org.logic2j.solve.ioc.SolutionListenerBase;

/**
 * Describe a primitive, either a:
 * <ul>
 * <li>Directive</li>
 * <li>Predicate</li>
 * <li>Functor</li>
 * </ul>
 *
 */
public class PrimitiveInfo {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrimitiveInfo.class);
  private static final boolean debug = logger.isDebugEnabled();

  public static enum PrimitiveType {
    DIRECTIVE,
    PREDICATE,
    FUNCTOR
  }

  private final PrimitiveType type;
  private final String name;
  private final PLibrary library; // The library instance on which the method will be invoked (they are not static methods)
  private final Method method; // The method that implements the primitive's logic
  private final boolean varargs;

  public PrimitiveInfo(PrimitiveType theType, PLibrary theLibrary, String theName, Method theMethod, boolean theVarargs) {
    super();
    this.type = theType;
    this.library = theLibrary;
    this.name = theName;
    this.method = theMethod;
    this.varargs = theVarargs;
  }

  /**
   * @param theGoalFrame 
   * @param theGoalVars 
   * @param theGoalStruct 
   */
  public Object invoke(Struct theGoalStruct, VarBindings theGoalVars, GoalFrame theGoalFrame, SolutionListener theListener) {
    if (debug) {
      logger.debug("PRIMITIVE > invocation of {}", this);
    }
    final int arity = theGoalStruct.roArity;
    final int nbargs = isVarargs() ? 4 : (3 + arity);
    final Object[] args = new Object[nbargs];
    int i = 0;
    args[i++] = theListener;
    args[i++] = theGoalFrame;
    args[i++] = theGoalVars;
    if (isVarargs()) {
      // All arguments as an array
      final Term[] varargArray = new Term[arity];
      int j = 0;
      while (j < arity) {
        varargArray[j] = theGoalStruct.getArg(j);
        j++;
      }
      args[i++] = varargArray;
    } else {
      // Regular argument passing
      int j = 0;
      while (j < arity) {
        args[i++] = theGoalStruct.getArg(j++);
      }
    }
    try {
      Object result = getMethod().invoke(getLibrary(), args);
      if (debug) {
        logger.debug("PRIMITIVE < result={}", result);
      }
      return result;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (IllegalAccessException e) {
      throw new InvalidTermException("Could not access method " + getMethod(), e);
    } catch (InvocationTargetException e) {
      final Throwable targetException = e.getTargetException();
      if (targetException instanceof RecursionException) {
        // If we already have trouble in recursivity, don't add further exceptions - just rethrow.
        throw (RecursionException) targetException;
      }
      if (targetException instanceof StackOverflowError) {
        final RecursionException recursionException = new RecursionException("Stack overflow while executing primitive " + this);
        recursionException.stackOverflow = true;
        throw recursionException;
      }
      if (targetException instanceof RuntimeException) {
        throw (RuntimeException) targetException;
      }
      throw new InvalidTermException("Primitive threw an exception: " + this + ": " + targetException, targetException);
    }
  }

  public Term invokeFunctor(Struct theGoalStruct, VarBindings theGoalVars) {
    final GoalFrame unusedGoalFrame = null;
    final SolutionListenerBase noListener = null;
    return (Term) invoke(theGoalStruct, theGoalVars, unusedGoalFrame, noListener);
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public PrimitiveType getType() {
    return this.type;
  }

  public PLibrary getLibrary() {
    return this.library;
  }

  public Method getMethod() {
    return this.method;
  }

  public String getName() {
    return this.name;
  }

  public boolean isVarargs() {
    return this.varargs;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{lib=" + getLibrary() + ", type=" + getType() + ", name=" + getName() + ", method="
        + getMethod().getName() + '}';
  }

}
