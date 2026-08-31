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

import com.codename1.components.OtpField;
import com.codename1.testing.junit.CodenameOneTest;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Renders an {@link OtpField} through the real JavaSE rasterizer and reads the
 * pixels back.
 *
 * <p>The field is one editor drawing nothing behind a row of boxes that draw the
 * value, which is what lets a whole code arrive at once. Nothing in a unit test
 * against the no-op test implementation can tell that apart from a field that
 * draws nothing at all, so this asserts what a user would see: ink inside every
 * box, and each digit inside its own box rather than all of them at the left.</p>
 */
@CodenameOneTest
public class OtpFieldRenderingTest {

    @Test
    public void everyBoxDrawsItsOwnDigit() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        final AtomicReference<OtpField> fieldRef = new AtomicReference<OtpField>();
        final AtomicReference<int[]> pixelsRef = new AtomicReference<int[]>();
        final AtomicReference<Form> formRef = new AtomicReference<Form>();

        runOnCn1AndWait(new Runnable() {
            public void run() {
                Form f = new Form("Otp", BoxLayout.y());
                OtpField otp = new OtpField(6);
                f.add(otp);
                f.show();
                f.revalidate();
                otp.setText("123456");
                f.revalidate();
                fieldRef.set(otp);
                formRef.set(f);

                Image img = Image.createImage(f.getWidth(), f.getHeight(), 0xffffffff);
                f.paintComponent(img.getGraphics(), true);
                int[] rgb = img.getRGB();
                pixelsRef.set(rgb);
            }
        });

        OtpField otp = fieldRef.get();
        Form form = formRef.get();
        int[] rgb = pixelsRef.get();
        assertEquals(6, otp.getLength());

        // Ink anywhere at all. Both mistakes that make a rendering test vacuous --
        // a form that was never shown, and a rasterizer that draws nothing -- land
        // here rather than passing quietly.
        assertTrue(ink(rgb, 0, 0, form.getWidth(), form.getHeight(), form.getWidth()) > 0,
                "nothing was rasterized at all");

        for (int i = 0; i < otp.getLength(); i++) {
            TextField box = otp.getBox(i);
            assertTrue(box.getWidth() > 0 && box.getHeight() > 0, "box " + i + " has no size");
            long boxInk = ink(rgb, box.getAbsoluteX(), box.getAbsoluteY(),
                    box.getWidth(), box.getHeight(), form.getWidth());
            assertTrue(boxInk > 0, "box " + i + " drew nothing; the code is not one digit per box");
        }
    }

    @Test
    public void anEmptyFieldDrawsBoxesButNoDigits() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        final AtomicReference<long[]> inkRef = new AtomicReference<long[]>();
        runOnCn1AndWait(new Runnable() {
            public void run() {
                Form f = new Form("Otp", BoxLayout.y());
                OtpField otp = new OtpField(6);
                f.add(otp);
                f.show();
                f.revalidate();

                long empty = totalInk(f, otp);
                otp.setText("123456");
                f.revalidate();
                long filled = totalInk(f, otp);
                inkRef.set(new long[]{empty, filled});
            }
        });

        long[] measured = inkRef.get();
        assertTrue(measured[1] > measured[0],
                "a filled field must carry more ink than an empty one -- the digits are "
                        + "drawn by the boxes, and if the value only lived in the editor above "
                        + "them nothing would change");
    }

    private static long totalInk(Form f, OtpField otp) {
        Image img = Image.createImage(f.getWidth(), f.getHeight(), 0xffffffff);
        f.paintComponent(img.getGraphics(), true);
        int[] rgb = img.getRGB();
        return ink(rgb, otp.getAbsoluteX(), otp.getAbsoluteY(), otp.getWidth(), otp.getHeight(),
                f.getWidth());
    }

    /// Summed darkness over a rectangle, so "did anything draw here" is a number
    /// rather than a guess.
    private static long ink(int[] rgb, int x, int y, int w, int h, int stride) {
        long total = 0;
        for (int row = Math.max(0, y); row < y + h; row++) {
            int base = row * stride;
            for (int col = Math.max(0, x); col < x + w; col++) {
                int idx = base + col;
                if (idx < 0 || idx >= rgb.length) {
                    continue;
                }
                int p = rgb[idx];
                total += 255 - (p & 0xff);
                total += 255 - ((p >> 8) & 0xff);
                total += 255 - ((p >> 16) & 0xff);
            }
        }
        return total;
    }

    private void runOnCn1AndWait(final Runnable r) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                try {
                    r.run();
                } catch (Throwable t) {
                    err.set(t);
                } finally {
                    latch.countDown();
                }
            }
        });
        assertTrue(latch.await(15, TimeUnit.SECONDS), "Codename One EDT work timed out");
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
    }
}
