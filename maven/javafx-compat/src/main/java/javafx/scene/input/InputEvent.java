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
package javafx.scene.input;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/// The base of every event that comes from the user: mouse, key, scroll.
public class InputEvent extends Event {

    private static final long serialVersionUID = 1L;

    /// Every input event.
    public static final EventType<InputEvent> ANY = new EventType<InputEvent>(Event.ANY, "INPUT");

    /// Creates an input event with no source and no target.
    public InputEvent(@NamedArg("eventType") EventType<? extends InputEvent> eventType) {
        super(eventType);
    }

    /// Creates an input event.
    public InputEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            @NamedArg("eventType") EventType<? extends InputEvent> eventType) {
        super(source, target, eventType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<? extends InputEvent> getEventType() {
        return (EventType<? extends InputEvent>) super.getEventType();
    }
}
