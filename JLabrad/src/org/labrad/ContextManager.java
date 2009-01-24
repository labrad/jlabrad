package org.labrad;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.Record;
import org.labrad.data.Request;
import org.labrad.errors.LabradException;

public class ContextManager {
	final ServerConnection connection;
	final ContextServer server;
	final ExecutorService executor;
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
			Class<? extends ContextServer> serverClass, Context context)
				throws InstantiationException, IllegalAccessException {
		ContextServer server;
		server = serverClass.newInstance();
        server.setContext(context);
        server.setConnection(cxn);
        return new ContextManager(cxn, server);
	}
	
	private ContextManager(ServerConnection connection, ContextServer server) {
		this.connection = connection;
		this.server = server;
		this.executor = connection.getExecutor();
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
                if (currentlyServing) {
                	requestBuffer.add(request);
                } else {
                    executor.submit(new RequestTask(request));
                    currentlyServing = true;
                }
            }
        } catch (Exception ex) {
        	// note that this catch clause should be unnecessary
            Request response = responseFor(request);
    		response.add(request.getRecords().get(0).getID(), errorFor(ex));
    		sendResponse(request, response);
        }
	}
	
	/**
     * Serves requests in a single context.  When done serving a request, we check to see whether
     * there are more requests pending in this context, and if so we continue serving.  If not,
     * This task will exit.  This allows for more contexts to be served than if each context had
     * a long-lived thread.
     *
     */
    public class RequestTask implements Runnable {
    	private Packet request;
    	
        public RequestTask(Packet request) {
        	this.request = request;
        }

        /**
         * Serve requests until all pending requests are finished.
         */
        public void run() {
            while (true) {
                serveRequest(request);
                synchronized (lock) {
                    if (requestBuffer.size() == 0) {
                        currentlyServing = false;
                        break;
                    } else {
                        request = requestBuffer.remove(0);
                    }
                }
            }
        }

        /**
         * Serve a single request.
         * @param packet
         */
        private void serveRequest(Packet packet) {
            server.setSource(packet.getTarget());
            Request response = responseFor(packet);
            try {
                for (Record rec : packet.getRecords()) {
                    Method m = connection.getHandler(rec.getID());
                    Data respData;
                    try {
                        respData = (Data) m.invoke(server, rec.getData());
                    } catch (LabradException ex) {
                        respData = errorFor(ex);
                    } catch (Exception ex) {
                        respData = errorFor(ex);
                    }
                    response.add(rec.getID(), respData);
                    if (respData.isError()) break;
                }
            } catch (Exception ex) {
            	// note that this try-catch should be unneccessary
                response.add(packet.getRecords().get(0).getID(), errorFor(ex));
            }
            sendResponse(packet, response);
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
	private Data errorFor(Exception ex) {
		StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
		return Data.ofType("E").setError(0, sw.toString());
	}
}
