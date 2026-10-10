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

/// The mouse wheel was rotated in a component.
public class MouseWheelEvent extends MouseEvent {

    public static final int WHEEL_UNIT_SCROLL = 0;

    public static final int WHEEL_BLOCK_SCROLL = 1;

    private final int scrollType;

    private final int scrollAmount;

    private final int wheelRotation;

    private final double preciseWheelRotation;

    public MouseWheelEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount,
            boolean popupTrigger, int scrollType, int scrollAmount, int wheelRotation) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger);
        this.scrollType = scrollType;
        this.scrollAmount = scrollAmount;
        this.wheelRotation = wheelRotation;
        this.preciseWheelRotation = wheelRotation;
    }

    public MouseWheelEvent(Component source, int id, long when, int modifiers, int x, int y, int xAbs, int yAbs,
            int clickCount, boolean popupTrigger, int scrollType, int scrollAmount, int wheelRotation) {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger, NOBUTTON);
        this.scrollType = scrollType;
        this.scrollAmount = scrollAmount;
        this.wheelRotation = wheelRotation;
        this.preciseWheelRotation = wheelRotation;
    }

    public MouseWheelEvent(Component source, int id, long when, int modifiers, int x, int y, int xAbs, int yAbs,
            int clickCount, boolean popupTrigger, int scrollType, int scrollAmount, int wheelRotation,
            double preciseWheelRotation) {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger, NOBUTTON);
        this.scrollType = scrollType;
        this.scrollAmount = scrollAmount;
        this.wheelRotation = wheelRotation;
        this.preciseWheelRotation = preciseWheelRotation;
    }

    public int getScrollType() {
        return scrollType;
    }

    public int getScrollAmount() {
        return scrollAmount;
    }

    public int getWheelRotation() {
        return wheelRotation;
    }

    public double getPreciseWheelRotation() {
        return preciseWheelRotation;
    }

    public int getUnitsToScroll() {
        return scrollAmount * wheelRotation;
    }

    @Override
    public String paramString() {
        return super.paramString() + ",scrollType="
                + (scrollType == WHEEL_UNIT_SCROLL ? "WHEEL_UNIT_SCROLL" : scrollType == WHEEL_BLOCK_SCROLL
                        ? "WHEEL_BLOCK_SCROLL" : "unknown scroll type")
                + ",scrollAmount=" + scrollAmount + ",wheelRotation=" + wheelRotation
                + ",preciseWheelRotation=" + preciseWheelRotation;
    }
}
