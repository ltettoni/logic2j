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

package org.logic2j.core.unify;


/**
 * Utilities for white-box testing of {@link BindingTrail}, lies in the same package to leverage package scope.
 * This class just exposes functions to call {@link BindingTrail}'s method in the same package.
 */
public class BindingTrailTestUtils {

    @SuppressWarnings("deprecation")
    public static void reset() {
        BindingTrail.reset();
    }

    @SuppressWarnings("deprecation")
    public static int nbBindings() {
        return BindingTrail.nbBindings();
    }

    @SuppressWarnings("deprecation")
    public static int size() {
        return BindingTrail.size();
    }

}
