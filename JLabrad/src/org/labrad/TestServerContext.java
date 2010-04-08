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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.labrad.annotations.Accepts;
import org.labrad.annotations.Returns;
import org.labrad.annotations.Setting;
import org.labrad.annotations.SettingOverload;
import org.labrad.data.Data;
import org.labrad.data.Hydrant;
import org.labrad.data.Request;
import org.labrad.data.Setters;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 * @author maffoo
 */
public class TestServerContext extends AbstractServerContext {
  private Map<String, Data> registry = Maps.newHashMap();


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
  @Setting(id = 1,
           name = "Echo",
           doc = "Echoes back any data sent to this setting.")
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
  @Setting(id = 2,
           name = "Delayed Echo",
           doc = "Echoes back data after a specified delay.")
  @Returns("? {same as input}")
  public Data delayedEcho(@Accepts("v[s] {delay}") double delay, @Accepts("? {anything}") Data payload)
      throws InterruptedException {
    log("Delayed Echo (%g seconds): %s", delay, payload.pretty());
    Thread.sleep((long)(delay*1000));
    return payload;
  }


  /**
   * Set a key in this context.
   * @param data
   * @return
   */
  @Setting(id = 3,
           name = "Set",
           doc = "Sets a key value pair in the current context.")
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
  @Setting(id = 4,
           name = "Get",
           doc = "Gets a key from the current context.")
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
  @Setting(id = 5,
           name = "Get All",
           doc = "Gets all of the key-value pairs defined in this context.")
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
  @Setting(id = 6,
           name = "Keys",
           doc = "Returns a list of all keys defined in this context.")
  @Returns("*s")
  public Data getKeys() {
    log("Keys");
    return Data.listOf(Lists.newArrayList(registry.keySet()), Setters.stringSetter);
  }


  /**
   * Remove a key from this context
   * @param key
   */
  @Setting(id = 7,
           name = "Remove",
           doc = "Removes the specified key from this context.")
  public void remove(String key) {
    log("Remove: %s", key);
    registry.remove(key);
  }


  /**
   * Get a random LabRAD data object.
   * @param data
   * @return
   */
  @Setting(id = 8,
           name = "Get Random Data",
           doc = "Returns random LabRAD data.\n\n"
               + "If a type is specified, the data will be of that type; "
               + "otherwise it will be of a random type.")
  public Data getRandomData(String type) {
    log("Get Random Data: %s", type);
    return Hydrant.getRandomData(type);
  }
  @SettingOverload
  public Data getRandomData() {
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
  @Setting(id = 9,
           name = "Get Random Data Remote",
           doc = "Fetches random data by making a request to the python test server.")
  public Data getRandomDataRemote(String type) throws InterruptedException, ExecutionException {
    log("Get Random Data Remote: %s", type);
    return makeRequest("Python Test Server", "Get Random Data", Data.valueOf(type));
  }
  @SettingOverload
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
  @Setting(id = 10,
           name = "Forward Request",
           doc = "Forwards a request on to another server, specified by name and setting.")
  public Data forwardRequest(String server, String setting, Data payload)
      throws InterruptedException, ExecutionException {
    log("Forward Request: server='%s', setting='%s', payload=%s", server, setting, payload);
    return makeRequest(server, setting, payload);
  }

  // commented annotations will give errors

  @Setting(id = 11,
           name = "Test No Args",
           doc = "Test setting that takes no arguments.")
  @Returns("b")
  public Data noArgs() {
    log("Test No Args");
    return Data.valueOf(true);
  }

  @Setting(id = 12,
           name = "Test No Return",
           doc = "Test setting with no return value.")
  public void noReturn(Data data) {
    log("Test No Return", data);
  }

  @Setting(id = 13,
           name = "Test No Args No Return",
           doc = "Test setting that takes no arguments and has no return value.")
  public void noArgsNoReturn() {
    log("Test No Args No Return");
  }
}
