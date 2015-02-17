package org.labrad;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.Record;
import org.labrad.data.Request;
import org.labrad.errors.LabradException;

public class ContextManager {
  final ServerConnection connection;
  ServerContext server;
  Context context;
  final Object lock = new Object();
  boolean initialized = false;
  boolean currentlyServing = false;
  boolean expired = false;
  final List<Packet> requestBuffer = new ArrayList<Packet>();

  /**
   * Create a context manager that will serve the given context.
   * @param cxn
   * @param serverClass
   * @param context
   * @return
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static ContextManager create(ServerConnection cxn, Context context) {
    return new ContextManager(cxn, context);
  }

  private ContextManager(ServerConnection connection, Context context) {
    this.connection = connection;
    this.context = context;
  }

  protected ServerContext getServerContext() {
    return server;
  }

  // suppose an expire message comes in.  What should we do?  We should cancel pending
  // requests and then call the expire() function on the context and wait for it to finish.
  // All this may take some time.  What if another request comes in in the same context before
  // we are done?  One possibility: when we are done with the cleanup we tell the serverConnection
  // that we are done so that it can remove us from the list.  If another request comes in before
  // that happens, the ServerConnection will pass it along to us as it normally would.
  // what api do these loops need?
  // - queue up an event
  // - cancel any pending events
  // - wait for pending events to finish
  // - stop the loop (and wait for pending events to finish)
  //

  // incoming requests: synchronized for this context
  // incoming messages: asynchronous, asynchronous with requests, or synchronous with requests
  // how does the user specify which model to use?
  // how is this ever going to work?
  // should we revert back to a model like Twisted's?  Probably not so suitable for java (ugly code)
  //
  // have two event loops: request loop, message loop.
  // messages can be handled in one of three ways:
  // - dispatched to request loop (serialized with requests)
  // - dispatched to message loop (serialized with messages)
  // - dispatched to underlying thread pool directly (fully asynchronous)

  public void expire() {
    // the executor will get shutdown by the server connection who owns it,
    // so we just tell the server context object to expire itself.
    // TODO need to fix up the concurrency here
    // what do we actually want to do here:
    // - wait for any pending requests to finish or expire
    // - then tell the server to expire and wait for that to finish
    // - do we do this in the (global) message dispatch thread, or our own current thread?
    //
    // the way contexts are currently implemented, each context has its own message thread,
    // though the identity of that thread can change since we dispatch new continuations to
    // the thread each time something happens.  Thus, we should probably abstract out an
    // "eventQueue" implementation, and then allow each context to run it's own event dispatch
    // thread.  That way, different contexts can communicate by dispatching to each other's
    // event loops, as well as to the main event loop of the main server object.
    // if desired, of course, contexts can call methods on the global server object directly
    // in their current thread.  Could this be done with some kind of executor?  basically,
    // we want an executor that shares a pool of threads with a set of other executors.
    server.expire();
  }

  /**
   * Serve a single request.  If a request task is already running for this context,
   * we just queue up the packet to be served by that task.  Otherwise, we start a
   * new task to serve requests in this context.
   * @param request
   */
  public void serveRequest(Packet request) {
    try {
      if (!initialized) {
        server = connection.getServerClass().newInstance();
        server.setContext(context);
        server.setConnection(connection);
        server.init();
        initialized = true;
      }
      if (request.getRecords().size() == 0) {
        sendResponse(request, responseFor(request));
        return;
      }
      synchronized (lock) {
        requestBuffer.add(request);
        if (!currentlyServing) {
          currentlyServing = true;
          connection.submit(new RequestProcessor());
        }
      }
    } catch (Exception ex) {
      // note that this catch clause should be unnecessary
      // it will only get called if something happens in adding
      // the request to the buffer, or in submitting to the executor.
      // If an error occurs in either of those places, something
      // has gone very wrong.
      Request response = responseFor(request);
      response.add(request.getRecords().get(0).getID(), errorFor(ex));
      sendResponse(request, response);
    }
  }

  /**
   * Serve a single request.
   * @param packet
   */
  private void processRequest(Packet packet) {
    server.setSource(packet.getTarget());
    Request response = responseFor(packet);
    try {
      for (Record rec : packet.getRecords()) {
        SettingHandler h = connection.getHandler(rec.getID());
        Data respData;
        try {
          respData = h.handle(server, rec.getData());
        } catch (LabradException ex) {
          respData = errorFor(ex);
        } catch (Throwable ex) {
          respData = errorFor(ex);
        }
        response.add(rec.getID(), respData);
        if (respData.isError()) break;
      }
    } catch (Exception ex) {
      // note that this try-catch should be unneccessary
      // the only way it could be triggered is if the call
      // to getHandler were to fail, which should be impossible
      // since the manager checks the setting IDs sent to us.
      response.add(packet.getRecords().get(0).getID(), errorFor(ex));
    }
    sendResponse(packet, response);
  }

  /**
   * Processes requests in a single context.  When done serving a request, we check to see whether
   * there are more requests pending in this context, and if so we continue serving.  If not,
   * This task will exit.  This allows for more contexts to be served than if each context had
   * a long-lived thread.
   *
   */
  private class RequestProcessor implements Runnable {
    public void run() {
      Packet request;
      while (true) {
        synchronized (lock) {
          if (requestBuffer.size() > 0) {
            request = requestBuffer.remove(0);
          } else {
            currentlyServing = false;
            break;
          }
        }
        processRequest(request);
      }
    }
  }

  /**
   * Create a response to the given request packet.
   * @param request
   * @return
   */
  private Request responseFor(Packet request) {
    return Request.to(request.getTarget(), request.getContext());
  }

  /**
   * Send back a response to the given request packet.
   * @param request
   * @param response
   */
  private void sendResponse(Packet request, Request response) {
    connection.sendResponse(Packet.forRequest(response, -request.getRequest()));
  }

  /**
   * Turn a LabradException into an error record.
   * @param ex
   * @return
   */
  private Data errorFor(LabradException ex) {
    return Data.ofType("E").setError(ex.getCode(), ex.getMessage());
  }

  /**
   * Turn a generic exception into an error record.
   * @param ex
   * @return
   */
  private Data errorFor(Throwable ex) {
    StringWriter sw = new StringWriter();
    ex.printStackTrace(new PrintWriter(sw));
    return Data.ofType("E").setError(0, sw.toString());
  }
}
