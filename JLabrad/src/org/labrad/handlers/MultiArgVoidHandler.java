package org.labrad.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.labrad.annotations.Setting;
import org.labrad.data.Data;
import org.labrad.data.Getter;

public class MultiArgVoidHandler extends AbstractHandler {
	@SuppressWarnings("unchecked")
	private final List<Getter> getters;
	private final int nArgs;
	
	@SuppressWarnings("unchecked")
	public MultiArgVoidHandler(Method method, Setting setting, List<String> acceptedTypes, List<Getter> getters) {
		super(method, setting, acceptedTypes);
		this.getters = getters;
		nArgs = getters.size();
	}
	
	@SuppressWarnings("unchecked")
	public Data handle(Object obj, Data data) throws Throwable {
		try {
			Object[] args = new Object[nArgs];
			for (int i = 0; i < nArgs; i++) {
				Getter g = getters.get(i);
				args[i] = g != null ? g.get(data.get(i)) : data;
			}
			getMethod().invoke(obj, data);
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
