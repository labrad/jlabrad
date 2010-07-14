package org.labrad.browser.client.event;


import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("events")
public interface RemoteEventService extends RemoteService {
  public String connect();
  public String disconnect();
  public List<RemoteEvent> getEvents();

  public String startServer(String node, String server);
  public String stopServer(String node, String instance);
  public String restartServer(String node, String instance);
}
