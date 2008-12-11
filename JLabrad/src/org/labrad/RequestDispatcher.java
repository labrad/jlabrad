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

    RequestDispatcher(BlockingQueue<Packet> writeQueue) {
        this.writeQueue = writeQueue;
    }

    RequestReceiver startRequest(Request request) {
        return startRequest(request, null);
    }

    synchronized RequestReceiver startRequest(Request request, RequestCallback callback) {
        int requestNum;
	    if (requestPool.isEmpty()) {
	    	requestNum = nextRequest++;
	    } else {
	    	requestNum = requestPool.remove(0);
	    }
	    RequestReceiver receiver = new RequestReceiver(request);
	    pendingRequests.put(requestNum, receiver);
	    writeQueue.add(Packet.forRequest(request, requestNum));
        return receiver;
    }

    synchronized void finishRequest(Packet packet) {
        int request = -packet.getRequest();
        if (pendingRequests.containsKey(request)) {
            RequestReceiver receiver = pendingRequests.remove(request);
            requestPool.add(request);
            receiver.set(packet);
        } else {
            // response to a request we didn't make
            // TODO log this as an error
        }
    }

    synchronized void failAll(Throwable cause) {
        for (RequestReceiver receiver : pendingRequests.values()) {
			receiver.fail(cause);
		}
    }
}
