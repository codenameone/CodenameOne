/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.events;

import com.codename1.ui.Display;
import com.codename1.ui.geom.Rectangle;

/**
 * Describes a change in the native window hosting the Codename One display on
 * desktop platforms.
 */
public class WindowEvent extends ActionEvent {

    /**
     * The type of window change that occurred.
     */
    public enum Type {
        /**
         * The window became visible.
         */
        Shown,
        /**
         * The window is no longer visible.
         */
        Hidden,
        /**
         * The window was minimized/iconified.
         */
        Minimized,
        /**
         * The window was restored from a minimized/iconified state.
         */
        Restored,
        /**
         * The window was resized.
         */
        Resized,
        /**
         * The window was moved.
         */
        Moved
    }

    private final Type type;
    private final Rectangle bounds;

    /**
     * Creates a new window event instance.
     *
     * @param source the display that generated the event
     * @param type   the type of the window event
     * @param bounds the bounds of the window, if known
     */
    public WindowEvent(Display source, Type type, Rectangle bounds) {
        super(source, ActionEvent.Type.Other);
        this.type = type;
        this.bounds = bounds;
    }

    /**
     * The type of window event.
     *
     * @return the event type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the window bounds associated with this event.
     *
     * @return the window bounds or {@code null} if not provided
     */
    public Rectangle getBounds() {
        return bounds;
    }
}
