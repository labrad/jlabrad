package org.labrad;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.labrad.data.Data;
import org.labrad.data.Request;

public abstract class AbstractPacket implements Sendable {
  private Connection cxn;
  private Request req;
  private Future<List<Data>> ans;
  private boolean sent = false;
  
  public AbstractPacket(Connection cxn, Request req) {
    this.cxn = cxn;
    this.req = req;
  }
  
  public void send() {
    sent = true;
    ans = cxn.send(req);
  }
  
  public boolean isSent() {
    return sent;
  }
  
  protected List<Data> getResult() {
    try {
      return ans.get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
