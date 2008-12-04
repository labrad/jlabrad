package org.labrad;

import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.PacketInputStream;
import org.labrad.data.PacketOutputStream;
import org.labrad.data.Record;
import org.labrad.errors.IncorrectPasswordException;
import org.labrad.errors.LabradException;

public class SimpleClient {
	private static final String ENCODING = "ISO-8859-1";
	private static final long MANAGER = 1;
	private static final long LOOKUP = 3;
	private static final long PROTOCOL = 1;
	private static final String NAME = "Java SimpleClient";
	
	private static final String DEFAULT_HOST = "localhost";
	private static final String DEFAULT_PORT = "7682";
	
	private String host, password = null;
	private int port;
	private long ID;
	private boolean connected = false;
	
	private Socket socket;
	PacketInputStream is;
	PacketOutputStream os;
	
	private static final Context defaultCtx = new Context(0, 0);
	private long nextContext = 1;
	
	Map<String, Long> serverCache = new HashMap<String, Long>();
	Map<Long, Map<String, Long>> settingCache = 
		new HashMap<Long, Map<String, Long>>();
	
	/**
	 * Get an environment variable, or fall back on the given default if not found.
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	private String getEnv(String key, String defaultVal) {
		Map<String, String> env = System.getenv();
		if (env.containsKey(key)) {
			return env.get(key);
		} else {
			return defaultVal;
		}
	}

    public String getHost() {
        ensureConnection();
        return host;
    }

    public int getPort() {
        ensureConnection();
        return port;
    }

    public long getID() {
        ensureConnection();
        return ID;
    }

    public void connect() throws IOException, IncorrectPasswordException {
        connect(getEnv("LABRADHOST", DEFAULT_HOST));
    }

    public void connect(String host)
                    throws IOException, IncorrectPasswordException {
        connect(host, Integer.parseInt(getEnv("LABRADPORT", DEFAULT_PORT)));
    }

    public void connect(String host, int port)
                    throws IOException, IncorrectPasswordException {
        if (password == null) {
            connect(host, port, getEnv("LABRADPASSWORD", ""));
        } else {
            connect(host, port, password);
        }
    }

    public void connect(String host, int port, String password)
	    throws IOException, IncorrectPasswordException {
    	doConnect(host, port, password);
	}
    
    private void doConnect(String host, int port, String password)
                    throws IOException, IncorrectPasswordException {
        if (connected) {
            throw new IOException("Already connected.");
        }
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(1000); // only block for one second on reads
            is = new PacketInputStream(socket.getInputStream());
            os = new PacketOutputStream(socket.getOutputStream());

            Data data;
            List<Record> response;
            connected = true;

            // send first (empty) packet
            response = sendRequest();

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] challenge = response.get(0).getData().getBytes();
            md.update(challenge);
            md.update(password.getBytes(ENCODING));

            // send password response
            data = new Data("s").setBytes(md.digest());
            try {
                response = sendRequest(new Record(0, data));
            } catch (IOException e) {
                throw new IncorrectPasswordException();
            }
            
            // print welcome message
            System.out.println(response.get(0).getData().getString());

            // send identification packet
            data = new Data("ws").setWord(PROTOCOL, 0).setString(NAME, 1);
            response = sendRequest(new Record(0, data));
            ID = response.get(0).getData().getWord();

            // if we get here, connection was successful
            this.host = host;
            this.port = port;
            this.password = password;
        } catch (NoSuchAlgorithmException e) {
            connected = false;
            throw new RuntimeException("MD5 hash not supported!");
        } catch (IOException e) {
            connected = false;
            throw e;
        }
    }

    public void disconnect() throws IOException {
        ensureConnection();
        socket.close();
        connected = false;
    }

    /**
     * Lookup the ID of a server, pulling from the cache if we already know it.
     * @param server
     * @return
     * @throws IOException
     */
    private long lookupServer(String server) throws IOException {
        ensureConnection();
        if (serverCache.containsKey(server)) {
            return serverCache.get(server);
        }
        Data response = sendRequest(
        		new Record(LOOKUP, new Data("s").setString(server))).get(0).getData();
        if (response.isError()) {
        	throw new LabradException(response);
        }
        long ID = response.getWord();
        serverCache.put(server, ID);
        settingCache.put(ID, new HashMap<String, Long>());
        return ID;
    }

