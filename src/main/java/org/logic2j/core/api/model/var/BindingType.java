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
package org.logic2j.core.api.model.var;

import org.logic2j.core.api.model.symbol.Var;

/**
 * Types of {@link Binding} that a variable can have at any time of its life cycle.
 */
public enum BindingType {

    /**
     * The {@link Var}iable associated to a {@link Binding} is currently free (ie. has no value, aka is "unbound").
     */
    FREE,

    /**
     * The {@link Var}iable associated to a {@link Binding} is bound to a literal term. The literal may be a pure constant, or a Struct
     * which further contains {@link Var}iables.
     */
    LITERAL,

    /**
     * The {@link Var}iable associated to a {@link Binding} is linked (bound) to another variable (via a linked {@link Binding}), which may
     * itself be bound to any of these {@link BindingType}s).
     */
    LINK

}
