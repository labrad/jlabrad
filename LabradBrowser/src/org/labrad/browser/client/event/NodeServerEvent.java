package org.labrad.browser.client.event;


public interface NodeServerEvent extends RemoteEvent {
  public String getNode();
  public String getServer();
  public String getInstance();
}
