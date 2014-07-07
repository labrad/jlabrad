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

public class Constants {
  /** Default hostname for the manager. */
  public static final String DEFAULT_HOST = "localhost";

  /** Default port to use when connecting to the manager. */
  public static final int DEFAULT_PORT = 7682;

  /** Default password to use when connecting to the manager. */
  public static final String DEFAULT_PASSWORD = "";

  /** ID of the LabRAD manager. */
  public static final long MANAGER = 1L;

  /** ID of the manager setting to retrieve a list of servers. */
  public static final long SERVERS = 1L;

  /** ID of the manager setting to retrieve a settings list for a server. */
  public static final long SETTINGS = 2L;

  /** ID of the lookup setting on the manager. */
  public static final long LOOKUP = 3L;

  /** Version number of the LabRAD protocol version implemented here. */
  public static final long PROTOCOL = 1L;

  /** Default context in which requests will be sent. */
  public static final Context DEFAULT_CONTEXT = new Context(0, 0);
}
