package org.labrad.browser.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("ip")
public interface IpListService extends RemoteService {
	public List<IpAddress> getIpList();
	public List<IpAddress> addToWhitelist(String ip);
	public List<IpAddress> addToBlacklist(String ip);
}
