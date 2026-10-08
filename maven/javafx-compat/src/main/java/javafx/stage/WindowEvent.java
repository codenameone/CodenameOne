/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package javafx.stage;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/// A window is being shown or hidden, or the user asked to close it.
public class WindowEvent extends Event {

    private static final long serialVersionUID = 1L;

    /// Every window event.
    public static final EventType<WindowEvent> ANY = new EventType<WindowEvent>(Event.ANY, "WINDOW");

    /// The window is about to be shown.
    public static final EventType<WindowEvent> WINDOW_SHOWING = new EventType<WindowEvent>(ANY, "WINDOW_SHOWING");

    /// The window was shown.
    public static final EventType<WindowEvent> WINDOW_SHOWN = new EventType<WindowEvent>(ANY, "WINDOW_SHOWN");

    /// The window is about to be hidden.
    public static final EventType<WindowEvent> WINDOW_HIDING = new EventType<WindowEvent>(ANY, "WINDOW_HIDING");

    /// The window was hidden.
    public static final EventType<WindowEvent> WINDOW_HIDDEN = new EventType<WindowEvent>(ANY, "WINDOW_HIDDEN");

    /// The user asked to close the window; consuming the event keeps it
    /// open.
    public static final EventType<WindowEvent> WINDOW_CLOSE_REQUEST = new EventType<WindowEvent>(ANY,
            "WINDOW_CLOSE_REQUEST");

    /// Creates a window event.
    public WindowEvent(@NamedArg("source") Window source, @NamedArg("eventType") EventType<? extends Event> eventType) {
        super(source, source, eventType);
    }

    @Override
    public WindowEvent copyFor(Object newSource, EventTarget newTarget) {
        Event copy = super.copyFor(newSource, newTarget);
        return copy instanceof WindowEvent ? (WindowEvent) copy : this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<WindowEvent> getEventType() {
        return (EventType<WindowEvent>) super.getEventType();
    }
}
