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

import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.javax.swing.plaf.ColorUIResource;
import com.codename1.desktopcompat.javax.swing.plaf.FontUIResource;

/// The theme called "Steel": the blue-violet and gray of the first Metal.
public class DefaultMetalTheme extends MetalTheme {

    private static final ColorUIResource PRIMARY1 = new ColorUIResource(102, 102, 153);
    private static final ColorUIResource PRIMARY2 = new ColorUIResource(153, 153, 204);
    private static final ColorUIResource PRIMARY3 = new ColorUIResource(204, 204, 255);
    private static final ColorUIResource SECONDARY1 = new ColorUIResource(102, 102, 102);
    private static final ColorUIResource SECONDARY2 = new ColorUIResource(153, 153, 153);
    private static final ColorUIResource SECONDARY3 = new ColorUIResource(204, 204, 204);

    public DefaultMetalTheme() {
    }

    @Override
    public String getName() {
        return "Steel";
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
    public FontUIResource getControlTextFont() {
        return new FontUIResource("Dialog", Font.BOLD, 12);
    }

    @Override
    public FontUIResource getSystemTextFont() {
        return new FontUIResource("Dialog", Font.PLAIN, 12);
    }

    @Override
    public FontUIResource getUserTextFont() {
        return new FontUIResource("Dialog", Font.PLAIN, 12);
    }

    @Override
    public FontUIResource getMenuTextFont() {
        return new FontUIResource("Dialog", Font.BOLD, 12);
    }

    @Override
    public FontUIResource getWindowTitleFont() {
        return new FontUIResource("Dialog", Font.BOLD, 12);
    }

    @Override
    public FontUIResource getSubTextFont() {
        return new FontUIResource("Dialog", Font.PLAIN, 10);
    }
}
