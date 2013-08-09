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
package org.logic2j.core.solve.listener;

import org.logic2j.core.Prolog;
import org.logic2j.core.solve.holder.SolutionHolder;

/**
 * The lowest-level API through which the inference engine provides solutions.
 * For easier programming, consider using {@link Prolog#solve(CharSequence)} and the
 * {@link SolutionHolder} API.
 */
public interface SolutionListener {

	/**
	 * Specifies the behaviour that calling code requests to the inference engine
	 * after a solution was found, via {@link SolutionListener#onSolution()}.
	 * @author tettoni
	 */
	public static enum Continuation {
		/**
		 * Value that {@link #onSolution()} must return for the inference engine 
		 * to continue solving (search for alternate solutions).
		 */
		CONTINUE,
		/**
		 * Value that {@link #onSolution()} must return for the inference engine 
		 * to stop solving (ie. means caller requests abort).
		 */
		USER_ABORT;


		public static Continuation requestContinuationWhen(boolean conditionApplies) {
			return conditionApplies ? CONTINUE : USER_ABORT;
        }

		public boolean isContinuing() {
	        return this==CONTINUE;
        }
		public boolean isUserAbort() {
			return this==USER_ABORT;
        }
	}
	
  /**
   * The inference engine notifies the caller code that a solution 
   * was proven; the real content to the solution must be
   * retrieved from the goal's variables.
   * 
   * @return The caller must return {@link #CONTINUE} ({@value SolutionListener#CONTINUE} for the inference engine to
   * continue searching for other solutions, or false
   * to break the search for other solutions (ie. user cancellation).
   */
  public Continuation onSolution();

}
