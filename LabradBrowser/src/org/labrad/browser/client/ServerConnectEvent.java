package org.labrad.browser.client;

@SuppressWarnings("serial")
public class ServerConnectEvent implements RemoteEvent {
  private String server;

  protected ServerConnectEvent() {}

  public ServerConnectEvent(String server) {
    this.server = server;
  }

  public String getServer() { return server; }

  @Override
  public String toString() { return "server: '" + server + "'"; }
}
