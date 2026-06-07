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
package com.codename1.impl.javase;

import com.codename1.testing.junit.CodenameOneTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies the simulator's over-pop clip guard (issue #5058): calling
/// `popClip` with no matching `pushClip` must throw rather than silently
/// corrupting the graphics state.
@CodenameOneTest
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
public class PaintScopeTest {

    private JavaSEPort port() {
        return JavaSEPort.instance;
    }

    @Test
    public void excessPopClipThrows() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            assertThrows(IllegalStateException.class, () -> port().popClip(g));
        } finally {
            g.dispose();
        }
    }

    @Test
    public void overPopAfterMatchingPushThrows() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            port().pushClip(g);
            port().popClip(g);
            // Second pop has no matching push.
            assertThrows(IllegalStateException.class, () -> port().popClip(g));
        } finally {
            port().disposeGraphics(g);
            g.dispose();
        }
    }
}
