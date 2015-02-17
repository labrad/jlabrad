package org.labrad.types;

import org.labrad.data.Data;

public interface Unflattenable<T> {
  public T unflatten(Data data);
}
