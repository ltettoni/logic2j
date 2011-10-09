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
package org.logic2j.solve.ioc;

import org.logic2j.model.var.Bindings;
import org.logic2j.solve.Solution;

/**
 * A {@link SolutionListener} that will focus on the first solution only.
 *
 */
public class IterableSolutionListener implements SolutionListener {

  protected Bindings bindings;

  /**
   * @param theBindings
   */
  public IterableSolutionListener(Bindings theBindings) {
    super();
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
    final Solution solution = new Solution(this.bindings);
    this.requestSolution.waitUntilAvailable();
    this.provideSolution.available(solution);
    return true;
  }

}
