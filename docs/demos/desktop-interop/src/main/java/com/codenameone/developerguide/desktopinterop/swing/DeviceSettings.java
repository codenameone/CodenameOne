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
package com.codenameone.developerguide.desktopinterop.swing;

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.codename1.ui.CN;

/// A Swing entry point that adjusts how the layer maps it onto the device
/// before it shows anything.
public class DeviceSettings {

    public static void main(String[] args) {
        // tag::desktopInteropDisplayProperties[]
        if (!CN.isDesktop()) {
            // Fit a little more of a wide form on a phone.
            CN.setProperty("desktopcompat.scale", "1.6");
            // Keep the menu bar in the window instead of the overflow menu.
            CN.setProperty("desktopcompat.menuBar", "window");
        }
        // A fixed width font the application ships among its resources.
        CN.setProperty("desktopcompat.monospaced", "DejaVu Sans Mono|DejaVuSansMono.ttf");

        JFrame frame = new JFrame("Orders");
        // end::desktopInteropDisplayProperties[]
        frame.add(new JLabel("Orders"));
        frame.setVisible(true);
    }
}
