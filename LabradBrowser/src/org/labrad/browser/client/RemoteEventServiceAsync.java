package org.labrad.browser.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface RemoteEventServiceAsync {
  public void connect(AsyncCallback<String> callback);
  public void disconnect(AsyncCallback<String> callback);
  public void getEvent(AsyncCallback<RemoteEventType> callback);

  public void getLabradConnectEvent(AsyncCallback<LabradConnectEvent> callback);
  public void getLabradDisconnectEvent(AsyncCallback<LabradDisconnectEvent> callback);

  public void getServerConnectEvent(AsyncCallback<ServerConnectEvent> callback);
  public void getServerDisconnectEvent(AsyncCallback<ServerDisconnectEvent> callback);

  public void startServer(String node, String server, AsyncCallback<String> callback);
  public void stopServer(String node, String instance, AsyncCallback<String> callback);
  public void restartServer(String node, String instance, AsyncCallback<String> callback);

  public void getNodeRequestFailedEvent(AsyncCallback<NodeRequestFailedException> callback);

  public void getNodeServerStartingEvent(AsyncCallback<NodeServerStartingEvent> callback);
  public void getNodeServerStartedEvent(AsyncCallback<NodeServerStartedEvent> callback);
  public void getNodeServerStoppingEvent(AsyncCallback<NodeServerStoppingEvent> callback);
  public void getNodeServerStoppedEvent(AsyncCallback<NodeServerStoppedEvent> callback);

  public void getNodeStatusEvent(AsyncCallback<NodeStatusEvent> callback);
}
