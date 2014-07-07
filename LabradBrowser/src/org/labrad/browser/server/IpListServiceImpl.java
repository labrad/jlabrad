package org.labrad.browser.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.labrad.Connection;
import org.labrad.browser.LabradConnection;
import org.labrad.browser.client.IpAddress;
import org.labrad.browser.client.IpListService;
import org.labrad.data.Data;
import org.labrad.data.Request;

import com.google.common.collect.Lists;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class IpListServiceImpl extends RemoteServiceServlet implements IpListService {

  /**
   * Get a list of allowed and disallowed ip addresses.
   */
  public List<IpAddress> getIpList() {
    Request req = Request.to("Manager");
    int whitelist = req.addRecord("Whitelist");
    int blacklist = req.addRecord("Blacklist");

    List<Data> ans;
    try {
      ans = LabradConnection.get().sendAndWait(req);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }

    List<IpAddress> addrs = Lists.newArrayList();
    for (String addr : ans.get(whitelist).getStringList()) {
      addrs.add(new IpAddress(addr, true));
    }
    for (String addr : ans.get(blacklist).getStringList()) {
      addrs.add(new IpAddress(addr, false));
    }

    Collections.sort(addrs, new Comparator<IpAddress>() {
      public int compare(IpAddress o1, IpAddress o2) {
        return o1.getAddress().compareTo(o2.getAddress());
      }			
    });

    return addrs;
  }

  /**
   * Add an ip address to the blacklist
   */
  public List<IpAddress> addToBlacklist(String ip) {
    try {
      Connection cxn = LabradConnection.get();
      cxn.sendAndWait(Request.to("Manager").add("Blacklist", Data.valueOf(ip)));
      return getIpList();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Add an ip address to the whitelist
   */
  public List<IpAddress> addToWhitelist(String ip) {
    try {
      Connection cxn = LabradConnection.get();
      cxn.sendAndWait(Request.to("Manager").add("Whitelist", Data.valueOf(ip)));
      return getIpList();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
