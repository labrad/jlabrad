package org.labrad.browser.client;

@SuppressWarnings("serial")
public class ServerDisconnectEvent implements RemoteEvent {
  private String server;

  protected ServerDisconnectEvent() {}

  public ServerDisconnectEvent(String server) {
    this.server = server;
  }

  public String getServer() { return server; }

  @Override
  public String toString() { return "server: '" + server + "'"; }
}
