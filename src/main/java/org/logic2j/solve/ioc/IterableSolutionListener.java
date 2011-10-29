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
 * A {@link SolutionListener} that allows the caller of the resolution engine
 * to enumerates solutions to his goal, like all Prolog APIs do.
 * This uses synchronization between two threads, the Prolog engine being the producer
 * thread that calls back this implementation of {@link SolutionListener#onSolution()}, which 
 * in turn notifies the consumer thread (the caller) of a solution.
 */
public class IterableSolutionListener implements SolutionListener {

  private Bindings bindings;

  /**
   * @param theBindings
   */
  public IterableSolutionListener(Bindings theBindings) {
    super();
    this.bindings = theBindings;
  }

  /**
   * Interface between the main thread (consumer) and the prolog solver thread (producer).
   */
  private SynchronizedInterface<Solution> clientToEngineInterface = new SynchronizedInterface<Solution>();

  /**
   * Interface between the prolog solver thread (producer) and the main thread (consumer).
   */
  private SynchronizedInterface<Solution> engineToClientInterface = new SynchronizedInterface<Solution>();

  @Override
  public boolean onSolution() {
    // We've got one solution already!
    final Solution solution = new Solution(this.bindings);
    // Ask our client to stop requesting more!
    this.clientToEngineInterface.waitUntilAvailable();
    // Provide the solution to the client, this wakes him up
    this.engineToClientInterface.hereIsTheData(solution);
    // Continue for more solutions
    return true;
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public SynchronizedInterface<Solution> clientToEngineInterface() {
    return this.clientToEngineInterface;
  }

  public SynchronizedInterface<Solution> engineToClientInterface() {
    return this.engineToClientInterface;
  }

}
