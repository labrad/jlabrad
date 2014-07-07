package org.labrad.browser;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.labrad.browser.client.Util;
import org.labrad.browser.client.event.RemoteEvent;
import org.mortbay.util.ajax.Continuation;

import com.google.common.collect.Lists;

public class ClientEventQueue {
  private List<RemoteEvent> events = Lists.newArrayList();

  public synchronized boolean hasEvents() {
    return events.size() > 0;
  }

  public synchronized List<RemoteEvent> getEvents() {
    List<RemoteEvent> evts = Lists.newArrayList(events);
    events.clear();
    return evts;
  }
  
  
  private Continuation continuation = null;
  
  public synchronized void setContinuation(Continuation continuation) {
    this.continuation = continuation;
  }

  public synchronized void addEvent(RemoteEvent e) {
    events.add(e);
    if (continuation != null) {
      continuation.resume();
    }
  }
  
  
  //
  // registry of all client event queues
  //

  private static final Map<String, ClientEventQueue> clients = Util.newHashMap();

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
