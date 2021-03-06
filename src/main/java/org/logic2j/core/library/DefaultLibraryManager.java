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
package org.logic2j.core.library;

import static org.logic2j.engine.model.TermApiLocator.termApiExt;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.logic2j.core.api.LibraryManager;
import org.logic2j.core.api.Prolog;
import org.logic2j.core.api.library.LibraryContent;
import org.logic2j.core.api.library.PLibrary;
import org.logic2j.core.api.library.PrimitiveInfo;
import org.logic2j.core.api.library.PrimitiveInfo.PrimitiveType;
import org.logic2j.core.api.library.annotation.Functor;
import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.core.impl.theory.TheoryContent;
import org.logic2j.core.impl.theory.TheoryManager;
import org.logic2j.engine.exception.PrologNonSpecificException;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Term;
import org.logic2j.engine.unify.UnifyContext;

public class DefaultLibraryManager implements LibraryManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultLibraryManager.class);

  /**
   * Difference between number of args in Prolog's primitive invocation, and number of varargs
   * passed to Java implementation of the primitive: (UnifyContext currentVars, Object...)
   */
  private static final int NB_EXTRA_PARAMS = 1;

  private static final Struct<?> ATOM_CUT = new Struct<>(Struct.FUNCTOR_CUT);

  private final Prolog prolog;

  private final LibraryContent wholeContent = new LibraryContent();

  private final Map<Class<? extends PLibrary>, PLibrary> libraries = new HashMap<>();

  /**
   * @param theProlog
   */
  public DefaultLibraryManager(Prolog theProlog) {
    this.prolog = theProlog;
  }

  /**
   * @return The extra content loaded.
   * @note Won't load any instance of a {@link PLibrary} of the same class more than once - if asked more than once, we won't fail, just
   * log, and return no loaded content.
   */
  @Override
  public LibraryContent loadLibrary(PLibrary theLibrary) {
    if (alreadyLoaded(theLibrary)) {
      logger.warn("Library \"{}\" already has an instance of the same class loaded - nothing done", theLibrary);
      final LibraryContent extraContentIsEmpty = new LibraryContent();
      return extraContentIsEmpty;
    }
    final LibraryContent extraContent = loadLibraryInternal(theLibrary);
    mergeExtraContent(extraContent);

    // Load the theory text associated to the library, if any exists
    final URL associatedTheory = locationOfAssociatedTheory(theLibrary);
    if (associatedTheory != null) {
      final TheoryManager theoryManager = this.prolog.getTheoryManager();
      final TheoryContent theory = theoryManager.load(associatedTheory);
      theoryManager.addTheory(theory);
      logger.debug("Library \"{}\" loaded with extra content from {}", theLibrary, associatedTheory);
    } else {
      logger.debug("Library \"{}\" loaded; no associated theory found", theLibrary);
    }
    return extraContent;
  }

  private URL locationOfAssociatedTheory(PLibrary theLibrary) {
    final Class<? extends PLibrary> libraryClass = theLibrary.getClass();
    final String name = libraryClass.getSimpleName() + ".pro";
    final URL contentUrl = libraryClass.getResource(name);
    return contentUrl;
  }

  /**
   * @param theLibrary
   * @return True when one library of the same class was already loaded in this engine.
   */
  private boolean alreadyLoaded(PLibrary theLibrary) {
    return this.libraries.containsKey(theLibrary.getClass());
  }

  private void mergeExtraContent(LibraryContent loadedContent) {
    this.wholeContent.addAll(loadedContent);
    // TODO Houston we have a problem - we need to reassign all primitives upon loading libs!
    // It's actually unclear if when we load a new library, the new available functors would influence theories currently loaded.

    // We need to assignPrimitiveInfo(), but let's use the TermApi directly and invoke normalize() it won't harm to do a little more.
    termApiExt().normalize(Struct.ATOM_TRUE, this.wholeContent);
    termApiExt().normalize(Struct.ATOM_FALSE, this.wholeContent);
    termApiExt().normalize(ATOM_CUT, this.wholeContent);
  }

  /**
   * Introspect annotations within the {@link PLibrary} and return a description of it.
   * Look for {@link org.logic2j.core.api.library.annotation.Predicate} annotations; notice that a primitive may have several names (to allow for non-Java identifiers such as
   * \=)
   *
   * @param theLibrary
   * @return The content of the library loaded
   */
  private LibraryContent loadLibraryInternal(PLibrary theLibrary) {
    final LibraryContent content = new LibraryContent();
    logger.debug("Loading library {}", theLibrary);
    final Class<? extends PLibrary> libraryClass = theLibrary.getClass();

    // Load all annotated methods
    for (final Method method : libraryClass.getMethods()) {
      final Predicate predicateAnnotation = method.getAnnotation(Predicate.class);
      final Functor functorAnnotation = method.getAnnotation(Functor.class);

      // Handle the methods of interest
      if (predicateAnnotation != null || functorAnnotation != null) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final Class<?> returnType = method.getReturnType();
        final PrimitiveType type;
        String primitiveName;
        final String[] synonyms;
        if (predicateAnnotation != null) {
          if (returnType != Integer.TYPE) {
            throw new PrologNonSpecificException("Unexpected return type, require \"int\" for predicate " + method);
          }
          type = PrimitiveType.PREDICATE;
          primitiveName = predicateAnnotation.name();
          synonyms = predicateAnnotation.synonyms();
        } else if (functorAnnotation != null) {
          type = PrimitiveType.FUNCTOR;
          primitiveName = functorAnnotation.name();
          synonyms = functorAnnotation.synonyms();
        } else {
          throw new PrologNonSpecificException("Should not be here, annotation handling error");
        }

        // Check method arguments
        final int nbMethodParams = paramTypes.length;
        int i = 0;
        if (!(UnifyContext.class.isAssignableFrom(paramTypes[i]))) {
          throw new PrologNonSpecificException("Argument type at index " + i + " of method " + method + " not of proper " + UnifyContext.class);
        }
        i++;
        boolean varargs = false;
        if (i < nbMethodParams) {
          if (Object[].class.isAssignableFrom(paramTypes[i])) {
            varargs = true;
          } else {
            while (i < nbMethodParams) {
              if (!(Object.class.isAssignableFrom(paramTypes[i]))) {
                throw new PrologNonSpecificException("Argument type at index " + i + " of method " + method + " not of proper " + Term.class);
              }
              i++;
            }
          }
        }

        // Main name (default = method's name) for the primitive

        if (primitiveName == null || primitiveName.isEmpty()) {
          primitiveName = method.getName();
        }
        final String aritySignature = varargs ? Struct.VARARG_ARITY_SIGNATURE : Integer.toString(nbMethodParams - NB_EXTRA_PARAMS);
        final String key1 = primitiveName + '/' + aritySignature;
        final PrimitiveInfo desc = new PrimitiveInfo(type, theLibrary, primitiveName, method, varargs);
        content.putPrimitive(key1, desc);

        // All other accepted synonyms for this primitive
        for (final String synonym : synonyms) {
          final String key2 = synonym + '/' + aritySignature;
          final PrimitiveInfo desc2 = new PrimitiveInfo(type, theLibrary, primitiveName, method, varargs);
          content.putPrimitive(key2, desc2);
        }
      }
    }
    this.libraries.put(libraryClass, theLibrary);
    return content;
  }

  /**
   * @return The merged content of all the {@link PLibrary}es loaded by {@link #loadLibrary(PLibrary)} so far.
   */
  @Override
  public LibraryContent wholeContent() {
    return this.wholeContent;
  }

}
