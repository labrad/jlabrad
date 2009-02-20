/*
 * Copyright 2008 Matthew Neeley
 *
 * This file is part of JLabrad.
 *
 * JLabrad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * JLabrad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JLabrad.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.labrad;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.PacketInputStream;
import org.labrad.data.PacketOutputStream;
import org.labrad.data.Request;
import org.labrad.errors.IncorrectPasswordException;
import org.labrad.errors.LoginFailedException;
import org.labrad.events.ConnectionListener;
import org.labrad.events.ConnectionListenerSupport;
import org.labrad.events.MessageListener;
import org.labrad.events.MessageListenerSupport;
import org.labrad.util.LookupProvider;
import org.labrad.util.Util;

/**
 *
 * @author maffoo
 */
public class ServerConnection implements Connection {
	/** Version for serialization. */
	private static final long serialVersionUID = 1L;

    /**
     * Create a new connection object.
     * Properties such as host, port and password will be initialized
     * from environment variable, if these have been set.  Otherwise,
     * default values will be used.
     */
    public ServerConnection() {
        // set defaults from the environment
    	setHost(Util.getEnv("LABRADHOST", Constants.DEFAULT_HOST));
        setPort(Util.getEnvInt("LABRADPORT", Constants.DEFAULT_PORT));
    	setPassword(Util.getEnv("LABRADPASSWORD", Constants.DEFAULT_PASSWORD));

        // start disconnected
    	connected = false;
    }


	// properties
	private String name;
    private String host;
    private int port;
    private String password;
    private long ID;
    private String loginMessage;
    private boolean connected = false;


    /**
     * Get the name used for this connection.
     * @return the connection name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name to be used for this connection.
     * @param the connection name
     */
    public void setName(String name) {
    	this.name = name;
    }


    /**
	 * @return the hostname to use for the connection
	 */
	public String getHost() {
        return host;
    }
    /**
     * Set the host to use for the connection to LabRAD.
     * @param host
     */
    public void setHost(String host) {
    	this.host = host;
    }


	/**
	 * @return the port to use for the connection
	 */
	public int getPort() {
        return port;
    }
    /**
     * Set the port to use for the connection to LabRAD.
     * @param port
     */
	public void setPort(int port) {
		this.port = port;
	}


	/**
     * Set the password to use for the connection to LabRAD.
     * @param password
     */
	public void setPassword(String password) {
		this.password = password;
	}


	/**
     * Get the ID assigned by the manager after connecting to LabRAD.
	 * @return the iD
	 */
	public long getID() {
		return ID;
	}


	/**
	 * Get the welcome message returned by the manager
     * after connecting to LabRAD.
	 * @return the login massage
	 */
	public String getLoginMessage() {
        return loginMessage;
    }

    private void setLoginMessage(String message) {
        loginMessage = message;
    }


    /**
     * Indicates whether we are connected to LabRAD.
     * @return a boolean indicating the connection status
     */
	public boolean isConnected() {
		return connected;
	}

	private void setConnected(boolean connected) {
        boolean old = this.connected;
        this.connected = connected;
        propertyChangeListeners.firePropertyChange("connected", old, connected);
        if (connected) {
            connectionListeners.fireConnected();
        } else {
            connectionListeners.fireDisconnected();
        }
	}


    // events
    private final PropertyChangeSupport propertyChangeListeners =
                    new PropertyChangeSupport(this);
    private final MessageListenerSupport messageListeners =
                    new MessageListenerSupport(this);
    private final ConnectionListenerSupport connectionListeners =
                    new ConnectionListenerSupport(this);

    /**
     * Add a listener for property change events.
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeListeners.addPropertyChangeListener(listener);
    }

    /**
     * Remove a listener for property change events.
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeListeners.removePropertyChangeListener(listener);
    }


    /**
     * Add a listener for LabRAD message events.
     * @param listener
     */
    public void addMessageListener(MessageListener listener) {
        messageListeners.addListener(listener);
    }

