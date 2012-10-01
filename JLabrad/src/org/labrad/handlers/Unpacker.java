package org.labrad.handlers;

import java.util.List;

import org.labrad.data.Data;
import org.labrad.data.Getter;

public class Unpacker {
  @SuppressWarnings("rawtypes")
  private final Getter[] getters;
  private final int n;

  @SuppressWarnings("rawtypes")
  public Unpacker(List<Getter> getters) {
    n = getters.size();
    this.getters = new Getter[n];
    for (int i = 0; i < n; i++) {
      this.getters[i] = getters.get(i);
    }
  }

  public Object[] unpack(Data data) {
    Object[] args = new Object[n];
    for (int i = 0; i < n; i++) {
      args[i] = getters[i].get(data.get(i));
    }
    return args;
  }
}
