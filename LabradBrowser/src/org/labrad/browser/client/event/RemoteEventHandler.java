package org.labrad.browser.client.event;

public interface RemoteEventHandler<T extends RemoteEvent> {
  public void onEvent(T event);
}
