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

import com.codename1.desktopcompat.javax.swing.plaf.ColorUIResource;

/// The theme called "Ocean": the pale blue Metal.
public class OceanTheme extends DefaultMetalTheme {

    private static final ColorUIResource PRIMARY1 = new ColorUIResource(0x6382BF);
    private static final ColorUIResource PRIMARY2 = new ColorUIResource(0xA3B8CC);
    private static final ColorUIResource PRIMARY3 = new ColorUIResource(0xB8CFE5);
    private static final ColorUIResource SECONDARY1 = new ColorUIResource(0x7A8A99);
    private static final ColorUIResource SECONDARY2 = new ColorUIResource(0xB8CFE5);
    private static final ColorUIResource SECONDARY3 = new ColorUIResource(0xEEEEEE);
    private static final ColorUIResource CONTROL_TEXT_COLOR = new ColorUIResource(0x333333);
    private static final ColorUIResource INACTIVE_CONTROL_TEXT_COLOR = new ColorUIResource(0x999999);
    private static final ColorUIResource MENU_DISABLED_FOREGROUND = new ColorUIResource(0x999999);
    private static final ColorUIResource OCEAN_BLACK = new ColorUIResource(0x333333);

    public OceanTheme() {
    }

    @Override
    public String getName() {
        return "Ocean";
    }

    @Override
    protected ColorUIResource getPrimary1() {
        return PRIMARY1;
    }

    @Override
    protected ColorUIResource getPrimary2() {
        return PRIMARY2;
    }

    @Override
    protected ColorUIResource getPrimary3() {
        return PRIMARY3;
    }

    @Override
    protected ColorUIResource getSecondary1() {
        return SECONDARY1;
    }

    @Override
    protected ColorUIResource getSecondary2() {
        return SECONDARY2;
    }

    @Override
    protected ColorUIResource getSecondary3() {
        return SECONDARY3;
    }

    @Override
    protected ColorUIResource getBlack() {
        return OCEAN_BLACK;
    }

    @Override
    public ColorUIResource getDesktopColor() {
        return getWhite();
    }

    @Override
    public ColorUIResource getInactiveControlTextColor() {
        return INACTIVE_CONTROL_TEXT_COLOR;
    }

    @Override
    public ColorUIResource getControlTextColor() {
        return CONTROL_TEXT_COLOR;
    }

    @Override
    public ColorUIResource getMenuDisabledForeground() {
        return MENU_DISABLED_FOREGROUND;
    }
}
