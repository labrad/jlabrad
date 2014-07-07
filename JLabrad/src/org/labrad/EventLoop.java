package org.labrad;

public interface EventLoop {
  public void invokeLater();
  public void invokeAndWait();
  public void flush();
}
