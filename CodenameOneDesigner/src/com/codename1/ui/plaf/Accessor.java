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

package com.codename1.ui.plaf;

import com.codename1.ui.Image;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * This tool exposes some internal border functionality from the package
 *
 * @author Shai Almog
 */
public class Accessor {
    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_LINE = 1;
    public static final int TYPE_ROUNDED = 2;
    public static final int TYPE_ROUNDED_PRESSED = 3;
    public static final int TYPE_ETCHED_LOWERED = 4;
    public static final int TYPE_ETCHED_RAISED = 5;
    public static final int TYPE_BEVEL_RAISED = 6;
    public static final int TYPE_BEVEL_LOWERED = 7;
    public static final int TYPE_IMAGE = 8;
    public static final int TYPE_COMPOUND = 9;
    public static final int TYPE_IMAGE_HORIZONTAL = 10;
    public static final int TYPE_IMAGE_VERTICAL = 11;
    public static final int TYPE_DASHED = 12;
    public static final int TYPE_DOTTED = 13;
    public static final int TYPE_DOUBLE = 14;
    public static final int TYPE_GROOVE = 15;
    public static final int TYPE_RIDGE = 16;
    public static final int TYPE_INSET = 17;
    public static final int TYPE_OUTSET = 18;
    public static final int TYPE_IMAGE_SCALED = 19;

    static {
        UIManager.accessible = false;
    }
    
    /**
     * Returns the border type
     */
    public static int getType(Border b) {
        return b.type;
    }
    
    public static String toString(Border b) {
        if(b == null) {
            return "[null]";
        }
        if(b instanceof RoundBorder) {
            return "Round";
        }
        switch(b.type) {
            case TYPE_EMPTY:
                return "Empty";
            case TYPE_LINE:
                return "Line";
            case TYPE_ROUNDED:
            case TYPE_ROUNDED_PRESSED:
                return "Rounded";
            case TYPE_ETCHED_LOWERED:
                return "Etched Lowered";
            case TYPE_ETCHED_RAISED:
                return "Etched Raised";
            case TYPE_BEVEL_RAISED:
                return "Bevel Raised";
            case TYPE_BEVEL_LOWERED:
                return "Bevel Lowered";
            case TYPE_IMAGE:
                return "Image";
            case TYPE_IMAGE_HORIZONTAL:
                return "Image Horizontal";
            case TYPE_IMAGE_VERTICAL:
                return "Image Vertical";
            case TYPE_IMAGE_SCALED:
                return "Image Scaled";
        }
        throw new IllegalArgumentException(b.toString());
    }
    
    public static Image[] getImages(Border b) {
        return b.images;
    }
    
    public static boolean isThemeColors(Border b) {
        return b.themeColors;
    }
    
    public static int getColorA(Border b) {
        return b.colorA;
    }
    
    public static int getColorB(Border b) {
        return b.colorB;
    }
    
    public static int getColorC(Border b) {
        return b.colorC;
    }
    
    public static int getColorD(Border b) {
        return b.colorD;
    }
    
    public static int getThickness(Border b) {
        return b.thickness;
    }

    public static int getArcWidth(Border b) {
        return b.arcWidth;
    }

    public static int getArcHeight(Border b) {
        return b.arcHeight;
    }

    public static Border getPressed(Border b) {
        return b.pressedBorder;
    }
    
    public static void setTheme(Hashtable theme) {
        UIManager.getInstance().setThemePropsImpl(theme);
    }

    public static void setResourceBundle(Hashtable resourceBundle) {
        UIManager.localeAccessible = true;
        UIManager.getInstance().setResourceBundle(resourceBundle);
        UIManager.localeAccessible = resourceBundle == null;
    }

    public static void setUIManager(UIManager u) {
        UIManager.instance = u;
    }

    public static HashMap<String, Object> getThemeProps() {
        return UIManager.getInstance().getThemeProps();
    }
}
