package org.labrad;

import org.labrad.events.MessageListenerSupport;
import org.labrad.events.MessageListener;
import org.labrad.events.ConnectionListener;
import org.labrad.events.ConnectionListenerSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.PacketInputStream;
import org.labrad.data.PacketOutputStream;
import org.labrad.data.Record;
import org.labrad.data.Request;
import org.labrad.errors.IncorrectPasswordException;

public class Connection implements Serializable {
	/** Version for serialization. */
	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_NAME = "Java Client";
    
	// properties
	private String name;
    private String host;
    private int port;
    private String password;
    private long ID;
    private String loginMessage;
    private boolean connected = false;

    public String getName() { return name; }
    public void setName(String name) {
    	this.name = name;
    }
    
    /**
	 * @return the host
	 */
	public String getHost() { return host; }
    public void setHost(String host) {
    	this.host = host;
    }

	/**
	 * @return the port
	 */
	public int getPort() { return port; }
	public void setPort(int port) {
		this.port = port;
	}

	
	public void setPassword(String password) {
		this.password = password;
	}
	

	/**
	 * @return the iD
	 */
	public long getID() {
		return ID;
	}
	
	/**
	 * Get the welcome message returned by the manager when we connected.
	 * @return
	 */
	public String getLoginMessage() { return loginMessage; }
    
	public boolean isConnected() {
		return connected;
	}
	private void setConnected(boolean connected) {
        boolean old = this.connected;
        this.connected = connected;
        pcs.firePropertyChange("connected", old, connected);
        if (connected) {
            connectionListeners.fireConnected();
        } else {
            connectionListeners.fireDisconnected();
        }
	}

