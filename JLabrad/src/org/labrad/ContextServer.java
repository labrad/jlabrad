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
public interface ContextServer {
	void setSource(long source);
    long getSource();

	void setContext(Context context);
    Context getContext();

    void setConnection(Connection cxn);
    Connection getConnection();
}
