package org.labrad.browser;

import java.util.List;
import java.util.Map;

import org.labrad.Client;
import org.labrad.browser.client.event.LabradConnectEvent;
import org.labrad.browser.client.event.LabradDisconnectEvent;
import org.labrad.browser.client.event.NodeServerStartedEvent;
import org.labrad.browser.client.event.NodeServerStartingEvent;
import org.labrad.browser.client.event.NodeServerStatus;
import org.labrad.browser.client.event.NodeServerStoppedEvent;
import org.labrad.browser.client.event.NodeServerStoppingEvent;
import org.labrad.browser.client.event.NodeStatusEvent;
import org.labrad.browser.client.event.ServerConnectEvent;
import org.labrad.browser.client.event.ServerDisconnectEvent;
import org.labrad.browser.server.NodeServiceImpl;
import org.labrad.data.Data;
import org.labrad.data.Request;
import org.labrad.events.ConnectionEvent;
import org.labrad.events.ConnectionListener;
import org.labrad.events.MessageEvent;
import org.labrad.events.MessageListener;

import com.google.common.collect.Maps;

public class LabradConnection {
  private static LabradConnection instance = null;

  /**
   * Get the singleton connection instance.
   * @return
   */
  public static synchronized Client get() {
    if (instance == null) {
      instance = new LabradConnection();
    }
    return instance.getClient();
  }

  /**
   * When the client disconnects, delete the connection instance
   */
  private static synchronized void disconnect() {
    instance = null;
  }



  private Client cxn;
  private long nextMessageId = 1;

