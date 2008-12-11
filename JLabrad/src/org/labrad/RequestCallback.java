/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import java.util.List;
import org.labrad.data.Data;
import org.labrad.data.Request;

/**
 *
 * @author maffoo
 */
public interface RequestCallback {
    /**
     * Called when the request completes successfully.
     * @param request
     * @param response
     */
    void onSuccess(Request request, List<Data> response);

    /**
     * Called when the request fails.
     * @param request
     * @param cause
     */
    void onFailure(Request request, Throwable cause);
}
