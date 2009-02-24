package org.labrad.handlers;

import java.lang.reflect.Method;

import org.labrad.Setting;
import org.labrad.data.Data;

public class NoArgNoReturnHandler extends AbstractHandler {
	public NoArgNoReturnHandler(Method method, Setting setting) {
		super(method, setting);
	}
	
	public Data handle(Object obj, Data data) throws Exception {
		getMethod().invoke(obj);
		return Data.EMPTY;
	}
}
