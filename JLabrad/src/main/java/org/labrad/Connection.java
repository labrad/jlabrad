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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Request;
import org.labrad.events.MessageListener;

/**
 *
 * @author maffoo
 */
public interface Connection {
  Future<List<Data>> send(final Request request);
  Future<List<Data>> send(final Request request, final RequestCallback callback);
  List<Data> sendAndWait(Request request) throws InterruptedException, ExecutionException;
  void sendMessage(final Request request) throws InterruptedException, ExecutionException;

  public void addMessageListener(MessageListener listener);
  public void removeMessageListener(MessageListener listener);

  public Context newContext();
  public long getId();
}
