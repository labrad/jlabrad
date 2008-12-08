package org.labrad;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.PacketInputStream;
import org.labrad.data.PacketOutputStream;
import org.labrad.data.Record;
import org.labrad.data.Request;
import org.labrad.errors.IncorrectPasswordException;
import org.labrad.errors.LabradException;

public class Client {
	private static final String NAME = "Java Client";
    
    private String host;
    private int port;
    private long ID;
    private String loginMessage;
    private boolean connected = false;

    private Socket socket;
    private Thread reader, writer;
    private PacketInputStream inputStream;
    private PacketOutputStream outputStream;
    private BlockingQueue<Packet> writeQueue;
    
    /** Low word of next context that will be created. */
	private Long nextContext = 1L;
    
    /** ID to be used for the next outgoing request. */
    private int nextRequest = 1;
    
    /** Request IDs that are available to be reused. */
    private List<Integer> requestPool = new ArrayList<Integer>();
    
    /** Maps request numbers to receivers for all pending requests. */
    private Map<Integer, RequestReceiver> pendingRequests =
    	new HashMap<Integer, RequestReceiver>();

    /** Performs server and method lookups. */
    private ExecutorService lookupService = Executors.newCachedThreadPool();
    
    /** Maps server names to IDs. */
    private ConcurrentMap<String, Long> serverCache = new ConcurrentHashMap<String, Long>();
    
    /** Maps server IDs to a map from setting names to IDs. */
    private ConcurrentMap<Long, ConcurrentMap<String, Long>> settingCache =
    	new ConcurrentHashMap<Long, ConcurrentMap<String, Long>>();
    
    private enum RequestStatus { PENDING, DONE, FAILED, CANCELLED; }
    
    /**
     * Represents a pending LabRAD request.
     */
    private class RequestReceiver implements Future<List<Data>> {
    	private RequestStatus status = RequestStatus.PENDING;
    	private List<Data> response;
    	private Throwable cause;
    	
    	/**
    	 * Cancel this request.
    	 * @return true if the request was cancelled
    	 */
		@Override
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			boolean cancelled = false;
			if (status == RequestStatus.PENDING) {
				status = RequestStatus.CANCELLED;
				if (mayInterruptIfRunning) {
					notifyAll();
				}
				cancelled = true;
			}
			return cancelled;
		}

		@Override
		public synchronized List<Data> get() throws InterruptedException, ExecutionException {
			while (!isDone()) {
				wait();
			}
			switch (status) {
				case CANCELLED: throw new CancellationException();
				case FAILED: throw new ExecutionException(cause);
				default:
			}
			return response;
		}

		@Override
		public synchronized List<Data> get(long duration, TimeUnit timeUnit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			while (!isDone()) {
				wait(TimeUnit.MILLISECONDS.convert(duration, timeUnit));
			}
			switch (status) {
				case CANCELLED: throw new CancellationException();
				case FAILED: throw new ExecutionException(cause);
				default:
			}
			return response;
		}

		@Override
		public synchronized boolean isCancelled() {
			return status == RequestStatus.CANCELLED;
		}

		@Override
		public synchronized boolean isDone() {
			return status != RequestStatus.PENDING;
		}
		
		protected synchronized void set(Packet packet) {
			if (!isCancelled()) {
				boolean failed = false;
				List<Data> response = new ArrayList<Data>();
				for (Record rec : packet.getRecords()) {
					Data data = rec.getData();
					if (data.isError()) {
						failed = true;
						this.cause = new LabradException(data);
						break;
					} else {
						response.add(data);
					}
				}
				this.response = response;
				status = failed ? RequestStatus.FAILED : RequestStatus.DONE;
			}
			notifyAll();
		}
		
