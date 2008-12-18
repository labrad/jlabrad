/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.PacketInputStream;
import org.labrad.data.PacketOutputStream;
import org.labrad.data.Record;
import org.labrad.data.Request;
import org.labrad.errors.IncorrectPasswordException;
import org.labrad.errors.LabradException;
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
public class ServerConnection<T> implements Connection {
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
            @Override
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
            @Override
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
		long mgr = Constants.MANAGER;
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
                @Override
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
    @Override
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

    public void serve() throws InterruptedException, ExecutionException {
        sendAndWait(Request.to("Manager").add("S: Start Serving"));
        System.out.println("Now serving...");
        
        while (!Thread.interrupted()) {
            Packet p = handlerQueue.poll(1, TimeUnit.SECONDS);
            if (p != null) serveRequest(p);
        }
    }

    private Map<Context, ContextServer> contexts = new HashMap<Context, ContextServer>();

    private void handleRequest(Packet packet) {
        handlerQueue.add(packet);
    }

    private void serveRequest(Packet packet) {
        Context context = packet.getContext();
        long source = packet.getTarget();
        Request response = Request.to(source, context);
        try {
            ContextServer server;
	        if (!contexts.containsKey(context)) {
                server = serverClass.newInstance();
                server.setContext(context);
                server.setConnection(this);
	            contexts.put(context, server);
	        } else {
                server = contexts.get(context);
            }
	        server.setSource(source);
	        for (Record rec : packet.getRecords()) {
	            Method m = dispatchTable.get(rec.getID());
	            Data respData;
	            try {
	                respData = (Data) m.invoke(server, rec.getData());
	            } catch (LabradException ex) {
	                respData = Data.ofType("E").setError(ex.getCode(), ex.getMessage());
	            } catch (Exception ex) {
	                StringWriter sw = new StringWriter();
	                ex.printStackTrace(new PrintWriter(sw));
	                respData = Data.ofType("E").setError(0, sw.toString());
	            }
	            response.add(rec.getID(), respData);
	            if (respData.isError()) break;
	        }
        } catch (Exception ex) {
        	if (packet.getRecords().size() > 0) {
        		StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
        		response.add(packet.getRecords().get(0).getID(),
        				     Data.ofType("E").setError(0, sw.toString()));
        	}
        }
        writeQueue.add(Packet.forRequest(response, -packet.getRequest()));
    }


    private Class<? extends ContextServer> serverClass;

    public Class<? extends ContextServer> getServerClass() { return serverClass; }
    public void setServerClass(Class<? extends ContextServer> serverClass) {
        this.serverClass = serverClass;
    }

    private Data getLoginData() {
        ServerInfo info = serverClass.getAnnotation(ServerInfo.class);
        Data data = Data.ofType("wsss");
        data.get(0).setWord(Constants.PROTOCOL);
        data.get(1).setString(info.name()); // TODO: allow environ vars in name
        data.get(2).setString(info.description());
        data.get(3).setString(info.notes());
        return data;
    }

    private Map<Long, Method> dispatchTable = new HashMap<Long, Method>();

    private void registerSettings()
            throws InterruptedException, ExecutionException {
        for (Method m : serverClass.getMethods()) {
            if (m.isAnnotationPresent(Setting.class)) {
                //System.out.println(m.getName() + " is remotely callable.");
                Setting s = m.getAnnotation(Setting.class);
                if (dispatchTable.containsKey(s.ID())) {
                    throw new RuntimeException("ID " + s.ID() + " already in use.");
                }
                dispatchTable.put(s.ID(), m);
                Data data = Data.ofType("wss*s*ss");
                data.get(0).setWord(s.ID());
                data.get(1).setString(s.name());
                data.get(2).setString(s.description());
                data.get(3).setStringList(Arrays.asList(s.accepts()));
                data.get(4).setStringList(Arrays.asList(s.returns()));
                data.get(5).setString(s.notes());
                //System.out.println(data.pretty());
                sendAndWait(Request.to("Manager").add("S: Register Setting", data));
            }
        }
    }


    public static <T> ServerConnection<T> create(Class<? extends ContextServer> server) {
        ServerConnection<T> cxn = new ServerConnection<T>();
        cxn.setServerClass(server);
        return cxn;
    }
}
