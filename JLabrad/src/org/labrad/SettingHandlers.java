/*
 * Copyright 2008 Matthew Neeley
 * 
 * This file is part of JLabrad.
 *
 * JLabrad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JLabrad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JLabrad.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.labrad;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.labrad.annotations.Setting;
import org.labrad.data.Getter;
import org.labrad.data.Getters;
import org.labrad.handlers.MultiArgHandler;
import org.labrad.handlers.MultiArgVoidHandler;
import org.labrad.handlers.SingleArgHandler;
import org.labrad.handlers.SingleArgVoidHandler;
import org.labrad.handlers.ZeroArgHandler;
import org.labrad.handlers.ZeroArgVoidHandler;

public class SettingHandlers {
	@SuppressWarnings("unchecked")
	public static SettingHandler forMethod(Method m) {
		// make sure this method has a setting annotation
		if (!m.isAnnotationPresent(Setting.class)) {
			Failure.fail("Method '%s' needs @Setting annotation.", m.getName());
		}
		Setting s = m.getAnnotation(Setting.class);
		
		int numArgs = m.getParameterTypes().length;
		List<String> accepts = new ArrayList<String>();
		List<Getter> getters = new ArrayList<Getter>();
		
		boolean isVoid = (m.getReturnType() == Void.TYPE);
		
		if (numArgs > 0) {
			// build a list of translators for the arguments
			// build a list of accepted types
			for (Class<?> t : m.getParameterTypes()) {
				Getter getter = null;
				if (t.equals(Boolean.TYPE) || t.equals(Boolean.class)) {
					getter = Getters.boolGetter; 
				} else if (t.equals(Integer.TYPE) || t.equals(Integer.class)) {
					getter = Getters.intGetter;
				} else if (t.equals(Long.TYPE) || t.equals(Long.class)) {
					getter = Getters.wordGetter;
				} else if (t.equals(String.class)) {
					getter = Getters.stringGetter;
				}
				getters.add(getter);
			}
			
		}
		
		// create a handler of the appropriate type for this setting
		switch (numArgs) {
		case 0:
			if (!isVoid) {
				return new ZeroArgHandler(m, s);
			} else {
				return new ZeroArgVoidHandler(m, s);
			}
			
		case 1:
			if (!isVoid) {
				return new SingleArgHandler(m, s, accepts, getters.get(0));
			} else {
				return new SingleArgVoidHandler(m, s, accepts, getters.get(0));
			}
			
		default:
			if (!isVoid) {
				return new MultiArgHandler(m, s, accepts, getters);
			} else {
				return new MultiArgVoidHandler(m, s, accepts, getters);
			}
		}
	}
}
