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
package org.logic2j.io.parse.tuprolog;

/**
 * Binary masks and values.
 */
class MaskConstants {
  static final int TYPEMASK = 0x00FF;
  static final int ATTRMASK = 0xFF00;

  // Type values
  static final int LPAR = 0x0001;
  static final int RPAR = 0x0002;
  static final int LBRA = 0x0003;
  static final int RBRA = 0x0004;
  static final int BAR = 0x0005;
  static final int INTEGER = 0x0006;
  static final int FLOAT = 0x0007;
  static final int ATOM = 0x0008;
  static final int VARIABLE = 0x0009;
  static final int SQ_SEQUENCE = 0x000A;
  static final int DQ_SEQUENCE = 0x000B;
  static final int END = 0x000D;
  static final int LBRA2 = 0x000E;
  static final int RBRA2 = 0x000F;

  // Attribute values
  static final int FUNCTOR = 0x0100;
  static final int OPERATOR = 0x0200;
  static final int EOF = 0x1000;
}
