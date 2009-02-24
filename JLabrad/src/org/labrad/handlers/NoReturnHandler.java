package org.labrad.handlers;

import java.lang.reflect.Method;

import org.labrad.Setting;
import org.labrad.data.Data;

public class NoReturnHandler extends AbstractHandler {
	public NoReturnHandler(Method method, Setting setting) {
		super(method, setting);
	}
	
	public Data handle(Object obj, Data data) throws Exception {
		getMethod().invoke(obj, data);
		return Data.EMPTY;
	}
}
