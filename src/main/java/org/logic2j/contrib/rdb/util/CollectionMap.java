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
package org.logic2j.contrib.rdb.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link Map} whose values are {@link Collection}s of TypeValue.
 */
public class CollectionMap<TypeKey, TypeValue> extends LinkedHashMap<TypeKey, Collection<TypeValue>> {

    private static final long serialVersionUID = -2612104820556647913L;

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

    public Collection<TypeValue> getOrCreate(TypeKey theKey) {
        if (this.containsKey(theKey)) {
            return get(theKey);
        }
        final Collection<TypeValue> value = instantiateCollection();
        this.put(theKey, value);
        return value;
    }

    protected Collection<TypeValue> instantiateCollection() {
        return new ArrayList<TypeValue>();
    }

    /**
     * @param theValue
     * @return true if contained in keys or collections of values (ie anywhere).
     */
    public boolean contains(TypeValue theValue) {
        if (this.keySet().contains(theValue)) {
            return true;
        }
        for (final Collection<TypeValue> values : this.values()) {
            if (values.contains(theValue)) {
                return true;
            }
        }
        return false;
    }

}
