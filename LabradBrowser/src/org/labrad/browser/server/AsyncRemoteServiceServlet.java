/*
 * Copyright 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.labrad.browser.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;

/**
 * Jetty Continuation support for GWT RPC.
 * 
 * @author Craig Day (craig@alderaan.com.au)
 */
@SuppressWarnings("serial")
public class AsyncRemoteServiceServlet extends RemoteServiceServlet {

  public static final String PAYLOAD = "com.google.gwt.payload";

  private static final String JETTY_RETRY_REQUEST_EXCEPTION = "org.mortbay.jetty.RetryRequest";

  /* ------------------------------------------------------------ */
  /* (non-Javadoc)
   * @see com.google.gwt.user.server.rpc.RemoteServiceServlet#readContent(javax.servlet.http.HttpServletRequest)
   */
  protected String readContent(HttpServletRequest request) throws IOException, ServletException {
    String payload = (String) request.getAttribute(PAYLOAD);
    if (payload == null) {
      payload = super.readContent(request);
      request.setAttribute(PAYLOAD, payload);
    }
    return payload;
  }


  /**
   * Overridden to really throw Jetty RetryRequest Exception (as opposed to sending failure to client).
   *
   * @param caught the exception
   */
  protected void doUnexpectedFailure(Throwable caught) {
    throwIfRetryRequest(caught);
    super.doUnexpectedFailure(caught);
  }

  /**
   * Throws the Jetty RetryRequest if found.
   *
   * @param caught the exception
   */
  protected void throwIfRetryRequest(Throwable caught) {
    if (caught instanceof UnexpectedException) {
      caught = caught.getCause();
    }

    if (JETTY_RETRY_REQUEST_EXCEPTION.equals(caught.getClass().getName())) {
      throw (RuntimeException) caught;
    }
  }


}
