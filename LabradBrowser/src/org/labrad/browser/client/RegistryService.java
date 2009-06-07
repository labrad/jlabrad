package org.labrad.browser.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("registry")
public interface RegistryService extends RemoteService {
	public RegistryListing getListing(List<String> path);
	public String set(List<String> path, String key, String value) throws RegistryError;
	public RegistryListing createDirectory(List<String> path);
}
