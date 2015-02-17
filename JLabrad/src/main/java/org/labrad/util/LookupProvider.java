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

package org.labrad.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.labrad.Connection;
import org.labrad.Constants;
import org.labrad.data.Data;
import org.labrad.data.Record;
import org.labrad.data.Request;

public class LookupProvider {
  private Connection connection;

  /** Maps server names to IDs. */
  private ConcurrentMap<String, Long> serverCache = new ConcurrentHashMap<String, Long>();

  /** Maps server IDs to a map from setting names to IDs. */
  private ConcurrentMap<Long, ConcurrentMap<String, Long>> settingCache =
    new ConcurrentHashMap<Long, ConcurrentMap<String, Long>>();

  /**
   * Create a new LookupService.
   * @param connection
   */
  public LookupProvider(Connection connection) {
    this.connection = connection;
  }

  public void clearCache() {
    serverCache.clear();
    settingCache.clear();
  }
  
  public void clearServer(String name) {
    // TODO connect this to server disconnection messages 
    Long id = serverCache.remove(name);
    settingCache.remove(id);
  }

  /**
   * Attempt to do necessary server/setting lookups from the local cache only.
   * @param request
   */
  public void doLookupsFromCache(Request request) {
    // lookup server ID
    if (request.needsServerLookup()) {
      Long serverId = serverCache.get(request.getServerName());
      if (serverId != null) {
        request.setServerID(serverId);
      }
    }
    // lookup setting IDs if server ID lookup succeeded
    if (!request.needsServerLookup() && request.needsSettingLookup()) {
      ConcurrentMap<String, Long> cache = settingCache.get(request.getServerID());
      if (cache != null) {
        for (Record r : request.getRecords()) {
          if (r.needsLookup()) {
            Long settingId = cache.get(r.getName());
            if (settingId != null) {
              r.setID(settingId);
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
  public void doLookups(Request request)
  throws InterruptedException, ExecutionException {
    // lookup server ID
    if (request.needsServerLookup()) {
      Long serverID = serverCache.get(request.getServerName());
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
            Long settingID = cache.get(r.getName());
            if (settingID != null) {
              r.setID(settingID);
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
  throws InterruptedException, ExecutionException {
    Request request = new Request(Constants.MANAGER);
    request.add(Constants.LOOKUP, Data.valueOf(server));
    Data response = connection.sendAndWait(request).get(0);
    long serverID = response.getWord();
    // cache this lookup result
    serverCache.putIfAbsent(server, serverID);
    settingCache.putIfAbsent(serverID, new ConcurrentHashMap<String, Long>());
    return serverID;
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
  throws InterruptedException, ExecutionException {
    Data data = new Data("w*s");
    data.get(0).setWord(serverID);
    data.get(1).setStringList(settings);
    Request request = new Request(Constants.MANAGER);
    request.add(Constants.LOOKUP, data);
    Data response = connection.sendAndWait(request).get(0);
    List<Long> settingIDs = response.get(1).getWordList();
    // cache these lookup results
    ConcurrentMap<String, Long> cache = settingCache.get(serverID);
    if (cache != null) {
      for (int i = 0; i < settings.size(); i++) {
        cache.put(settings.get(i), settingIDs.get(i));
      }
    }
    return settingIDs;
  }
}
