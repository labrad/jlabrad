/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.labrad.data.Data;
import org.labrad.data.Request;

/**
 *
 * @author maffoo
 */
public interface Connection {
    Future<List<Data>> send(final Request request);
    Future<List<Data>> send(final Request request, final RequestCallback callback);
    List<Data> sendAndWait(Request request) throws InterruptedException, ExecutionException;
    void sendMessage(final Request request) throws InterruptedException, ExecutionException;
}
