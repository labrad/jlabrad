package org.labrad.grapher.client;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class DirectoryListing implements Serializable {
  private List<String> dirs;
  private List<String> datasets;
  
  protected DirectoryListing() {}
  
  public DirectoryListing(List<String> dirs, List<String> datasets) {
    this.dirs = dirs;
    this.datasets = datasets;
  }
    
  public List<String> getDirs() { return dirs; }
  public List<String> getDatasets() { return datasets; }
}
