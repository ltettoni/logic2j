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

import org.logic2j.model.exception.IllegalSolutionException;
import org.logic2j.model.var.Bindings;

/**
 * A {@link SolutionListener} that will collect only the first solution yet make sure
 * there is no other solution provided by the solver.
 *
 */
public class FirstSolutionListener extends SingleSolutionListener {

  public FirstSolutionListener(Bindings theBindings) {
    // We are only interested in the first result so we will tell the SolutionListener
    // to stop the solver after the first solution. Using this argument we won't be
    // able to tell if there are actually more, or not. But we are not interested.
    super(theBindings, 1);
  }

  @Override
  protected void onSuperfluousSolution() {
    throw new IllegalSolutionException("Should not happen we have asked the SolutionListener to stop after one");
  }

}
