package org.labrad.browser.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.labrad.Connection;
import org.labrad.RequestCallback;
import org.labrad.browser.ClientEventQueue;
import org.labrad.browser.LabradConnection;
import org.labrad.browser.client.event.NodeRequestFailedException;
import org.labrad.browser.client.event.RemoteEvent;
import org.labrad.browser.client.event.RemoteEventService;
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
   * Get the event type that has been queued since the last time we checked
   */
  public List<RemoteEvent> getEvents() {
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
      return client.getEvents();
    }
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

}
