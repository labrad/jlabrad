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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.SwingUtilities;

import org.labrad.data.Data;
import org.labrad.data.Packet;
import org.labrad.data.Record;
import org.labrad.data.Request;
import org.labrad.errors.LabradException;

/**
 * Represents a pending LabRAD request.
 */
class RequestReceiver implements Future<List<Data>> {

  /** Status of this request. */
  private enum RequestStatus {
    PENDING, DONE, FAILED, CANCELLED
  }

  private Request request;
  private RequestCallback callback = null;
  private RequestStatus status = RequestStatus.PENDING;
  private List<Data> response;
  private Throwable cause;

  /**
   * Create a receiver for the given request.
   * @param request the LabRAD request that will be sent
   */
  RequestReceiver(Request request) {
    this(request, null);
  }

  /**
   * Create a receiver for the given request with a callback
   * to be called when completed.
   * @param request the LabRAD request that will be sent
   * @param callback to be called when the request completes or fails
   */
  RequestReceiver(Request request, RequestCallback callback) {
    this.request = request;
    this.callback = callback;
  }

  /**
   * Cancel this request.  If mayInterruptIfRunning is true, this
   * will immediately interrupt any threads waiting on this object,
   * notifying them of the cancellation.  Note that this does not
   * actually send any cancellation information to the server against
   * whom the request was made.
   * @param mayInterruptIfRunning a boolean indicating whether
   * to interrupt threads that are waiting to get the result
   * @return true if the request was cancelled
   */
  public synchronized boolean cancel(boolean mayInterruptIfRunning) {
    boolean cancelled = false;
    if (status == RequestStatus.PENDING) {
      status = RequestStatus.CANCELLED;
      cause = new CancellationException();
      cancelled = true;
      if (mayInterruptIfRunning) {
        notifyAll();
      }
      callbackFailure();
    }
    return cancelled;
  }

  /**
   * Wait for the request to complete and get the result.
   * @return the Data received in response to the original request
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @throws ExecutionException if an error occurred while making the request
   */
  public synchronized List<Data> get() throws InterruptedException, ExecutionException {
    while (!isDone()) {
      wait();
    }
    switch (status) {
      case CANCELLED:
        throw new CancellationException();
      case FAILED:
        throw new ExecutionException(cause);
      default:
    }
    return response;
  }

  /**
   * Wait for the specified amount of time for the request to complete.
   * @param duration the amount of time to wait
   * @param timeUnit the unit interval of time in which duration is specified
   * @return the Data received in response to the original request
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @throws ExecutionException if an error occurred while making the request
   * @throws TimeoutException if the request did not complete in the specified time
   */
  public synchronized List<Data> get(long duration, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
    while (!isDone()) {
      wait(TimeUnit.MILLISECONDS.convert(duration, timeUnit));
    }
    switch (status) {
      case CANCELLED:
        throw new CancellationException();
      case FAILED:
        throw new ExecutionException(cause);
      default:
    }
    return response;
  }

  /**
   * Returns true if this request was cancelled before it completed normally.
   * @return true if the request was cancelled before it completed
   */
  public synchronized boolean isCancelled() {
    return status == RequestStatus.CANCELLED;
  }

  /**
   * Returns true if this request is completed, due either to cancellation, normal
   * termination, or an ExecutionException
   * @return true if the request is completed
   */
  public synchronized boolean isDone() {
    return status != RequestStatus.PENDING;
  }

  /**
   * Set the result of this request.  Depending on whether the response
   * packet contains error records, this may result in either successful
   * completion, or in failure and an ExecutionException being created.
   * @param packet the LabRAD response packet received for this request
   */
  protected synchronized void set(Packet packet) {
    if (!isCancelled()) {
      boolean failed = false;
      List<Data> resp = new ArrayList<Data>();
      for (Record rec : packet.getRecords()) {
        Data data = rec.getData();
        if (data.isError()) {
          failed = true;
          cause = new LabradException(data);
          break;
        } else {
          resp.add(data);
        }
      }
      response = resp;
      status = failed ? RequestStatus.FAILED : RequestStatus.DONE;
      if (failed) {
        callbackFailure();
      } else {
        callbackSuccess();
      }
    }
    notifyAll();
  }

  /**
   * Record the failure of this request.
   * @param theCause
   */
  protected synchronized void fail(Throwable theCause) {
    status = RequestStatus.FAILED;
    cause = theCause;
    callbackFailure();
    notifyAll();
  }

  /**
   * Trigger the onSuccess callback of this request.
   */
  private void callbackSuccess() {
    doCallback(new Runnable() {
      public void run() {
        callback.onSuccess(request, response);
      }
    });
  }

  /**
   * Trigger the onFailure callback of this request.
   */
  private void callbackFailure() {
    doCallback(new Runnable() {
      public void run() {
        callback.onFailure(request, cause);
      }
    });
  }

  /**
   * Dispatch a callback by scheduling it to be invoked later.
   * We use the standard SwingUtilities.invokeLater mechanism to do this.
   * @param runnable the object to schedule for running by the event loop
   */
  private void doCallback(Runnable runnable) {
    if (callback != null) {
      SwingUtilities.invokeLater(runnable);
    }
  }
}
