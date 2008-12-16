/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.labrad.data.Data;
import org.labrad.data.Request;

/**
 *
 * @author maffoo
 */
public interface Connection {
    List<Data> sendAndWait(Request request)
            throws InterruptedException, ExecutionException;
}
