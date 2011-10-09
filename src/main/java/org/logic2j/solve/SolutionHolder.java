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
package org.logic2j.solve;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.logic2j.PrologImplementor;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.Bindings;
import org.logic2j.model.var.Bindings.FreeVarBehaviour;
import org.logic2j.solve.ioc.IterableSolutionListener;
import org.logic2j.solve.ioc.SolutionListenerBase;
import org.logic2j.solve.ioc.UniqueSolutionListener;

/**
 * Holds state necessary to describe the unique solution or multiple solutions 
 * to a goal; this object lies between the expression of a goal (a request or query) and 
 * the extraction of all aspects of the solution (a response or results).<br/>
 * This object exposes strongly-typed, templated methods to extract results 
 * depending on how the calling code expects them (unique or multiple solutions),
 * and the type of data needed (just the number, resolved-term solutions or single variable bindings).<br/>
 * Depending on the case, the actual calculation of the goal may be performed immediately 
 * (then results are stored and returned as needed), or delayed until access methods are called.
 *
 * TODO Maybe have a way to allow limiting "all" to a reasonable number.
 * 
 * <p/>
 * This type of API for extracting results from a data layer should further be analyzed and
 * compared to other APIs such as JDBC, JNDI, SAX/DOM, or more exotic ones such as 
 * JSon (MongoDB/Apache CouchDB), Neo4j and Protégé. Also RDF frameworks APIs may be considered.
 */
public class SolutionHolder {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolutionHolder.class);

  /**
   * A relay object to provide access to the results of the (expected) unique solution to a goal.
   */
  public static class UniqueSolutionHolder {

    private Solution solution;

    /**
     * @param theSolution 
     */
    UniqueSolutionHolder(Solution theSolution) {
      this.solution = theSolution;
    }

    /**
     * @param theVariableName
     * @return The value of var theVariableName
     * @see Solution#getBinding(String)
     */
    public Term binding(String theVariableName) {
      return this.solution.getBinding(theVariableName);
    }

    /**
     * @return All bindings.
     */
    public Map<String, Term> bindings() {
      return this.solution.getBindings();
    }

    /**
     * @return The solution as a {@link Term}.
     */
    public Term solution() {
      return this.solution.getSolution();
    }
  }

  /**
   * A relay object to provide access to the results of the (expected) multiple solutions to a goal.
   */
  public class MultipleSolutionsHolder {

    MultipleSolutionsHolder() {
      // Empty
    }

    private Integer lowest = null;
    private Integer highest = null;

    /**
     * Solves the goal and calculate number of results.
     * @return The number of solutions.
     */
    public int number() {
      final SolutionListenerBase listener = new SolutionListenerBase();
      SolutionHolder.this.prolog.getSolver().solveGoal(SolutionHolder.this.bindings, new GoalFrame(),
          listener);
      int counter = listener.getCounter();
      checkBounds(counter);
      return counter;
    }

    private void checkBounds(int counter) {
      if (this.lowest != null && counter < this.lowest) {
        throw new IllegalStateException("Number of solutions was expected to be at least " + this.lowest + " but was " + counter);
      }
      if (this.highest != null && counter > this.highest) {
        throw new IllegalStateException("Number of solutions was expected to be at most " + this.highest + " but was " + counter);
      }
    }

    /**
     * Solves the goal and extract the named variable values.
     * @param theVariableName
     */
    public List<Term> binding(final String theVariableName) {
      final List<Term> results = new ArrayList<Term>();
      final SolutionListenerBase listener = new SolutionListenerBase() {

        @Override
        public boolean onSolution() {
          Term substituted = SolutionHolder.this.bindings.explicitBinding(theVariableName, FreeVarBehaviour.FREE);
          results.add(substituted);
          return super.onSolution();
        }

      };
      SolutionHolder.this.prolog.getSolver().solveGoal(SolutionHolder.this.bindings, new GoalFrame(),
          listener);
      int size = results.size();
      checkBounds(size);
      return results;
    }

    /**
     * Solves the goal and extract all bindings.
     */
    public List<Map<String, Term>> bindings() {
      final List<Map<String, Term>> results = new ArrayList<Map<String, Term>>();
      final SolutionListenerBase listener = new SolutionListenerBase() {

        @Override
        public boolean onSolution() {
          results.add(SolutionHolder.this.bindings.explicitBindings(FreeVarBehaviour.FREE));
          return super.onSolution();
        }

      };
      SolutionHolder.this.prolog.getSolver().solveGoal(SolutionHolder.this.bindings, new GoalFrame(),
          listener);
      int size = results.size();
      checkBounds(size);
      return results;
    }

    /**
     * @param theExpectedExactNumber
     * @return this
     */
    public MultipleSolutionsHolder ensureNumber(int theExpectedExactNumber) {
      this.lowest = this.highest = theExpectedExactNumber;
      return this;
    }

    public MultipleSolutionsHolder ensureRange(int thePermissibleLowest, int thePermissibleHighest) {
      this.lowest = thePermissibleLowest;
      this.highest = thePermissibleHighest;
      return this;
    }

  }

  final PrologImplementor prolog;
  final Bindings bindings;

  /**
   * @param theBindings
   */
  public SolutionHolder(PrologImplementor theProlog, Bindings theBindings) {
    this.prolog = theProlog;
    this.bindings = theBindings;
  }

  /**
   * @return The value of {@link #all()}.{@link #all.number()}.
   */
  public int number() {
    return all().number();
  }

  /**
   * @return A relay object to access all solutions. Calling all() does NOT start solving.
   */
  public MultipleSolutionsHolder all() {
    return new MultipleSolutionsHolder();
  }

  /**
   * Solves the goal, and holds the solution for further dereferencing.
   * @return A relay object to access the unique solution.
   */
  public UniqueSolutionHolder unique() {
    final UniqueSolutionListener listener = new UniqueSolutionListener(this.bindings);
    this.prolog.getSolver().solveGoal(this.bindings, new GoalFrame(), listener);
    return new UniqueSolutionHolder(listener.getSolution());
  }

  public Iterator<Solution> iterator() {
    final IterableSolutionListener listener = new IterableSolutionListener(SolutionHolder.this.bindings);

    final Runnable prologSolverThread = new Runnable() {

      @Override
      public void run() {
        logger.debug("Started producer thread");
        // Start solving in a parallel thread, and rush to first solution (that will be called back in the listener)
        // and will wait for the main thread to extract it
        SolutionHolder.this.prolog.getSolver().solveGoal(SolutionHolder.this.bindings, new GoalFrame(), listener);
        logger.debug("Producer thread finishes");
        // Last solution was extracted. Producer's callback won't now be called any more - so to 
        // prevent the consumer for listening forever for the next solution that won't come...
        // The 2 following lines are exactly the sames as those in the listener's onSolution()
        listener.requestSolution.waitUntilAvailable();
        listener.provideSolution.available();
      }
    };
    new Thread(prologSolverThread).start();

    return new Iterator<Solution>() {

      private Solution solution;

      @Override
      public boolean hasNext() {
        listener.requestSolution.available();
        this.solution = listener.provideSolution.waitUntilAvailable();
        return this.solution != null;
      }

      @Override
      public Solution next() {
        if (this.solution == null) {
          throw new IllegalStateException(
              "Program error: next() called when either hasNext() did not return true previously, or next() was called more than once");
        }
        final Solution toReturn = this.solution;
        // Indicate that we have just "consumed" the solution, and any subsequent call to next() without first calling hasNext() will fail.
        this.solution = null;
        return toReturn;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("iterator() provides a read-only Term interator, cannot remove elements");
      }

    };
  }

}
