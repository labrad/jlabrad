package org.labrad.browser.client.event;

import java.util.List;

import org.labrad.browser.client.LogWindow;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class EventManager {
  /**
   * The RPC service that sends events and event notifications
   */
  private static final RemoteEventServiceAsync eventService = GWT.create(RemoteEventService.class);

  // how long to wait before retrying when we lose our connection
  public static final int ERROR_DELAY = 5000;

  // singleton object
  private static final EventManager instance = new EventManager();

  /**
   * Get the singleton EventManager instance.
   * @return
   */
  public static EventManager get() {
    return instance;
  }


  private boolean connected = false;
  @SuppressWarnings("unused")
  private String connectionId;

  private EventManager() {
    // callback for when we connect to the event service
    connectCallback = new AsyncCallback<String>() {
      public void onFailure(Throwable caught) {
        LogWindow.log("[Error] connect", caught);
        pollLater(ERROR_DELAY);
      }

      public void onSuccess(String result) {
        connectionId = result;
        connected = true;
        poll();
      }
    };

    // callback for when we are notified that an event is ready
    getEventCallback = new AsyncCallback<List<RemoteEvent>>() {
      public void onFailure(Throwable caught) {
        LogWindow.log("[Error] getEvent", caught);
        connected = false;
        pollLater(ERROR_DELAY);
      }

      public void onSuccess(List<RemoteEvent> events) {
        if (events != null) {
          for (RemoteEvent e : events) {
            String[] segments = e.getClass().getName().split("\\.");
            String className = segments[segments.length-1];
            LogWindow.log("[Event] " + className, e.toString());
            
            if (e instanceof LabradConnectEvent) {
              labradConnectHandlers.onEvent((LabradConnectEvent)e);
            
            } else if (e instanceof LabradDisconnectEvent) {
              labradDisconnectHandlers.onEvent((LabradDisconnectEvent)e);
            
            } else if (e instanceof NodeRequestFailedException) {
              nodeRequestFailedHandlers.onEvent((NodeRequestFailedException)e);
              
            } else if (e instanceof NodeServerStartedEvent) {
              nodeServerStartedHandlers.onEvent((NodeServerStartedEvent)e);
            
            } else if (e instanceof NodeServerStartingEvent) {
              nodeServerStartingHandlers.onEvent((NodeServerStartingEvent)e);
            
            } else if (e instanceof NodeServerStoppedEvent) {
              nodeServerStoppedHandlers.onEvent((NodeServerStoppedEvent)e);
            
            } else if (e instanceof NodeServerStoppingEvent) {
              nodeServerStoppingHandlers.onEvent((NodeServerStoppingEvent)e);
            
            } else if (e instanceof NodeStatusEvent) {
              nodeStatusHandlers.onEvent((NodeStatusEvent)e);
            
            }
          }
        }
        poll();
      }
    };

    pollTimer = new Timer() {
      public void run() { poll(); }
    };

    // start the message handling loop
    poll();
  }


  // callbacks for connection and event notifications
  private final AsyncCallback<String> connectCallback;
  private final AsyncCallback<List<RemoteEvent>> getEventCallback;

  /**
   * The main polling loop.  Tries to establish a connection to the server,
   * or if already connected, tries to fetch available events.
   */
  private void poll() {
    if (!connected) {
      eventService.connect(connectCallback);
    } else {
      eventService.getEvents(getEventCallback);
    }
  }


  /**
   * Timer that restarts the polling loop.
   */
  private final Timer pollTimer;

  /**
   * Restart the message polling loop after a specified delay
   * @param delayMillis
   */
  private void pollLater(int delayMillis) {
    pollTimer.schedule(delayMillis);
  }

  // lists of event handlers
  private final RemoteEventSupport<LabradConnectEvent> labradConnectHandlers = RemoteEventSupport.create();
  private final RemoteEventSupport<LabradDisconnectEvent> labradDisconnectHandlers = RemoteEventSupport.create();
  private final RemoteEventSupport<ServerConnectEvent> serverConnectHandlers = RemoteEventSupport.create();
  private final RemoteEventSupport<ServerDisconnectEvent> serverDisconnectHandlers = RemoteEventSupport.create();
  private final RemoteEventSupport<NodeRequestFailedException> nodeRequestFailedHandlers = RemoteEventSupport.create();
  private final RemoteEventSupport<NodeServerStartingEvent> nodeServerStartingHandlers = RemoteEventSupport.create();
  private final RemoteEventSupport<NodeServerStartedEvent> nodeServerStartedHandlers = RemoteEventSupport.create();
  private final RemoteEventSupport<NodeServerStoppingEvent> nodeServerStoppingHandlers = RemoteEventSupport.create();
  private final RemoteEventSupport<NodeServerStoppedEvent> nodeServerStoppedHandlers = RemoteEventSupport.create();
  private final RemoteEventSupport<NodeStatusEvent> nodeStatusHandlers = RemoteEventSupport.create();

  // add event handlers
  public void addHandler(LabradConnectHandler h) { labradConnectHandlers.add(h); }
  public void addHandler(LabradDisconnectHandler h) { labradDisconnectHandlers.add(h); }
  public void addHandler(ServerConnectHandler h) { serverConnectHandlers.add(h); }
  public void addHandler(ServerDisconnectHandler h) { serverDisconnectHandlers.add(h); }
  public void addHandler(NodeRequestFailedHandler h) { nodeRequestFailedHandlers.add(h); }
  public void addHandler(NodeServerStartingHandler h) { nodeServerStartingHandlers.add(h); }
  public void addHandler(NodeServerStartedHandler h) { nodeServerStartedHandlers.add(h); }
  public void addHandler(NodeServerStoppingHandler h) { nodeServerStoppingHandlers.add(h); }
  public void addHandler(NodeServerStoppedHandler h) { nodeServerStoppedHandlers.add(h); }
  public void addHandler(NodeStatusHandler h) { nodeStatusHandlers.add(h); }

  // remove event handlers
  public void removeHandler(LabradConnectHandler h) { labradConnectHandlers.remove(h); }
  public void removeHandler(LabradDisconnectHandler h) { labradDisconnectHandlers.remove(h); }
  public void removeHandler(ServerConnectHandler h) { serverConnectHandlers.remove(h); }
  public void removeHandler(ServerDisconnectHandler h) { serverDisconnectHandlers.remove(h); }
  public void removeHandler(NodeRequestFailedHandler h) { nodeRequestFailedHandlers.remove(h); }
  public void removeHandler(NodeServerStartingHandler h) { nodeServerStartingHandlers.remove(h); }
  public void removeHandler(NodeServerStartedHandler h) { nodeServerStartedHandlers.remove(h); }
  public void removeHandler(NodeServerStoppingHandler h) { nodeServerStoppingHandlers.remove(h); }
  public void removeHandler(NodeServerStoppedHandler h) { nodeServerStoppedHandlers.remove(h); }
  public void removeHandler(NodeStatusHandler h) { nodeStatusHandlers.remove(h); }

}
