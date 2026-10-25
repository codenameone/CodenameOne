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
package com.codename1.desktopcompat.java.awt;

import java.util.EventObject;

/// The root of every event the AWT and Swing layers deliver.
///
/// An event carries the object it came from, an integer id that tells the
/// event kinds of one class apart, and a consumed flag that lets a listener
/// stop the default handling. The legacy `java.awt.Event` based constructor
/// and the serialization support of the JDK class are not provided.
public abstract class AWTEvent extends EventObject {

    public static final long COMPONENT_EVENT_MASK = 0x01;

    public static final long CONTAINER_EVENT_MASK = 0x02;

    public static final long FOCUS_EVENT_MASK = 0x04;

    public static final long KEY_EVENT_MASK = 0x08;

    public static final long MOUSE_EVENT_MASK = 0x10;

    public static final long MOUSE_MOTION_EVENT_MASK = 0x20;

    public static final long WINDOW_EVENT_MASK = 0x40;

    public static final long ACTION_EVENT_MASK = 0x80;

    public static final long ADJUSTMENT_EVENT_MASK = 0x100;

    public static final long ITEM_EVENT_MASK = 0x200;

    public static final long TEXT_EVENT_MASK = 0x400;

    public static final long INPUT_METHOD_EVENT_MASK = 0x800;

    public static final long PAINT_EVENT_MASK = 0x2000;

    public static final long INVOCATION_EVENT_MASK = 0x4000;

    public static final long HIERARCHY_EVENT_MASK = 0x8000;

    public static final long HIERARCHY_BOUNDS_EVENT_MASK = 0x10000;

    public static final long MOUSE_WHEEL_EVENT_MASK = 0x20000;

    public static final long WINDOW_STATE_EVENT_MASK = 0x40000;

    public static final long WINDOW_FOCUS_EVENT_MASK = 0x80000;

    public static final int RESERVED_ID_MAX = 1999;

    /// The event kind within the event class.
    protected int id;

    /// Whether a listener consumed the event.
    protected boolean consumed;

    public AWTEvent(Object source, int id) {
        super(source);
        this.id = id;
    }

    public void setSource(Object newSource) {
        if (source == newSource) {
            return;
        }
        source = newSource;
    }

    public int getID() {
        return id;
    }

    public String paramString() {
        return "";
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + paramString() + "] on " + source;
    }

    protected void consume() {
        consumed = true;
    }

    protected boolean isConsumed() {
        return consumed;
    }
}
