package org.logic2j.core.api.model;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.logic2j.engine.model.Var.strVar;

import java.util.List;
import java.util.function.Function;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.TermUnmarshaller;
import org.logic2j.core.api.library.LibraryContent;
import org.logic2j.core.api.library.PrimitiveInfo;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.exception.PrologNonSpecificException;
import org.logic2j.engine.model.PrologLists;
import org.logic2j.engine.model.Struct;
import org.logic2j.engine.model.Term;
import org.logic2j.engine.model.TermApi;
import org.logic2j.engine.model.Var;
import org.logic2j.engine.unify.UnifyContext;
import org.logic2j.engine.util.TypeUtils;

public class TermApiExt extends TermApi {

  public boolean isAtom(Object theTerm) {
    if (theTerm instanceof String) {
      // Now plain Strings are atoms!
      return true;
    }
    if (theTerm instanceof Struct) {
      final Struct<?> s = (Struct<?>) theTerm;
      return s.getArity() == 0 || PrologLists.isEmptyList(s);
    }
    return false;
  }

  //---------------------------------------------------------------------------
  // Access and extract data from Terms
  //---------------------------------------------------------------------------

  /**
   * Evaluates an expression if its reified value is a functor and there's a primitive
   * defined for it.
   *
   * @return null if the argument is not an evaluable expression
   */
  public Object evaluate(Object theTerm, UnifyContext currentVars) {
    if (theTerm == null) {
      return null;
    }
    theTerm = currentVars.reify(theTerm);
    if (theTerm instanceof Var<?>) {
      // Free var
      return null;
    }

    if (theTerm instanceof Struct) {
      final Struct<PrimitiveInfo> struct = (Struct<PrimitiveInfo>) theTerm;
      final PrimitiveInfo primInfo = struct.getContent();
      if (primInfo == null) {
        // throw new IllegalArgumentException("Predicate's functor " + struct.getName() + " is not a primitive");
        return null;
      }
      if (primInfo.getType() != PrimitiveInfo.PrimitiveType.FUNCTOR) {
        // throw new IllegalArgumentException("Predicate's functor " + struct.getName() + " is a primitive, but not a functor");
        return null;
      }
      return primInfo.invoke(struct, currentVars);
    }
    return theTerm;
  }


  /**
   * Extract one {@link Term} from within another ({@link Struct}) using a rudimentary XPath-like expression language.
   *
   * @param theTerm            To select from
   * @param theTPathExpression The expression to select from theTerm, see the associated TestCase for specification.
   * @param theClass           The {@link Term} class or one of its subclass that the desired returned object should be.
   */
  // TODO Should this go to TermAdapter instead? - since we return a new Term
  @SuppressWarnings("unchecked")
  public <T> T selectTerm(Object theTerm, String theTPathExpression, Class<T> theClass) {
    if (theTPathExpression.isEmpty()) {
      return TypeUtils.safeCastNotNull("selecting term", theTerm, theClass);
    }
    if (theTerm instanceof String) {
      if (!theTerm.equals(theTPathExpression)) {
        throw new InvalidTermException("Term \"" + theTerm + "\" cannot match expression \"" + theTPathExpression + '"');
      }
      return TypeUtils.safeCastNotNull("selecting term", theTerm, theClass);
    }

    final Struct<?> s = (Struct<?>) theTerm;
    int position = 0;
    String level0 = theTPathExpression;
    int end = theTPathExpression.length();
    final int slash = theTPathExpression.indexOf('/');
    if (slash >= 1) {
      end = slash;
      level0 = theTPathExpression.substring(0, slash);
      position = 1;
    }
    String functor = level0;
    final int par = level0.indexOf('[');
    if (par >= 0) {
      end = max(par, end);
      functor = level0.substring(0, par);
      if (!level0.endsWith("]")) {
        throw new InvalidTermException("Malformed TPath expression: \"" + theTPathExpression + "\": missing ending ']'");
      }
      position = Integer.parseInt(level0.substring(par + 1, level0.length() - 1));
      if (position <= 0) {
        throw new InvalidTermException("Index " + position + " in \"" + theTPathExpression + "\" is <=0");
      }
      if (position > s.getArity()) {
        throw new InvalidTermException("Index " + position + " in \"" + theTPathExpression + "\" is > arity of " + s.getArity());
      }
    }
    // In case functor was defined ("f[n]", since the expression "[n]" without f is also allowed)
    if (!functor.isEmpty()) {
      // Make sure the root name matches the struct at level 0
      if (!s.getName().equals(functor)) {
        throw new InvalidTermException("Term \"" + theTerm + "\" does not start with functor  \"" + functor + '"');
      }
    }
    if (position >= 1) {
      final String levelsTail = theTPathExpression.substring(min(theTPathExpression.length(), end + 1));
      return selectTerm(s.getArg(position - 1), levelsTail, theClass);
    }
    if (!(theClass.isAssignableFrom(theTerm.getClass()))) {
      throw new PrologNonSpecificException("Cannot extract Term of " + theClass + " at expression=" + theTPathExpression + " from " + theTerm);
    }
    return (T) theTerm;
  }


