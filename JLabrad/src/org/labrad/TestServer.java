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
import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.errors.IncorrectPasswordException;
import org.labrad.errors.LoginFailedException;

/**
 *
 * @author maffoo
 */
@ServerInfo(name="Java Test Server",
            description="Basic server to test JLabrad API.",
            notes="Not much else to say, really.")
public class TestServer implements ContextualServer<Map<String, Data>> {
    @Override
    public Map<String, Data> newContext(Context context, long source) {
        Map<String, Data> ctxData = new HashMap<String, Data>();
        ctxData.put("Test", Data.valueOf("blah"));
        return ctxData;
    }

    @Override
    public void expireContext(Context context, Map<String, Data> data) {
        // cleanup here
    }

    private void log(String method, Data data) {
        System.out.println(method + " called [" + data.getTag() + "]: " + data.pretty());
    }

    @Setting(ID=1, name="Echo", returns="?",
             description="Echoes back any data sent to this setting.")
    public Data echo(RequestContext<Map<String, Data>> ctx, Data data) {
        log("Echo", data);
        return data;
    }

    @Setting(ID=2, name="Set", accepts="s?", returns="?",
             description="Sets a key value pair in the current context.")
    public Data set(RequestContext<Map<String, Data>> ctx, Data data) {
        log("Set", data);
        ctx.getData().put(data.get(0).getString(), data.get(1));
        return data.get(1);
    }

    @Setting(ID=3, name="Get", accepts="s", returns="?",
             description="Gets a key from the current context.")
    public Data get(RequestContext<Map<String, Data>> ctx, Data data) {
        log("Get", data);
        String key = data.getString();
        if (!ctx.getData().containsKey(key)) {
            throw new RuntimeException("Invalid key: " + key);
        }
        return ctx.getData().get(key);
    }

    @Setting(ID=4, name="Get All", accepts="", returns="?",
             description="Gets all of the key-value pairs defined in this context.")
    public Data getAll(RequestContext<Map<String, Data>> ctx, Data data) {
        log("Get All", data);
        Map<String, Data> map = ctx.getData();
        List<Data> items = new ArrayList<Data>();
        for (String key : map.keySet()) {
            items.add(Data.of(Data.valueOf(key), map.get(key)));
        }
        return Data.of(items);
    }

    @Setting(ID=5, name="Keys", accepts="", returns="*s",
             description="Returns a list of all keys defined in this context.")
    public Data getKeys(RequestContext<Map<String, Data>> ctx, Data data) {
        log("Keys", data);
        return Data.ofType("*s").setStringList(new ArrayList<String>(ctx.getData().keySet()));
    }


    public static void main(String[] args)
            throws UnknownHostException, IOException,
                   LoginFailedException, IncorrectPasswordException {
        ServerConnection cxn = ServerConnection.create(new TestServer());
        cxn.setPassword("");
        cxn.connect();
    }
}
