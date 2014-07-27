/*
 * Copyright (c) ISO (International Organization for Standardization).
 * 
 * THIS SOFTWARE IS PROVIDED BY ISOurce (ISO Source FOR SOFTWARE DEVELOPMENT AND
 * COLLABORATION) AMONG ISO/CS AND ISO NATIONAL MEMBER BODIES, "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ISOurce OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * For more information on the ISOurce project, please see
 * http://isource.iso.org/
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
