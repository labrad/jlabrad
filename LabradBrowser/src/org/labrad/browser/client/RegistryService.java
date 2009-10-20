package org.labrad.browser.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("registry")
public interface RegistryService extends RemoteService {
  public RegistryListing getListing(List<String> path);
  public RegistryListing set(List<String> path, String key, String value) throws RegistryError;
  public RegistryListing del(List<String> path, String key) throws RegistryError;
  public RegistryListing mkdir(List<String> path, String dir) throws RegistryError;
  public RegistryListing rmdir(List<String> path, String dir) throws RegistryError;
}
