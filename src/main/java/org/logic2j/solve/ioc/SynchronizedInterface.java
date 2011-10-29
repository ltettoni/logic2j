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

/**
 * Synchronized interface with temporal rendez-vous and data exchange between 
 * two threads.
 */
public class SynchronizedInterface<T> {
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
   * @param theContent
   */
  public synchronized void hereIsTheData(T theContent) {
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