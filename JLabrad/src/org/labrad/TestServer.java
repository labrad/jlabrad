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

import org.labrad.annotations.ServerInfo;

/*
 * A very basic skeleton for a server to test the JLabrad API.
 * 
 * Most of the logic for this server is in the TestServerContext class.
 */
@ServerInfo(name = "Java Test Server",
            doc = "Basic server to test JLabrad API.",
            notes = "Not much else to say, really.")
public class TestServer extends AbstractServer {
  public void init() {
    System.out.println("init() called on server.");
  }

  public void shutdown() {
    System.out.println("shutdown() called on server.");
  }

  public static void main(String[] args) {
    Servers.runServer(TestServer.class, TestServerContext.class, args);
  }
}
