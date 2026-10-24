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
import com.codename1.desktopcompat.java.awt.Adjustable;

/// An adjustable value, such as a scroll bar position, changed.
public class AdjustmentEvent extends AWTEvent {

    public static final int ADJUSTMENT_FIRST = 601;

    public static final int ADJUSTMENT_LAST = 601;

    public static final int ADJUSTMENT_VALUE_CHANGED = 601;

    public static final int UNIT_INCREMENT = 1;

    public static final int UNIT_DECREMENT = 2;

    public static final int BLOCK_DECREMENT = 3;

    public static final int BLOCK_INCREMENT = 4;

    public static final int TRACK = 5;

    private final int adjustmentType;

    private final int value;

    private final boolean isAdjusting;

    public AdjustmentEvent(Adjustable source, int id, int type, int value) {
        this(source, id, type, value, false);
    }

    public AdjustmentEvent(Adjustable source, int id, int type, int value, boolean isAdjusting) {
        super(source, id);
        this.adjustmentType = type;
        this.value = value;
        this.isAdjusting = isAdjusting;
    }

    public Adjustable getAdjustable() {
        return source instanceof Adjustable ? (Adjustable) source : null;
    }

    public int getValue() {
        return value;
    }

    public int getAdjustmentType() {
        return adjustmentType;
    }

    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }

    @Override
    public String paramString() {
        String typeStr;
        switch (id) {
            case ADJUSTMENT_VALUE_CHANGED:
                typeStr = "ADJUSTMENT_VALUE_CHANGED";
                break;
            default:
                typeStr = "unknown type";
                break;
        }
        String adjTypeStr;
        switch (adjustmentType) {
            case UNIT_INCREMENT:
                adjTypeStr = "UNIT_INCREMENT";
                break;
            case UNIT_DECREMENT:
                adjTypeStr = "UNIT_DECREMENT";
                break;
            case BLOCK_INCREMENT:
                adjTypeStr = "BLOCK_INCREMENT";
                break;
            case BLOCK_DECREMENT:
                adjTypeStr = "BLOCK_DECREMENT";
                break;
            case TRACK:
                adjTypeStr = "TRACK";
                break;
            default:
                adjTypeStr = "unknown type";
                break;
        }
        return typeStr + ",adjType=" + adjTypeStr + ",value=" + value + ",isAdjusting=" + isAdjusting;
    }
}
