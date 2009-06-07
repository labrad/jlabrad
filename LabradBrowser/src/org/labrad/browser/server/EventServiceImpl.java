package org.labrad.browser.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.labrad.Connection;
import org.labrad.RequestCallback;
import org.labrad.browser.ClientEventQueue;
import org.labrad.browser.LabradConnection;
import org.labrad.browser.client.LabradConnectEvent;
import org.labrad.browser.client.LabradDisconnectEvent;
import org.labrad.browser.client.NodeRequestFailedException;
import org.labrad.browser.client.NodeServerStartedEvent;
import org.labrad.browser.client.NodeServerStartingEvent;
import org.labrad.browser.client.NodeServerStoppedEvent;
import org.labrad.browser.client.NodeServerStoppingEvent;
import org.labrad.browser.client.NodeStatusEvent;
import org.labrad.browser.client.RemoteEventService;
import org.labrad.browser.client.RemoteEventType;
import org.labrad.browser.client.ServerConnectEvent;
import org.labrad.browser.client.ServerDisconnectEvent;
import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Request;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;


@SuppressWarnings("serial")
public class EventServiceImpl extends AsyncRemoteServiceServlet implements RemoteEventService {

	public static final int DELAY_MILLIS = 10000;
	
	/**
	 * Connect a new client that will be notified of messages
	 */
	public String connect() {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		ClientEventQueue.connectClient(session);
		return session.getId();
	}
	
	/**
	 * Disconnect a client and stop holding messages for them
	 */
	public String disconnect() {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		ClientEventQueue.disconnectClient(session);
		return session.getId();
	}
	
	/**
	 * Get a the next event type that have been queued since the last time we checked
	 */
	public RemoteEventType getEvent() {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		ClientEventQueue client = ClientEventQueue.forSession(session);
		
		synchronized (client) {

	        // Are there any messages ready to send?
	        if (!client.hasEvents()) {
	            // No - so prepare a continuation
	            Continuation continuation = ContinuationSupport.getContinuation(request, client);
	            client.setContinuation(continuation);

	            // wait for a message or timeout
	            continuation.suspend(DELAY_MILLIS);
	        }
	        client.setContinuation(null);

	        // send any events
	        return client.getNextEventType();
	    }
	}
	
	/**
	 * Get the event queue for the current request and session
	 * @return
	 */
	private ClientEventQueue getQueue() {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		return ClientEventQueue.forSession(session);
	}

	public LabradConnectEvent getLabradConnectEvent() {
		return getQueue().getEvent(LabradConnectEvent.class);
	}
	
	public LabradDisconnectEvent getLabradDisconnectEvent() {
		return getQueue().getEvent(LabradDisconnectEvent.class);
	}
	
	
	public ServerConnectEvent getServerConnectEvent() {
		return getQueue().getEvent(ServerConnectEvent.class);
	}
	
	public ServerDisconnectEvent getServerDisconnectEvent() {
		return getQueue().getEvent(ServerDisconnectEvent.class);
	}
	
	
	public String startServer(String node, String server) {
		return doRequest(node, server, "start");
	}
	
	public String stopServer(String node, String instance) {
		return doRequest(node, instance, "stop");
	}
	
	public String restartServer(String node, String instance) {
		return doRequest(node, instance, "restart");
	}
	
	private String doRequest(final String node, final String server, final String action) {
		Connection cxn = LabradConnection.get();
		Context ctx = cxn.newContext();
		Request req = Request.to(node, ctx);
		req.add(action, Data.valueOf(server));
		cxn.send(req, new RequestCallback() {
			public void onFailure(Request request, Throwable cause) {
				if (cause.getCause() != null) {
					cause = cause.getCause();
				}
				NodeRequestFailedException e;
				e = new NodeRequestFailedException(node, server, action, cause.getMessage());
				ClientEventQueue.addGlobalEvent(e);
			}

			public void onSuccess(Request request, List<Data> response) {
				// do nothing, since a message will get sent by the node
			}
		});
		return null;
	}
	
	public NodeRequestFailedException getNodeRequestFailedEvent() {
		return getQueue().getEvent(NodeRequestFailedException.class);
	}	
	
	
	public NodeServerStartingEvent getNodeServerStartingEvent() {
		return getQueue().getEvent(NodeServerStartingEvent.class);
	}
	
	public NodeServerStartedEvent getNodeServerStartedEvent() {
		return getQueue().getEvent(NodeServerStartedEvent.class);
	}
	
	public NodeServerStoppingEvent getNodeServerStoppingEvent() {
		return getQueue().getEvent(NodeServerStoppingEvent.class);
	}
	
	public NodeServerStoppedEvent getNodeServerStoppedEvent() {
		return getQueue().getEvent(NodeServerStoppedEvent.class);
	}
	
	public NodeStatusEvent getNodeStatusEvent() {
		return getQueue().getEvent(NodeStatusEvent.class);
	}
}
