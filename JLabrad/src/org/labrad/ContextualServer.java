/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.labrad;

import org.labrad.data.Context;


/**
 *
 * @author maffoo
 */
public interface ContextualServer {
	void setSource(long source);
	void setContext(Context context);
}
