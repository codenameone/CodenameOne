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
package com.codename1.desktopcompat.java.awt.event;

import com.codename1.desktopcompat.java.awt.Window;

/// A window was opened, closed, iconified, activated, focused or changed
/// state.
public class WindowEvent extends ComponentEvent {

    public static final int WINDOW_FIRST = 200;

    public static final int WINDOW_OPENED = 200;

    public static final int WINDOW_CLOSING = 201;

    public static final int WINDOW_CLOSED = 202;

    public static final int WINDOW_ICONIFIED = 203;

    public static final int WINDOW_DEICONIFIED = 204;

    public static final int WINDOW_ACTIVATED = 205;

    public static final int WINDOW_DEACTIVATED = 206;

    public static final int WINDOW_GAINED_FOCUS = 207;

    public static final int WINDOW_LOST_FOCUS = 208;

    public static final int WINDOW_STATE_CHANGED = 209;

    public static final int WINDOW_LAST = 209;

    private final Window opposite;

    private final int oldState;

    private final int newState;

    public WindowEvent(Window source, int id, Window opposite, int oldState, int newState) {
        super(source, id);
        this.opposite = opposite;
        this.oldState = oldState;
        this.newState = newState;
    }

    public WindowEvent(Window source, int id, Window opposite) {
        this(source, id, opposite, 0, 0);
    }

    public WindowEvent(Window source, int id, int oldState, int newState) {
        this(source, id, null, oldState, newState);
    }

    public WindowEvent(Window source, int id) {
        this(source, id, null, 0, 0);
    }

    public Window getWindow() {
        return source instanceof Window ? (Window) source : null;
    }

    public Window getOppositeWindow() {
        return opposite;
    }

    public int getOldState() {
        return oldState;
    }

    public int getNewState() {
        return newState;
    }

    @Override
    public String paramString() {
        String typeStr;
        switch (id) {
            case WINDOW_OPENED:
                typeStr = "WINDOW_OPENED";
                break;
            case WINDOW_CLOSING:
                typeStr = "WINDOW_CLOSING";
                break;
            case WINDOW_CLOSED:
                typeStr = "WINDOW_CLOSED";
                break;
            case WINDOW_ICONIFIED:
                typeStr = "WINDOW_ICONIFIED";
                break;
            case WINDOW_DEICONIFIED:
                typeStr = "WINDOW_DEICONIFIED";
                break;
            case WINDOW_ACTIVATED:
                typeStr = "WINDOW_ACTIVATED";
                break;
            case WINDOW_DEACTIVATED:
                typeStr = "WINDOW_DEACTIVATED";
                break;
            case WINDOW_GAINED_FOCUS:
                typeStr = "WINDOW_GAINED_FOCUS";
                break;
            case WINDOW_LOST_FOCUS:
                typeStr = "WINDOW_LOST_FOCUS";
                break;
            case WINDOW_STATE_CHANGED:
                typeStr = "WINDOW_STATE_CHANGED";
                break;
            default:
                typeStr = "unknown type";
                break;
        }
        return typeStr + ",opposite=" + opposite + ",oldState=" + oldState + ",newState=" + newState;
    }
}
