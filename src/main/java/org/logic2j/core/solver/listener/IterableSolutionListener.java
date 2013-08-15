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
package org.logic2j.core.solver.listener;

import org.logic2j.core.model.exception.PrologNonSpecificError;
import org.logic2j.core.model.var.Bindings;

/**
 * A {@link SolutionListener} that allows the caller of the inference engine to enumerates solutions to his goal, like all Prolog APIs do.
 * This uses synchronization between two threads, the Prolog engine being the producer thread that calls back this implementation of
 * {@link SolutionListener#onSolution()}, which in turn notifies the consumer thread (the caller) of a solution.
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

    /**
     * Implementation of the core logic2j's callback-based notification of solutions.
     */
    @Override
    public Continuation onSolution() {
        // We've got one solution already!
        final Solution solution = new Solution(this.bindings);
        // Ask our client to stop requesting more and wait!
        this.clientToEngineInterface.waitUntilAvailable();
        // Provide the solution to the client, this wakes him up
        this.engineToClientInterface.hereIsTheData(solution);
        // Continue for more solutions
        return Continuation.CONTINUE;
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public SynchronizedInterface<Solution> clientToEngineInterface() {
        return this.clientToEngineInterface;
    }

    public SynchronizedInterface<Solution> engineToClientInterface() {
        return this.engineToClientInterface;
    }

    // ---------------------------------------------------------------------------
    // Synchronized interface with temporal rendez-vous and data exchange between
    // two threads.
    // ---------------------------------------------------------------------------

    public static class SynchronizedInterface<T> {
        public boolean ready = false;
        public T content = null;

        /**
         * Indicate that the peer thread can be restart - but there is no data exchanged.
         */
        public synchronized void wakeUp() {
            this.ready = true;
            this.content = null;
            this.notify();
        }

        /**
         * Indicate that the peer thread can be restart - with exchanged data.
         * 
         * @param theContent
         */
        public synchronized void hereIsTheData(T theContent) {
            this.ready = true;
            this.content = theContent;
            this.notify();
        }

        /**
         * Tell this thread that content (or just a signal without content) is expected from the other thread. If nothing is yet ready, this
         * thread goes to sleep.
         * 
         * @return The content exchanged, or null when none.
         */
        public synchronized T waitUntilAvailable() {
            while (!this.ready) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new PrologNonSpecificError("Exception not handled: " + e, e);
                }
            }
            this.ready = false;
            return this.content;
        }
    }
}
