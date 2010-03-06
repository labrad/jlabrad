package org.labrad.grapher.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DataVaultServiceAsync {

  void getListing(List<String> path, AsyncCallback<DirectoryListing> callback);

  void getDatasetInfo(List<String> path, String dataset,
      AsyncCallback<DatasetInfo> callback);

  void getData(List<String> path, String dataset,
      AsyncCallback<double[][]> callback);

  void getDatasetInfo(List<String> path, int dataset,
      AsyncCallback<DatasetInfo> callback);

  void getData(List<String> path, int dataset,
      AsyncCallback<double[][]> callback);

}
