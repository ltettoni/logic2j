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
package org.logic2j.core.model.exception;

/**
 * Indicate that recursion limit has been reached by the solver. Caused by either too many loops, or a JVM's {@link StackOverflowError} was
 * caught somewhere.
 */
public class RecursionException extends PrologException {

    private static final long serialVersionUID = -4416801118548866803L;

    public boolean stackOverflow = false;

    public RecursionException(String theString) {
        super(theString);
    }

    public RecursionException(String theString, Throwable theRootCause) {
        super(theString, theRootCause);
    }

}
