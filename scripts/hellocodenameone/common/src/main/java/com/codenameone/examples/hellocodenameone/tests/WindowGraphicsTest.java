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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;

/**
 * Direct drawing inside a desktop window.
 *
 * <p>This is the case that exercises the port's graphics pipeline on a non-primary
 * surface: the window has its own render target, its own dirty queue and its own clip
 * universe. Getting the clip clamp wrong here is what leaves stale pixels on a retained
 * surface, so the shapes deliberately reach the window's edges.</p>
 *
 * @author Shai Almog
 */
public class WindowGraphicsTest extends WindowHostTest {

    @Override
    protected String baseImageName() {
        return "Window-Graphics";
    }

    @Override
    protected Component createWindowContent(final int width, final int height) {
        return new Component() {
            @Override
            public void paint(Graphics g) {
                int w = getWidth();
                int h = getHeight();
                g.setColor(0x102030);
                g.fillRect(getX(), getY(), w, h);

                g.setColor(0xe94f37);
                g.fillArc(getX() + w / 10, getY() + h / 10, w / 3, h / 3, 0, 270);

                g.setColor(0x44bba4);
                g.drawRect(getX() + 1, getY() + 1, w - 3, h - 3);

                g.setColor(0xf6f7eb);
                for (int iter = 0; iter < 8; iter++) {
                    int y = getY() + h * iter / 8;
                    g.drawLine(getX(), y, getX() + w, y + h / 8);
                }

                g.setColor(0xffd166);
                g.fillRect(getX() + w / 2, getY() + h / 2, w / 3, h / 3);
            }
        };
    }
}