  /**
   * Will set the "primitiveInfo" field to directly relate a token to an existing primitive
   * defined in theContent
   *
   * @param struct
   * @param theContent Primitives in a Library
   */
  public void assignPrimitiveInfo(Struct<PrimitiveInfo> struct, LibraryContent theContent) {
    // Find by exact arity match
    struct.setContent(theContent.getPrimitive(struct.getPredicateSignature()));
    if (struct.getContent() == null) {
      // Alternate find by wildcard (varargs signature)
      struct.setContent(theContent.getPrimitive(struct.getVarargsPredicateSignature()));
    }
    for (Object child : struct.getArgs()) {
      // Not sure what was the intention in the following section but this looks invoked, but useless.
      // All test cases pass OK without.
      //      if (child instanceof String) {
      //        if (theContent.hasPrimitive(TermApi.predicateSignature(child))) {
      //          // Convert to Struct so that we can assign a primitive
      //          child = new Struct<>((String) child);
      //          child = TermApi.normalize(child, theContent);
      //          this.args[i] = child; // Not 100% sure it's good to mutate
      //        }
      //      }
      if (child instanceof Struct<?>) {
        assignPrimitiveInfo(((Struct<PrimitiveInfo>) child), theContent);
      }
    }
  }

  /**
   * Normalize any term and refer to the primitives and operators defined in LibraryContent.
   *
   * @param theTerm           To be normalized
   * @param theLibraryContent where primitives are to be found and assigned
   * @return A normalized COPY of theTerm ready to be used for inference (in a Theory ore as a goal)
   */
  public Object normalize(Object theTerm, LibraryContent theLibraryContent) {
    Object term2 = replaceStructByFOPredicates(theTerm, theLibraryContent);
    Object preNormalized = super.normalize(term2);
    if (theLibraryContent != null) {
      if (preNormalized instanceof String) {
        // Strings mustb be converted to atoms if they are primitives (need to be executed)
        if (theLibraryContent.hasPrimitive(predicateSignature(preNormalized))) {
          // Just recursing
          return normalize(new Struct<>((String) preNormalized), theLibraryContent);
        }
      }
      if (preNormalized instanceof Struct<?>) {
        assignPrimitiveInfo(((Struct<PrimitiveInfo>) preNormalized), theLibraryContent);
      }
    }
    return preNormalized;
  }

  private Object replaceStructByFOPredicates(Object theTerm, LibraryContent theLibraryContent) {
    final List<Function<Struct<?>, Struct<?>>> factories = theLibraryContent.getFOPredicateFactories();
    final Function<Struct<?>, Struct<?>> mappingFunction = struct -> {
      for (Function<Struct<?>, Struct<?>> factory : factories) {
        final Struct pred = factory.apply(struct);
        if (pred != null) {
          return pred;
        }
      }
      return struct;
    };


    final Object transformed = depthFirstStructTransform(theTerm, mappingFunction);
    return transformed;
  }

