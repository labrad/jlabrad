package org.labrad.grapher.client;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class DatasetInfo implements Serializable {
  private List<String> path;
  private String name;
  private int num;
  
  private List<String> independents;
  private List<String> dependents;
  
  private Map<String, String> parameters;
  
  protected DatasetInfo() {}
  
  public DatasetInfo(List<String> path, String name, int num, List<String> indeps, List<String> deps,
      Map<String, String> params) {
    this.path = path;
    this.name = name;
    this.num = num;
    this.independents = indeps;
    this.dependents = deps;
    this.parameters = params;
  }
  
  public List<String> getPath() { return path; }
  public String getName() { return name; }
  public int getNum() { return num; }
  public List<String> getIndependents() { return independents; }
  public List<String> getDependents() { return dependents; }
  public Map<String, String> getParameters() { return parameters; }
}
