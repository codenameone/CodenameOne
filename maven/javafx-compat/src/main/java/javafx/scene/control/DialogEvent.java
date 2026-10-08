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
package javafx.scene.control;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/// An event of a [Dialog]: it is showing, shown, hiding or hidden, or the
/// user asked its window to close.
public class DialogEvent extends Event {

    private static final long serialVersionUID = 1L;

    /// The super type of every dialog event.
    public static final EventType<DialogEvent> ANY = new EventType<DialogEvent>(Event.ANY, "DIALOG");

    /// Sent before the dialog shows.
    public static final EventType<DialogEvent> DIALOG_SHOWING = new EventType<DialogEvent>(ANY, "DIALOG_SHOWING");

    /// Sent after the dialog showed.
    public static final EventType<DialogEvent> DIALOG_SHOWN = new EventType<DialogEvent>(ANY, "DIALOG_SHOWN");

    /// Sent before the dialog hides.
    public static final EventType<DialogEvent> DIALOG_HIDING = new EventType<DialogEvent>(ANY, "DIALOG_HIDING");

    /// Sent after the dialog hid.
    public static final EventType<DialogEvent> DIALOG_HIDDEN = new EventType<DialogEvent>(ANY, "DIALOG_HIDDEN");

    /// Sent when the user asks the window of the dialog to close. A
    /// handler that consumes it keeps the dialog open.
    public static final EventType<DialogEvent> DIALOG_CLOSE_REQUEST = new EventType<DialogEvent>(ANY,
            "DIALOG_CLOSE_REQUEST");

    /// Creates an event of a dialog.
    public DialogEvent(@NamedArg("source") Dialog<?> source,
            @NamedArg("eventType") EventType<? extends Event> eventType) {
        super(source, source, eventType);
    }

    @Override
    public DialogEvent copyFor(Object newSource, EventTarget newTarget) {
        Event copy = super.copyFor(newSource, newTarget);
        return copy instanceof DialogEvent ? (DialogEvent) copy : this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<DialogEvent> getEventType() {
        return (EventType<DialogEvent>) super.getEventType();
    }

    @Override
    public String toString() {
        return "DialogEvent[source=" + getSource() + ", eventType=" + getEventType() + ", consumed=" + isConsumed()
                + "]";
    }
}
