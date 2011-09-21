package org.logic2j.library.mgmt;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.logic2j.LibraryManager;
import org.logic2j.Prolog;
import org.logic2j.model.prim.PLibrary;
import org.logic2j.model.prim.PrimitiveInfo;
import org.logic2j.model.prim.PrimitiveInfo.PrimitiveType;
import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;
import org.logic2j.model.var.VarBindings;
import org.logic2j.solve.GoalFrame;
import org.logic2j.solve.ioc.SolutionListener;
import org.logic2j.theory.TheoryManager;

/**
 */
public class DefaultLibraryManager implements LibraryManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultLibraryManager.class);

  /**
   * SolutionListener, GoalFrame, VarBindings, Term, ...
   */
  private static final int NB_EXTRA_PARAMS = 3;

  // TODO Should this be a PrologImplementor? If yes it would allow to have Prolog.getLibararyManager moved to PrologImplementor.
  private final Prolog prolog;

  private LibraryContent wholeContent = new LibraryContent();

  private Map<Class<? extends PLibrary>, PLibrary> libraries = new HashMap<Class<? extends PLibrary>, PLibrary>();

  /**
   * @param theProlog
   */
  public DefaultLibraryManager(Prolog theProlog) {
    this.prolog = theProlog;
  }

  @Override
  public LibraryContent loadLibrary(PLibrary theLibrary) {
    if (alreadyLoaded(theLibrary)) {
      return this.wholeContent;
    }
    final LibraryContent loadedContent = loadLibraryInternal(theLibrary);
    updateWholeContent(loadedContent);
    // Load the theory text associated to the library
    final TheoryManager theoryManager = this.prolog.getTheoryManager();
    theoryManager.addTheory(theoryManager.load(theLibrary));
    return loadedContent;
  }

  /**
   * @param theLibrary
   * @return True when one library of the same class was already loaded in this engine.
   */
  private boolean alreadyLoaded(PLibrary theLibrary) {
    return this.libraries.containsKey(theLibrary.getClass());
  }

  private void updateWholeContent(LibraryContent loadedContent) {
    this.wholeContent.addAll(loadedContent);
    // Houston we have a problem - we need to reassign our primitives upon loading libs!
    Struct.ATOM_TRUE.assignPrimitiveInfo(this.wholeContent);
    Struct.ATOM_FALSE.assignPrimitiveInfo(this.wholeContent);
    Struct.ATOM_CUT.assignPrimitiveInfo(this.wholeContent);
  }

  private LibraryContent loadLibraryInternal(PLibrary theLibrary) {
    final LibraryContent content = new LibraryContent();
    logger.debug("Loading new library {}", theLibrary);
    final Class<? extends PLibrary> libraryClass = theLibrary.getClass();

    // Load all annotated methods
    for (Method method : libraryClass.getMethods()) {
      final Primitive annotation = method.getAnnotation(Primitive.class);
      if (annotation != null) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final Class<?> returnType = method.getReturnType();
        final PrimitiveType type;
        if (Void.class.equals(returnType) || Void.TYPE.equals(returnType)) {
          type = PrimitiveType.PREDICATE;
        } else if (Term.class.equals(returnType)) {
          type = PrimitiveType.FUNCTOR;
        } else if (Void.TYPE.equals(returnType)) {
          type = PrimitiveType.DIRECTIVE;
        } else {
          throw new IllegalStateException("Unexpected return type " + returnType.getName() + " for primitive " + annotation);
        }

        final int nbMethodParams = paramTypes.length;
        int i = 0;
        if (!(SolutionListener.class.isAssignableFrom(paramTypes[i]))) {
          throw new IllegalStateException("Argument type at index " + i + " of metohd " + method + " not of proper "
              + SolutionListener.class);
        }
        i++;
        if (!(GoalFrame.class.isAssignableFrom(paramTypes[i]))) {
          throw new IllegalStateException("Argument type at index " + i + " of metohd " + method + " not of proper "
              + GoalFrame.class);
        }
        i++;
        if (!(VarBindings.class.isAssignableFrom(paramTypes[i]))) {
          throw new IllegalStateException("Argument type at index " + i + " of metohd " + method + " not of proper "
              + VarBindings.class);
        }
        i++;
        boolean varargs = false;
        if (i < nbMethodParams) {
          if (Term[].class.isAssignableFrom(paramTypes[i])) {
            varargs = true;
          } else {
            while (i < nbMethodParams) {
              if (!(Term.class.isAssignableFrom(paramTypes[i]))) {
                throw new IllegalStateException("Argument type at index " + i + " of metohd " + method + " not of proper "
                    + Term.class);
              }
              i++;
            }
          }
        }
        // Main name (default = method's name) for the primitive
        String primitiveName = annotation.name();
        if (primitiveName == null || primitiveName.isEmpty()) {
          primitiveName = method.getName();
        }
        final String arityIndicator = varargs ? VARARG_ARITY_INDICATOR : Integer.toString(nbMethodParams - NB_EXTRA_PARAMS);
        final String key1 = primitiveName + '/' + arityIndicator;
        final PrimitiveInfo desc = new PrimitiveInfo(type, theLibrary, primitiveName, method, varargs);
        content.putPrimitive(key1, desc);

        // All other accepted synonyms for this primitive
        for (String synonym : annotation.synonyms()) {
          final String key2 = synonym + '/' + arityIndicator;
          final PrimitiveInfo desc2 = new PrimitiveInfo(type, theLibrary, primitiveName, method, varargs);
          content.putPrimitive(key2, desc2);
        }
      }
    }
    this.libraries.put(libraryClass, theLibrary);
    return content;
  }

  /**
   * @return The whole libraries content.
   */
  @Override
  public LibraryContent wholeContent() {
    return this.wholeContent;
  }

  @Override
  public <T extends PLibrary> T getLibrary(Class<T> theClass) {
    final T pLibrary = (T) this.libraries.get(theClass);
    if (pLibrary == null) {
      throw new IllegalArgumentException("No library bound of " + theClass);
    }
    return pLibrary;
  }

}
