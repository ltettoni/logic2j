package org.logic2j.solve;

/**
 */
public class MissingSolutionException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public MissingSolutionException(CharSequence theMessage) {
    super(theMessage.toString());
  }

}
