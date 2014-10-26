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
package org.logic2j.core.library;

import org.logic2j.core.api.LibraryManager;
import org.logic2j.core.api.library.LibraryContent;
import org.logic2j.core.api.library.PLibrary;
import org.logic2j.core.api.Prolog;
import org.logic2j.core.api.library.annotation.Functor;
import org.logic2j.core.api.library.annotation.Predicate;
import org.logic2j.core.api.library.PrimitiveInfo;
import org.logic2j.core.api.solver.listener.SolutionListener;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.term.Struct;
import org.logic2j.core.api.model.term.Term;
import org.logic2j.core.api.model.term.TermApi;
import org.logic2j.core.api.unify.UnifyContext;
import org.logic2j.core.impl.theory.TheoryContent;
import org.logic2j.core.impl.theory.TheoryManager;
import org.logic2j.core.api.library.PrimitiveInfo.PrimitiveType;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DefaultLibraryManager implements LibraryManager {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultLibraryManager.class);

    /**
     * Difference between number of args in Prolog's primitive invocation, and number of varargs
     * passed to Java implementation of the primitive: (SolutionListener theListener, UnifierState currentVars, Object...)
     */
    private static final int NB_EXTRA_PARAMS = 2;

    private final Prolog prolog;

    private final LibraryContent wholeContent = new LibraryContent();

    private final Map<Class<? extends PLibrary>, PLibrary> libraries = new HashMap<Class<? extends PLibrary>, PLibrary>();

    /**
     * @param theProlog
     */
    public DefaultLibraryManager(Prolog theProlog) {
        this.prolog = theProlog;
    }

    /**
     * @return The extra content loaded.
     * @note Won't load any instance of a {@link PLibrary} of the same class more than once - if asked more than once, we won't fail, just
     *       log, and return no loaded content.
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
        TermApi.normalize(Struct.ATOM_TRUE, this.wholeContent);
        TermApi.normalize(Struct.ATOM_FALSE, this.wholeContent);
        TermApi.normalize(Struct.ATOM_CUT, this.wholeContent);
    }

    /**
     * Introspect annotations within the {@link PLibrary} and return a description of it.
     * Look for {@link org.logic2j.core.api.library.annotation.Predicate} annotations; notice that a privmitive may have several names (to allow for non-Java identifiers such as
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
            if (predicateAnnotation != null || functorAnnotation != null) {
                final Class<?>[] paramTypes = method.getParameterTypes();
                final Class<?> returnType = method.getReturnType();
                final PrimitiveType type;
                String primitiveName;
                final String[] synonyms;
                if (predicateAnnotation!=null) {
                    if (returnType != Integer.class) {
                        throw new PrologNonSpecificError("Unexpected return type, require Integer for predicate " + method);
                    }
                    type = PrimitiveType.PREDICATE;
                    primitiveName = predicateAnnotation.name();
                    synonyms = predicateAnnotation.synonyms();
                } else if (functorAnnotation!=null) {
                    type = PrimitiveType.FUNCTOR;
                    primitiveName = functorAnnotation.name();
                    synonyms = functorAnnotation.synonyms();
                } else {
                    throw new PrologNonSpecificError("Should not be here, annotation handling error");
                }
                /*
                final PrimitiveType type;
                if (Integer.class.equals(returnType)) {
                    type = PrimitiveType.PREDICATE;
                } else if (Void.TYPE.equals(returnType)) {
                    type = PrimitiveType.DIRECTIVE;
                } else if (Term.class.equals(returnType)) {
                    type = PrimitiveType.FUNCTOR;
                } else if (Object.class.equals(returnType)) {
                    type = PrimitiveType.FUNCTOR;
                } else {
                    throw new PrologNonSpecificError("Unexpected return type " + returnType.getName() + " for primitive " + method);
                }
                */
                final int nbMethodParams = paramTypes.length;
                int i = 0;
                if (!(SolutionListener.class.isAssignableFrom(paramTypes[i]))) {
                    throw new PrologNonSpecificError("Argument type at index " + i + " of method " + method + " not of proper " + SolutionListener.class);
                }
                i++;
                if (!(UnifyContext.class.isAssignableFrom(paramTypes[i]))) {
                    throw new PrologNonSpecificError("Argument type at index " + i + " of method " + method + " not of proper " + UnifyContext.class);
                }
                i++;
                boolean varargs = false;
                if (i < nbMethodParams) {
                    if (Object[].class.isAssignableFrom(paramTypes[i])) {
                        varargs = true;
                    } else {
                        while (i < nbMethodParams) {
                            if (!(Object.class.isAssignableFrom(paramTypes[i]))) {
                                throw new PrologNonSpecificError("Argument type at index " + i + " of method " + method + " not of proper " + Term.class);
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
