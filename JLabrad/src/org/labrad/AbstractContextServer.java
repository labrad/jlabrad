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
public abstract class AbstractContextServer implements ContextServer {
    public AbstractContextServer() {}

    private long source;
    public void setSource(long source) { this.source = source; }
    public long getSource() { return source; }

    private Context context;
    public void setContext(Context context) { this.context = context; }
    public Context getContext() { return context; }

    private Connection connection;
    public void setConnection(Connection cxn) { this.connection = cxn; }
    public Connection getConnection() { return connection; }
}
