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
 * A base implementation of {@link SolutionListener} holds a counter of the number of solutions reached,
 * and whose {@link #onSolution()} returns always true (potentially infinite generation).
 * 
 */
public class SolutionListenerBase implements SolutionListener {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolutionListenerBase.class);
  private static boolean debug = logger.isDebugEnabled();

  private long startTime = System.currentTimeMillis();
  private long lastSolutionTime = this.startTime;
  private int counter = 0;

  @Override
  public boolean onSolution() {
    if (debug) {
      logger.debug(" onSolution(), iter=#{}", this.counter);
    }
    this.counter++;
    this.lastSolutionTime = System.currentTimeMillis();
    return true;
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public int getCounter() {
    return this.counter;
  }

  public long elapse() {
    return this.lastSolutionTime - this.startTime;
  }

}
