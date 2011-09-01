package org.labrad.test;

import java.util.List;

import org.labrad.Client;
import org.labrad.RequestCallback;
import org.labrad.data.Data;
import org.labrad.data.Request;

public class RequestTest {
  public static void main(String[] args) throws Exception {
    Client client = new Client();
    client.connect();
    
    try {
      testSuccess(client, Request.to("Manager").add("Servers"));
      testFailure(client, Request.to("Manager").add("Non-existent setting"));
      testFailure(client, Request.to("Non-existent Server").add("Non-existent setting"));
    } finally {
      client.close();
    }
  }
  
  static void testFailure(Client client, Request request) throws InterruptedException {
    TestCallback callback = new TestCallback();
    client.send(request, callback);
    Thread.sleep(2000);
    callback.assertOnFailureCalled();
  }
  
  static void testSuccess(Client client, Request request) throws InterruptedException {
    TestCallback callback = new TestCallback();
    client.send(request, callback);
    Thread.sleep(2000);
    callback.assertOnSuccessCalled();
  }
  
  static class TestCallback implements RequestCallback {
    private boolean _onSuccessCalled = false;
    private boolean _onFailureCalled = false;
    
    public void onSuccess(Request request, List<Data> response) {
      if (_onSuccessCalled || _onFailureCalled) throw new RuntimeException("TestCallback already called");
      _onSuccessCalled = true;
    }

    public void onFailure(Request request, Throwable cause) {
      if (_onSuccessCalled || _onFailureCalled) throw new RuntimeException("TestCallback already called");
      _onFailureCalled = true;
    }
    
    public void assertOnSuccessCalled() {
      if (!_onSuccessCalled) throw new RuntimeException("onSuccess not called");
      if (_onFailureCalled) throw new RuntimeException("onFailure called");
    }
    
    public void assertOnFailureCalled() {
      if (_onSuccessCalled) throw new RuntimeException("onSuccess called");
      if (!_onFailureCalled) throw new RuntimeException("onFailure not called");
    }
  }
}