    /**
     * Remove a listener for LabRAD message events.
     * @param listener
     */
    public void removeMessageListener(MessageListener listener) {
        messageListeners.removeListener(listener);
    }


    /**
     * Add a listener for connection events.
     * @param listener
     */
    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.addListener(listener);
    }

    /**
     * Remove a listener for connection events.
     * @param listener
     */
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.removeListener(listener);
    }


	// networking stuff
    private Socket socket;
    private Thread reader, writer;
    private PacketInputStream inputStream;
    private PacketOutputStream outputStream;
    private BlockingQueue<Packet> writeQueue, handlerQueue;

    /** Request IDs that are available to be reused. */
    private RequestDispatcher requestDispatcher;

    /** Thread pool for handling lookups. */
    private ExecutorService executor = Executors.newCachedThreadPool();

    /** Performs server and method lookups. */
    private LookupProvider lookupProvider = new LookupProvider(this);


    /**
     * Connect to the LabRAD manager.
     * @throws UnknownHostException if the host and port are not valid
     * @throws IOException if a network error occurred
     * @throws IncorrectPasswordException if the password was not correct
     * @throws LoginFailedException if the login failed for some other reason
     */
    public void connect()
    		throws UnknownHostException, IOException,
                   LoginFailedException, IncorrectPasswordException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        inputStream = new PacketInputStream(socket.getInputStream());
        outputStream = new PacketOutputStream(socket.getOutputStream());

        writeQueue = new LinkedBlockingQueue<Packet>();
        handlerQueue = new LinkedBlockingQueue<Packet>();
        requestDispatcher = new RequestDispatcher(writeQueue);

        reader = new Thread(new Runnable() {
            public void run() {
                try {
                    while (!Thread.interrupted())
                        handlePacket(inputStream.readPacket());
                } catch (IOException e) {
                    // let the client know that we have disconnected.
                    if (!Thread.interrupted())
                        close(e);
                } catch (Exception e) {
                    close(e);
                }
            }
        }, "Packet Reader Thread");

        writer = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        Packet p = writeQueue.take();
                        outputStream.writePacket(p);
                    }
                } catch (InterruptedException e) {
                    // this happens when the connection is closed.
                } catch (IOException e) {
                    // let the client know that we have disconnected.
                    close(e);
                } catch (Exception e) {
                    close(e);
                }
            }
        }, "Packet Writer Thread");

        reader.start();
        writer.start();

        try {
            connected = true; // set this so that login requests will complete
            doLogin(password);
        } finally {
            connected = false;
        }
        setConnected(true);
	}


    /**
	 * Logs in to LabRAD using the standard protocol.
	 * @param password
	 * @throws IncorrectPasswordException if the password was not correct
	 * @throws LoginFailedException if the login failed for some other reason
	 */
	private void doLogin(String password)
			throws LoginFailedException, IncorrectPasswordException {
		final long mgr = Constants.MANAGER;
		Data data, response;

	    try {
            // send first ping packet
            response = sendAndWait(new Request(mgr)).get(0);

            // get password challenge
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                // TODO provide fallback MD5 hash implementation
                throw new RuntimeException("MD5 hash not supported.");
            }
            byte[] challenge = response.getBytes();
            md.update(challenge);
            md.update(password.getBytes(Data.STRING_ENCODING));

            // send password response
            try {
                data = Data.valueOf(md.digest());
                response = sendAndWait(new Request(mgr).add(0, data)).get(0);
            } catch (ExecutionException ex) {
                throw new IncorrectPasswordException();
            }

            // print welcome message
            setLoginMessage(response.getString());

            // send identification packet
            response = sendAndWait(new Request(mgr).add(0, getLoginData())).get(0);
            ID = response.getWord();

            // register settings
            registerSettings();
        } catch (InterruptedException ex) {
            throw new LoginFailedException(ex);
        } catch (ExecutionException ex) {
            throw new LoginFailedException(ex);
        } catch (IOException ex) {
            throw new LoginFailedException(ex);
        }
	}

    /**
	 * Closes the network connection to LabRAD.
	 */
	public void close() {
		close(new IOException("Connection closed."));
	}


	/**
	 * Closes the connection to LabRAD after an error.
	 * @param cause
	 */
	private synchronized void close(Throwable cause) {
		if (isConnected()) {
            // set our status as closed
            setConnected(false);

			// shutdown the lookup service
			executor.shutdown();

			// cancel all pending requests
            requestDispatcher.failAll(cause);

	    	// interrupt the writer thread
	    	writer.interrupt();
	    	try {
				writer.join();
			} catch (InterruptedException e) {}

			// interrupt the reader thread
	    	reader.interrupt();
			// this doesn't actually kill the thread, because it is blocked
	    	// on a stream read.  To kill the reader, we close the socket.
	    	try {
				socket.close();
			} catch (IOException e) {}
			try {
				reader.join();
			} catch (InterruptedException e) {}
		}
	}


    /** Low word of next context that will be created. */
	private long nextContext = 1L;
    private final Object contextLock = new Object();

	/**
	 * Create a new context for this connection.
	 * @return
	 */
	public Context newContext() {
    	synchronized (contextLock) {
    		return new Context(0, nextContext++);
    	}
    }


	/**
	 * Send a LabRAD request.
	 * @param request the request that will be made
	 * @return a Future that returns a list of Data when the request is done
	 */
	public Future<List<Data>> send(final Request request) {
	    return send(request, null);
    }

    /**
     * Send a request with an explicit callback.  When the request is
     * completed, the callback will be dispatched using the
     * EventQueue.invokeLater mechanism.
     * @param request the request that will be made
     * @param callback provides methods that will be called when done
     * @return a Future that returns a list of Data when the request is done
     */
    public Future<List<Data>> send(final Request request, final RequestCallback callback) {
        Future<List<Data>> result;
        lookupProvider.doLookupsFromCache(request);
		if (request.needsLookup()) {
	    	result = executor.submit(new Callable<List<Data>>() {
                public List<Data> call() throws Exception {
					lookupProvider.doLookups(request);
					return sendWithoutLookups(request, callback).get();
				}
	    	});
		} else {
			result = sendWithoutLookups(request, callback);
		}
        return result;
    }


	/**
     * Makes a LabRAD request synchronously.  The request is sent over LabRAD and the
     * calling thread will block until the result is available.
     * @param request the request that will be sent
     * @return a list of Data, one for each record in the request
     * @throws InterruptedException if the network thread was interrupted
     * @throws ExecutionException if the request returned an error or was canceled
     * @throws IOException if a network error occurred
     */
    public List<Data> sendAndWait(Request request)
    		throws InterruptedException, ExecutionException {
    	return send(request).get();
    }


    /**
     * Sends a LabRAD message to the specified server.  In this case,
     * lookups are done synchronously so that any exceptions will
     * be thrown to the caller immediately.
     * @param server
     * @param records
     */
    public void sendMessage(final Request request)
            throws InterruptedException, ExecutionException {
        lookupProvider.doLookups(request);
        sendMessageWithoutLookups(request);
    }


	/**
	 * Makes a LabRAD request asynchronously.  The request is sent over LabRAD and an
	 * object is returned that can be queried to see if the request has completed or
	 * to wait for completion.
	 * @param request the request that will be sent
	 * @param callback an optional callback to be invoked on completion
     * @throws RuntimeException if not connected or IDs not looked up
	 */
	private Future<List<Data>> sendWithoutLookups(
            final Request request, final RequestCallback callback) {
		if (!isConnected()) {
			throw new RuntimeException("Not connected.");
		}
		if (request.needsLookup()) {
			throw new RuntimeException("Server and/or setting IDs not looked up!");
		}
	    return requestDispatcher.startRequest(request, callback);
	}

	/**
     * Sends a LabRAD message without making any lookup requests.
     * @param request the request that will be made
     * @throws RuntimeException if not connected or IDs not looked up
     */
    private void sendMessageWithoutLookups(final Request request) {
        if (!isConnected()) {
    		throw new RuntimeException("Not connected.");
    	}
        if (request.needsLookup()) {
            throw new RuntimeException("Server and/or setting IDs not looked up!");
        }
        writeQueue.add(Packet.forMessage(request));
    }


	/**
     * Handle packets coming in from the wire.
     * @param packet
     */
    private void handlePacket(Packet packet) {
        int request = packet.getRequest();
        if (request < 0) {
        	// response
            requestDispatcher.finishRequest(packet);
        } else if (request == 0) {
        	// handle incoming message
            messageListeners.fireMessage(packet);
        } else {
        	// handle incoming request
            handleRequest(packet);
        }
    }



    // new stuff for servers...

    /**
     * add incoming requests to a queue to be server
     */
    private void handleRequest(Packet packet) {
        handlerQueue.add(packet);
    }
    
    /**
     * The main serve loop.  This function pulls requests from the incoming
     * queue and serves them.  Note that this function does not return unless
     * the server is interrupted.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void serve() throws InterruptedException, ExecutionException {
    	// initialize the server
    	server.init();
    	
        sendAndWait(Request.to("Manager").add("S: Start Serving"));
        System.out.println("Now serving...");
        
    	while (!Thread.interrupted()) {
    		Packet p = handlerQueue.poll(1, TimeUnit.SECONDS);
    		// TODO use interrupt to exit here, rather than polling timeout
    		if (p != null) {
    			serveRequest(p);
    		}
    		if (shouldShutdown || !isConnected()) {
    			break;
    		}
    	}
    	if (shouldShutdown) {
    		// finish serving pending requests
    		requestExecutor.shutdown();
    		// expire all contexts
    		for (ContextManager ctx : contexts.values()) {
    			ctx.expire();
    		}
    		// shutdown the server
    		server.shutdown();
    		// close the connection to LabRAD
    		close();
    		// notify anyone waiting for shutdown to complete
    		finishShutdown();
    	}
    }

    
    // shutdown handling
    
    private void finishShutdown() {
    	synchronized (shutdownStatus) {
    		shutdownFinished = true;
    		shutdownStatus.notifyAll();
    	}
    }
    
    Object shutdownStatus = new Object();
    boolean shouldShutdown = false;
    boolean shutdownFinished = false;
    
    public void triggerShutdown() {
    	shouldShutdown = true;
    	synchronized (shutdownStatus) {
	    	while (!shutdownFinished) {
	    		try {
	    			shutdownStatus.wait();
				} catch (InterruptedException e) {
					// if we get interrupted here, just quit
					break;
				}
	    	}
    	}
    }
    
    // request serving
    
    /**
     * Serve a single request.  If there is already a task running to
     * serve this context, we simply buffer this packet into that
     * tasks incoming buffer.  Otherwise, we spawn a new task and
     * give it to the executor.
     * @param packet
     */
    private void serveRequest(Packet packet) {
        Context context = packet.getContext();
        try {
	        getContextManager(context).serveRequest(packet);
        } catch (Exception ex) {
            Request response = Request.to(packet.getTarget(), context);
        	if (packet.getRecords().size() > 0) {
        		StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
        		response.add(packet.getRecords().get(0).getID(),
        				     Data.ofType("E").setError(0, sw.toString()));
        	}
            sendResponse(Packet.forRequest(response, -packet.getRequest()));
        }
    }
    
    public void sendResponse(Packet packet) {
    	writeQueue.add(packet);
    }
    
    /**
     * Map of contexts in which requests have been made and the managers for those contexts.
     */
    private final Map<Context, ContextManager> contexts = new HashMap<Context, ContextManager>();
    
    /**
     * Get a context manager for the given context.  If this is the first
     * time we have seen a particular context, a new manager will be created.
     * @param context
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private ContextManager getContextManager(Context context)
    		throws InstantiationException, IllegalAccessException {
    	ContextManager manager;
        if (!contexts.containsKey(context)) {
        	manager = ContextManager.create(this, serverClass, context);
        	contexts.put(context, manager);
        } else {
        	manager = contexts.get(context);
        }
        return manager;
    }
    
    /** Thread pool for serving requests. */
    private final ExecutorService requestExecutor = Executors.newFixedThreadPool(100);
    public void submit(Runnable task) {
    	requestExecutor.submit(task);
    }

    /**
     * The server that will be associated with this connection.
     * This provides global functionality accessible to any context.
     */
    private Server server;
    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }
    
    /**
     * The context server class associated with this connection.  A new instance
     * of this server class will be created for each context in which requests are made.
     */
    private Class<? extends ServerContext> serverClass;

    public Class<? extends ServerContext> getServerClass() { return serverClass; }
    public void setServerClass(Class<? extends ServerContext> serverClass) {
        this.serverClass = serverClass;
    }
    
    /**
     * Get the data required to log in to LabRAD as a server.
     * @return
     */
    private Data getLoginData() {
    	Class<?> cls = server.getClass();
    	if (!cls.isAnnotationPresent(ServerInfo.class)) {
    		String name = cls.getName();
    		String message = "Server class '" + name + "' needs @ServerInfo annotation.";
    		throw new RuntimeException(message);
    	}
        ServerInfo info = cls.getAnnotation(ServerInfo.class);
        String name = info.name();
        // interpolate environment vars
        Pattern p = Pattern.compile("%([^%]*)%");
        Matcher m = p.matcher(name);
        // find all environment vars in the string
        List<String> keys = new ArrayList<String>();
        while (m.find()) {
        	keys.add(m.group(1));
        }
        // substitute environment variable into string
        for (String key : keys) {
        	String val = Util.getEnv(key, null);
        	System.out.println(key + " => " + val);
        	if (val != null) {
        		name = name.replaceAll("%" + key + "%", val);
        	}
        }
        
        Data data = Data.ofType("wsss");
        data.get(0).setWord(Constants.PROTOCOL);
        data.get(1).setString(name);
        data.get(2).setString(info.description());
        data.get(3).setString(info.notes());
        return data;
    }

    /**
     * Map from setting IDs to methods on the ContextServer that handle
     * those settings.  This table is constructed after logging in but
     * before we begin serving.
     */
    private Map<Long, Method> dispatchTable = new HashMap<Long, Method>();
    public Method getHandler(final long ID) {
    	return dispatchTable.get(ID);
    }
    
    /**
     * Loop over all methods of the ContextServer, looking for those
     * marked as remotely-callable settings.  Send a registration packet
     * to the manager to register all these settings.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void registerSettings()
            throws InterruptedException, ExecutionException {
    	Request registrations = Request.to("Manager");
        for (Method m : serverClass.getMethods()) {
            if (m.isAnnotationPresent(Setting.class)) {
                Setting s = m.getAnnotation(Setting.class);
                if (dispatchTable.containsKey(s.ID())) {
                    throw new RuntimeException("Setting ID " + s.ID() + " is already in use.");
                }
                dispatchTable.put(s.ID(), m);
                Data data = Data.ofType("wss*s*ss");
                data.get(0).setWord(s.ID());
                data.get(1).setString(s.name());
                data.get(2).setString(s.description());
                data.get(3).setStringList(Arrays.asList(s.accepts()));
                data.get(4).setStringList(Arrays.asList(s.returns()));
                data.get(5).setString(s.notes());
                registrations.add("S: Register Setting", data);
            }
        }
        sendAndWait(registrations);
    }

    /**
     * Create a new server connection that will use a particular context server object.
     * @param server
     * @return
     */
    public static ServerConnection create(Server server, Class<? extends ServerContext> contextClass) {
        ServerConnection cxn = new ServerConnection();
        cxn.setServer(server);
        cxn.setServerClass(contextClass);
        return cxn;
    }
}
