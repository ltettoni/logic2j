package org.logic2j.util;

/**
 * Utilities for reporting / logging / etc.
 *
 */
public class ReportUtils {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportUtils.class);
  private static boolean isDebug = logger.isDebugEnabled();

  public static String shortDescription(Object theInstance) {
    final String details = isDebug ? ('@' + Integer.toHexString(theInstance.hashCode())) : "";
    return shortClassName(theInstance) + details;
  }

  public static String shortClassName(Object theInstance) {
    if (theInstance == null) {
      return null;
    }
    String theName = theInstance.getClass().getName();
    final int lastDot = theName.lastIndexOf('.');
    if (lastDot >= 0) {
      return theName.substring(lastDot + 1);
    }
    return theName;
  }

}