    /**
     * Lookup IDs for a list of settings on the specified server.
     * @param serverID
     * @param settings
     * @return
     * @throws IOException
     */
    private long[] lookupSettings(long serverID, String[] settings)
            throws IOException {
        ensureConnection();
        long[] IDs = new long[settings.length];
        int[] indices = new int[settings.length];
        String[] lookups = new String[settings.length];
        int nLookups = 0;

        Map<String, Long> cache;
        if (settingCache.containsKey(serverID)) {
        	cache = settingCache.get(serverID);
        } else {
        	cache = new HashMap<String, Long>();
            settingCache.put(serverID, cache);
        }

        for (int i = 0; i < settings.length; i++) {
            String key = settings[i];
            if (cache.containsKey(key)) {
                IDs[i] = cache.get(key);
            } else {
                lookups[nLookups] = settings[i];
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
	        Data response = sendRequest(new Record(LOOKUP, data)).get(0).getData();
	        if (response.isError()) {
	        	throw new LabradException(response);
	        }
	        for (int i = 0; i < nLookups; i++) {
	            long ID = response.getWord(1, i);
	            cache.put(lookups[i], ID);
	            IDs[indices[i]] = ID;
	        }
        }
        return IDs;
    }

    public ServerProxy getServer(String name) throws IOException {
        long ID = lookupServer(name);
        return new ServerProxy(this, ID, name);
    }

    public Context newContext() {
    	return new Context(0, nextContext++);
    }
    
    public List<Record> sendRequest(Record... records) throws IOException {
        return sendRequest(defaultCtx, MANAGER, records);
    }

    public List<Record> sendRequest(String target, Record... records)
            throws IOException {
        return sendRequest(lookupServer(target), records);
    }

    public List<Record> sendRequest(long target, Record... records)
    		throws IOException {
        return sendRequest(defaultCtx, target, records);
    }

    public List<Record> sendRequest(Context context, String target, Record... records)
            throws IOException {
        return sendRequest(context, lookupServer(target), records);
    }

    public List<Record> sendRequest(Context context, long target, Record... records)
            throws IOException {
        ensureConnection();

        Record[] lookedUpRecords = new Record[records.length];
        String[] lookups = new String[records.length];
        int[] indices = new int[records.length];
        int nLookups = 0;

        for (int i = 0; i < records.length; i++) {
            if (records[i].needsLookup()) {
                lookups[nLookups] = records[i].getName();
                indices[nLookups] = i;
                nLookups++;
            } else {
                lookedUpRecords[i] = records[i];
            }
        }
        if (nLookups > 0) {
            long[] IDs = lookupSettings(target, lookups);
            for (int i = 0; i < nLookups; i++) {
                lookedUpRecords[indices[i]] = new Record(IDs[i],
                        records[indices[i]].getData());
            }
        }

        os.writePacket(new Packet(context, target, 1, lookedUpRecords));
        Packet packet = is.readPacket();
        return packet.getRecords();
    }

    private void ensureConnection() {
        if (!connected) {
            throw new RuntimeException("Not connected!");
        }
    }

    public static void main(String[] args) throws IOException, IncorrectPasswordException {
        Data response;
        long start, end;

        String server = "Python Test Server";
        String setting = "Get Random Data";
        
        // connect to LabRAD
        SimpleClient sc = new SimpleClient();
        sc.connect("localhost", 7682, "martinisgroup");
        
        // lookup hydrant server
        ServerProxy hydrant = sc.getServer(server);
        
        // random hydrant data
        System.out.println("getting random data, with printing...");
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            response = hydrant.sendRequest(setting);
            System.out.println("got packet: " + response.toString());
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");

        start = System.currentTimeMillis();
        response = hydrant.sendRequest("debug");
        System.out.println(response.toString());
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");

        // random hydrant data
        System.out.println("getting random data, make pretty, but don't print...");
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            response = hydrant.sendRequest(setting);
            response.toString();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");

        // random hydrant data
        System.out.println("getting random data, no printing...");
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            hydrant.sendRequest(setting);
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");

        // ping manager
        System.out.println("pinging manager 10000 times...");
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            sc.sendRequest();
        }
        end = System.currentTimeMillis();
        System.out.println("done.  elapsed: " + (end - start) + " ms.");
    }
}
