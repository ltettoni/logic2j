package org.logic2j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 */
public class CollectionMap<TypeKey, TypeValue> extends LinkedHashMap<TypeKey, Collection<TypeValue>> {

  /**
   * 
   */
  private static final long serialVersionUID = -2612104820556647913L;

  /**
   * Add a value to the collection addressed by theKey. If nothing was registered a new
   * collection is instantiated by {@link #instantiateCollection()} and the theElement is added to it.
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
    Collection<TypeValue> value = instantiateCollection();
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
    for (Collection<TypeValue> values : this.values()) {
      if (values.contains(theValue)) {
        return true;
      }
    }
    return false;
  }

}
