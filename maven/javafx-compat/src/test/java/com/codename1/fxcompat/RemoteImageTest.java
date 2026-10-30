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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.io.Storage;

import javafx.scene.image.Image;

/// `http:` and `https:` images load in the background and cache in `Storage`.
public class RemoteImageTest {

    @Rule
    public final MainThreadRule main = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        // An encoded image is decoded for real, so the cached PNG has a size.
        HeadlessImplementation.pixelImages = true;
    }

    @After
    public void tearDown() {
        HeadlessImplementation.pixelImages = false;
    }

    @Test
    public void remoteImageNeverBlocksOrThrowsInTheConstructor() {
        Image image = new Image("https://example.invalid/never-there.png");
        assertEquals(0, image.getWidth(), 0);
        assertEquals(0, image.getProgress(), 0);
        assertFalse("not failed before the request ran", image.isError());
    }

    @Test
    public void cachedRemoteImageArrivesWithItsSize() throws Exception {
        String url = "https://example.invalid/cached-logo.png";
        java.awt.image.BufferedImage picture = new java.awt.image.BufferedImage(7, 5,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(picture, "png", png);
        OutputStream out = Storage.getInstance().createOutputStream(cacheKey(url));
        out.write(png.toByteArray());
        out.close();

        final Image image = new Image(url);
        // The test runs on the EDT, where the image's callback is delivered:
        // wait off it, with the EDT still dispatching.
        com.codename1.ui.Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 500 && image.getProgress() < 1 && !image.isError(); i++) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException stop) {
                        return;
                    }
                }
            }
        });
        assertFalse(String.valueOf(image.getException()), image.isError());
        assertEquals(1, image.getProgress(), 0);
        assertEquals(7, image.getWidth(), 0);
        assertEquals(5, image.getHeight(), 0);
        assertNotNull(image.cn1Native());
    }

    @Test
    public void storageKeysAreFlatAndDistinct() {
        String a = cacheKey("https://example.com/a/b/c.png?x=1");
        String b = cacheKey("https://example.com/a/b/c.png?x=2");
        assertTrue(a, a.indexOf('/') < 0 && a.indexOf('?') < 0 && a.indexOf(':') < 0);
        assertFalse(a.equals(b));
    }

    private static String cacheKey(String url) {
        return javafx.scene.image.ImageTestAccess.storageKey(url);
    }
}
