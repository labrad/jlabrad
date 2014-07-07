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

import org.labrad.data.Context;

/**
 *
 * @author maffoo
 */
public abstract class AbstractServerContext implements ServerContext {
  public AbstractServerContext() {}

  private long source;
  public void setSource(long source) { this.source = source; }
  public long getSource() { return source; }

  private Context context;
  public void setContext(Context context) { this.context = context; }
  public Context getContext() { return context; }

  private Server server;
  public void setServer(Server server) { this.server = server; }
  public Server getServer() { return server; }

  public ServerContext getServerContext(Context context) {
    return getServer().getServerContext(context);
  }

  private Connection connection;
  public void setConnection(Connection cxn) { connection = cxn; }
  public Connection getConnection() { return connection; }

  public abstract void init();
  public abstract void expire();
}
