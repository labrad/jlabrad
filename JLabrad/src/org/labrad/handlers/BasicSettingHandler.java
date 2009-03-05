package org.labrad.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.labrad.Setting;
import org.labrad.data.Data;

public class BasicSettingHandler extends AbstractHandler {
	public BasicSettingHandler(Method method, Setting setting) {
		super(method, setting);
	}
	
	public Data handle(Object obj, Data data) throws Throwable {
		try {
			return (Data) getMethod().invoke(obj, data);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
}
