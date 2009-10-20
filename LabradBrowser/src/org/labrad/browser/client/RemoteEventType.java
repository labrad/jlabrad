package org.labrad.browser.client;

import java.util.Map;

public enum RemoteEventType {
  LABRAD_CONNECT(LabradConnectEvent.class),
  LABRAD_DISCONNECT(LabradDisconnectEvent.class),

  SERVER_CONNECT(ServerConnectEvent.class),
  SERVER_DISCONNECT(ServerDisconnectEvent.class),

  NODE_REQUEST_FAILED(NodeRequestFailedException.class),
  NODE_SERVER_STARTING(NodeServerStartingEvent.class),
  NODE_SERVER_STARTED(NodeServerStartedEvent.class),
  NODE_SERVER_STOPPING(NodeServerStoppingEvent.class),
  NODE_SERVER_STOPPED(NodeServerStoppedEvent.class),

  NODE_STATUS(NodeStatusEvent.class);

  private final Class<? extends RemoteEvent> cls;
  private static final Map<Class<?>, RemoteEventType> map = Util.newHashMap();

  RemoteEventType(Class<? extends RemoteEvent> cls) {
    this.cls = cls;
  }

  static {
    for (RemoteEventType t : values()) {
      map.put(t.cls, t);
    }
  }

  public static RemoteEventType forClass(Class<?> cls) {
    return map.get(cls);
  }
}
