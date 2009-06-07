package org.labrad.browser.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface RegistryServiceAsync {
	public void getListing(List<String> path, AsyncCallback<RegistryListing> callback);
	public void set(List<String> path, String key, String value, AsyncCallback<String> callback);
	public void createDirectory(List<String> path, AsyncCallback<RegistryListing> callback);
}
