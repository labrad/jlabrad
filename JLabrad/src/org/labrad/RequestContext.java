/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import org.labrad.data.Context;
import org.labrad.data.Data;

/**
 *
 * @author maffoo
 */
public class RequestContext<T> {

    private Context context;
    private long source;
    private T data;

    public RequestContext(Context context, long source, T data) {
        this.context = context;
        this.source = source;
        this.data = data;
    }

    public long getSource() {
        return source;
    }

    public Context getContext() {
        return context;
    }

    public T getData() {
        return data;
    }

    public void finish(Data data) {
        
    }
}