    // events
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final MessageListenerSupport mls = new MessageListenerSupport(this);
    private final ConnectionListenerSupport connectionListeners =
                      new ConnectionListenerSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void addMessageListener(MessageListener listener) {
        mls.addListener(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        mls.removeListener(listener);
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.addListener(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.removeListener(listener);
    }

	// networking stuff
    private Socket socket;
    private Thread reader, writer;
    private PacketInputStream inputStream;
    private PacketOutputStream outputStream;
    private BlockingQueue<Packet> writeQueue;
    
    /** Low word of next context that will be created. */
	private Long nextContext = 1L;
        
    /** Request IDs that are available to be reused. */
    private RequestDispatcher requestDispatcher;
    
    /** Performs server and method lookups. */
    private ExecutorService lookupService = Executors.newCachedThreadPool();
    
    /** Maps server names to IDs. */
    private ConcurrentMap<String, Long> serverCache = new ConcurrentHashMap<String, Long>();
    
    /** Maps server IDs to a map from setting names to IDs. */
    private ConcurrentMap<Long, ConcurrentMap<String, Long>> settingCache =
    	new ConcurrentHashMap<Long, ConcurrentMap<String, Long>>();
    
    /**
     * Create a new connection object.
     */
    public Connection() {
    	setName(DEFAULT_NAME);
    	// set defaults from the environment
    	setHost(Util.getEnv("LABRADHOST", Constants.DEFAULT_HOST));
        setPort(Util.getEnvInt("LABRADPORT", Constants.DEFAULT_PORT));
    	setPassword(Util.getEnv("LABRADPASSWORD", Constants.DEFAULT_PASSWORD));
    	// always start in the disconnected state
    	setConnected(false);
    }
    
    /**
     * Connect to the LabRAD manager.
     * @throws UnknownHostException
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws IncorrectPasswordException
     */
    public void connect()
    		throws UnknownHostException, IOException, ExecutionException,
				   InterruptedException, IncorrectPasswordException {
        // TODO: clean up exceptions thrown here
        // TODO: try/catch around login with close called on an exception
	    socket = new Socket(host, port);
	    socket.setTcpNoDelay(true);
	    socket.setKeepAlive(true);
	    inputStream = new PacketInputStream(socket.getInputStream());
	    outputStream = new PacketOutputStream(socket.getOutputStream());
	
	    writeQueue = new LinkedBlockingQueue<Packet>();
        requestDispatcher = new RequestDispatcher(writeQueue);
	
	    reader = new Thread(new Runnable() {
            @Override public void run() {
	            try {
	                while (!Thread.interrupted())
	                    handlePacket(inputStream.readPacket());
	            } catch (IOException e) {
	            	// let the client know that we have disconnected.
	            	if (!Thread.interrupted())
	            		close(e);
	            }
	        }
	    }, "Packet Reader Thread");
	    
	    writer = new Thread(new Runnable() {
	    	@Override public void run() {
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
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws NoSuchAlgorithmException
	 * @throws IncorrectPasswordException 
	 * @throws IOException 
	 */
	private void doLogin(String password)
			throws InterruptedException, ExecutionException,
			IncorrectPasswordException, IOException {
		long mgr = Constants.MANAGER;
		Data data, response;
	    
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
	    } catch (ExecutionException e) {
	    	throw new IncorrectPasswordException();
	    }
	    
	    // print welcome message
	    loginMessage = response.getString();
	    System.out.println(loginMessage);
	
	    // send identification packet
	    data = new Data("ws").setWord(Constants.PROTOCOL, 0).setString(name, 1);
	    response = sendAndWait(new Request(mgr).add(0, data)).get(0);
	    ID = response.getWord();
	}
	

	/**
	 * Closes the connection to LabRAD after an error.
	 * @param cause
	 */
	private synchronized void close(Throwable cause) {
		if (connected) {
			// shutdown the lookup service
			lookupService.shutdownNow();
			
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
			
			connected = false;
		}
	}


	/**
	 * Closes the network connection to LabRAD.
	 */
	public void close() {
		close(new IOException("Connection closed."));
	}
	
	
	/**
	 * Create a new context for this connection.
	 * @return
	 */
	public Context newContext() {
    	synchronized (nextContext) {
    		return new Context(0, nextContext++);
    	}
    }

	
	// Message functions
    
    /**
     * Sends a LabRAD message to the specified server.
     * @param server
     * @param records
     */
    public synchronized void sendMessage(Request request) {
    	// TODO: do lookups before sending a message
    	if (!connected) {
    		throw new RuntimeException("not connected!");
    	}
    	writeQueue.add(Packet.forMessage(request));
    }
	
	
    // Request functions
    
	/**
	 * 
	 * @param server
	 * @param records
	 * @return
	 * @throws IOException
	 */
	public Future<List<Data>> send(final Request request)
			throws IOException {
	    return send(request, null);
    }

    /**
     * Send a request with an explicit callback.
     * @param request
     * @param callback
     * @return
     * @throws java.io.IOException
     */
    public Future<List<Data>> send(final Request request, final RequestCallback callback)
            throws IOException {
        doLookupsFromCache(request);
		if (request.needsLookup()) {
	    	return lookupService.submit(new Callable<List<Data>>() {
				@Override
				public List<Data> call() throws Exception {
					doLookups(request);
					return sendWithoutLookups(request, callback).get();
				}
	    	});
		} else {
			return sendWithoutLookups(request, callback);
		}
    }
	
	/**
     * Makes a LabRAD request synchronously.  The request is sent over LabRAD and the
     * calling thread will block until the result is available.
     * @param server
     * @param records
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws IOException 
     */
    public List<Data> sendAndWait(Request request)
    		throws InterruptedException, ExecutionException, IOException {
    	return send(request).get();
    }
	
    
	/**
	 * Makes a LabRAD request asynchronously.  The request is sent over LabRAD and an
	 * object is returned that can be queried to see if the request has completed or
	 * to wait for completion.
	 * @param server
	 * @param records
	 */
	private synchronized Future<List<Data>>
            sendWithoutLookups(final Request request,
                               final RequestCallback callback)
                throws IOException {
		if (!isConnected()) {
			throw new IOException("not connected.");
		}
		if (request.needsLookup()) {
			throw new RuntimeException("Server and/or setting IDs not looked up!");
		}
	    return requestDispatcher.startRequest(request);
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
            mls.fireMessage(packet);
        } else {
        	// handle incoming request
        }
    }
	
	
	// functions used by the lookup service
	
	/**
	 * Attempt to do necessary server/setting lookups from the local cache only.
	 * @param request
	 */
	private void doLookupsFromCache(Request request) {
		// lookup server ID
		if (request.needsServerLookup()) {
			Long serverID = serverCache.get(request.getServerID());
			if (serverID != null) {
				request.setServerID(serverID);
			}
		}
		// lookup setting IDs if server ID lookup succeeded
		if (!request.needsServerLookup() && request.needsSettingLookup()) {
			ConcurrentMap<String, Long> cache = settingCache.get(request.getServerID());
			if (cache != null) {
				for (Record r : request.getRecords()) {
					if (r.needsLookup()) {
						Long settingID = cache.get(r.getName());
						if (settingID != null) {
							r.setID(settingID);
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Do necessary server/setting lookups, making requests to the manager as necessary.
	 * @param request
	 * @throws IOException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void doLookups(Request request)
			throws InterruptedException, ExecutionException, IOException {
		// lookup server ID
		if (request.needsServerLookup()) {
			Long serverID = serverCache.get(request.getServerID());
			if (serverID == null) {
				serverID = lookupServer(request.getServerName());
			}
			request.setServerID(serverID);
		}
		// lookup setting IDs
		if (request.needsSettingLookup()) {
			List<Record> lookups = new ArrayList<Record>();
			ConcurrentMap<String, Long> cache = settingCache.get(request.getServerID());
			if (cache != null) {
				for (Record r : request.getRecords()) {
					if (r.needsLookup()) {
						Long settingID = cache.get(r.getName());
						if (settingID != null) {
							r.setID(settingID);
						} else {
							lookups.add(r);
						}
					}
				}
			}
			if (lookups.size() > 0) {
				List<String> names = new ArrayList<String>();
				for (Record r : lookups) {
					names.add(r.getName());
				}
				List<Long> IDs = lookupSettings(request.getServerID(), names);
				for (int i = 0; i < lookups.size(); i++) {
					lookups.get(i).setID(IDs.get(i));
				}
			}
		}
	}
	
	
    /**
     * Lookup the ID of a server, pulling from the cache if we already know it.
     * The looked up ID is stored in the local cache for future use.
     * @param server
     * @return
     * @throws IOException
     * @throws ExecutionException 
     * @throws InterruptedException
     */
    private long lookupServer(String server)
    		throws IOException, InterruptedException, ExecutionException {
	    Request request = new Request(Constants.MANAGER);
	    request.add(Constants.LOOKUP, new Data("s").setString(server));
    	long serverID = sendAndWait(request).get(0).getWord();
        serverCache.putIfAbsent(server, serverID);
        settingCache.putIfAbsent(serverID, new ConcurrentHashMap<String, Long>());
    	return serverID;
    }
    
    
    /**
     * Lookup IDs for a list of settings on the specified server.  All the setting
     * IDs are stored in the local cache for future use.
     * @param serverID
     * @param settings
     * @return
     * @throws IOException
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    private List<Long> lookupSettings(long serverID, List<String> settings)
            throws IOException, InterruptedException, ExecutionException {
    	// TODO: may need to do an s*s lookup if cache has been invalidated in the meantime.
    	// TODO: maybe need to implement the cache as an opaque object.
    	Data data = new Data("w*s");
    	data.get(0).setWord(serverID);
    	data.get(1).setStringList(settings);
    	Request request = new Request(Constants.MANAGER);
    	request.add(Constants.LOOKUP, data);
    	ConcurrentMap<String, Long> cache = settingCache.get(serverID);
    	Data response = sendAndWait(request).get(0);
    	List<Long> result = response.get(1).getWordList();
    	// cache all the lookup results
    	for (int i = 0; i < settings.size(); i++) {
    		cache.put(settings.get(i), result.get(i));
    	}
    	return result;
    }
    
    
    /**
     * Tests some of the basic functionality of the client connection.
     * This method requires that the "Python Test Server" be running
     * to complete all of its tests successfully.
     * @param args
     * @throws IOException
     * @throws IncorrectPasswordException
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     */
    public static void main(String[] args)
    		throws IOException, IncorrectPasswordException, ExecutionException, InterruptedException,
    		       NoSuchAlgorithmException {
        Data response;
        long start, end;
        int nRandomData = 100;
        int nPings = 1000;
        
        String server = "Python Test Server";
        String setting = "Get Random Data";
        
        List<Future<List<Data>>> requests = new ArrayList<Future<List<Data>>>();
        
        // connect to LabRAD
        Connection c = new Connection();
        c.setHost("localhost");
        c.setPort(7682);
        c.setPassword("martinisgroup");
        c.connect();
                
        // set delay to 1 second
        c.sendAndWait(new Request(server).add("Echo Delay", new Data("v[s]").setValue(1.0)));
        
        // echo with delays
        System.out.println("echo with delays...");
        start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
        	requests.add(c.send(new Request(server).add("Delayed Echo", new Data("w").setWord(4))));
        }
        for (Future<List<Data>> request : requests) {
        	request.get();
        	System.out.println("Got one!");
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        requests.clear();
        
        // random data
        System.out.println("getting random data, with printing...");
        start = System.currentTimeMillis();
        for (int i = 0; i < nRandomData; i++) {
        	requests.add(c.send(new Request(server).add(setting)));
        }
        for (Future<List<Data>> request : requests) {
            response = request.get().get(0);
            System.out.println("got packet: " + response.pretty());
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        requests.clear();
        
        // random data
        System.out.println("getting random data, make pretty, but don't print...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < nRandomData; i++) {
            requests.add(c.send(new Request(server).add(setting)));
        }
        for (Future<List<Data>> request : requests) {
        	request.get().get(0).pretty();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        // random data
        System.out.println("getting random data, no printing...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < nRandomData; i++) {
        	requests.add(c.send(new Request(server).add(setting)));
        }
        for (Future<List<Data>> request : requests) {
        	request.get();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");

        // debug
        start = System.currentTimeMillis();
        response = c.sendAndWait(new Request(server).add("debug")).get(0);
        System.out.println("Debug output: " + response.pretty());
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        // ping manager
        System.out.println("pinging manager " + nPings + " times...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < nPings; i++) {
        	requests.add(c.send(new Request("Manager")));
        }
        for (Future<List<Data>> request : requests) {
        	request.get();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        c.close();
    }
}
