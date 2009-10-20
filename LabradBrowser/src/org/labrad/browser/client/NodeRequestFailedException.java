package org.labrad.browser.client;

@SuppressWarnings("serial")
public class NodeRequestFailedException extends Exception implements RemoteEvent {
  private String node;
  private String server;
  private String action;
  private String details;

  // default constructor for serialization
  protected NodeRequestFailedException() {}

  public NodeRequestFailedException(String node, String server, String action, String details) {
    this.node = node;
    this.server = server;
    this.action = action;
    this.details = details;
  }

  public String getNode() { return node; }
  public String getServer() { return server; }
  public String getAction() { return action; }
  public String getDetails() { return details; }
}
