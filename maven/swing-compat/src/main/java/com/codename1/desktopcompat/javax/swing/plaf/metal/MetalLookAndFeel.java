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

import com.codename1.desktopcompat.javax.swing.LookAndFeel;

/// The Metal look and feel, as far as a program names it: its identity and
/// its current theme.
///
/// It is not the look and feel of this layer and it does not draw: see the
/// package description. A theme set here is answered by
/// [#getCurrentTheme()] and changes no component.
public class MetalLookAndFeel extends LookAndFeel {

    private static MetalTheme currentTheme = new OceanTheme();

    public MetalLookAndFeel() {
    }

    @Override
    public String getName() {
        return "Metal";
    }

    @Override
    public String getID() {
        return "Metal";
    }

    @Override
    public String getDescription() {
        return "The Java(tm) Look and Feel";
    }

    @Override
    public boolean isNativeLookAndFeel() {
        return false;
    }

    @Override
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    public static void setCurrentTheme(MetalTheme theme) {
        if (theme == null) {
            throw new NullPointerException("Can't have null theme");
        }
        currentTheme = theme;
    }

    public static MetalTheme getCurrentTheme() {
        return currentTheme;
    }
}
