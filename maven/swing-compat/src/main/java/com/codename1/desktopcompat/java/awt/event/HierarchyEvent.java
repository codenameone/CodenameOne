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
import com.codename1.desktopcompat.java.awt.Container;

/// The hierarchy a component belongs to changed: a parent was swapped, or
/// the component became displayable or showing.
public class HierarchyEvent extends AWTEvent {

    public static final int HIERARCHY_FIRST = 1400;

    public static final int HIERARCHY_CHANGED = 1400;

    public static final int ANCESTOR_MOVED = 1401;

    public static final int ANCESTOR_RESIZED = 1402;

    public static final int HIERARCHY_LAST = 1402;

    public static final int PARENT_CHANGED = 0x1;

    public static final int DISPLAYABILITY_CHANGED = 0x2;

    public static final int SHOWING_CHANGED = 0x4;

    private final Component changed;

    private final Container changedParent;

    private final long changeFlags;

    public HierarchyEvent(Component source, int id, Component changed, Container changedParent) {
        this(source, id, changed, changedParent, 0L);
    }

    public HierarchyEvent(Component source, int id, Component changed, Container changedParent,
            long changeFlags) {
        super(source, id);
        this.changed = changed;
        this.changedParent = changedParent;
        this.changeFlags = changeFlags;
    }

    public Component getComponent() {
        return source instanceof Component ? (Component) source : null;
    }

    public Component getChanged() {
        return changed;
    }

    public Container getChangedParent() {
        return changedParent;
    }

    public long getChangeFlags() {
        return changeFlags;
    }

    @Override
    public String paramString() {
        String typeStr;
        switch (id) {
            case ANCESTOR_MOVED:
                typeStr = "ANCESTOR_MOVED (" + changed + "," + changedParent + ")";
                break;
            case ANCESTOR_RESIZED:
                typeStr = "ANCESTOR_RESIZED (" + changed + "," + changedParent + ")";
                break;
            case HIERARCHY_CHANGED:
                typeStr = "HIERARCHY_CHANGED (" + changed + "," + changedParent + ")";
                if ((changeFlags & PARENT_CHANGED) != 0) {
                    typeStr += ",PARENT_CHANGED";
                }
                if ((changeFlags & DISPLAYABILITY_CHANGED) != 0) {
                    typeStr += ",DISPLAYABILITY_CHANGED";
                }
                if ((changeFlags & SHOWING_CHANGED) != 0) {
                    typeStr += ",SHOWING_CHANGED";
                }
                break;
            default:
                typeStr = "unknown type";
                break;
        }
        return typeStr;
    }
}
