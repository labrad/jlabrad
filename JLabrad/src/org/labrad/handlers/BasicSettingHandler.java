package org.labrad.handlers;

import java.lang.reflect.Method;

import org.labrad.Setting;
import org.labrad.data.Data;

public class BasicSettingHandler extends AbstractHandler {
	public BasicSettingHandler(Method method, Setting setting) {
		super(method, setting);
	}
	
	public Data handle(Object obj, Data data) throws Exception {
		return (Data) getMethod().invoke(obj, data);
	}
}
