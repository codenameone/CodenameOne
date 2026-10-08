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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;

/// A component was added to or removed from a container.
public class ContainerEvent extends ComponentEvent {

    public static final int CONTAINER_FIRST = 300;

    public static final int CONTAINER_LAST = 301;

    public static final int COMPONENT_ADDED = 300;

    public static final int COMPONENT_REMOVED = 301;

    private final Component child;

    public ContainerEvent(Component source, int id, Component child) {
        super(source, id);
        this.child = child;
    }

    public Container getContainer() {
        return source instanceof Container ? (Container) source : null;
    }

    public Component getChild() {
        return child;
    }

    @Override
    public String paramString() {
        String typeStr;
        switch (id) {
            case COMPONENT_ADDED:
                typeStr = "COMPONENT_ADDED";
                break;
            case COMPONENT_REMOVED:
                typeStr = "COMPONENT_REMOVED";
                break;
            default:
                typeStr = "unknown type";
                break;
        }
        return typeStr + ",child=" + child;
    }
}
