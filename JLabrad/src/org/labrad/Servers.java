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

public class Servers {
  /*
   * Convenience method to run a server by passing in the server and context
   * classes to be instantiated.  This also takes any command line args, though
   * none are processed at present.
   */
  public static void runServer(Class<? extends Server> serverClass,
      Class<? extends ServerContext> contextClass,
      String[] args) {
    try {
      Server server = serverClass.newInstance();
      final ServerConnection cxn = ServerConnection.create(server, contextClass);
      cxn.connect();
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          //System.out.println("in shutdown hook.");
          cxn.triggerShutdown();
          //System.out.println("exiting shutdown hook.");
        }
      });
      cxn.serve();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
