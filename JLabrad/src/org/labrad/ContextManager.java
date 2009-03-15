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
	final ServerContext server;
	final Object lock = new Object();
	boolean currentlyServing = false;
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
	public static ContextManager create(ServerConnection cxn,
			Class<? extends ServerContext> serverClass, Context context)
				throws InstantiationException, IllegalAccessException {
		ServerContext server;
		server = serverClass.newInstance();
        server.setContext(context);
        server.setConnection(cxn);
        server.init();
        return new ContextManager(cxn, server);
	}
	
	private ContextManager(ServerConnection connection, ServerContext server) {
		this.connection = connection;
		this.server = server;
	}
	
	public void expire() {
		// the executor will get shutdown by the server connection who owns it,
		// so we just tell the server context object to expire itself.
		// TODO need to fix up the concurrency here
		server.expire();
	}
	
	/**
	 * Serve a single request.  If a request task is already running for this context,
	 * we just queue up the packet to be served by that task.  Otherwise, we start a
	 * new task to serve requests in this context.
	 * @param request
	 */
	public void serveRequest(Packet request) {
		if (request.getRecords().size() == 0) {
			sendResponse(request, responseFor(request));
			return;
		}
		try {
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
