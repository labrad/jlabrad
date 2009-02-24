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

import org.labrad.handlers.BasicSettingHandler;
import org.labrad.handlers.NoArgHandler;
import org.labrad.handlers.NoArgNoReturnHandler;
import org.labrad.handlers.NoReturnHandler;

public class SettingHandlers {
	public static SettingHandler forMethod(Method m) {
		// make sure this method has a setting annotation
		if (!m.isAnnotationPresent(Setting.class)) {
			String msg = "Cannot create a setting handler for method '"
				       + m.getName() + "': needs @Setting annotation.";
			throw new RuntimeException(msg);
		}
		Setting s = m.getAnnotation(Setting.class);
		
		boolean noArgs = (m.getParameterTypes().length == 0);
		boolean noReturn = (m.getReturnType() == Void.TYPE);
		if (noArgs) {
			// make sure that the only specified LabRAD accepted type is ""
			for (String t : s.accepts()) {
				if (!t.isEmpty()) {
					String msg = "Setting '" + s.name()
							   + "' takes no args, but specifies non-empty accepted type: " + t;
					throw new RuntimeException(msg);
				}
			}
		}
		if (noReturn) {
			// make sure that the only specified LabRAD return type is ""
			for (String t : s.returns()) {
				if (!t.isEmpty()) {
					String msg = "Setting '" + s.name()
							   + "' returns void, but specifies non-empty return type: " + t;
					throw new RuntimeException(msg);
				}
			}
		}
		
		// create a handler of the appropriate type for this setting
		if (noArgs && noReturn) {
			return new NoArgNoReturnHandler(m, s);
		} else if (noArgs) {
			return new NoArgHandler(m, s);
		} else if (noReturn) {
			return new NoReturnHandler(m, s);
		}
		return new BasicSettingHandler(m, s);
	}
}
