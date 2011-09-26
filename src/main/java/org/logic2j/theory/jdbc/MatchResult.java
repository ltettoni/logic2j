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
package org.logic2j.theory.jdbc;

/**
 * Pojo describing the result of a FactBase match (well, a database match in practice).
 *
 */
public class MatchResult implements Comparable<MatchResult> {

  private String predicate;
  private Object entity;
  private Object value;

  public MatchResult(String thePredicate, Object theEntity, Object theValue) {
    super();
    this.predicate = thePredicate;
    this.entity = theEntity;
    this.value = theValue;
  }

  public Object getEntity() {
    return this.entity;
  }

  public Object getValue() {
    return this.value;
  }

  public String getPredicate() {
    return this.predicate;
  }

  //---------------------------------------------------------------------------
  // Core
  //---------------------------------------------------------------------------

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + '(' + this.entity + ',' + this.value + ')';
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.entity == null) ? 0 : this.entity.hashCode());
    result = prime * result + ((this.predicate == null) ? 0 : this.predicate.hashCode());
    result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MatchResult other = (MatchResult) obj;
    if (this.entity == null) {
      if (other.entity != null) {
        return false;
      }
    } else if (!this.entity.equals(other.entity)) {
      return false;
    }
    if (this.predicate == null) {
      if (other.predicate != null) {
        return false;
      }
    } else if (!this.predicate.equals(other.predicate)) {
      return false;
    }
    if (this.value == null) {
      if (other.value != null) {
        return false;
      }
    } else if (!this.value.equals(other.value)) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(MatchResult that) {
    int comp;
    comp = this.predicate.compareTo(that.predicate);
    if (comp != 0) {
      return comp;
    }
    comp = String.valueOf(this.entity).compareTo(String.valueOf(that.entity));
    if (comp != 0) {
      return comp;
    }
    comp = String.valueOf(this.value).compareTo(String.valueOf(that.value));
    if (comp != 0) {
      return comp;
    }
    return 0;
  }

}
