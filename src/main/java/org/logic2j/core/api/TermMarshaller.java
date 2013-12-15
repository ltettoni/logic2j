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
 * Marshall Prolog {@link Term} hierarchies to streamable representations.
 * <p>
 * <em>
 * Marshalling (Wikipedia): In computer science, marshalling (sometimes spelled marshaling) is the process of
 * transforming the memory representation of an object to a data format suitable for
 * storage or transmission, and it is typically used when data must be moved between
 * different parts of a computer program or from one program to another
 * </em>
 * </p>
 */
public interface TermMarshaller {

    /**
     * Formats a {@link Term} to its character representation.
     * 
     * @param theTerm
     * @return The character representation of theTerm.
     */
    CharSequence marshall(Object theTerm);

}
