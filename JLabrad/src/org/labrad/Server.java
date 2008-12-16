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
import java.util.Arrays;
import java.util.Map;
import org.labrad.data.Context;
import org.labrad.data.Data;
import org.labrad.events.ContextListener;
import org.labrad.events.ContextListenerSupport;
import org.labrad.types.Type;

/**
 *
 * @author maffoo
 */
public class Server<T> {

    private static final Type SETTING_REGISTRATION = Type.fromTag("wss*s*ss");

    // properties
    private String description;
    public String getDescription() { return description; }
    public void setString(String description) { this.description = description; }


    private String notes;
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }


    private ContextualServer<T> serverImpl;
    private Map<Context, T> contexts;

    public ContextualServer<T> getServerImpl() { return serverImpl; }
    public void setServerImpl(ContextualServer<T> serverImpl) {
        this.serverImpl = serverImpl;
    }

    public static void findSettings(Class<?> serverClass) {
        for (Method m : serverClass.getMethods()) {
            if (m.isAnnotationPresent(Setting.class)) {
                System.out.println(m.getName() + " is remotely callable.");
                Setting s = m.getAnnotation(Setting.class);
                Data data = Data.ofType(SETTING_REGISTRATION);
                data.get(0).setWord(s.ID());
                data.get(1).setString(s.name());
                data.get(2).setString(s.description());
                data.get(3).setStringList(Arrays.asList(s.accepts()));
                data.get(4).setStringList(Arrays.asList(s.returns()));
                data.get(5).setString(s.notes());
                System.out.println(data.pretty());
            }
        }
    }

}
