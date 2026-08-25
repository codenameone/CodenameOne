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

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Guards clearing a window's icon on the JavaSE port.
 *
 * <p>{@code setIcon} treated a null image as a missing argument and returned, so
 * {@code setWindowIcon(null)} changed the framework's own state and left the previous
 * image on the {@code JFrame}: the title bar and taskbar went on showing an icon the
 * application had removed, and {@code getWindowIcon()} disagreed with what was on
 * screen. A null icon is a request to clear one.</p>
 *
 * <p>Needs a display, because the icon lives on a real frame. Skipped headless, as the
 * other frame-backed tests here are.</p>
 */
@CodenameOneTest
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
class JavaSEWindowManagerIconTest {

    @Test
    void aNullIconTakesTheOldOneOffTheFrame() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "the icon lives on a real frame");
        JavaSEWindowManager wm = new JavaSEWindowManager(JavaSEPort.instance);
        Object peer = wm.createWindow(1, "icon", 40, 40, 320, 240, true, true, null, false, false);
        assertNotNull(peer, "the window manager has to produce a peer to test against");
        try {
            wm.setIcon(peer, imageOf(0xff0000));
            flushAwt();
            assertNotNull(frameIcon(peer), "the icon it was given is on the frame");

            wm.setIcon(peer, null);
            flushAwt();
            assertNull(frameIcon(peer),
                    "clearing the icon has to take it off the frame; leaving it there "
                            + "shows an icon the application has already removed");
        } finally {
            wm.dispose(peer);
            flushAwt();
        }
    }

    private static java.awt.Image frameIcon(Object peer) {
        java.awt.Frame f = ((JavaSEWindowManager.Peer) peer).asFrame();
        return f == null ? null : f.getIconImage();
    }

    private static com.codename1.ui.Image imageOf(int rgb) {
        BufferedImage buffered = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        buffered.setRGB(0, 0, rgb);
        return com.codename1.ui.Image.createImage(buffered);
    }

    /** setIcon hops to the AWT thread, so the assertion has to wait for it. */
    private static void flushAwt() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
    }
}
