package org.labrad.browser.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.labrad.browser.client.event.EventManager;
import org.labrad.browser.client.event.NodeRequestFailedException;
import org.labrad.browser.client.event.NodeRequestFailedHandler;
import org.labrad.browser.client.event.NodeServerEvent;
import org.labrad.browser.client.event.NodeServerStartedEvent;
import org.labrad.browser.client.event.NodeServerStartedHandler;
import org.labrad.browser.client.event.NodeServerStartingEvent;
import org.labrad.browser.client.event.NodeServerStartingHandler;
import org.labrad.browser.client.event.NodeServerStatus;
import org.labrad.browser.client.event.NodeServerStoppedEvent;
import org.labrad.browser.client.event.NodeServerStoppedHandler;
import org.labrad.browser.client.event.NodeServerStoppingEvent;
import org.labrad.browser.client.event.NodeServerStoppingHandler;
import org.labrad.browser.client.event.NodeStatusEvent;
import org.labrad.browser.client.event.NodeStatusHandler;
import org.labrad.browser.client.event.ServerDisconnectEvent;
import org.labrad.browser.client.event.ServerDisconnectHandler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ControlPanel extends VerticalPanel {
  /**
   * Service for getting node information
   */
  private static final NodeServiceAsync nodeService = GWT.create(NodeService.class);

  /**
   * Images for buttons, etc
   */
  private final static NodeImageBundle images = GWT.create(NodeImageBundle.class);

  // instance variables
  private Grid table = null;

  private final EventManager eventManager = EventManager.get();
  private final List<String> nodes = Util.newArrayList();
  private final List<String> globalServers = Util.newArrayList();
  private final List<String> localServers = Util.newArrayList();
  private final Map<String, Map<String, InstanceController>> controllers = Util.newHashMap();

  /**
   * Create a new control panel
   */
  public ControlPanel() {
    // update status
    updateStatus();

    // listen for status update messages
    eventManager.addHandler(new NodeStatusHandler() {
      public void onEvent(NodeStatusEvent event) {
        updateNodeStatus(event);
        makeTables();
      }
    });

    // listen for server disconnect messages
    eventManager.addHandler(new ServerDisconnectHandler() {
      public void onEvent(ServerDisconnectEvent e) {
        if (nodeExists(e.getServer())) {
          removeNode(e.getServer());
        }
      }
    });

    // listen for node request errors
    eventManager.addHandler(new NodeRequestFailedHandler() {
      public void onEvent(NodeRequestFailedException e) {
        LogWindow.log("[Error] failed to " + e.getAction() + " " +
            "server '" + e.getServer() + "' " +
            "on node '" + e.getNode() + "'",
            e.getDetails());
      }
    });

    // when server startup begins
    eventManager.addHandler(new NodeServerStartingHandler() {
      public void onEvent(NodeServerStartingEvent e) {
        notifyInstanceControllers(e, InstanceStatus.STARTING);
      }
    });

    // when server startup succeeds
    eventManager.addHandler(new NodeServerStartedHandler() {
      public void onEvent(NodeServerStartedEvent e) {
        notifyInstanceControllers(e, InstanceStatus.STARTED);
      }
    });

    // when server begins shutting down
    eventManager.addHandler(new NodeServerStoppingHandler() {
      public void onEvent(NodeServerStoppingEvent e) {
        notifyInstanceControllers(e, InstanceStatus.STOPPING);
      }
    });

    // when server stops
    eventManager.addHandler(new NodeServerStoppedHandler() {
      public void onEvent(NodeServerStoppedEvent e) {
        notifyInstanceControllers(e, InstanceStatus.STOPPED);
      }
    });
  }

  /**
   * Fetch the current status of all running nodes.
   */
  public void updateStatus() {
    nodeService.getNodeInfo(new AsyncCallback<List<NodeStatusEvent>>() {
      public void onFailure(Throwable caught) {
        LogWindow.log("[Error] getNodeInfo", caught);
      }

      public void onSuccess(List<NodeStatusEvent> result) {
        nodes.clear();
        globalServers.clear();
        localServers.clear();
        clearControllers();
        for (NodeStatusEvent info : result) {
          updateNodeStatus(info);
        }
        makeTables();
      }
    });
  }

  private void clearControllers() {
    // remove all registered event handlers
    for (String node : controllers.keySet()) {
      clearControllers(node);
    }
    // clear all controllers
    controllers.clear();
  }

  private void clearControllers(String node) {
    // remove all registered event handlers
    if (controllers.containsKey(node)) {
      Map<String, InstanceController> ics = controllers.get(node);
      for (InstanceController ic : ics.values()) {
        ic.unregisterHandlers();
      }
      ics.clear();
    }
  }

  /**
   * Update the status of servers available on a single node.
   * @param node
   * @param globalServers
   */
  private void updateNodeStatus(NodeStatusEvent nodeInfo) {
    String node = nodeInfo.getName();
    // insert node name into node list in sorted order
    if (!nodeExists(node)) {
      insertSorted(node, nodes);
    }
    clearControllers(node);
    controllers.put(node, new HashMap<String, InstanceController>());
    for (NodeServerStatus serverInfo : nodeInfo.getServers()) {
      updateServerInfo(node, serverInfo);
    }
  }

  /**
   * Remove a node from the list of nodes.
   * @param node
   */
  private void removeNode(String node) {
    nodes.remove(node);
    clearControllers(node);
    controllers.remove(node);
    makeTables();
  }

  private void insertSorted(String s, List<String> list) {
    int i = 0;
    while ((i < list.size()) && (s.compareTo(list.get(i))) > 0) i++;
    list.add(i, s);
  }

  /**
   * Update info about a particular server on a particular node.
   * @param node
   * @param server
   * @param info
   */
  private void updateServerInfo(String node, NodeServerStatus info) {
    String server = info.getName();
    // insert server name into appropriate server list in sorted order
    if (!serverExists(server)) {
      if (info.getEnvironmentVars().size() == 0) {
        insertSorted(server, globalServers);
      } else {
        insertSorted(server, localServers);
      }
    }
    // create a new instance controller
    InstanceController ic = new InstanceController(
        this, node, server,
        info.getInstanceName(), info.getVersion(),
        info.getInstances(),
        info.getEnvironmentVars());
    controllers.get(node).put(server, ic);
  }

  private void removeUnusedServers(List<String> list) {
    List<String> removals = new ArrayList<String>();
    for (String server : list) {
      int count = 0;
      for (String node : nodes) {
        if (controllers.get(node).containsKey(server)) {
          count += 1;
        }
      }
      if (count == 0) removals.add(server);
    }
    for (String server : removals) {
      list.remove(server);
    }
  }

  /**
   * Build the control panel table.
   * @param globalServers
   * @param nodes
   * @param info
   */
  private void makeTables() {
    // remove servers that are no longer available on any node
    removeUnusedServers(globalServers);
    removeUnusedServers(localServers);

    // create the new table widgets
    Grid table = new Grid(globalRows() + localRows(), serverCols());

    // give some indication when there are no nodes
    if (nodes.size() == 0) {
      table.setText(0, 0, "No nodes are connected.");
    } else {
      table.setText(globalHeaderRow(), 0, "Global Servers");
      table.setText(localHeaderRow(), 0, "Local Servers");
      table.getCellFormatter().addStyleName(globalHeaderRow(), 0, "server-group");
      table.getCellFormatter().addStyleName(localHeaderRow(), 0, "server-group");
    }

    // create node controls in the column headers
    for (int col = 0; col < nodes.size(); col++) {
      table.setText(0, serverCol(col), nodes.get(col));
      table.setWidget(0, serverCol(col), makeNodeControl(nodes.get(col)));
      table.getCellFormatter().setAlignment(0, serverCol(col),
          HasHorizontalAlignment.ALIGN_CENTER,
          HasVerticalAlignment.ALIGN_MIDDLE);
      table.getCellFormatter().addStyleName(0, serverCol(col), "padded-cell");
    }

    // add server names for the row headers
    for (int i = 0; i < globalServers.size(); i++) {
      table.setText(globalRow(i), 0, globalServers.get(i));
      table.getCellFormatter().addStyleName(globalRow(i), 0, "server-name");
    }
    for (int i = 0; i < localServers.size(); i++) {
      table.setText(localRow(i), 0, localServers.get(i));
      table.getCellFormatter().addStyleName(localRow(i), 0, "server-name");
    }


    // add instance controllers for available servers
    for (int i = 0; i < globalServers.size(); i++) {
      String server = globalServers.get(i);
      int row = globalRow(i);
      String version = null;
      boolean versionConflict = false;
      for (int col = 0; col < nodes.size(); col++) {
        String node = nodes.get(col);
        if (controllers.get(node).containsKey(server)) {
          InstanceController ic = controllers.get(node).get(server);
          table.setWidget(row, serverCol(col), ic);
          table.getCellFormatter().addStyleName(row, serverCol(col), "padded-cell");
          if (version == null) {
            version = ic.getVersion();
          } else if (!ic.getVersion().equals(version)) {
            versionConflict = true;
          }
        }
      }
      if (i % 2 == 0) table.getRowFormatter().addStyleName(row, "odd-row");
      if (versionConflict) {
        table.getRowFormatter().addStyleName(row, "version-conflict");
        table.setText(row, 1, "version conflict");
      } else {
        table.setText(row, 1, version);
      }
      table.getCellFormatter().addStyleName(row, 1, "server-name");
    }
    for (int i = 0; i < localServers.size(); i++) {
      String server = localServers.get(i);
      int row = localRow(i);
      String version = null;
      boolean versionConflict = false;
      for (int col = 0; col < nodes.size(); col++) {
        String node = nodes.get(col);
        if (controllers.get(node).containsKey(server)) {
          InstanceController ic = controllers.get(node).get(server);
          table.setWidget(row, serverCol(col), ic);
          table.getCellFormatter().addStyleName(row, serverCol(col), "padded-cell");
          if (version == null) {
            version = ic.getVersion();
          } else if (!ic.getVersion().equals(version)) {
            versionConflict = true;
          }
        }
      }
      if (i % 2 == 0) table.getRowFormatter().addStyleName(row, "odd-row");
      if (versionConflict) {
        table.getRowFormatter().addStyleName(row, "version-conflict");
        table.setText(row, 1, "version conflict");
      } else {
        table.setText(row, 1, version);
      }
      table.getCellFormatter().addStyleName(row, 1, "server-name");
    }

    // for all singleton servers, tell all instance controllers that
    // singletons are running somewhere so they can disable themselves
    for (String node : controllers.keySet()) {
      for (String server : controllers.get(node).keySet()) {
        InstanceController ic = controllers.get(node).get(server);
        if (ic.isLocal() || !ic.isRunning()) continue;
        // tell all controllers that this server is running
        NodeServerEvent e = new NodeServerStartedEvent(node, server, null);
        notifyInstanceControllers(e, InstanceStatus.STARTED);
      }
    }

    // add tables, removing previous tables if necessary
    if (this.table != null) {
      remove(this.table);
    }
    this.table = table;
    add(table);
  }

  /**
   * Make a controller widget for a single node.
   * The controller widget allows the user to trigger a refresh of the
   * server list on this node.
   * @param nodename
   * @return
   */
  private Widget makeNodeControl(final String nodename) {
    final PushButton b = new PushButton(new Image(images.restartServerIcon()));
    b.getUpDisabledFace().setImage(new Throbber());
    b.setTitle("Update the list of available servers");
    b.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        b.setEnabled(false);
        nodeService.refreshServers(nodename, new AsyncCallback<String>() {
          public void onFailure(Throwable caught) {
            LogWindow.log("[Error] refreshServers", caught);
            b.setEnabled(true);
          }

          public void onSuccess(String result) {
            b.setEnabled(true);
          }

        });
      }
    });
    HorizontalPanel p = new HorizontalPanel();
    p.add(new Label(nodename.substring(5, nodename.length())));
    p.add(b);
    return p;
  }

  /**
   * Notify all instance controllers about a server status event.
   * This is used to pass events to controllers so they don't have
   * to register their own event handlers, which could leak memory.
   * In addition, an event is generated to let them known initially
   * when an instance is running on a different node, so they can
   * disable themselves appropriately.
   * @param e
   * @param status
   */
  private void notifyInstanceControllers(NodeServerEvent e, InstanceStatus status) {
    for (Map<String, InstanceController> map : controllers.values()) {
      if (map.containsKey(e.getServer())) {
        map.get(e.getServer()).onStatusEvent(e, status);
      }
    }
  }

  /**
   * Highlight the row corresponding to a particular server
   * @param server
   */
  public void highlight(String server) {
    int i = globalServers.indexOf(server);
    int j = localServers.indexOf(server);
    if (i >= 0) table.getRowFormatter().addStyleName(globalRow(i), "highlight");
    if (j >= 0) table.getRowFormatter().addStyleName(localRow(j), "highlight");
  }

  /**
   * Unhighlight the row corresponding to a particular server
   * @param server
   */
  public void unhighlight(String server) {
    int i = globalServers.indexOf(server);
    int j = localServers.indexOf(server);
    if (i >= 0) table.getRowFormatter().removeStyleName(globalRow(i), "highlight");
    if (j >= 0) table.getRowFormatter().removeStyleName(localRow(j), "highlight");
  }


  private int serverCol(int i) {
    return i + 2;
  }

  private int serverCols() {
    return nodes.size() + 2;
  }

  /**
   * Return the table row for a global server
   * @param i
   * @return
   */
  private int globalRow(int i) {
    return globalHeaderRow() + i + 1;
  }

  private int globalHeaderRow() {
    return 0;
  }

  private int globalRows() {
    return globalServers.size() + 2;
  }



  /**
   * Return the table row for a local server
   * @param i
   * @return
   */
  private int localRow(int i) {
    return localHeaderRow() + i + 1;
  }

  private int localHeaderRow() {
    return globalRows();
  }

  private int localRows() {
    return localServers.size() + 1;
  }

  /**
   * Check whether the given node exists in the list
   * @param node
   * @return
   */
  private boolean nodeExists(String node) {
    return nodes.contains(node);
  }

  /**
   * Check whether the given server exists in the list
   * @param server
   * @return
   */
  private boolean serverExists(String server) {
    return globalServers.contains(server) || localServers.contains(server);
  }
}
