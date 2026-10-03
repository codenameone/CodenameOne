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
package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

import java.util.function.Consumer;

/// A paintable `Component` from a drawing lambda, for quick sketches and game
/// loops without declaring a `Component` subclass (which Playground code can also
/// do directly):
///
/// ```java
/// Component view = GameScripting.canvas(320, 480, g -> {
///     g.setColor(0x10182a);
///     g.fillRect(0, 0, 320, 480);   // local 0,0-relative coordinates
///     // ... draw the scene ...
/// });
/// ```
///
/// The `Graphics` is translated to the component's origin before the lambda
/// runs, so the lambda draws in local (0,0-relative) coordinates -- it doesn't
/// need the component's absolute position. This is the 2D counterpart to
/// [GpuScripting] for the GPU `Renderer`.
public final class GameScripting {
    private GameScripting() {
    }

    /// Builds a `Component` of the given preferred size whose `paint` forwards to
    /// the supplied lambda. `painter` receives a `Graphics` already translated to
    /// the component's top-left, so it draws in local coordinates.
    public static Component canvas(final int width, final int height, final Consumer<Graphics> painter) {
        return new Component() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(width, height);
            }

            @Override
            public void paint(Graphics g) {
                if (painter == null) {
                    return;
                }
                int tx = getX();
                int ty = getY();
                g.translate(tx, ty);
                try {
                    painter.accept(g);
                } finally {
                    g.translate(-tx, -ty);
                }
            }
        };
    }
}
