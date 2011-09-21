package org.logic2j.library.mgmt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a method that implements a Prolog primitive.
 * 
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Primitive {

  /**
   * When "name" is defined, then the annotated method's name won't be used to register the primitive.
   * Otherwise, when name is the default, the method's name becomes the primitive's name.
   */
  String name() default "";

  /**
   * Alternate names.
   */
  String[] synonyms() default {};

}
