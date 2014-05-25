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
package org.logic2j.core.library.mgmt;

import org.logic2j.core.api.PLibrary;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;

import java.util.HashMap;
import java.util.Map;

/**
 * Describe the content of a {@link PLibrary}: its primitives, directives, functors and predicates.
 */
public class LibraryContent {

    private final Map<String, PrimitiveInfo> directiveMap = new HashMap<String, PrimitiveInfo>();
    private final Map<String, PrimitiveInfo> predicateMap = new HashMap<String, PrimitiveInfo>();
    private final Map<String, PrimitiveInfo> functorMap = new HashMap<String, PrimitiveInfo>();
    private final Map<String, PrimitiveInfo> primitiveMap = new HashMap<String, PrimitiveInfo>();

    public void putDirective(String theKey, PrimitiveInfo theDesc) {
        if (this.directiveMap.containsKey(theKey)) {
            throw new PrologNonSpecificError("A directive is already defined for key " + theKey + ", cannot override with " + theDesc);
        }
        this.directiveMap.put(theKey, theDesc);
    }

    public void putPredicate(String theKey, PrimitiveInfo theDesc) {
        if (this.predicateMap.containsKey(theKey)) {
            throw new PrologNonSpecificError("A predicate is already defined for key " + theKey + ", cannot override with " + theDesc);
        }
        this.predicateMap.put(theKey, theDesc);
    }

    public void putFunctor(String theKey, PrimitiveInfo theDesc) {
        if (this.functorMap.containsKey(theKey)) {
            throw new PrologNonSpecificError("A functor is already defined for key " + theKey + ", cannot override with " + theDesc);
        }
        this.functorMap.put(theKey, theDesc);
    }

    public void putPrimitive(String theKey, PrimitiveInfo theDesc) {
        switch (theDesc.getType()) {
        case DIRECTIVE:
            putDirective(theKey, theDesc);
            break;
        case PREDICATE:
            putPredicate(theKey, theDesc);
            break;
        case FUNCTOR:
            putFunctor(theKey, theDesc);
            break;
        }
        if (this.primitiveMap.containsKey(theKey)) {
            throw new PrologNonSpecificError("A primitive is already defined for key " + theKey + ", cannot override with " + theDesc);
        }
        this.primitiveMap.put(theKey, theDesc);
    }

    /**
     * Merge the content of theLoadedContent into this, by adding all directives, primitives, functors adn predicates.
     * 
     * @param theLoadedContent
     */
    public void addAll(LibraryContent theLoadedContent) {
        this.directiveMap.putAll(theLoadedContent.directiveMap);
        this.predicateMap.putAll(theLoadedContent.predicateMap);
        this.functorMap.putAll(theLoadedContent.functorMap);
        this.primitiveMap.putAll(theLoadedContent.primitiveMap);
    }

    /**
     * @param thePredicateSignature
     * @return The {@link PrimitiveInfo} for the specified primitive's signature, or null when not registered in this {@link LibraryContent}
     */
    public PrimitiveInfo getPrimitive(String thePredicateSignature) {
        return this.primitiveMap.get(thePredicateSignature);
    }

}
