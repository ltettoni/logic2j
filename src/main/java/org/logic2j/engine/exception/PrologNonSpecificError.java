/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.logic2j.engine.exception;

/**
 * Indicate some condition that requires breaking the execution flow but for which we do not YET have a proper Exception class.
 */
public class PrologNonSpecificError extends PrologException {

  private static final long serialVersionUID = 1;

  @Override
  public boolean isStacktraceUsefulOnClient() {
    return true;
  }

  public PrologNonSpecificError(String theString) {
    super(theString);
  }

  public PrologNonSpecificError(String theString, Throwable theRootCause) {
    super(theString, theRootCause);
  }

}
