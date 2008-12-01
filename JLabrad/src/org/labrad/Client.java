package org.labrad;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
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

public class Client {
	private static final long MANAGER = 1;
	private static final long LOOKUP = 3;
	private static final long PROTOCOL = 1;
	private static final String NAME = "Java SimpleClient";
	
    private Reader reader;
    private Writer writer;

    private Socket socket;

    private String host;
    private int port;
    private long ID;

    BlockingQueue<Packet> writeQueue;

    int nextRequest = 1;
    List<Integer> requestPool = new ArrayList<Integer>();
    Map<Integer, RequestReceiver> pendingRequests =
    	new HashMap<Integer, RequestReceiver>();

    /**
     * Represents a pending LabRAD request.
     * @author maffoo
     *
     */
    class RequestReceiver implements Future<Packet> {

    	private boolean cancelled = false;
    	private boolean done = false;
    	private Packet response;
    	
    	/**
    	 * Cancel this request.
    	 * @return true if the request was cancelled
    	 */
		@Override
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			if (!done && !cancelled) {
				cancelled = true;
				return true;
			}
			return false;
		}

		@Override
		public synchronized Packet get() throws InterruptedException, ExecutionException {
			while (!done) {
				wait();
			}
			return response;
		}

		@Override
		public synchronized Packet get(long duration, TimeUnit timeUnit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			while (!done) {
				wait(TimeUnit.MILLISECONDS.convert(duration, timeUnit));
			}
			return response;
		}

		@Override
		public synchronized boolean isCancelled() {
			return cancelled;
		}

		@Override
		public synchronized boolean isDone() {
			return done;
		}
    	
		protected synchronized void set(Packet response) {
			this.done = true;
			this.response = response;
			notifyAll();
		}
		
		protected synchronized void fail(Throwable cause) {
			// TODO add failure mechanism here
		}
    }

    /**
     * Thread that writes queued packets to the output stream.
     * @author maffoo
     *
     */
    class Writer extends Thread {
        BlockingQueue<Packet> queue;
        PacketOutputStream os;

        Writer(BlockingQueue<Packet> queue, PacketOutputStream os) {
        	super("Packet Writer thread");
            this.queue = queue;
            this.os = os;
        }

        public void run() {
            try {
                while (true) {
                    Packet p = queue.take();
                    //System.out.println("Sending request: " + p.getRequest());
                    os.writePacket(p);
                }
            } catch (InterruptedException e) {
                System.out.println("Writer Thread interrupted.");
            } catch (IOException e) {
                System.out.println("IOException in Writer Thread.");
            }
        }
    }

    /**
     * Thread that reads packets coming in on an input stream.
     * @author maffoo
     *
     */
    class Reader extends Thread {
        PacketInputStream is;

        Reader(PacketInputStream is) {
        	super("Packet Reader thread");
            this.is = is;
        }

        public void run() {
            try {
                while (!Thread.interrupted()) {
                    handleResponse(is.readPacket());
                }
            } catch (IOException e) {
                System.out.println("IOException in Reader Thread.");
            }
        }
    }
    
    /**
     * Handle response packets coming in from the wire.
     * @param packet
     */
    private synchronized void handleResponse(Packet packet) {
    	//System.out.println("Handling response: " + packet.getRequest());
        int request = -packet.getRequest();
        if (pendingRequests.containsKey(request)) {
        	RequestReceiver receiver = pendingRequests.get(request);
        	pendingRequests.remove(request);
        	requestPool.add(request);
        	receiver.set(packet);
        }
    }
    
    
    // Request functions
    
    /**
     * Makes a LabRAD request asynchronously.  The request is sent over LabRAD and an
     * object is returned that can be queried to see if the request has completed or
     * to wait for completion.
     * @param server
     * @param records
     */
    public synchronized Future<Packet> sendRequest(long server, Record... records) {
        Context context = new Context(0, 1);
        int requestNum;
        if (!requestPool.isEmpty()) {
        	requestNum = requestPool.remove(0);
        } else {
        	requestNum = nextRequest++;
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
     */
    public Packet sendRequestAndWait(long server, Record... records)
    		throws InterruptedException, ExecutionException {
    	return sendRequest(server, records).get();
    }
    
    /**
     * Sends a LabRAD message to the specified server.
     * @param server
     * @param records
     */
    public synchronized void sendMessage(long server, Record... records) {
    	Context context = new Context(0, 1);
    	writeQueue.add(new Packet(context, server, 0, records));
    }
    
    Client(String host, int port, String password)
    		throws UnknownHostException, IOException, ExecutionException, InterruptedException, NoSuchAlgorithmException {
        this.host = host;
        this.port = port;

        socket = new Socket(host, port);
        PacketInputStream is = new PacketInputStream(socket.getInputStream());
        PacketOutputStream os = new PacketOutputStream(socket.getOutputStream());

        writeQueue = new LinkedBlockingQueue<Packet>();

        reader = new Reader(is);
        writer = new Writer(writeQueue, os);
        reader.start();
        writer.start();
        
        doLogin(password);
    }
    
    /**
     * Logs in to LabRAD using the standard protocol.
     * @param password
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    private void doLogin(String password) throws InterruptedException, ExecutionException, NoSuchAlgorithmException, UnsupportedEncodingException {
    	Data data, response;
        
        // send first ping packet
        response = sendRequestAndWait(MANAGER).getRecord(0).getData();
        
        // get password challenge
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] challenge = response.getBytes();
        md.update(challenge);
        md.update(password.getBytes(Data.STRING_ENCODING));

        // send password response
        data = new Data("s").setBytes(md.digest());
        response = sendRequestAndWait(MANAGER, new Record(0, data)).getRecord(0).getData();
        
        // print welcome message
        System.out.println(response.getString());

        // send identification packet
        data = new Data("ws").setWord(PROTOCOL, 0).setString(NAME, 1);
        response = sendRequestAndWait(MANAGER, new Record(0, data)).getRecord(0).getData();
        ID = response.getWord();
    }
    
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
    }
}
