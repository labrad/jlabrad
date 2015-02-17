package org.labrad.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.labrad.annotations.Setting;
import org.labrad.data.Data;

public class ZeroArgHandler extends AbstractHandler {
  public final static List<String> EMPTY_ONLY = new ArrayList<String>();

  static {
    EMPTY_ONLY.add("");
  }

  public ZeroArgHandler(Method method, Setting setting, List<String> returnedTypes) {
    super(method, setting, EMPTY_ONLY, returnedTypes);
  }

  public Data handle(Object obj, Data data) throws Throwable {
    try {
      return (Data) getMethod().invoke(obj);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (IllegalAccessException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }
}