  private LabradConnection() {
    try {
      // create a connection and connect to LabRAD
      cxn = new Client();
      cxn.connect();	

      handleConnectionEvents();

      // sign up for messages we want to get
      Request req = Request.to("Manager");
      subscribeToServerConnectMessages(req);
      subscribeToServerDisconnectMessages(req);
      subscribeToNodeServerStartingMessages(req);
      subscribeToNodeServerStartedMessages(req);
      subscribeToNodeServerStoppingMessages(req);
      subscribeToNodeServerStoppedMessages(req);
      subscribeToNodeStatusMessages(req);
      cxn.sendAndWait(req);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Client getClient() {
    return cxn;
  }

  /**
   * Handle connection and disconnection events on our client
   */
  private void handleConnectionEvents() {
    cxn.addConnectionListener(new ConnectionListener() {
      public void connected(ConnectionEvent evt) {
        ClientEventQueue.addGlobalEvent(
            new LabradConnectEvent(cxn.getHost()));
      }

      public void disconnected(ConnectionEvent evt) {
        ClientEventQueue.addGlobalEvent(
            new LabradDisconnectEvent(cxn.getHost()));

        // delete the static connection instance 
        disconnect();
      }
    });
  }

  /**
   * Get the next unique message id
   * @return
   */
  private long getMessageId() {
    long id = nextMessageId++;
    return id;
  }

  /**
   * Add a call to subscribe to a named message onto an existing Request
   * @param req
   * @param message
   * @param id
   */
  private void addSubscription(Request req, String message, long id) {
    req.add("Subscribe to Named Message",
      Data.clusterOf(
        Data.valueOf(message),
        Data.valueOf(id),
        Data.valueOf(true)));
  }

  /**
   * Subscribe to server connection messages
   * @param req
   */
  private void subscribeToServerConnectMessages(Request req) {
    final long id = getMessageId();
    cxn.addMessageListener(new MessageListener() {
      public void messageReceived(MessageEvent e) {
        if (e.getMessageID() != id) return;
        String server = e.getData().get(1).getString();
        ClientEventQueue.addGlobalEvent(new ServerConnectEvent(server));
      }
    });
    addSubscription(req, "Server Connect", id);
  }


  /**
   * Subscribe to server disconnection messages
   * @param req
   */
  private void subscribeToServerDisconnectMessages(Request req) {
    final long id = getMessageId();
    cxn.addMessageListener(new MessageListener() {
      public void messageReceived(MessageEvent e) {
        if (e.getMessageID() != id) return;
        String server = e.getData().get(1).getString();
        ClientEventQueue.addGlobalEvent(new ServerDisconnectEvent(server));
      }
    });
    addSubscription(req, "Server Disconnect", id);
  }


  /**
   * Subscribe to connection messages from the node servers
   * @param req
   */
  private void subscribeToNodeServerStartingMessages(Request req) {
    final long id = getMessageId();
    cxn.addMessageListener(new MessageListener() {
      public void messageReceived(MessageEvent e) {
        if (e.getMessageID() != id) return;
        Map<String, Data> map = parseNodeMessage(e.getData());
        String node = map.get("node").getString();
        String server = map.get("server").getString();
        String instance = map.get("instance").getString();
        ClientEventQueue.addGlobalEvent(new NodeServerStartingEvent(node, server, instance));
      }
    });
    addSubscription(req, "node.server_starting", id);
  }

  /**
   * Subscribe to connection messages from the node servers
   * @param req
   */
  private void subscribeToNodeServerStartedMessages(Request req) {
    final long id = getMessageId();
    cxn.addMessageListener(new MessageListener() {
      public void messageReceived(MessageEvent e) {
        if (e.getMessageID() != id) return;
        Map<String, Data> map = parseNodeMessage(e.getData());
        String node = map.get("node").getString();
        String server = map.get("server").getString();
        String instance = map.get("instance").getString();
        ClientEventQueue.addGlobalEvent(new NodeServerStartedEvent(node, server, instance));
      }
    });
    addSubscription(req, "node.server_started", id);
  }


  /**
   * Subscribe to disconnection messages from node servers
   * @param req
   */
  private void subscribeToNodeServerStoppingMessages(Request req) {
    final long id = getMessageId();
    cxn.addMessageListener(new MessageListener() {
      public void messageReceived(MessageEvent e) {
        if (e.getMessageID() != id) return;
        Map<String, Data> map = parseNodeMessage(e.getData());
        String node = map.get("node").getString();
        String server = map.get("server").getString();
        String instance = map.get("instance").getString();
        ClientEventQueue.addGlobalEvent(new NodeServerStoppingEvent(node, server, instance));
      }
    });
    addSubscription(req, "node.server_stopping", id);
  }

  /**
   * Subscribe to disconnection messages from node servers
   * @param req
   */
  private void subscribeToNodeServerStoppedMessages(Request req) {
    final long id = getMessageId();
    cxn.addMessageListener(new MessageListener() {
      public void messageReceived(MessageEvent e) {
        if (e.getMessageID() != id) return;
        Map<String, Data> map = parseNodeMessage(e.getData());
        String node = map.get("node").getString();
        String server = map.get("server").getString();
        String instance = map.get("instance").getString();
        ClientEventQueue.addGlobalEvent(new NodeServerStoppedEvent(node, server, instance));
      }
    });
    addSubscription(req, "node.server_stopped", id);
  }


  /**
   * Subscribe to status messages from node servers
   * @param req
   */
  private void subscribeToNodeStatusMessages(Request req) {
    final long id = getMessageId();
    cxn.addMessageListener(new MessageListener() {
      public void messageReceived(MessageEvent e) {
        if (e.getMessageID() != id) return;
        Map<String, Data> map = parseNodeMessage(e.getData());
        String node = map.get("node").getString();
        Data serversData = map.get("servers");
        List<NodeServerStatus> statuses = NodeServiceImpl.getServerStatuses(serversData);
        ClientEventQueue.addGlobalEvent(new NodeStatusEvent(node, statuses));
      }
    });
    addSubscription(req, "node.status", id);
  }

  /**
   * Parse messages coming from the nodes, which contain several key-value pairs
   * @param msg
   * @return
   */
  private static Map<String, Data> parseNodeMessage(Data msg) {
    Map<String, Data> map = Maps.newHashMap();
    Data payload = msg.get(1);
    for (int i = 0; i < payload.getClusterSize(); i++) {
      Data item = payload.get(i);
      map.put(item.get(0).getString(), item.get(1));
    }
    return map;
  }

}
