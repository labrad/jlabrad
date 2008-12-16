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
public interface ContextualServer<T> {
    T newContext(Context context, long source);
    void expireContext(Context context, T data);
}
