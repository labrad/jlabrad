package org.labrad.browser.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.labrad.Connection;
import org.labrad.browser.LabradConnection;
import org.labrad.browser.client.NodeService;
import org.labrad.browser.client.event.NodeRequestFailedException;
import org.labrad.browser.client.event.NodeServerStatus;
import org.labrad.browser.client.event.NodeStatusEvent;
import org.labrad.data.Data;
import org.labrad.data.Request;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class NodeServiceImpl extends RemoteServiceServlet implements
NodeService {

  public List<NodeStatusEvent> getNodeInfo() {
    try {
      Connection cxn = LabradConnection.get();
      Data serverData = cxn.sendAndWait(Request.to("Manager").add("Servers")).get(0);
      List<String> servers = new ArrayList<String>();
      for (int i = 0; i < serverData.getArraySize(); i++) {
        servers.add(serverData.get(i, 1).getString());
      }
      List<String> nodeNames = new ArrayList<String>();
      List<Future<List<Data>>> statusRequests = new ArrayList<Future<List<Data>>>();
      for (String server : servers) {
        if (server.toLowerCase().startsWith("node")) {
          nodeNames.add(server);
          statusRequests.add(cxn.send(Request.to(server).add("status")));
        }
      }
      List<NodeStatusEvent> ans = new ArrayList<NodeStatusEvent>();
      for (int i = 0; i < nodeNames.size(); i++) {
        String nodeName = nodeNames.get(i);
        Data nodeData = statusRequests.get(i).get().get(0);
        ans.add(new NodeStatusEvent(nodeName, getServerStatuses(nodeData)));
      }
      return ans;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<NodeServerStatus> getServerStatuses(Data servers) {
    List<NodeServerStatus> serverStatuses = new ArrayList<NodeServerStatus>();
    for (Data statusData : servers.getDataList()) {
      serverStatuses.add(getServerStatus(statusData));
    }
    return serverStatuses;
  }

  public static NodeServerStatus getServerStatus(Data statusData) {
    NodeServerStatus status = new NodeServerStatus();
    status.setName(statusData.get(0).getString());
    status.setDescription(statusData.get(1).getString());
    status.setVersion(statusData.get(2).getString());
    status.setInstanceName(statusData.get(3).getString());
    status.setEnvironmentVars(statusData.get(4).getStringList());
    status.setInstances(statusData.get(5).getStringList());
    return status;
  }

  public String refreshServers(String node) {
    try {
      Request req = Request.to(node).add("refresh_servers");
      LabradConnection.get().sendAndWait(req);
      return null;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String restartServer(String node, String server) throws NodeRequestFailedException {
    return doRequest(node, server, "restart");
  }

  public String startServer(String node, String server) throws NodeRequestFailedException {
    return doRequest(node, server, "start");
  }

  public String stopServer(String node, String server) throws NodeRequestFailedException {
    return doRequest(node, server, "stop");
  }

  private String doRequest(String node, String server, String action) throws NodeRequestFailedException {
    try {
      Request req = Request.to(node).add(action, Data.valueOf(server));
      Data ans = LabradConnection.get().sendAndWait(req).get(0);
      return ans.getString();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      throw new NodeRequestFailedException(node, server, action, cause.getMessage());
    } catch (Exception e) {
      throw new NodeRequestFailedException(node, server, action, e.getMessage());
    }
  }

}
