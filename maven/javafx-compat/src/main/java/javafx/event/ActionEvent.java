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
package javafx.event;

/// The event of a control being activated: a button pressed, a menu item
/// chosen, a text field committed.
public class ActionEvent extends Event {

    private static final long serialVersionUID = 1L;

    /// The only type of action event.
    public static final EventType<ActionEvent> ACTION = new EventType<ActionEvent>(Event.ANY, "ACTION");

    /// Every action event.
    public static final EventType<ActionEvent> ANY = ACTION;

    /// Creates an action event with no source and no target.
    public ActionEvent() {
        super(ACTION);
    }

    /// Creates an action event.
    public ActionEvent(Object source, EventTarget target) {
        super(source, target, ACTION);
    }

    @Override
    public ActionEvent copyFor(Object newSource, EventTarget newTarget) {
        if (getClass() == ActionEvent.class) {
            ActionEvent copy = new ActionEvent(newSource, newTarget);
            copy.consumed = consumed;
            return copy;
        }
        super.copyFor(newSource, newTarget);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<? extends ActionEvent> getEventType() {
        return (EventType<? extends ActionEvent>) super.getEventType();
    }
}
