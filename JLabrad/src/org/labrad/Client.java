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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
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
	private static final long MANAGER = 1;
	private static final long LOOKUP = 3;
	private static final long PROTOCOL = 1;
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


	// Request functions
    
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
	    response = sendRequestAndWait(MANAGER).getRecord(0).getData();
	    
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
	    	response = sendRequestAndWait(MANAGER, new Record(0, data)).getRecord(0).getData();
	    } catch (ExecutionException e) {
	    	throw new IncorrectPasswordException();
	    }
	    
	    // print welcome message
	    System.out.println(response.getString());
	
	    // send identification packet
	    data = new Data("ws").setWord(PROTOCOL, 0).setString(NAME, 1);
	    response = sendRequestAndWait(MANAGER, new Record(0, data)).getRecord(0).getData();
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
    public synchronized Future<Packet> sendRequest(long server, Record... records)
    		throws IOException {
    	if (!connected) {
    		throw new IOException("not connected.");
    	}
        Context context = new Context(0, 1);
        int requestNum;
        if (requestPool.isEmpty()) {
        	requestNum = nextRequest++;
        } else {
        	requestNum = requestPool.remove(0);
        }
        RequestReceiver receiver = new RequestReceiver();
        pendingRequests.put(requestNum, receiver);
        writeQueue.add(new Packet(context, server, requestNum, records));
        return receiver;
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
        long serverID, settingID, debugID, echoID, delayID;
        long start, end;
        
        String server = "Python Test Server";
        String setting = "Get Random Data";
        String password = "martinisgroup";
        
        List<Future<Packet>> requests = new ArrayList<Future<Packet>>();
        
        // connect to LabRAD
        Client c = new Client("localhost", 7682, password);
        
        data = new Data("s*s").setString(server, 0)
                              .setArraySize(4, 1)
                                  .setString(setting, 1, 0)
                                  .setString("debug", 1, 1)
                                  .setString("Delayed Echo", 1, 2)
                                  .setString("Echo Delay", 1, 3);
        data = c.sendRequest(MANAGER, new Record(LOOKUP, data)).get().getRecord(0).getData();
        serverID = data.getWord(0);
        settingID = data.getWord(1, 0);
        debugID = data.getWord(1, 1);
        echoID = data.getWord(1, 2);
        delayID = data.getWord(1, 3);
        
        System.out.println("Server '" + server + "' has ID: " + serverID);
        System.out.println("Setting '" + setting + "' has ID: " + settingID);
        
        // lookup hydrant server
        //ServerProxy hydrant = c.getServer(server);
        
        // echo with delays
        System.out.println("echo with delays...");
        // set delay to 1 second
        c.sendRequestAndWait(serverID, new Record(delayID, new Data("v[s]").setValue(1.0)));
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
        	requests.add(c.sendRequest(serverID, new Record(echoID, new Data("w").setWord(4))));
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
        	requests.add(c.sendRequest(serverID, new Record(settingID)));
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
            requests.add(c.sendRequest(serverID, new Record(settingID)));
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
        	requests.add(c.sendRequest(serverID, new Record(settingID)));
        }
        for (Future<Packet> request : requests) {
        	request.get();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");

        // debug
        start = System.currentTimeMillis();
        response = c.sendRequestAndWait(serverID, new Record(debugID)).getRecord(0).getData();
        System.out.println("Debug output: " + response.pretty());
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        // ping manager
        System.out.println("pinging manager 10000 times...");
        requests.clear();
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
        	requests.add(c.sendRequest(MANAGER));
        }
        for (Future<Packet> request : requests) {
        	request.get();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
        
        c.close();
    }
}
