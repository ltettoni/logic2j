package org.logic2j.solve;

/**
 */
public class IllegalSolutionException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * @param theMessage
   */
  public IllegalSolutionException(CharSequence theMessage) {
    super(theMessage.toString());
  }

  @Override
  public String getMessage() {
    return super.getMessage();
  }

}
