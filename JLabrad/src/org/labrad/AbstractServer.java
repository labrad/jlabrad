package org.labrad;

import org.labrad.data.Context;

public abstract class AbstractServer implements Server {

  private Connection connection;

  public Connection getConnection() {
    return connection;
  }

  public void setConnection(Connection cxn) {
    connection = cxn;
  }

  public ServerContext getServerContext(Context context) {
    // FIXME hack to allow contexts to communicate
    ServerConnection cxn = (ServerConnection)connection;
    return cxn.getServerContext(context);
  }

  public abstract void init();
  public abstract void shutdown();

}
