package org.labrad.handlers;

import java.lang.reflect.Method;

import org.labrad.SettingHandler;
import org.labrad.annotations.Setting;

public abstract class AbstractHandler implements SettingHandler {
	Method method;
	Setting setting;
	
	public AbstractHandler(Method method, Setting setting) {
		this.method = method;
		this.setting = setting;
	}
	
	protected Method getMethod() {
		return method;
	}
	
	public Setting getSettingInfo() {
		return setting;
	}
}
