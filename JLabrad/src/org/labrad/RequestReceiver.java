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

    RequestReceiver(Request request) {
        this(request, null);
    }

    RequestReceiver(Request request, RequestCallback callback) {
        this.request = request;
        this.callback = callback;
    }

    /**
     * Cancel this request.
     * @return true if the request was cancelled
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = false;
        if (status == RequestStatus.PENDING) {
            status = RequestStatus.CANCELLED;
            if (mayInterruptIfRunning) {
                notifyAll();
            }
            cancelled = true;
            cause = new CancellationException();
            callbackFailure();
        }
        return cancelled;
    }

    @Override
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

    @Override
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

    @Override
    public synchronized boolean isCancelled() {
        return status == RequestStatus.CANCELLED;
    }

    @Override
    public synchronized boolean isDone() {
        return status != RequestStatus.PENDING;
    }

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

    protected synchronized void fail(Throwable theCause) {
        cause = theCause;
        status = RequestStatus.FAILED;
        callbackFailure();
        notifyAll();
    }

    private void callbackSuccess() {
        doCallback(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(request, response);
            }
        });
    }

    private void callbackFailure() {
        doCallback(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(request, cause);
            }
        });
    }

    private void doCallback(Runnable runnable) {
        if (callback != null) {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
