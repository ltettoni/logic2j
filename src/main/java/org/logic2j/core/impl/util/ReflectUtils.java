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
package org.logic2j.core.impl.util;

/**
 * Provide minimal convenience functions to determine run-time accessibility of classes and methods. This class can be considered as a
 * micro-helper to java.lang.reflect.
 * TODO Rename this class this has nothing to do with the Java Reflection API.
 * 
 * @version $Revision: 1.24 $
 */
public abstract class ReflectUtils {

    /**
     * Dynamic runtime checking of an instance against a class or interface; tolerates null values.
     * 
     * @param context Contextual information to be reported, in case of an exception being thrown, to the beginning of the exception's
     *            message. Typical use case would be safeCastOrNull("Downloading " + this, eventDate, Date.class).
     * @param instance The instance to check, can be null.
     * @param desiredClassOrInterface The class or interface that we want to make sure the instance is "instanceof".
     * @return The instance checked, or null.
     * @throws ClassCastException If the instance was not of the desiredClassOrInterface, i.e. if desiredClassOrInterface is not assignable
     *             to instance.
     */
    @SuppressWarnings("unchecked")
    public static <T> T safeCastOrNull(String context, Object instance, Class<? extends T> desiredClassOrInterface) throws ClassCastException {
        if (instance == null) {
            return null;
        }
        if (!(desiredClassOrInterface.isAssignableFrom(instance.getClass()))) {
            final String message = "Could not cast an instance of " + instance.getClass() + " to expected " + desiredClassOrInterface + " [formatted object was " + instance + "]";
            throw new ClassCastException(message);
        }
        return (T) instance;
    }

    /**
     * Dynamic runtime checking of an instance against a class or interface; does now allow null values.
     * 
     * @param context Contextual information to be reported, in case of an exception being thrown, to the beginning of the exception's
     *            message. Typical use case would be safeCastNotNull("Obtaining PMDB API", api, PMDB.class).
     * @param instance The instance to check, must not be null
     * @param desiredClassOrInterface The class or interface that we want to make sure the instance is "instanceof".
     * @return The instance checked, never null.
     * @throws ClassCastException If the instance was not of the desiredClassOrInterface, i.e. if desiredClassOrInterface is not assignable
     *             to instance.
     */
    public static <T> T safeCastNotNull(String context, Object instance, Class<? extends T> desiredClassOrInterface) throws ClassCastException {
        if (instance == null) {
            throw new ClassCastException("null value not allowed, expected an instance of " + desiredClassOrInterface + ", while " + context);
        }
        final String effectiveContext = (context != null) ? context : "casting undescribed object";
        return safeCastOrNull(effectiveContext, instance, desiredClassOrInterface);
    }

}
