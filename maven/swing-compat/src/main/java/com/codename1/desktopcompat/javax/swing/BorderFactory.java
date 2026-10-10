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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.javax.swing.border.BevelBorder;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.border.CompoundBorder;
import com.codename1.desktopcompat.javax.swing.border.EmptyBorder;
import com.codename1.desktopcompat.javax.swing.border.EtchedBorder;
import com.codename1.desktopcompat.javax.swing.border.LineBorder;
import com.codename1.desktopcompat.javax.swing.border.MatteBorder;
import com.codename1.desktopcompat.javax.swing.border.TitledBorder;

/// Factory methods for the standard borders.
///
/// The soft bevel, stroke and dashed borders are not provided.
public class BorderFactory {

    private static final Border EMPTY = new EmptyBorder(0, 0, 0, 0);
    private static final Border RAISED_BEVEL = new BevelBorder(BevelBorder.RAISED);
    private static final Border LOWERED_BEVEL = new BevelBorder(BevelBorder.LOWERED);
    private static final Border ETCHED = new EtchedBorder();

    private BorderFactory() {
    }

    public static Border createLineBorder(Color color) {
        return new LineBorder(color, 1);
    }

    public static Border createLineBorder(Color color, int thickness) {
        return new LineBorder(color, thickness);
    }

    public static Border createLineBorder(Color color, int thickness, boolean rounded) {
        return new LineBorder(color, thickness, rounded);
    }

    public static Border createRaisedBevelBorder() {
        return RAISED_BEVEL;
    }

    public static Border createLoweredBevelBorder() {
        return LOWERED_BEVEL;
    }

    public static Border createBevelBorder(int type) {
        if (type == BevelBorder.RAISED) {
            return RAISED_BEVEL;
        }
        if (type == BevelBorder.LOWERED) {
            return LOWERED_BEVEL;
        }
        return null;
    }

    public static Border createBevelBorder(int type, Color highlight, Color shadow) {
        return new BevelBorder(type, highlight, shadow);
    }

    public static Border createBevelBorder(int type, Color highlightOuter, Color highlightInner, Color shadowOuter,
            Color shadowInner) {
        return new BevelBorder(type, highlightOuter, highlightInner, shadowOuter, shadowInner);
    }

    public static Border createEtchedBorder() {
        return ETCHED;
    }

    public static Border createEtchedBorder(Color highlight, Color shadow) {
        return new EtchedBorder(highlight, shadow);
    }

    public static Border createEtchedBorder(int type) {
        if (type != EtchedBorder.RAISED && type != EtchedBorder.LOWERED) {
            throw new IllegalArgumentException("type must be one of EtchedBorder.RAISED or EtchedBorder.LOWERED");
        }
        return type == EtchedBorder.LOWERED ? ETCHED : new EtchedBorder(EtchedBorder.RAISED);
    }

    public static Border createEtchedBorder(int type, Color highlight, Color shadow) {
        return new EtchedBorder(type, highlight, shadow);
    }

    public static TitledBorder createTitledBorder(String title) {
        return new TitledBorder(title);
    }

    public static TitledBorder createTitledBorder(Border border) {
        return new TitledBorder(border);
    }

    public static TitledBorder createTitledBorder(Border border, String title) {
        return new TitledBorder(border, title);
    }

    public static TitledBorder createTitledBorder(Border border, String title, int titleJustification,
            int titlePosition) {
        return new TitledBorder(border, title, titleJustification, titlePosition);
    }

    public static TitledBorder createTitledBorder(Border border, String title, int titleJustification,
            int titlePosition, Font titleFont) {
        return new TitledBorder(border, title, titleJustification, titlePosition, titleFont);
    }

    public static TitledBorder createTitledBorder(Border border, String title, int titleJustification,
            int titlePosition, Font titleFont, Color titleColor) {
        return new TitledBorder(border, title, titleJustification, titlePosition, titleFont, titleColor);
    }

    public static Border createEmptyBorder() {
        return EMPTY;
    }

    public static Border createEmptyBorder(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    public static CompoundBorder createCompoundBorder() {
        return new CompoundBorder();
    }

    public static CompoundBorder createCompoundBorder(Border outsideBorder, Border insideBorder) {
        return new CompoundBorder(outsideBorder, insideBorder);
    }

    public static MatteBorder createMatteBorder(int top, int left, int bottom, int right, Color color) {
        return new MatteBorder(top, left, bottom, right, color);
    }

    public static MatteBorder createMatteBorder(int top, int left, int bottom, int right, Icon tileIcon) {
        return new MatteBorder(top, left, bottom, right, tileIcon);
    }
}
