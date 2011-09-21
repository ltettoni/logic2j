package org.logic2j.library.mgmt;

import java.util.HashMap;
import java.util.Map;

import org.logic2j.model.prim.PrimitiveInfo;

/**
 *
 */
public class LibraryContent {

  public Map<String, PrimitiveInfo> directiveMap = new HashMap<String, PrimitiveInfo>();
  public Map<String, PrimitiveInfo> predicateMap = new HashMap<String, PrimitiveInfo>();
  public Map<String, PrimitiveInfo> functorMap = new HashMap<String, PrimitiveInfo>();
  public Map<String, PrimitiveInfo> primitiveMap = new HashMap<String, PrimitiveInfo>();

  public void putDirective(String theKey, PrimitiveInfo theDesc) {
    if (this.directiveMap.containsKey(theKey)) {
      throw new IllegalStateException("A directive is already defined for key " + theKey + ", cannot override with " + theDesc);
    }
    this.directiveMap.put(theKey, theDesc);
  }

  public void putPredicate(String theKey, PrimitiveInfo theDesc) {
    if (this.predicateMap.containsKey(theKey)) {
      throw new IllegalStateException("A predicate is already defined for key " + theKey + ", cannot override with " + theDesc);
    }
    this.predicateMap.put(theKey, theDesc);
  }

  public void putFunctor(String theKey, PrimitiveInfo theDesc) {
    if (this.functorMap.containsKey(theKey)) {
      throw new IllegalStateException("A functor is already defined for key " + theKey + ", cannot override with " + theDesc);
    }
    this.functorMap.put(theKey, theDesc);
  }

  /**
   * @param theKey
   * @param theDesc
   */
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
      throw new IllegalStateException("A primitive is already defined for key " + theKey + ", cannot override with " + theDesc);
    }
    this.primitiveMap.put(theKey, theDesc);
  }

  /**
   * @param theLoadedContent
   */
  public void addAll(LibraryContent theLoadedContent) {
    this.directiveMap.putAll(theLoadedContent.directiveMap);
    this.predicateMap.putAll(theLoadedContent.predicateMap);
    this.functorMap.putAll(theLoadedContent.functorMap);
    this.primitiveMap.putAll(theLoadedContent.primitiveMap);
  }

}
