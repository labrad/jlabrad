package org.labrad.grapher.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("vault")
public interface DataVaultService extends RemoteService {
  DirectoryListing getListing(List<String> path) throws Exception;
  DatasetInfo getDatasetInfo(List<String> path, String dataset) throws Exception;
  DatasetInfo getDatasetInfo(List<String> path, int dataset) throws Exception;
  double[][] getData(List<String> path, String dataset) throws Exception;
  double[][] getData(List<String> path, int dataset) throws Exception;
}
