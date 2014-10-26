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
package org.logic2j.core.api.library.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods of a {@link org.logic2j.core.api.library.PLibrary} that implement a Prolog functor in Java.
 * A functor returns a value, and is typically invoked with: X is my_functor(args).
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public abstract @interface Functor {

    /**
     * When "name" is defined, then the annotated method's name won't be used to register the primitive. Very useful when the primitive name
     * is not an allowed Java identifier, for example the "=" unification primitive. Otherwise, when name is the default, the method's name
     * becomes the primitive's name.
     */
    String name() default "";

    /**
     * Alternate names for the primitive.
     */
    String[] synonyms() default {};

}
