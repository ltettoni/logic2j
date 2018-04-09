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
package org.logic2j.contrib.rdb.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * A {@link java.util.LinkedHashMap} whose values are {@link java.util.Collection}s of TypeValue.
 */
public class CollectionMap<TypeKey, TypeValue> extends LinkedHashMap<TypeKey, Collection<TypeValue>> {
  private static final long serialVersionUID = 1L;

  /**
   * Add a value to the collection addressed by theKey. If nothing was registered a new collection is instantiated by
   * {@link #instantiateCollection()} and the theElement is added to it.
   *
   * @param theKey
   * @param theElement
   */
  public void add(TypeKey theKey, TypeValue theElement) {
    Collection<TypeValue> collection = get(theKey);
    if (collection == null) {
      collection = instantiateCollection();
      put(theKey, collection);
    }
    collection.add(theElement);
  }

  /**
   * Override this to provide your preferred implementation of Collection.
   *
   * @return An empty {@link java.util.ArrayList}.
   */
  protected Collection<TypeValue> instantiateCollection() {
    return new ArrayList<>();
  }

}
