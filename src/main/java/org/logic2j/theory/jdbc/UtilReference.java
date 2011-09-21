package org.logic2j.theory.jdbc;

import org.logic2j.model.symbol.Struct;
import org.logic2j.model.symbol.Term;

/**
 * Define, parse, and format references to DB entities (artifacts).
 *
 */
public class UtilReference {
  public static final String REFERENCE_HEADER = "id";

  //---------------------------------------------------------------------------
  // Check and parse reference from a String
  //---------------------------------------------------------------------------

  public static final boolean isReference(String thePotentialRef) {
    return thePotentialRef != null && thePotentialRef.startsWith(REFERENCE_HEADER);
  }

  public static String parseReference(String theRef) {
    if (!isReference(theRef)) {
      throw new IllegalArgumentException("Cannot extract reference from something that's not a reference: '" + theRef + '\'');
    }
    String ref = theRef.substring(REFERENCE_HEADER.length());
    return ref;
  }

  //---------------------------------------------------------------------------
  // Check and parse reference from a Term
  //---------------------------------------------------------------------------

  public static final boolean isReference(Term thePotentialRef) {
    if (thePotentialRef instanceof Struct && thePotentialRef.isAtom() && isReference(((Struct) thePotentialRef).getName())) {
      return true;
    }
    return false;
  }

  public static String parseReference(Term theRef) {
    if (!isReference(theRef)) {
      throw new IllegalArgumentException("Cannot extract reference from a Term that's not a reference: '" + theRef + '\'');
    }
    return parseReference(((Struct) theRef).getName());
  }

  public static String formatReference(long theLongValue) {
    return REFERENCE_HEADER + theLongValue;
  }

  //  //---------------------------------------------------------------------------
  //  // Format reference as a Term
  //  //---------------------------------------------------------------------------
  //
  //  //  public static Struct buildRefOfArtifact(AArtifact pArtifact) {
  //  //    Struct refStruct = new Struct(pArtifact.getRefname());
  //  //    return refStruct;
  //  //  }
  //
  //  public static Struct buildRefOfArtifact(String pRef) {
  //    Struct refStruct = new Struct(pRef);
  //    return refStruct;
  //  }
  //
  //  public static String buildClassname(Class<?> pClass) {
  //    return "'" + pClass.getName() + "'";
  //  }
  //
  //
  //  public static String buildStringRuleClauseFromRuleOperator(String pHead, String pValue) {
  //    return "':-'(" + pHead + "," + pValue + ")";
  //  }

}
