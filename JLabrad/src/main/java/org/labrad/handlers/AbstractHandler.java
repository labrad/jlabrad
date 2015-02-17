package org.labrad.handlers;

import java.lang.reflect.Method;
import java.util.List;

import org.labrad.SettingHandler;
import org.labrad.annotations.Setting;
import org.labrad.data.Data;

public abstract class AbstractHandler implements SettingHandler {
  private final Method method;
  private final Setting setting;
  private final List<String> accepts;
  private final List<String> returns;

  public AbstractHandler(Method method, Setting setting, List<String> accepts, List<String> returns) {
    this.method = method;
    this.setting = setting;
    this.accepts = accepts;
    this.returns = returns;
  }

  protected Method getMethod() {
    return method;
  }

  public Setting getSettingInfo() {
    return setting;
  }

  public Data getRegistrationInfo() {
    Data data = Data.ofType("wss*s*ss");
    data.get(0).setWord(setting.id());
    data.get(1).setString(setting.name());
    data.get(2).setString(setting.doc());
    data.get(3).setStringList(accepts);
    data.get(4).setStringList(returns);
    data.get(5).setString(setting.notes());
    return data;
  }
}
