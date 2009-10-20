package org.labrad.browser.client;

@SuppressWarnings("serial")
public class RegistryError extends Exception {
  private String message;

  protected RegistryError() {}

  public RegistryError(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
