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

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Rectangle;

/// A component was moved, resized, shown or hidden.
public class ComponentEvent extends AWTEvent {

    public static final int COMPONENT_FIRST = 100;

    public static final int COMPONENT_LAST = 103;

    public static final int COMPONENT_MOVED = 100;

    public static final int COMPONENT_RESIZED = 101;

    public static final int COMPONENT_SHOWN = 102;

    public static final int COMPONENT_HIDDEN = 103;

    public ComponentEvent(Component source, int id) {
        super(source, id);
    }

    public Component getComponent() {
        return source instanceof Component ? (Component) source : null;
    }

    @Override
    public String paramString() {
        String type;
        Component c = getComponent();
        Rectangle b = c == null ? new Rectangle() : c.getBounds();
        switch (id) {
            case COMPONENT_SHOWN:
                type = "COMPONENT_SHOWN";
                break;
            case COMPONENT_HIDDEN:
                type = "COMPONENT_HIDDEN";
                break;
            case COMPONENT_MOVED:
                type = "COMPONENT_MOVED (" + b.x + "," + b.y + " " + b.width + "x" + b.height + ")";
                break;
            case COMPONENT_RESIZED:
                type = "COMPONENT_RESIZED (" + b.x + "," + b.y + " " + b.width + "x" + b.height + ")";
                break;
            default:
                type = "unknown type";
                break;
        }
        return type;
    }
}
