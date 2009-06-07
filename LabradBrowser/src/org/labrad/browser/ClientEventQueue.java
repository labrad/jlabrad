package org.labrad.browser;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.labrad.browser.client.RemoteEvent;
import org.labrad.browser.client.RemoteEventType;
import org.labrad.browser.client.Util;
import org.mortbay.util.ajax.Continuation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ClientEventQueue {
	private Continuation continuation = null;
	
	private List<RemoteEventType> eventTypes = Lists.newArrayList();
	private Map<Class<?>, List<RemoteEvent>> eventLists = Maps.newHashMap();
	
	public synchronized void setContinuation(Continuation continuation) {
		this.continuation = continuation;
	}

	public synchronized Continuation getContinuation() {
		return continuation;
	}
		
	public synchronized void addEvent(RemoteEvent e) {
		Class<?> cls = e.getClass();
		RemoteEventType t = RemoteEventType.forClass(cls);
		if (t == null) {
			return;
		}
		List<RemoteEvent> events;
		if (!eventLists.containsKey(cls)) {
			events = Util.newArrayList();
			eventLists.put(cls, events);
		} else {
			events = eventLists.get(cls);
		}
		events.add(e);
		eventTypes.add(t);
		if (getContinuation() != null) {
			getContinuation().resume();
		}
	}
	
	public synchronized boolean hasEvents() {
		return eventTypes.size() > 0;
	}
	
	public synchronized RemoteEventType getNextEventType() {
		if (!hasEvents()) return null;
		return eventTypes.remove(0);
	}
	
	@SuppressWarnings("unchecked")
	public synchronized <T extends RemoteEvent> T getEvent(Class<T> cls) {
		List<T> messages = (List<T>)eventLists.get(cls);
		return messages.remove(0);
	}
	
	
	//
	// registry of all client event queues
	//
	
	private static final Map<String, ClientEventQueue> clients = Util.newHashMap();

	public static synchronized void connectClient(HttpSession session) {
		String id = session.getId();
		clients.put(id, new ClientEventQueue());
	}
	
	public static synchronized void disconnectClient(HttpSession session) {
		String id = session.getId();
		clients.remove(id);
	}
	
	public static synchronized ClientEventQueue forSession(HttpSession session) {
		String id = session.getId();
		return clients.get(id);
	}
	
	public static synchronized void addGlobalEvent(RemoteEvent e) {
		for (ClientEventQueue queue : clients.values()) {
			queue.addEvent(e);
		}
	}
}
