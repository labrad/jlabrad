package org.labrad.grapher;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.labrad.grapher.client.RemoteEvent;
import org.mortbay.util.ajax.Continuation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ClientEventQueue {
  private Continuation continuation = null;

  private List<RemoteEvent> events = Lists.newArrayList();

  public synchronized void setContinuation(Continuation continuation) {
    this.continuation = continuation;
  }

  public synchronized Continuation getContinuation() {
    return continuation;
  }

  public synchronized void addEvent(RemoteEvent e) {
    events.add(e);
    if (getContinuation() != null) {
      getContinuation().resume();
    }
  }

  public synchronized boolean hasEvents() {
    return events.size() > 0;
  }

  public synchronized RemoteEvent getEvent() {
    return events.remove(0);
  }


  //
  // registry of all client event queues
  //

  private static final Map<String, ClientEventQueue> clients = Maps.newHashMap();

  public static synchronized void connectClient(HttpSession session) {
    String id = session.getId();
    clients.put(id, new ClientEventQueue());
  }

  public static synchronized void disconnectClient(HttpSession session) {
    String id = session.getId();
    clients.remove(id);
  }

  public static synchronized ClientEventQueue forSession(HttpSession session) {
    String id = session.getId();
    return clients.get(id);
  }

  public static synchronized void addGlobalEvent(RemoteEvent e) {
    for (ClientEventQueue queue : clients.values()) {
      queue.addEvent(e);
    }
  }
}
