package org.labrad.browser.client;

import java.util.List;

@SuppressWarnings("serial")
public class NodeStatusEvent implements RemoteEvent {
  private String name;
  private List<NodeServerStatus> servers;

  protected NodeStatusEvent() {}

  public NodeStatusEvent(String name, List<NodeServerStatus> servers) {
    this.name = name;
    this.servers = servers;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setServers(List<NodeServerStatus> servers) {
    this.servers = servers;
  }

  public String getName() {
    return name;
  }

  public List<NodeServerStatus> getServers() {
    return servers;
  }

  @Override
  public String toString() {
    return "node: '" + name + "'";
  }
}
