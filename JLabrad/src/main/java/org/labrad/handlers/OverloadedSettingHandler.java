package org.labrad.handlers;

import java.util.List;
import java.util.Map;

import org.labrad.SettingHandler;
import org.labrad.annotations.Setting;
import org.labrad.data.Data;
import org.labrad.types.Type;

public class OverloadedSettingHandler extends AbstractHandler {
  private final Map<Type, SettingHandler> typeMap;

  public OverloadedSettingHandler(Setting setting, List<String> acceptedTypes, List<String> returnedTypes, Map<Type, SettingHandler> typeMap) {
    super(null, setting, acceptedTypes, returnedTypes);
    this.typeMap = typeMap;
  }

  public Data handle(Object obj, Data data) throws Throwable {
    for (Type t : typeMap.keySet()) {
      if (data.matchesType(t)) {
        return typeMap.get(t).handle(obj, data);
      }
    }
    throw new RuntimeException("No matching handler found for type '" + data.getTag() + "'");
  }
}
