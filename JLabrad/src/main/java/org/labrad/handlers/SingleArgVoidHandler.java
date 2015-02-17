package org.labrad.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.labrad.annotations.Setting;
import org.labrad.data.Data;
import org.labrad.data.Getter;

public class SingleArgVoidHandler extends AbstractHandler {
  @SuppressWarnings("rawtypes")
  private final Getter getter;

  @SuppressWarnings("rawtypes")
  public SingleArgVoidHandler(Method method, Setting setting, List<String> accepts, Getter getter) {
    super(method, setting, accepts, ZeroArgHandler.EMPTY_ONLY);
    this.getter = getter;
  }

  public Data handle(Object obj, Data data) throws Throwable {
    try {
      Object arg = getter != null ? getter.get(data) : data;
      getMethod().invoke(obj, arg);
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
