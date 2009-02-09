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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.labrad.data.Data;
import org.labrad.data.Hydrant;
import org.labrad.data.Request;
import org.labrad.errors.IncorrectPasswordException;
import org.labrad.errors.LoginFailedException;
import org.labrad.types.Str;

/**
 *
 * @author maffoo
 */
@ServerInfo(name="Java Test Server",
            description="Basic server to test JLabrad API.",
            notes="Not much else to say, really.")
public class TestServer extends AbstractContextServer {
	private Map<String, Data> registry = new HashMap<String, Data>();
	
	public static void initServer() {
		// do server initialization here
		// this gets called after we have connected to LabRAD, but before any contexts have been created
		// TODO: need a way to get at a connection object here
	}
	
	public static void shutdown() {
		// do server cleanup here
		// note that we may have already lost our connection by this point.
	}
	
	public void init() {
    	registry.put("Test", Data.valueOf("blah"));
    	System.out.println("Context " + getContext() + " created.");
    }
	
	public void expire() {
		System.out.println("Context " + getContext() + " expired.");
	}
	
    private void log(String method, Data data) {
        System.out.println(method + " called [" + data.getTag() + "]: " + data.pretty());
    }

    @Setting(ID=1, name="Echo", accepts="?", returns="?",
             description="Echoes back any data sent to this setting.")
    public Data echo(Data data) {
        log("Echo", data);
        return data;
    }
    
    @Setting(ID=2, name="Delayed Echo", accepts="v[s]?", returns="?",
    		 description="Echoes back data after a specified delay.")
    public Data delayedEcho(Data data) throws InterruptedException {
    	log("Delayed Echo", data);
    	double delay = data.get(0).getValue();
    	Data payload = data.get(1);
    	Thread.sleep((long)(delay*1000));
    	return payload;
    }
    
    @Setting(ID=3, name="Set", accepts="s?", returns="?",
             description="Sets a key value pair in the current context.")
    public Data set(Data data) {
        log("Set", data);
        String key = data.get(0).getString();
        Data value = data.get(1).clone();
        registry.put(key, value);
        return value;
    }

    @Setting(ID=4, name="Get", accepts="s", returns="?",
             description="Gets a key from the current context.")
    public Data get(Data data) {
        log("Get", data);
        String key = data.getString();
        if (!registry.containsKey(key)) {
            throw new RuntimeException("Invalid key: " + key);
        }
        return registry.get(key);
    }

    /**
     * Get all key-value pairs defined in this context.
     * @param data
     * @return
     */
    @Setting(ID=5, name="Get All", accepts="", returns="?",
             description="Gets all of the key-value pairs defined in this context.")
    public Data getAll(Data data) {
        log("Get All", data);
        List<Data> items = new ArrayList<Data>();
        for (String key : registry.keySet()) {
            items.add(Data.clusterOf(Data.valueOf(key), registry.get(key)));
        }
        return Data.clusterOf(items);
    }

    /**
     * Get all keys defined in this context.
     * @param data
     * @return
     */
    @Setting(ID=6, name="Keys", accepts="", returns="*s",
             description="Returns a list of all keys defined in this context.")
    public Data getKeys(Data data) {
        log("Keys", data);
        return Data.ofType("*s").setStringList(new ArrayList<String>(registry.keySet()));
    }

    @Setting(ID=7, name="Get Random Data", accepts={"s", ""}, returns="?",
             description="Fetches random data by making a request to the python test server.")
    public Data getRandomData(Data data) {
        log("Get Random Data", data);
    	if (data.getType() instanceof Str) {
    		return Hydrant.getRandomData(data.getString());
    	}
        return Hydrant.getRandomData();
    }
    
    @Setting(ID=8, name="Get Random Data Remote", accepts="", returns="?",
            description="Fetches random data by making a request to the python test server.")
   public Data getRandomDataRemote(Data data) throws InterruptedException, ExecutionException {
       log("Get Random Data Remote", data);
       Request request = Request.to("Python Test Server").add("Get Random Data", data);
       List<Data> response = getConnection().sendAndWait(request);
       return response.get(0);
   }
    
    /**
     * Forward a request on to another server.
     * @param data
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Setting(ID=9, name="Forward Request", accepts="ss?", returns="?",
             description="Forwards a request on to another server, specified by name and setting.")
    public Data forwardRequest(Data data) throws InterruptedException, ExecutionException {
        log("Forward Request", data);
        String server = data.get(0).getString();
        String setting = data.get(1).getString();
        Data payload = data.get(2);
        Request request = Request.to(server).add(setting, payload);
        List<Data> response = getConnection().sendAndWait(request);
        return response.get(0);
    }
    
    public static void main(String[] args)
            throws UnknownHostException, IOException,
                   LoginFailedException, IncorrectPasswordException,
                   InterruptedException, ExecutionException {
        ServerConnection cxn = ServerConnection.create(TestServer.class);
        cxn.connect();
        cxn.serve();
    }
}
