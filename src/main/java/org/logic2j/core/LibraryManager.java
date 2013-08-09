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
package org.logic2j.core;

import org.logic2j.core.library.PLibrary;
import org.logic2j.core.library.mgmt.LibraryContent;

/**
 * An API to manage libraries implementing Prolog features in Java.
 * TODO: unless this interface is needed in root package, move this in subpackage
 */
public interface LibraryManager {

  /**
   * Indicate the arity of a variable arguments predicate, such as write/N.
   * (this is an extension to classic Prolog where only fixed arity is supported).
   */
  public static final String VARARG_ARITY_INDICATOR = "N";

  public abstract LibraryContent loadLibrary(PLibrary theLibrary);

  /**
   * @return The whole library's content.
   */
  public abstract LibraryContent wholeContent();

  public abstract <T extends PLibrary> T getLibrary(Class<T> theClass);

}
