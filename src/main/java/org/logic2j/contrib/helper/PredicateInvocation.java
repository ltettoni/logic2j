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
package org.logic2j.contrib.helper;


import java.util.Arrays;

/**
 * Bundles a predicate name, and a number of parameters,
 * and builds the Prolog goal to represent it.
 *
 * @version $Id$
 */
public class PredicateInvocation {

  private final String predicateName;
  private final String projectionVariable; // What we want to project. Normally one of the arguments.
  private final Object[] arguments;

  /**
   * Private constructor, application code should use static factories.
   * @param theProjectionVariable
   * @param thePredicateName
   * @param theArguments
   */
  private PredicateInvocation(String theProjectionVariable, String thePredicateName, Object... theArguments) {
    super();
    this.projectionVariable = theProjectionVariable;
    this.predicateName = thePredicateName;
    this.arguments = theArguments;
  }


  
  
  /**
   * Create a PredicateInvocation for checking a goal without obtaining results.
   * @param thePredicateName
   * @param theArguments Any objects, the configured TermAdapter will be used.
   * @return A PredicateInvocation that is not projecting a variable.
   */
  public static PredicateInvocation invocation(String thePredicateName, Object... theArguments) {
    final PredicateInvocation invocation = new PredicateInvocation(null, thePredicateName, theArguments);
    return invocation;
  }

  
  /**
   * Create a PredicateInvocation for projecting a single variable. This means: give me all X such that predicate(...., X, ...) yields true.
   * @param theProjectionVariable
   * @param thePredicateName
   * @param theArguments May be Object, toString() will be used
   * @return A PredicateInvocation that is projecting a variable.
   */
  public static PredicateInvocation projection(String theProjectionVariable, String thePredicateName, Object... theArguments) {
    final PredicateInvocation invocation = new PredicateInvocation(theProjectionVariable, thePredicateName, theArguments);
    // Normally we should (at least) warn if theProjectionVariable does not appear within arguments...
    return invocation;
  }

  /**
   * @return the name of the projectionVariable
   */
  public String getProjectionVariable() {
    return this.projectionVariable;
  }

  public String getPredicateName() {
    return predicateName;
  }

  public Object[] getArguments() {
    return arguments;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
    sb.append('{');
    if (this.getProjectionVariable()!=null) {
      sb.append(getProjectionVariable());
      sb.append('|');
    }
    sb.append(getPredicateName());
    sb.append(Arrays.asList(getArguments()));
    sb.append('}');
    return sb.toString();
  }
}
