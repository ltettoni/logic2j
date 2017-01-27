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
package org.logic2j.core.api.model.exception;

/**
 * Root class for all logic2j Prolog Exceptions - all a {@link RuntimeException}s.
 */
public abstract class PrologException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
    * Certain classes of serious exceptions require to report the stacktrace to the client application.
    * Others such as term formatting errors don't need.
    * @return true if client application should report the stacktrace in addition to the message.
    */
    public abstract boolean isStacktraceUsefulOnClient();

    public PrologException(CharSequence theMessage) {
        super(theMessage.toString());
    }

    public PrologException(CharSequence theMessage, Throwable theRootCause) {
        super(theMessage.toString(), theRootCause);
    }

}
