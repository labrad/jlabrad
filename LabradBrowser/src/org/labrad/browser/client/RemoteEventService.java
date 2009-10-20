package org.labrad.browser.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("events")
public interface RemoteEventService extends RemoteService {
  public String connect();
  public String disconnect();
  public RemoteEventType getEvent();

  public LabradConnectEvent getLabradConnectEvent();
  public LabradDisconnectEvent getLabradDisconnectEvent();

  public ServerConnectEvent getServerConnectEvent();
  public ServerDisconnectEvent getServerDisconnectEvent();

  public String startServer(String node, String server);
  public String stopServer(String node, String instance);
  public String restartServer(String node, String instance);

  public NodeRequestFailedException getNodeRequestFailedEvent();

  public NodeServerStartingEvent getNodeServerStartingEvent();
  public NodeServerStartedEvent getNodeServerStartedEvent();
  public NodeServerStoppingEvent getNodeServerStoppingEvent();
  public NodeServerStoppedEvent getNodeServerStoppedEvent();

  public NodeStatusEvent getNodeStatusEvent();
}
