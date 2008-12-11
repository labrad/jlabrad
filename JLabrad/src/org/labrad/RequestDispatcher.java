/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.labrad.Connection;
import org.labrad.data.Packet;
import org.labrad.data.Request;

/**
 *
 * @author maffoo
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
