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
package com.codename1.desktopcompat.javax.swing.event;

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.javax.swing.JInternalFrame;

/// Something that happened to an internal frame.
public class InternalFrameEvent extends AWTEvent {

    public static final int INTERNAL_FRAME_FIRST = 25549;

    public static final int INTERNAL_FRAME_LAST = 25555;

    public static final int INTERNAL_FRAME_OPENED = INTERNAL_FRAME_FIRST;

    public static final int INTERNAL_FRAME_CLOSING = 1 + INTERNAL_FRAME_FIRST;

    public static final int INTERNAL_FRAME_CLOSED = 2 + INTERNAL_FRAME_FIRST;

    public static final int INTERNAL_FRAME_ICONIFIED = 3 + INTERNAL_FRAME_FIRST;

    public static final int INTERNAL_FRAME_DEICONIFIED = 4 + INTERNAL_FRAME_FIRST;

    public static final int INTERNAL_FRAME_ACTIVATED = 5 + INTERNAL_FRAME_FIRST;

    public static final int INTERNAL_FRAME_DEACTIVATED = 6 + INTERNAL_FRAME_FIRST;

    public InternalFrameEvent(JInternalFrame source, int id) {
        super(source, id);
    }

    /// The frame the event is about, or `null` when the source is none.
    public JInternalFrame getInternalFrame() {
        Object s = getSource();
        return s instanceof JInternalFrame ? (JInternalFrame) s : null;
    }

    @Override
    public String paramString() {
        switch (getID()) {
            case INTERNAL_FRAME_OPENED:
                return "INTERNAL_FRAME_OPENED";
            case INTERNAL_FRAME_CLOSING:
                return "INTERNAL_FRAME_CLOSING";
            case INTERNAL_FRAME_CLOSED:
                return "INTERNAL_FRAME_CLOSED";
            case INTERNAL_FRAME_ICONIFIED:
                return "INTERNAL_FRAME_ICONIFIED";
            case INTERNAL_FRAME_DEICONIFIED:
                return "INTERNAL_FRAME_DEICONIFIED";
            case INTERNAL_FRAME_ACTIVATED:
                return "INTERNAL_FRAME_ACTIVATED";
            case INTERNAL_FRAME_DEACTIVATED:
                return "INTERNAL_FRAME_DEACTIVATED";
            default:
                return "unknown type";
        }
    }
}