  public <T> String formatStruct(Struct<T> struct) {
    if (PrologLists.isEmptyList(struct)) {
      return PrologLists.FUNCTOR_EMPTY_LIST;
    }
    final StringBuilder sb = new StringBuilder();
    final int nArity = struct.getArity();
    // list case
    if (PrologLists.isListNode(struct)) {
      sb.append(PrologLists.LIST_OPEN);
      PrologLists.formatPListRecursive(struct, sb);
      sb.append(PrologLists.LIST_CLOSE);
      return sb.toString();
    }
    sb.append(quoteIfNeeded(struct.getName()));
    if (nArity > 0) {
      sb.append(Struct.PAR_OPEN);
      for (int c = 0; c < nArity; c++) {
        final Object arg = struct.getArg(c);
        final String formatted = arg.toString();
        sb.append(formatted);
        if (c < nArity - 1) {
          sb.append(Struct.ARG_SEPARATOR);
        }
      }
      sb.append(Struct.PAR_CLOSE);
    }
    return sb.toString();
  }

  public Object valueOf(Object theObject) {
    return valueOf(theObject, TermAdapter.FactoryMode.ANY_TERM);
  }

  /**
   * Primitive factory for simple {@link Term}s from plain Java {@link Object}s, use this
   * with parsimony at low-level.
   * Higher-level must use {@link TermAdapter} or {@link TermUnmarshaller} instead which can be
   * overridden and defined with user logic and features.
   * <p>
   * Character input will be converted to Struct or Var according to Prolog's syntax convention:
   * when starting with an underscore or an uppercase, this is a {@link Var}.
   * This method is not capable of instantiating a compound {@link Struct}, it may only create atoms.
   *
   * @param theObject Should usually be {@link CharSequence}, {@link Number}, {@link Boolean}
   * @param theMode
   * @return An instance of a subclass of {@link Term}.
   * @throws InvalidTermException If theObject cannot be converted to a Term
   */
  public Object valueOf(Object theObject, TermAdapter.FactoryMode theMode) throws InvalidTermException {
    if (theObject == null) {
      throw new InvalidTermException("Cannot create Term from a null argument");
    }
    final Object result;
    if (theObject instanceof Term) {
      // Idempotence
      result = theObject;
    } else if (theObject instanceof Integer) {
      result = theObject;
    } else if (theObject instanceof Long) {
      result = ((Long) theObject).intValue();
    } else if (theObject instanceof Float) {
      result = ((Float) theObject).doubleValue();
    } else if (theObject instanceof Double) {
      result = theObject;
    } else if (theObject instanceof Boolean) {
      result = (Boolean) theObject ? Struct.ATOM_TRUE : Struct.ATOM_FALSE;
    } else if (theObject instanceof CharSequence || theObject instanceof Character) {
      // Very very vary rudimentary parsing
      final String chars = theObject.toString();
      if (theMode == FactoryMode.ATOM) {
        result = Struct.atom(chars);
      } else {
        if (Var.ANONYMOUS_VAR_NAME.equals(chars)) {
          result = Var.anon();
        } else if (chars.isEmpty()) {
          // Dubious for real programming, but some data sources may contain empty fields, and this is the only way to represent
          // them
          // as a Term
          result = new Struct<>("");
        } else if (Character.isUpperCase(chars.charAt(0)) || chars.startsWith(Var.ANONYMOUS_VAR_NAME)) {
          // Use Prolog's convention re variables starting with uppercase or underscore
          result = strVar(chars);
        } else {
          // Otherwise it's an atom
          result = chars.intern();
        }
      }
    } else if (theObject instanceof Number) {
      // Other types of numbers
      final Number nbr = (Number) theObject;
      if (nbr.doubleValue() % 1 != 0) {
        // Has floating point number
        result = nbr.doubleValue();
      } else {
        // Is just an integer
        result = nbr.longValue();
      }
    } else if (theObject instanceof Enum<?>) {
      // Enums are just valid terms
      result = theObject;
    } else {
      // POJOs are also valid terms now
      result = theObject;
    }
    return result;
  }


}
