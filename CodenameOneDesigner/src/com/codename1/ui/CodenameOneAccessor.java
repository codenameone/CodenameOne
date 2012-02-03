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

package com.codename1.ui;

/**
 * Utility to access package protected Codename One classes and attributes
 *
 * @author Shai Almog
 */
public class CodenameOneAccessor {
    public static com.codename1.ui.Image getImage(com.codename1.ui.Font f) {
        return ((com.codename1.ui.CustomFont)f).cache;
    }

    public static int[] getOffsets(com.codename1.ui.Font f) {
        return ((com.codename1.ui.CustomFont)f).cutOffsets;
    }

    public static int[] getWidths(com.codename1.ui.Font f) {
        return ((com.codename1.ui.CustomFont)f).charWidth;
    }
    
    public static int[] getPalette(com.codename1.ui.IndexedImage p) {
        return p.palette;
    }

    public static byte[] getImageData(com.codename1.ui.IndexedImage p) {
        return p.imageDataByte;
    }
    
    public static boolean isScrollableX(com.codename1.ui.Container c) {
        if(c instanceof com.codename1.ui.Form) {
            c = ((com.codename1.ui.Form)c).getContentPane();
        }
        return c.scrollableX;
    }

    public static void setScrollableX(com.codename1.ui.Container c, boolean scrollableX) {
        if(c instanceof com.codename1.ui.Form) {
            c = ((com.codename1.ui.Form)c).getContentPane();
        }
        c.scrollableX = scrollableX;
    }

    public static boolean isScrollableY(com.codename1.ui.Container c) {
        if(c instanceof com.codename1.ui.Form) {
            c = ((com.codename1.ui.Form)c).getContentPane();
        }
        return c.scrollableY;
    }

    public static void setScrollableY(com.codename1.ui.Container c, boolean scrollableY) {
        if(c instanceof com.codename1.ui.Form) {
            c = ((com.codename1.ui.Form)c).getContentPane();
        }
        c.scrollableY = scrollableY;
    }
}
