package org.labrad.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.labrad.annotations.Setting;
import org.labrad.data.Data;

public class ZeroArgVoidHandler extends AbstractHandler {
	private final static List<String> EMPTY_ONLY = new ArrayList<String>();
	
	static {
		EMPTY_ONLY.add("");
	}
	
	public ZeroArgVoidHandler(Method method, Setting setting) {
		super(method, setting, EMPTY_ONLY);
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
