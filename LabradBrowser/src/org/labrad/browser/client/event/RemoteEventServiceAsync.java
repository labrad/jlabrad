package org.labrad.browser.client.event;


import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface RemoteEventServiceAsync {
  public void connect(AsyncCallback<String> callback);
  public void disconnect(AsyncCallback<String> callback);
  public void getEvents(AsyncCallback<List<RemoteEvent>> callback);

  public void startServer(String node, String server, AsyncCallback<String> callback);
  public void stopServer(String node, String instance, AsyncCallback<String> callback);
  public void restartServer(String node, String instance, AsyncCallback<String> callback);
}
