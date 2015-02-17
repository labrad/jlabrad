package org.labrad.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.labrad.annotations.Setting;
import org.labrad.data.Data;
import org.labrad.data.Getter;

public class MultiArgHandler extends AbstractHandler {
  @SuppressWarnings("rawtypes")
  private final List<Getter> getters;
  private final int nArgs;

  @SuppressWarnings("rawtypes")
  public MultiArgHandler(Method method, Setting setting, List<String> acceptedTypes, List<String> returnedTypes, List<Getter> getters) {
    super(method, setting, acceptedTypes, returnedTypes);
    this.getters = getters;
    nArgs = getters.size();
  }

  @SuppressWarnings("rawtypes")
  public Data handle(Object obj, Data data) throws Throwable {
    try {
      Object[] args = new Object[nArgs];
      for (int i = 0; i < nArgs; i++) {
        Getter g = getters.get(i);
        Data arg = data.get(i);
        args[i] = g != null ? g.get(arg) : arg;
      }
      return (Data) getMethod().invoke(obj, args);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (IllegalAccessException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }
}
