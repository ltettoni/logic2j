package org.logic2j.solve.ioc;

import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.Solution;

/**
 * A {@link SolutionListener} that will focus on the first solution only.
 *
 */
public class IterableSolutionListener implements SolutionListener {

  protected Term goal;
  protected VarBindings bindings;

  /**
   * @param theGoal
   * @param theBindings
   */
  public IterableSolutionListener(Term theGoal, VarBindings theBindings) {
    super();
    this.goal = theGoal;
    this.bindings = theBindings;
  }

  /**
   * Synchronized interface with temporal and content exchange between 
   * two threads.
   *
   */
  public static class SynchronizedInterface<T> {
    public boolean ready = false;
    public T content = null;

    /**
     * Indicate that the other thread can be restart - but without exchanged content.
     */
    public synchronized void available() {
      this.ready = true;
      this.content = null;
      this.notify();
    }

    /**
     * Indicate that the other thread can be restart - with exchanged content.
     * @param theContent
     */
    public synchronized void available(T theContent) {
      this.ready = true;
      this.content = theContent;
      this.notify();
    }

    /**
     * Tell this thread that content (or just a signal without content) is 
     * expected from the other thread. If nothing is yet ready, this thread
     * goes to sleep.
     * @return The content exchanged, or null when none.
     */
    public synchronized T waitUntilAvailable() {
      while (!this.ready) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException("Exception not handled: " + e, e);
        }
      }
      this.ready = false;
      return this.content;
    }
  }

  /**
   * Interface between the main thread (consumer) and the prolog solver thread (producer).
   */
  public SynchronizedInterface<Solution> requestSolution = new SynchronizedInterface<Solution>();

  /**
   * Interface between the prolog solver thread (producer) and the main thread (consumer).
   */
  public SynchronizedInterface<Solution> provideSolution = new SynchronizedInterface<Solution>();

  @Override
  public boolean onSolution() {
    final Solution solution = new Solution(this.goal, this.bindings);
    this.requestSolution.waitUntilAvailable();
    this.provideSolution.available(solution);
    return true;
  }

}
