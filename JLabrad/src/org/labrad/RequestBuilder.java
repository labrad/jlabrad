package org.labrad;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.data.Record;
import org.labrad.data.Request;

public class RequestBuilder {
  private Connection cxn;
  private String serverName;
  private long serverId;
  private Context context = Constants.DEFAULT_CONTEXT;
  private Request req;
  private List<Record> records;
  
  private RequestBuilder(Connection cxn) {
    this.cxn = cxn;
  }
  
  public RequestBuilder to(String serverName) {
    this.serverName = serverName;
    return this;
  }
  
  public RequestBuilder context(Context context) {
    this.context = context;
    return this;
  }
  
  public Future<Data> add(String string) {
    return null;
  }
  
  public void send(Connection cxn) {
    
  }
  
  public static RequestBuilder with(Connection cxn) {
    return new RequestBuilder(cxn);
  }
  
  private class RecordFuture implements Future<Data> {

    public boolean cancel(boolean mayInterruptIfRunning) {
      // TODO Auto-generated method stub
      return false;
    }

    public Data get() throws InterruptedException, ExecutionException {
      // TODO Auto-generated method stub
      return null;
    }

    public Data get(long timeout, TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException {
      // TODO Auto-generated method stub
      return null;
    }

    public boolean isCancelled() {
      // TODO Auto-generated method stub
      return false;
    }

    public boolean isDone() {
      // TODO Auto-generated method stub
      return false;
    }
    
  }
}
