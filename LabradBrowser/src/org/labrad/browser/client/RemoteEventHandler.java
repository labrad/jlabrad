package org.labrad.browser.client;

public interface RemoteEventHandler<T extends RemoteEvent> {
	public void onEvent(T event);
}
