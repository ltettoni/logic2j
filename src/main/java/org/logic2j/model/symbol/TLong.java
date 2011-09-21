/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.logic2j.model.symbol;

import org.logic2j.model.TermVisitor;

/**
 * TLong class represents the long prolog data type
 */
public class TLong extends TNumber {
  private static final long serialVersionUID = 1L;

  private long value;

  public TLong(long v) {
    this.value = v;
  }

  public TLong(int v) {
    this.value = v;
  }

  @Override
  final public double doubleValue() {
    return this.value;
  }

  @Override
  final public long longValue() {
    return this.value;
  }

  @Override
  public <T> T accept(TermVisitor<T> theVisitor) {
    return theVisitor.visit(this);
  }

  //---------------------------------------------------------------------------
  // Core
  //---------------------------------------------------------------------------

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TLong)) {
      return false;
    }
    final TLong that = (TLong) other;
    return this.value == that.value;
  }

  @Override
  public int hashCode() {
    return new Long(this.value).hashCode();
  }

  /**
   * @author Paolo Contessi
   */
  @Override
  public int compareTo(TNumber that) {
    return (new Long(this.value)).compareTo(that.longValue());
  }

}
