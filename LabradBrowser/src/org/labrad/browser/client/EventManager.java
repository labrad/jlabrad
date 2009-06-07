package org.labrad.browser.client;

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
		getEventCallback = new AsyncCallback<RemoteEventType>() {
			public void onFailure(Throwable caught) {
				LogWindow.log("[Error] getEvent", caught);
				connected = false;
				pollLater(ERROR_DELAY);
			}

			public void onSuccess(RemoteEventType type) {
				fetchEvent(type);
				poll();
			}
		};
		
		pollTimer = new Timer() {
			public void run() { poll(); }
		};
		
		// callbacks to handle remote events
		createEventCallbacks();
		
		// start the message handling loop
		poll();
	}
	
	
	// callbacks for connection and event notifications
	private final AsyncCallback<String> connectCallback;
	private final AsyncCallback<RemoteEventType> getEventCallback;
	
	/**
	 * The main polling loop.  Tries to establish a connection to the server,
	 * or if already connected, tries to fetch available events.
	 */
	private void poll() {
		if (!connected) {
			eventService.connect(connectCallback);
		} else {
			eventService.getEvent(getEventCallback);
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
	
	
	/**
	 * Fetch an event when one becomes available.
	 * @param type
	 */
	private void fetchEvent(RemoteEventType type) {
		if (type == null) return;
		switch (type) {
		case LABRAD_CONNECT: eventService.getLabradConnectEvent(labradConnectCallback); break;
		case LABRAD_DISCONNECT: eventService.getLabradDisconnectEvent(labradDisconnectCallback); break;
		case SERVER_CONNECT: eventService.getServerConnectEvent(serverConnectCallback); break;
		case SERVER_DISCONNECT: eventService.getServerDisconnectEvent(serverDisconnectCallback); break;
		case NODE_REQUEST_FAILED: eventService.getNodeRequestFailedEvent(nodeRequestFailedCallback); break;
		case NODE_SERVER_STARTING: eventService.getNodeServerStartingEvent(nodeServerStartingCallback); break;	
		case NODE_SERVER_STARTED: eventService.getNodeServerStartedEvent(nodeServerStartedCallback); break;	
		case NODE_SERVER_STOPPING: eventService.getNodeServerStoppingEvent(nodeServerStoppingCallback); break;
		case NODE_SERVER_STOPPED: eventService.getNodeServerStoppedEvent(nodeServerStoppedCallback); break;
		case NODE_STATUS: eventService.getNodeStatusEvent(nodeStatusCallback); break;
		}
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
	
	// event callbacks
	private AsyncCallback<LabradConnectEvent> labradConnectCallback;
	private AsyncCallback<LabradDisconnectEvent> labradDisconnectCallback;
	private AsyncCallback<ServerConnectEvent> serverConnectCallback;
	private AsyncCallback<ServerDisconnectEvent> serverDisconnectCallback;
	private AsyncCallback<NodeRequestFailedException> nodeRequestFailedCallback;
	private AsyncCallback<NodeServerStartingEvent> nodeServerStartingCallback;
	private AsyncCallback<NodeServerStartedEvent> nodeServerStartedCallback;
	private AsyncCallback<NodeServerStoppingEvent> nodeServerStoppingCallback;
	private AsyncCallback<NodeServerStoppedEvent> nodeServerStoppedCallback;
	private AsyncCallback<NodeStatusEvent> nodeStatusCallback;
	
	/**
	 * Create callbacks for the various events we need to handle.
	 */
	private void createEventCallbacks() {
		labradConnectCallback = createCallback(labradConnectHandlers);
		labradDisconnectCallback = createCallback(labradDisconnectHandlers);
		serverConnectCallback = createCallback(serverConnectHandlers);
		serverDisconnectCallback = createCallback(serverDisconnectHandlers);
		nodeRequestFailedCallback = createCallback(nodeRequestFailedHandlers);
		nodeServerStartingCallback = createCallback(nodeServerStartingHandlers);
		nodeServerStartedCallback = createCallback(nodeServerStartedHandlers);
		nodeServerStoppingCallback = createCallback(nodeServerStoppingHandlers);
		nodeServerStoppedCallback = createCallback(nodeServerStoppedHandlers);
		nodeStatusCallback = createCallback(nodeStatusHandlers);
	}

	/**
	 * Create a callback to handle a particular event type.
	 * @param <T> The RemoteEventType handled by this callback
	 * @param handlers The handlers that should be notified about this event
	 * @return a new AsyncCallback that will pass this event on to registered handlers
	 */
	private <T extends RemoteEvent> AsyncCallback<T> createCallback(final RemoteEventSupport<T> handlers) {
		return new AsyncCallback<T>() {
			public void onFailure(Throwable caught) {
				LogWindow.log("[Error] getRemoteEvent", caught);
			}
			public void onSuccess(T event) {
				String[] segments = event.getClass().getName().split("\\.");
				String className = segments[segments.length-1];
				LogWindow.log("[Event] " + className, event.toString());
				handlers.onEvent(event);
			}
		};
	}
}
