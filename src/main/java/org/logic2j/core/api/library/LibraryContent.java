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
package org.logic2j.core.api.library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.logic2j.engine.exception.PrologNonSpecificException;
import org.logic2j.engine.model.Struct;

/**
 * Describe the content of a {@link PLibrary}: its primitives, directives, functors and predicates.
 */
public class LibraryContent {

  private final Map<String, PrimitiveInfo> directiveMap = new HashMap<>();

  private final Map<String, PrimitiveInfo> predicateMap = new HashMap<>();

  private final Map<String, PrimitiveInfo> functorMap = new HashMap<>();

  private final Map<String, PrimitiveInfo> primitiveMap = new HashMap<>();

  private List<Function<Struct<?>, Struct<?>>> foPredicateFactories = new ArrayList<>();

  public void putDirective(String theKey, PrimitiveInfo theDesc) {
    if (this.directiveMap.containsKey(theKey)) {
      throw new PrologNonSpecificException("A directive is already defined for key " + theKey + ", cannot override with " + theDesc);
    }
    this.directiveMap.put(theKey, theDesc);
  }

  public void putPredicate(String theKey, PrimitiveInfo theDesc) {
    if (this.predicateMap.containsKey(theKey)) {
      throw new PrologNonSpecificException("A predicate is already defined for key " + theKey + ", cannot override with " + theDesc);
    }
    this.predicateMap.put(theKey, theDesc);
  }

  public void putFunctor(String theKey, PrimitiveInfo theDesc) {
    if (this.functorMap.containsKey(theKey)) {
      throw new PrologNonSpecificException("A functor is already defined for key " + theKey + ", cannot override with " + theDesc);
    }
    this.functorMap.put(theKey, theDesc);
  }

  public void putPrimitive(String theKey, PrimitiveInfo theDesc) {
    if (this.primitiveMap.containsKey(theKey)) {
      throw new PrologNonSpecificException("A primitive is already defined for key " + theKey + ", cannot override with " + theDesc);
    }
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
      default:
        assert false : "Unplanned case in switch on PrimitiveType";
    }

    this.primitiveMap.put(theKey, theDesc);
  }

  /**
   * Merge the content of theLoadedContent into this, by adding all directives, primitives, functors and predicates.
   *
   * @param theLoadedContent
   */
  public void addAll(LibraryContent theLoadedContent) {
    this.directiveMap.putAll(theLoadedContent.directiveMap);
    this.predicateMap.putAll(theLoadedContent.predicateMap);
    this.functorMap.putAll(theLoadedContent.functorMap);
    this.primitiveMap.putAll(theLoadedContent.primitiveMap);
  }


  public void addFOPredicateFactory(Function<Struct<?>, Struct<?>>... factory) {
    Arrays.stream(factory).forEach(getFOPredicateFactories()::add);
  }

  // --------------------------------------------------------------------------
  // Accessors
  // --------------------------------------------------------------------------

  /**
   * @param thePredicateSignature
   * @return The {@link PrimitiveInfo} for the specified primitive's signature, or null when not registered in this {@link LibraryContent}
   */
  public PrimitiveInfo getPrimitive(String thePredicateSignature) {
    return this.primitiveMap.get(thePredicateSignature);
  }

  public boolean hasPrimitive(String thePredicateSignature) {
    return this.primitiveMap.containsKey(thePredicateSignature);
  }

  public List<Function<Struct<?>, Struct<?>>> getFOPredicateFactories() {
    return foPredicateFactories;
  }

  public void setFOPredicateFactories(List<Function<Struct<?>, Struct<?>>> foPredicateFactories) {
    this.foPredicateFactories = foPredicateFactories;
  }
}
