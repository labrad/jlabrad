package org.labrad.browser.client.event;

import java.util.List;

import org.labrad.browser.client.Util;

public class RemoteEventSupport<T extends RemoteEvent> {
  private List<RemoteEventHandler<T>> handlers = Util.newArrayList();

  public void add(RemoteEventHandler<T> handler) {
    handlers.add(handler);
  }

  public void remove(RemoteEventHandler<T> handler) {
    handlers.remove(handler);
  }

  public void onEvent(T event) {
    for (RemoteEventHandler<T> handler : handlers) {
      handler.onEvent(event);
    }
  }

  /**
   * Generic factory to create RemoteEventSupport for a new RemoteEvent type
   * @param <T>
   * @return
   */
  public static <T extends RemoteEvent> RemoteEventSupport<T> create() {
    return new RemoteEventSupport<T>();
  }
}
