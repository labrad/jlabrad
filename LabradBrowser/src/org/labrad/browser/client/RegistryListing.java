package org.labrad.browser.client;

import java.io.Serializable;
import java.util.List;


@SuppressWarnings("serial")
public class RegistryListing implements Serializable {
  private List<String> path;
  private List<String> dirs;
  private List<String> keys;
  private List<String> vals;

  protected RegistryListing() {}
  public RegistryListing(List<String> path, List<String> dirs,
      List<String> keys, List<String> vals) {
    this.path = path;
    this.dirs = dirs;
    this.keys = keys;
    this.vals = vals;
  }

  public List<String> getPath() { return path; }
  public List<String> getDirs() { return dirs; }	
  public List<String> getKeys() { return keys; }
  public List<String> getVals() { return vals; }
}
