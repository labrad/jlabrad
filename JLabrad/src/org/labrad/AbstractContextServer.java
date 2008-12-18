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
    @Override public void setSource(long source) { this.source = source; }
    @Override public long getSource() { return source; }

    private Context context;
    @Override public void setContext(Context context) { this.context = context; }
    @Override public Context getContext() { return context; }

    private Connection connection;
    @Override public void setConnection(Connection cxn) { this.connection = cxn; }
    @Override public Connection getConnection() { return connection; }
}
