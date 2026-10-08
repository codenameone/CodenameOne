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

/// A component gained or lost the keyboard focus.
///
/// The JDK 9 `Cause` of a focus change is not provided.
public class FocusEvent extends ComponentEvent {

    public static final int FOCUS_FIRST = 1004;

    public static final int FOCUS_LAST = 1005;

    public static final int FOCUS_GAINED = 1004;

    public static final int FOCUS_LOST = 1005;

    private final boolean temporary;

    private final Component opposite;

    public FocusEvent(Component source, int id, boolean temporary, Component opposite) {
        super(source, id);
        this.temporary = temporary;
        this.opposite = opposite;
    }

    public FocusEvent(Component source, int id, boolean temporary) {
        this(source, id, temporary, null);
    }

    public FocusEvent(Component source, int id) {
        this(source, id, false);
    }

    public boolean isTemporary() {
        return temporary;
    }

    public Component getOppositeComponent() {
        return opposite;
    }

    @Override
    public String paramString() {
        String typeStr;
        switch (id) {
            case FOCUS_GAINED:
                typeStr = "FOCUS_GAINED";
                break;
            case FOCUS_LOST:
                typeStr = "FOCUS_LOST";
                break;
            default:
                typeStr = "unknown type";
                break;
        }
        return typeStr + (temporary ? ",temporary" : ",permanent") + ",opposite=" + opposite;
    }
}
