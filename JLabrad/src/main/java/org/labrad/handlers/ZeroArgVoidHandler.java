package org.labrad.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.labrad.annotations.Setting;
import org.labrad.data.Data;

public class ZeroArgVoidHandler extends AbstractHandler {
  public ZeroArgVoidHandler(Method method, Setting setting) {
    super(method, setting, ZeroArgHandler.EMPTY_ONLY, ZeroArgHandler.EMPTY_ONLY);
  }

  public Data handle(Object obj, Data data) throws Throwable {
    try {
      getMethod().invoke(obj);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (IllegalAccessException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
    return Data.EMPTY;
  }
}
