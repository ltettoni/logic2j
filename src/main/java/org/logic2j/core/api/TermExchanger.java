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
package org.logic2j.core.api;

import org.logic2j.core.api.model.symbol.Term;

/**
 * Marshall Prolog {@link Term} hierarchies to streamable representations,
 * and umnarshalls streamable representations back to {@link Term}s.
 * TODO not sure if this interface definition should reside in main package or a subpackage
 */
public interface TermExchanger {

    /**
     * Parse a character stream into a {@link Term}.
     * 
     * @param theChars
     * @return
     */
    Object unmarshall(CharSequence theChars);

    /**
     * Formats a {@link Term} into a character stream.
     * 
     * @param theTerm
     * @return
     */
    CharSequence marshall(Object theTerm);

}
