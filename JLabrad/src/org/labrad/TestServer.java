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
import org.labrad.data.Request;
import org.labrad.errors.IncorrectPasswordException;
import org.labrad.errors.LoginFailedException;

/**
 *
 * @author maffoo
 */
@ServerInfo(name="Java Test Server",
            description="Basic server to test JLabrad API.",
            notes="Not much else to say, really.")
public class TestServer extends AbstractContextServer {
    public TestServer() {
    	registry.put("Test", Data.valueOf("blah"));
    }
        
    private Map<String, Data> registry = new HashMap<String, Data>();
    
    private void log(String method, Data data) {
        System.out.println(method + " called [" + data.getTag() + "]: " + data.pretty());
    }

    @Setting(ID=1, name="Echo", accepts="?", returns="?",
             description="Echoes back any data sent to this setting.")
    public Data echo(Data data) {
        log("Echo", data);
        return data;
    }

    @Setting(ID=2, name="Set", accepts="s?", returns="?",
             description="Sets a key value pair in the current context.")
    public Data set(Data data) {
        log("Set", data);
        String key = data.get(0).getString();
        Data value = data.get(1).clone();
        registry.put(key, value);
        return value;
    }

    @Setting(ID=3, name="Get", accepts="s", returns="?",
             description="Gets a key from the current context.")
    public Data get(Data data) {
        log("Get", data);
        String key = data.getString();
        if (!registry.containsKey(key)) {
            throw new RuntimeException("Invalid key: " + key);
        }
        return registry.get(key);
    }

    @Setting(ID=4, name="Get All", accepts="", returns="?",
             description="Gets all of the key-value pairs defined in this context.")
    public Data getAll(Data data) {
        log("Get All", data);
        List<Data> items = new ArrayList<Data>();
        for (String key : registry.keySet()) {
            items.add(Data.clusterOf(Data.valueOf(key), registry.get(key)));
        }
        return Data.clusterOf(items);
    }

    @Setting(ID=5, name="Keys", accepts="", returns="*s",
             description="Returns a list of all keys defined in this context.")
    public Data getKeys(Data data) {
        log("Keys", data);
        return Data.ofType("*s").setStringList(new ArrayList<String>(registry.keySet()));
    }

    @Setting(ID=6, name="Get Random Data", accepts="", returns="?",
             description="Fetches random data by making a request to the python test server.")
    public Data getRandomData(Data data) throws InterruptedException, ExecutionException {
        log("Get Random Data", data);
        Request request = Request.to("Python Test Server").add("Get Random Data");
        List<Data> response = getConnection().sendAndWait(request);
        return response.get(0);
    }

    public static void main(String[] args)
            throws UnknownHostException, IOException,
                   LoginFailedException, IncorrectPasswordException,
                   InterruptedException, ExecutionException {
        ServerConnection cxn = ServerConnection.create(TestServer.class);
        cxn.setPassword("");
        cxn.connect();
        cxn.serve();
    }
}
