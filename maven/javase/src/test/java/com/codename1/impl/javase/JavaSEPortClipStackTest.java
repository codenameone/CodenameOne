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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * pushClip/popClip restore the clip that was pushed, in device pixels. Under a
 * scale (the simulator zoom is one on every screen graphics) the stack used to
 * convert the already user-space clip through the inverse transform once more,
 * so popClip restored a clip of the wrong size and position.
 */
public class JavaSEPortClipStackTest {
    private JavaSEPort port;
    private JavaSEPort originalInstance;
    private Graphics2D g2d;
    private Object graphics;

    @BeforeEach
    void setUp() {
        originalInstance = JavaSEPort.instance;
        port = new JavaSEPort();
        graphics = port.getNativeGraphics(new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB));
        assertTrue(graphics instanceof Graphics2D, "an image's native graphics is its Graphics2D");
        g2d = (Graphics2D) graphics;
    }

    @AfterEach
    void tearDown() {
        JavaSEPort.instance = originalInstance;
    }

    @Test
    void popRestoresTheClipUnderAScale() {
        g2d.scale(2, 2);
        port.setClip(graphics, 4, 6, 30, 20);
        port.pushClip(graphics);
        port.clipRect(graphics, 10, 10, 2, 2);
        port.popClip(graphics);
        assertEquals(new Rectangle(4, 6, 30, 20), g2d.getClipBounds());
    }

    @Test
    void popRestoresTheSamePixelsAfterTheTransformChanged() {
        g2d.translate(10, 5);
        port.setClip(graphics, 0, 0, 30, 20);
        port.pushClip(graphics);
        g2d.setTransform(new AffineTransform());
        port.clipRect(graphics, 0, 0, 1, 1);
        port.popClip(graphics);
        assertEquals(new Rectangle(10, 5, 30, 20), g2d.getClipBounds());
    }

    @Test
    void popRemovesAClipSetAfterPushingNone() {
        g2d.setClip(null);
        port.pushClip(graphics);
        port.setClip(graphics, 1, 1, 5, 5);
        port.popClip(graphics);
        assertNull(g2d.getClip(), "the graphics had no clip when it was pushed");
    }
}
