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
import org.labrad.errors.IncorrectPasswordException;
import org.labrad.errors.LabradException;

public class Client {
	private static final String NAME = "Java Client";
	
	private Socket socket;
    private Thread reader, writer;
    private PacketInputStream inputStream;
    private PacketOutputStream outputStream;
    private BlockingQueue<Packet> writeQueue;
    
    private String host;
    private int port;
    private long ID;
    private boolean connected = false;

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
    private class RequestReceiver implements Future<Packet> {
    	private RequestStatus status = RequestStatus.PENDING;
    	private Packet response;
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
		public synchronized Packet get() throws InterruptedException, ExecutionException {
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
		public synchronized Packet get(long duration, TimeUnit timeUnit)
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
		
		protected synchronized void set(Packet response) {
			if (!isCancelled()) {
				boolean failed = false;
				for (Record rec : response.getRecords()) {
					Data data = rec.getData();
					if (data.isError()) {
						failed = true;
						this.cause = new LabradException(data);
						break;
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
    
    
    Client(String host, int port, String password)
			throws UnknownHostException, IOException, ExecutionException,
			InterruptedException, IncorrectPasswordException {
	    this.host = host;
	    this.port = port;
	
	    socket = new Socket(host, port);
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
	    response = sendRequestAndWait(Constants.MANAGER).getRecord(0).getData();
	    
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
	    	response = sendRequestAndWait(Constants.MANAGER, new Record(0, data)).getRecord(0).getData();
	    } catch (ExecutionException e) {
	    	throw new IncorrectPasswordException();
	    }
	    
	    // print welcome message
	    System.out.println(response.getString());
	
	    // send identification packet
	    data = new Data("ws").setWord(Constants.PROTOCOL, 0).setString(NAME, 1);
	    response = sendRequestAndWait(Constants.MANAGER, new Record(0, data)).getRecord(0).getData();
	    ID = response.getWord();
	}


	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}


	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}


	/**
	 * @return the iD
	 */
	public long getID() {
		return ID;
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
     * Makes a LabRAD request asynchronously.  The request is sent over LabRAD and an
     * object is returned that can be queried to see if the request has completed or
     * to wait for completion.
     * @param server
     * @param records
     */
    private synchronized Future<Packet> sendRequestNoLookup(long server, Record... records)
    		throws IOException {
    	if (!connected) {
    		throw new IOException("not connected.");
    	}
        int requestNum;
        if (requestPool.isEmpty()) {
        	requestNum = nextRequest++;
        } else {
        	requestNum = requestPool.remove(0);
        }
        RequestReceiver receiver = new RequestReceiver();
        pendingRequests.put(requestNum, receiver);
        writeQueue.add(new Packet(new Context(0, 1), server, requestNum, records));
        return receiver;
    }
    
    
    /**
	 * Makes a LabRAD request asynchronously.  The request is sent over LabRAD and an
	 * object is returned that can be queried to see if the request has completed or
	 * to wait for completion.
	 * @param server
	 * @param records
	 */
	public Future<Packet> sendRequest(final long server, final Record... records)
			throws IOException {
		boolean needsLookup = needsLookup(server, records);
		Future<Packet> result;
		if (needsLookup) {
			result = lookupService.submit(new Callable<Packet>() {
				@Override
				public Packet call() throws Exception {
					return sendRequestWithLookup(server, records);
				}
			});
		} else {
			result = sendRequestNoLookup(server, records);
		}
		return result;
	}


	/**
	 * 
	 * @param server
	 * @param records
	 * @return
	 * @throws IOException
	 */
	public Future<Packet> sendRequest(final String server, final Record... records)
			throws IOException {
		boolean needsLookup = needsLookup(server, records);
		Future<Packet> result;
		if (needsLookup) {
	    	result = lookupService.submit(new Callable<Packet>() {
				@Override
				public Packet call() throws Exception {
				    return sendRequestWithLookup(server, records);
				}
	    	});
		} else {
			result = sendRequestNoLookup(serverCache.get(server), records);
		}
		return result;
    }
    
    
    // functions used by the lookup service

	/**
	 * Checks whether the server and settings IDs can be pulled from cache,
	 * or need to be looked up.
	 */
	private boolean needsLookup(String server, Record[] records) {
		Long serverID = serverCache.get(server);
		if (serverID == null) {
			return true;
		} else {
			return needsLookup(serverID, records);
		}
	}
	
	/**
	 * Check whether the setting IDs can be pulled from cache,
	 * or need to be looked up.
	 */
	private boolean needsLookup(long server, Record[] records) {
		boolean needsLookup = false;
		for (int i = 0; i < records.length; i++) {
			Record r = records[i];
			if (r.needsLookup()) {
				ConcurrentMap<String, Long> cache = settingCache.get(server);
				if (cache == null) {
					needsLookup = true;
				} else {
					Long ID = cache.get(r.getName());
					if (ID == null) {
						needsLookup = true;
					} else {
						records[i] = new Record(ID, r.getData());
					}
				}
			}
		}
		return needsLookup;
	}
	
	/**
	 * Sends a request after looking up the server ID.
	 */
	private Packet sendRequestWithLookup(String server, Record[] records)
			throws InterruptedException, ExecutionException, IOException {
		// lookup server ID
		long serverID = lookupServer(server);
		return sendRequestWithLookup(serverID, records);
	}
		
	/**
	 * Sends a request after looking up setting IDs
	 * @param server
	 * @param records
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private Packet sendRequestWithLookup(long server, Record[] records)
			throws IOException, InterruptedException, ExecutionException {
	    List<Integer> indices = new ArrayList<Integer>();
	    List<String> strings = new ArrayList<String>();
	    
		for (int i = 0; i < records.length; i++) {
			Record r = records[i];
			if (r.needsLookup()) {
				indices.add(i);
				strings.add(r.getName());
			}
		}
		
		// lookup settings and cache them
		lookupSettings(server, strings);
		
		Record[] newRecs = new Record[records.length];
		for (int i = 0; i < records.length; i++) {
			Record r = records[i];
			if (r.needsLookup()) {
				long settingID = settingCache.get(server).get(r.getName());
				newRecs[i] = new Record(settingID, r.getData());
			} else {
				newRecs[i] = r;
			}
		}
		
		return sendRequestAndWait(server, newRecs);
	}
	
    /**
     * Lookup the ID of a server, pulling from the cache if we already know it.
     * @param server
     * @return
     * @throws IOException
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    private long lookupServer(String server)
    		throws InterruptedException, ExecutionException, IOException {
    	Long ID = serverCache.get(server);
    	if (ID == null) {
	        Data response = sendRequestAndWait(Constants.MANAGER,
	        		new Record(Constants.LOOKUP, new Data("s").setString(server))).getRecord(0).getData();
	        ID = response.getWord();
	        serverCache.putIfAbsent(server, ID);
	        settingCache.putIfAbsent(ID, new ConcurrentHashMap<String, Long>());
    	}
        return ID;
    }
    
    
    /**
     * Lookup IDs for a list of settings on the specified server.
     * @param serverID
     * @param settings
     * @return
     * @throws IOException
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    private long[] lookupSettings(long serverID, List<String> settings)
            throws IOException, InterruptedException, ExecutionException {
        long[] IDs = new long[settings.size()];
        int[] indices = new int[settings.size()];
        String[] lookups = new String[settings.size()];
        int nLookups = 0;

        ConcurrentMap<String, Long> cache = settingCache.get(serverID);
        if (cache == null) {
            cache = settingCache.putIfAbsent(serverID, new ConcurrentHashMap<String, Long>());
        }

        for (int i = 0; i < settings.size(); i++) {
            String key = settings.get(i);
            Long ID = cache.get(key);
            if (ID != null) {
                IDs[i] = ID;
            } else {
                lookups[nLookups] = key;
                indices[nLookups] = i;
                nLookups++;
            }
        }

        if (nLookups > 0) {
	        Data data = new Data("w*s");
	        data.setWord(serverID, 0);
	        data.setArraySize(nLookups, 1);
	        for (int i = 0; i < nLookups; i++) {
	            data.setString(lookups[i], 1, i);
	        }
	        Data response = sendRequestAndWait(Constants.MANAGER, new Record(Constants.LOOKUP, data)).getRecord(0).getData();
	        for (int i = 0; i < nLookups; i++) {
	            long ID = response.getWord(1, i);
	            cache.put(lookups[i], ID);
	            IDs[indices[i]] = ID;
	        }
        }
        return IDs;
    }
    
    
    
    // Message functions
    
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
    public Packet sendRequestAndWait(long server, Record... records)
    		throws InterruptedException, ExecutionException, IOException {
    	return sendRequest(server, records).get();
    }
    
    public Packet sendRequestAndWait(String server, Record... records)
    		throws InterruptedException, ExecutionException, IOException {
    	return sendRequest(server, records).get();
	}
    
    /**
     * Sends a LabRAD message to the specified server.
     * @param server
     * @param records
     */
    public synchronized void sendMessage(long server, Record... records) {
    	if (!connected) {
    		throw new RuntimeException("not connected!");
    	}
    	Context context = new Context(0, 1);
    	writeQueue.add(new Packet(context, server, 0, records));
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
        Data response, data;
        long start, end;
        
        String server = "Python Test Server";
        String setting = "Get Random Data";
        String password = "martinisgroup";
        
        List<Future<Packet>> requests = new ArrayList<Future<Packet>>();
        
        // connect to LabRAD
        Client c = new Client("localhost", 7682, password);
                
        // lookup hydrant server
        //ServerProxy hydrant = c.getServer(server);
        
        // set delay to 1 second
        c.sendRequestAndWait(server, new Record("Echo Delay", new Data("v[s]").setValue(1.0)));
        
        // echo with delays
        System.out.println("echo with delays...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
        	requests.add(c.sendRequest(server, new Record("Delayed Echo", new Data("w").setWord(4))));
        }
        for (Future<Packet> request : requests) {
        	request.get();
        	System.out.println("Got one!");
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        // random hydrant data
        System.out.println("getting random data, with printing...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
        	requests.add(c.sendRequest(server, new Record(setting)));
        }
        for (Future<Packet> request : requests) {
            response = request.get().getRecord(0).getData();
            System.out.println("got packet: " + response.pretty());
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        // random hydrant data
        System.out.println("getting random data, make pretty, but don't print...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            requests.add(c.sendRequest(server, new Record(setting)));
        }
        for (Future<Packet> request : requests) {
        	request.get().getRecord(0).getData().pretty();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        // random hydrant data
        System.out.println("getting random data, no printing...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
        	requests.add(c.sendRequest(server, new Record(setting)));
        }
        for (Future<Packet> request : requests) {
        	request.get();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");

        // debug
        start = System.currentTimeMillis();
        response = c.sendRequestAndWait(server, new Record("debug")).getRecord(0).getData();
        System.out.println("Debug output: " + response.pretty());
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        // ping manager
        System.out.println("pinging manager 10000 times...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
        	requests.add(c.sendRequest("Manager"));
        }
        for (Future<Packet> request : requests) {
        	request.get();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        c.close();
    }
}
