package org.labrad;

import java.awt.EventQueue;
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
