package org.labrad.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.labrad.annotations.Setting;
import org.labrad.data.Data;
import org.labrad.data.Getter;

public class SingleArgHandler extends AbstractHandler {
	private final Getter getter;
	
	public SingleArgHandler(Method method, Setting setting, List<String> acceptedTypes, Getter getter) {
		super(method, setting, acceptedTypes);
		this.getter = getter;
	}
	
	public Data handle(Object obj, Data data) throws Throwable {
		try {
			Object arg = getter != null ? getter.get(data) : data;
			return (Data) getMethod().invoke(obj, arg);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
}
