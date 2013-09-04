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
package org.logic2j.core.api.model.exception;

/**
 * Indicate an internal error in the Prolog implementation, or a condition that should never have happened, or some condition that was not
 * specific enough to raise a better exception, or some condition that requires breaking the execution flow but for which we do not YET have
 * a proper Exception class.
 */
public class PrologInternalError extends PrologException {

    private static final long serialVersionUID = 1;

    public PrologInternalError(String theString) {
        super(theString);
    }

    public PrologInternalError(String theString, Throwable theRootCause) {
        super(theString, theRootCause);
    }

}
