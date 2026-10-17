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
package com.codename1.desktopcompat.javax.swing.plaf.metal;

import com.codename1.desktopcompat.javax.swing.UIDefaults;
import com.codename1.desktopcompat.javax.swing.plaf.ColorUIResource;
import com.codename1.desktopcompat.javax.swing.plaf.FontUIResource;

/// The colors and fonts of the Metal look and feel, each derived from
/// three primary and three secondary colors unless a theme says
/// otherwise.
public abstract class MetalTheme {

    private static final ColorUIResource WHITE = new ColorUIResource(255, 255, 255);

    private static final ColorUIResource BLACK = new ColorUIResource(0, 0, 0);

    public abstract String getName();

    protected abstract ColorUIResource getPrimary1();

    protected abstract ColorUIResource getPrimary2();

    protected abstract ColorUIResource getPrimary3();

    protected abstract ColorUIResource getSecondary1();

    protected abstract ColorUIResource getSecondary2();

    protected abstract ColorUIResource getSecondary3();

    public abstract FontUIResource getControlTextFont();

    public abstract FontUIResource getSystemTextFont();

    public abstract FontUIResource getUserTextFont();

    public abstract FontUIResource getMenuTextFont();

    public abstract FontUIResource getWindowTitleFont();

    public abstract FontUIResource getSubTextFont();

    protected ColorUIResource getWhite() {
        return WHITE;
    }

    protected ColorUIResource getBlack() {
        return BLACK;
    }

    public ColorUIResource getFocusColor() {
        return getPrimary2();
    }

    public ColorUIResource getDesktopColor() {
        return getPrimary2();
    }

    public ColorUIResource getControl() {
        return getSecondary3();
    }

    public ColorUIResource getControlShadow() {
        return getSecondary2();
    }

    public ColorUIResource getControlDarkShadow() {
        return getSecondary1();
    }

    public ColorUIResource getControlInfo() {
        return getBlack();
    }

    public ColorUIResource getControlHighlight() {
        return getWhite();
    }

    public ColorUIResource getControlDisabled() {
        return getSecondary2();
    }

    public ColorUIResource getPrimaryControl() {
        return getPrimary3();
    }

    public ColorUIResource getPrimaryControlShadow() {
        return getPrimary2();
    }

    public ColorUIResource getPrimaryControlDarkShadow() {
        return getPrimary1();
    }

    public ColorUIResource getPrimaryControlInfo() {
        return getBlack();
    }

    public ColorUIResource getPrimaryControlHighlight() {
        return getWhite();
    }

    public ColorUIResource getSystemTextColor() {
        return getBlack();
    }

    public ColorUIResource getControlTextColor() {
        return getControlInfo();
    }

    public ColorUIResource getInactiveControlTextColor() {
        return getControlDisabled();
    }

    public ColorUIResource getInactiveSystemTextColor() {
        return getSecondary2();
    }

    public ColorUIResource getUserTextColor() {
        return getBlack();
    }

    public ColorUIResource getTextHighlightColor() {
        return getPrimary3();
    }

    public ColorUIResource getHighlightedTextColor() {
        return getControlTextColor();
    }

    public ColorUIResource getWindowBackground() {
        return getWhite();
    }

    public ColorUIResource getWindowTitleBackground() {
        return getPrimary3();
    }

    public ColorUIResource getWindowTitleForeground() {
        return getBlack();
    }

    public ColorUIResource getWindowTitleInactiveBackground() {
        return getSecondary3();
    }

    public ColorUIResource getWindowTitleInactiveForeground() {
        return getBlack();
    }

    public ColorUIResource getMenuBackground() {
        return getSecondary3();
    }

    public ColorUIResource getMenuForeground() {
        return getBlack();
    }

    public ColorUIResource getMenuSelectedBackground() {
        return getPrimary2();
    }

    public ColorUIResource getMenuSelectedForeground() {
        return getBlack();
    }

    public ColorUIResource getMenuDisabledForeground() {
        return getSecondary2();
    }

    public ColorUIResource getSeparatorBackground() {
        return getWhite();
    }

    public ColorUIResource getSeparatorForeground() {
        return getPrimary1();
    }

    public ColorUIResource getAcceleratorForeground() {
        return getPrimary1();
    }

    public ColorUIResource getAcceleratorSelectedForeground() {
        return getBlack();
    }

    /// Where a theme adds its own entries to the defaults of the Metal
    /// look and feel. That look and feel is never installed here, so this
    /// is not called by the layer.
    public void addCustomEntriesToTable(UIDefaults table) {
    }
}