		protected synchronized void fail(Throwable cause) {
			this.cause = cause;
			status = RequestStatus.FAILED;
			notifyAll();
		}
    }

    /**
     * Thread that writes queued packets to the output stream.
     * @author maffoo
     *
     */
    class Writer extends Thread {
        Writer() { super("Packet Writer thread"); }
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
            }
        }
    }

    /**
     * Thread that reads packets coming in on an input stream.
     * @author maffoo
     *
     */
    class Reader extends Thread {
        Reader() { super("Packet Reader thread"); }
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    handleResponse(inputStream.readPacket());
                }
            } catch (IOException e) {
            	// let the client know that we have disconnected.
            	if (!Thread.interrupted()) {
            		close(e);
            	}
            }
        }
    }
    
    
    /**
     * Create a new client connection to a LabRAD manager at the given host and port.
     * @param host
     * @param port
     * @param password
     * @throws UnknownHostException
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws IncorrectPasswordException
     */
    Client(String host, int port, String password)
			throws UnknownHostException, IOException, ExecutionException,
			InterruptedException, IncorrectPasswordException {
    	// TODO: create clients using a builder to allow for fallback to environment defaults.
	    this.host = host;
	    this.port = port;
	
	    socket = new Socket(host, port);
	    socket.setTcpNoDelay(true);
	    socket.setKeepAlive(true);
	    inputStream = new PacketInputStream(socket.getInputStream());
	    outputStream = new PacketOutputStream(socket.getOutputStream());
	
	    writeQueue = new LinkedBlockingQueue<Packet>();
	
	    reader = new Reader();
	    writer = new Writer();
	    reader.start();
	    writer.start();
	    
	    connected = true;
	    doLogin(password);
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
		Data data, response;
	    
	    // send first ping packet
	    response = sendAndWait(new Request(Constants.MANAGER)).get(0);
	    
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
	    data = new Data("s").setBytes(md.digest());
	    try {
	    	response = sendAndWait(new Request(Constants.MANAGER).add(0, data)).get(0);
	    } catch (ExecutionException e) {
	    	throw new IncorrectPasswordException();
	    }
	    
	    // print welcome message
	    loginMessage = response.getString();
	    System.out.println(loginMessage);
	
	    // send identification packet
	    data = new Data("ws").setWord(Constants.PROTOCOL, 0).setString(NAME, 1);
	    response = sendAndWait(new Request(Constants.MANAGER).add(0, data)).get(0);
	    ID = response.getWord();
	}


	/**
	 * @return the host
	 */
	public String getHost() { return host; }


	/**
	 * @return the port
	 */
	public int getPort() { return port; }


	/**
	 * @return the iD
	 */
	public long getID() { return ID; }

	/**
	 * Get the welcome message returned by the manager when we connected.
	 * @return
	 */
	public String getLoginMessage() { return loginMessage; }
	

	/**
	 * Closes the connection to LabRAD after an error.
	 * @param cause
	 */
	private synchronized void close(Throwable cause) {
		if (connected) {
			// shutdown the lookup service
			lookupService.shutdownNow();
			
			// cancel all pending requests
			for (RequestReceiver receiver : pendingRequests.values()) {
				receiver.fail(cause);
			}
	
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


	/**
	 * 
	 * @param server
	 * @param records
	 * @return
	 * @throws IOException
	 */
	public Future<List<Data>> send(final Request request)
			throws IOException {
	    doLookupsFromCache(request);
		if (request.needsLookup()) {
	    	return lookupService.submit(new Callable<List<Data>>() {
				@Override
				public List<Data> call() throws Exception {
					doLookups(request);
					return sendWithoutLookups(request).get();
				}
	    	});
		} else {
			return sendWithoutLookups(request);
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
	private synchronized Future<List<Data>> sendWithoutLookups(final Request request)
			throws IOException {
		if (!connected) {
			throw new IOException("not connected.");
		}
		if (request.needsLookup()) {
			throw new RuntimeException("Server and/or setting IDs not looked up!");
		}
	    int requestNum;
	    if (requestPool.isEmpty()) {
	    	requestNum = nextRequest++;
	    } else {
	    	requestNum = requestPool.remove(0);
	    }
	    RequestReceiver receiver = new RequestReceiver();
	    pendingRequests.put(requestNum, receiver);
	    writeQueue.add(Packet.forRequest(request, requestNum));
	    return receiver;
	}

	
	/**
     * Handle response packets coming in from the wire.
     * @param packet
     */
    private synchronized void handleResponse(Packet packet) {
        int request = -packet.getRequest();
        if (request == 0) {
        	// handle incoming message
        } else if (pendingRequests.containsKey(request)) {
        	requestPool.add(request);
        	pendingRequests.remove(request).set(packet);
        } else {
        	// response to a request we didn't make
        	// TODO log this as an error
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
						Long ID = cache.get(r.getName());
						if (ID != null) {
							r.setID(ID);
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
						Long ID = cache.get(r.getName());
						if (ID != null) {
							r.setID(ID);
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
    	long ID = sendAndWait(request).get(0).getWord();
        serverCache.putIfAbsent(server, ID);
        settingCache.putIfAbsent(ID, new ConcurrentHashMap<String, Long>());
    	return ID;
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
    	Data data = new Data("w*s");
    	data.setWord(serverID, 0);
    	data.setStringList(settings, 1);
    	//data.setArraySize(settings.size(), 1);
    	//for (int i = 0; i < settings.size(); i++) {
    	//    data.setString(settings.get(i), 1, i);
    	//}
    	Request request = new Request(Constants.MANAGER);
    	request.add(Constants.LOOKUP, data);
    	ConcurrentMap<String, Long> cache = settingCache.get(serverID);
    	Data response = sendAndWait(request).get(0);
    	List<Long> result = response.getWordList(1);
    	// cache all the lookup results
    	for (int i = 0; i < settings.size(); i++) {
    		cache.put(settings.get(i), result.get(i));
    	}
    	return result;
    }
    
    
    // Message functions
    
    /**
     * Sends a LabRAD message to the specified server.
     * @param server
     * @param records
     */
    public synchronized void sendMessage(Request request) {
    	if (!connected) {
    		throw new RuntimeException("not connected!");
    	}
    	writeQueue.add(Packet.forMessage(request));
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
        
        String server = "Python Test Server";
        String setting = "Get Random Data";
        String password = "martinisgroup";
        
        List<Future<List<Data>>> requests = new ArrayList<Future<List<Data>>>();
        
        // connect to LabRAD
        Client c = new Client("localhost", 7682, password);
                
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
        for (int i = 0; i < 1000; i++) {
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
        for (int i = 0; i < 1000; i++) {
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
        for (int i = 0; i < 1000; i++) {
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
        System.out.println("pinging manager 10000 times...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
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
