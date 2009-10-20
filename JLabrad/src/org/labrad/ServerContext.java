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
public interface ServerContext {
  void setSource(long source);
  long getSource();

  void setContext(Context context);
  Context getContext();

  void setServer(Server server);
  Server getServer();

  ServerContext getServerContext(Context context);

  void setConnection(Connection cxn);
  Connection getConnection();

  void init();
  void expire();
}
