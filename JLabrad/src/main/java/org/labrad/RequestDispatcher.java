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
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.labrad.data.Packet;
import org.labrad.data.Request;

/**
 *
 * @author Matthew Neeley
 */
class RequestDispatcher {
  int nextRequest = 1;
  List<Integer> requestPool = new ArrayList<Integer>();
  Map<Integer, RequestReceiver> pendingRequests = new HashMap<Integer, RequestReceiver>();
  BlockingQueue<Packet> writeQueue;

  /**
   * Create a new dispatcher to manage requests going out on the given queue.
   * @param writeQueue a queue to which packets will be passed to send
   */
  RequestDispatcher(BlockingQueue<Packet> writeQueue) {
    this.writeQueue = writeQueue;
  }

  /**
   * Start a request and return a receiver that can be used to get the result.
   * @param request the request to be sent
   * @return a receiver for getting the result
   */
  RequestReceiver startRequest(Request request) {
    return startRequest(request, null);
  }

  /**
   * Start a request with an optional callback.  We dispatch to the callback
   * using the EventQueue.invokeLater function.
   * @param request the request to be sent
   * @param callback a callback to be called when the request finishes
   * @return a receiver for getting the result
   */
  synchronized RequestReceiver startRequest(Request request, RequestCallback callback) {
    int requestNum;
    if (requestPool.isEmpty()) {
      requestNum = nextRequest++;
    } else {
      requestNum = requestPool.remove(0);
    }
    RequestReceiver receiver = new RequestReceiver(request, callback);
    pendingRequests.put(requestNum, receiver);
    writeQueue.add(Packet.forRequest(request, requestNum));
    return receiver;
  }

  /**
   * Finish a request from an incoming response packet.
   * @param packet the received response packet
   */
  synchronized void finishRequest(Packet packet) {
    int request = -packet.getRequest();
    if (pendingRequests.containsKey(request)) {
      RequestReceiver receiver = pendingRequests.remove(request);
      requestPool.add(request);
      receiver.set(packet);
    } else {
      // response to a request we didn't make
      String message = "Received a response to an unknown request: " + request + ".";
      Logger.getLogger("RequestDispatcher").log(Level.WARNING, message);
    }
  }

  /**
   * Cause all pending requests to fail.  This is called when the
   * connection using this dispatcher is closed.
   * @param cause of the failure
   */
  synchronized void failAll(Throwable cause) {
    for (RequestReceiver receiver : pendingRequests.values()) {
      receiver.fail(cause);
    }
  }
}
