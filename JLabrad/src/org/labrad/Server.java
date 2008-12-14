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

import org.labrad.events.ContextListener;
import org.labrad.events.ContextListenerSupport;

/**
 *
 * @author maffoo
 */
public class Server {

    // properties
    private String description;
    public String getDescription() { return description; }
    public void setString(String description) { this.description = description; }


    private String notes;
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }


    private ContextListenerSupport contextListeners = new ContextListenerSupport(this);
    public void addContextListener(ContextListener listener) {
        contextListeners.addListener(listener);
    }
    public void remoteContextListener(ContextListener listener) {
        contextListeners.removeListener(listener);
    }

}
