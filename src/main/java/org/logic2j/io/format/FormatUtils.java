package org.logic2j.io.format;

/**
 * Utilities for formatting.
 */
public class FormatUtils {
  public static String removeApices(String st) {
    if (st.startsWith("'") && st.endsWith("'")) {
      return st.substring(1, st.length() - 1);
    } else {
      return st;
    }
  }
}
