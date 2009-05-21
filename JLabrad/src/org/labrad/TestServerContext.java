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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.labrad.annotations.Returns;
import org.labrad.annotations.Setting;
import org.labrad.annotations.Units;
import org.labrad.data.Data;
import org.labrad.data.Hydrant;
import org.labrad.data.Request;

/**
 *
 * @author maffoo
 */
public class TestServerContext extends AbstractServerContext {
	private Map<String, Data> registry = new HashMap<String, Data>();
	
	
	/**
	 * Called when this context is first created.
	 */
	public void init() {
    	registry.put("Test", Data.valueOf("blah"));
    	System.out.format("Context %s created.", getContext());
    }
	
	
	/**
	 * Called when this context has expired.  Do any cleanup here.
	 */
	public void expire() {
		System.out.format("Context %s expired", getContext());
	}
	
	
	/**
	 * Print out a log message when a setting is called.
	 * @param setting
	 * @param data
	 */
    private void log(String setting, Data data) {
    	System.out.format("%s called [%s]: %s", setting, data.getTag(), data.pretty());
    }

    
    private void log(String format, Object... arguments) {
    	System.out.format(format, arguments);
    }
    
    
    /**
	 * Print out a log message when a setting is called.
	 * @param setting
	 * @param data
	 */
    private void log(String setting) {
    	System.out.format("%s called with no data", setting);
    }
    
    
    /**
	 * Make a request to the given server and setting.
	 * @param server
	 * @param setting
	 * @param data
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private Data makeRequest(String server, String setting, Data data)
			throws InterruptedException, ExecutionException {
		Request request = Request.to(server, getContext()).add(setting, data);
	    List<Data> response = getConnection().sendAndWait(request);
	    return response.get(0);
	}
	
	/**
	 * Make an empty request to the given server and setting.
	 * @param server
	 * @param setting
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private Data makeRequest(String server, String setting)
			throws InterruptedException, ExecutionException {
		return makeRequest(server, setting, Data.EMPTY);
	}
	

	//
	// Settings
	//
	
	/**
     * Echo back the data sent to us.
     * @param data
     * @return
     */
    @Setting(ID=1, name="Echo", description="Echoes back any data sent to this setting.")
    public Data echo(Data data) {
        log("Echo", data);
        return data;
    }
    
    
    /**
     * Echo back data after a specified delay.
     * @param data
     * @return
     * @throws InterruptedException
     */
    @Setting(ID=2, name="Delayed Echo", description="Echoes back data after a specified delay.")
    public Data delayedEcho(@Units("s") double delay, Data payload) throws InterruptedException {
    	log("Delayed Echo (%g seconds): %s", delay, payload.pretty());
    	Thread.sleep((long)(delay*1000));
    	return payload;
    }
    
    
    /**
     * Set a key in this context.
     * @param data
     * @return
     */
    @Setting(ID=3, name="Set", description="Sets a key value pair in the current context.")
    public Data set(String key, Data value) {
        log("Set: %s = %s", key, value);
        registry.put(key, value);
        return value;
    }

    
    /**
     * Get a key from this context.
     * @param data
     * @return
     */
    @Setting(ID=4, name="Get", description="Gets a key from the current context.")
    public Data get(String key) {
        log("Get: %s", key);
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
    @Setting(ID=5, name="Get All", description="Gets all of the key-value pairs defined in this context.")
    public Data getAll() {
        log("Get All");
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
    @Setting(ID=6, name="Keys", description="Returns a list of all keys defined in this context.")
    @Returns("*s")
    public Data getKeys() {
        log("Keys");
        return Data.ofType("*s").setStringList(new ArrayList<String>(registry.keySet()));
    }

    
    /**
     * Remove a key from this context
     * @param key
     */
    @Setting(ID=7, name="Remove", description="Removes the specified key from this context.")
    public void remove(String key) {
    	log("Remove: %s", key);
    	registry.remove(key);
    }
    
    
    /**
     * Get a random LabRAD data object.
     * @param data
     * @return
     */
    @Setting(ID=8, name="Get Random Data",
             description="Returns random LabRAD data.\n\n" +
             		     "If a type is specified, the data will be of that type; " +
             		     "otherwise it will be of a random type.")
    public Data getRandomData(String type) {
        log("Get Random Data: %s", type);
    	return Hydrant.getRandomData(type);
    }
    
    public Data getRandomData() {
    	// TODO dispatch to this method
    	// TODO do we need an annotation here?
    	log("Get Random Data (no type)");
    	return Hydrant.getRandomData();
    }
    
    
    /**
     * Get Random data by calling the python test server
     * @param data
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Setting(ID=9, name="Get Random Data Remote",
            description="Fetches random data by making a request to the python test server.")
    public Data getRandomDataRemote(String type) throws InterruptedException, ExecutionException {
        log("Get Random Data Remote: %s", type);
        return makeRequest("Python Test Server", "Get Random Data", Data.valueOf(type));
    }
    
    public Data getRandomDataRemote() throws InterruptedException, ExecutionException {
        log("Get Random Data Remote (no type)");
        return makeRequest("Python Test Server", "Get Random Data");
    }
    
    
    /**
     * Forward a request on to another server.
     * @param data
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Setting(ID=10, name="Forward Request",
             description="Forwards a request on to another server, specified by name and setting.")
    public Data forwardRequest(String server, String setting, Data payload)
    		throws InterruptedException, ExecutionException {
        log("Forward Request: server='%s', setting='%s', payload=%s", server, setting, payload);
        return makeRequest(server, setting, payload);
    }
    
    // commented annotations will give errors
    
    @Setting(ID=11, name="Test No Args", description="")
    //@Setting(ID=11, name="Test No Args", accepts="s", description="")
    @Returns("b")
    public Data noArgs() {
    	log("Test No Args");
    	return Data.valueOf(true);
    }
    
    @Setting(ID=12, name="Test No Return", description="")
    //@Setting(ID=12, name="Test No Return", accepts="?", description="")
    public void noReturn(Data data) {
    	log("Test No Return", data);
    }
    
    @Setting(ID=13, name="Test No Args No Return", description="")
    //@Setting(ID=13, name="Test No Args No Return", returns="", description="")
    //@Setting(ID=13, name="Test No Args No Return", returns="s", description="")
    public void noArgsNoReturn() {
    	log("Test No Args No Return");
    }
}
