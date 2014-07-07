package org.labrad.grapher.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.labrad.data.Data;
import org.labrad.data.Request;
import org.labrad.data.Setters;
import org.labrad.grapher.LabradConnection;
import org.labrad.grapher.client.DataVaultService;
import org.labrad.grapher.client.DatasetInfo;
import org.labrad.grapher.client.DirectoryListing;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class DataVaultServiceImpl extends RemoteServiceServlet implements
    DataVaultService {

  private List<String> sanitizePath(List<String> path) {
    if (path.size() == 0 || !"".equals(path.get(0))) {
      path = new ArrayList<String>(path);
      path.add(0, "");
    }
    return path;
  }
  
  @Override
  public DirectoryListing getListing(List<String> path) throws InterruptedException, ExecutionException {
    path = sanitizePath(path);
    Request req = Request.to("Data Vault");
    req.add("cd", Data.listOf(path, Setters.stringSetter));
    int idx = req.addRecord("dir");
    Data answer = LabradConnection.get().sendAndWait(req).get(idx);
    List<String> dirs = answer.get(0).getStringList();
    List<String> datasets = answer.get(1).getStringList();
    return new DirectoryListing(dirs, datasets);
  }

  @Override
  public DatasetInfo getDatasetInfo(List<String> path, String dataset)
      throws InterruptedException, ExecutionException {
    path = sanitizePath(path);
    Request req = Request.to("Data Vault");
    req.add("cd", Data.listOf(path, Setters.stringSetter));
    int idxOpen = req.addRecord("open", Data.valueOf(dataset));
    int idxVars = req.addRecord("variables");
    int idxParams = req.addRecord("get parameters");
    
    List<Data> answer = LabradConnection.get().sendAndWait(req);
    return parseDatasetInfo(idxOpen, idxVars, idxParams, answer);
  }

  @Override
  public DatasetInfo getDatasetInfo(List<String> path, int dataset)
      throws Exception {
    path = sanitizePath(path);
    Request req = Request.to("Data Vault");
    req.add("cd", Data.listOf(path, Setters.stringSetter));
    int idxOpen = req.addRecord("open", Data.valueOf((long)dataset));
    int idxVars = req.addRecord("variables");
    int idxParams = req.addRecord("get parameters");
    
    List<Data> answer = LabradConnection.get().sendAndWait(req);
    return parseDatasetInfo(idxOpen, idxVars, idxParams, answer);
  }

  private DatasetInfo parseDatasetInfo(int idxOpen,
      int idxVars, int idxParams, List<Data> answer) {
    List<String> path = answer.get(idxOpen).get(0).getStringList();
    String name = answer.get(idxOpen).get(1).getString();
    int num = Integer.valueOf(name.substring(0, 5));
    
    List<String> indeps = new ArrayList<String>();
    List<String> deps = new ArrayList<String>();
    Data varData = answer.get(idxVars);
    for (int i = 0; i < varData.get(0).getArraySize(); i++) {
      indeps.add(varData.get(0, i, 0).getString());
    }
    for (int i = 0; i < varData.get(1).getArraySize(); i++) {
      deps.add(varData.get(1, i, 0).getString());
    }
    
    Map<String, String> params = new HashMap<String, String>();
    Data paramData = answer.get(idxParams);
    if (paramData.isCluster()) { // might be Empty if there are no params
      for (int i = 0; i < paramData.getClusterSize(); i++) {
        Data item = paramData.get(i);
        params.put(item.get(0).getString(), item.get(1).pretty());
      }
    }
    return new DatasetInfo(path, name, num, indeps, deps, params);
  }

  @Override
  public double[][] getData(List<String> path, String dataset)
      throws InterruptedException, ExecutionException {
    path = sanitizePath(path);
    Request req = Request.to("Data Vault");
    req.add("cd", Data.listOf(path, Setters.stringSetter));
    req.add("open", Data.valueOf(dataset));
    int idx = req.addRecord("get");
    
    Data answer = LabradConnection.get().sendAndWait(req).get(idx);
    return parseData(answer);
  }

  @Override
  public double[][] getData(List<String> path, int dataset) throws Exception {
    path = sanitizePath(path);
    Request req = Request.to("Data Vault");
    req.add("cd", Data.listOf(path, Setters.stringSetter));
    req.add("open", Data.valueOf((long)dataset));
    int idx = req.addRecord("get");
    
    Data answer = LabradConnection.get().sendAndWait(req).get(idx);
    return parseData(answer);
  }

  private double[][] parseData(Data answer) {
    int[] shape = answer.getArrayShape();
    double[][] data = new double[shape[0]][shape[1]];
    for (int i = 0; i < shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        data[i][j] = answer.get(i, j).getValue();
      }
    }
    return data;
  }
}
