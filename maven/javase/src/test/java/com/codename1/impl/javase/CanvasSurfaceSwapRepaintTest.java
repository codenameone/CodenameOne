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
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Regression test for the black desktop window of issue #5443.
 *
 * The canvas paints into a {@code TYPE_INT_RGB} buffer, and that buffer is
 * thrown away and reallocated whenever the surface geometry changes. The
 * replacement starts out blank - black - but nothing marked the form dirty, so
 * the following {@code paintDirty()} refilled only whatever happened to be in
 * the dirty list. A window that was up and correct became a black rectangle
 * with a stray component or two still painted in it, and stayed that way: the
 * only thing that healed it was an AWT-initiated paint, which a settled desktop
 * window with no native peers may never get.
 *
 * Swapping the surface has to invalidate the whole form.
 */
@CodenameOneTest
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
public class CanvasSurfaceSwapRepaintTest {

    /** Counts whole-form paints; a partial repaint of a child never gets here. */
    private static final class CountingForm extends Form {
        final AtomicInteger fullPaints = new AtomicInteger();

        CountingForm() {
            super("surface swap", new BorderLayout());
        }

        @Override
        public void paint(Graphics g) {
            fullPaints.incrementAndGet();
            super.paint(g);
        }
    }

    @Test
    public void replacingTheCanvasSurfaceRepaintsTheWholeForm() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        final CountingForm form = new CountingForm();
        final Label child = new Label("child");
        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                form.add(BorderLayout.NORTH, child);
                form.show();
            }
        });
        settle();

        int fullPaintsBeforeSwap = form.fullPaints.get();
        assertTrue(fullPaintsBeforeSwap > 0, "the form must paint at least once before the swap");

        // Force exactly one surface swap with no size-change event behind it:
        // the buffer is sized from the canvas bounds times the retina scale, so
        // moving the scale invalidates the buffer geometry on its own. This is
        // the isolated form of what a HiDPI window resize does in the wild.
        double originalScale = JavaSEPort.retinaScale;
        try {
            JavaSEPort.retinaScale = originalScale == 1.0 ? 2.0 : 1.0;
            // Only one small component is dirty. Before the fix this was the
            // only thing repainted into the fresh black buffer.
            runOnCn1AndWait(new Runnable() {
                @Override
                public void run() {
                    child.repaint();
                }
            });
            settle();
        } finally {
            JavaSEPort.retinaScale = originalScale;
        }

        assertTrue(form.fullPaints.get() > fullPaintsBeforeSwap,
                "replacing the canvas buffer must invalidate the whole form - otherwise the new"
                + " blank surface keeps whatever the dirty list happened to hold and the window"
                + " reads as black (issue #5443)");
        settle();
    }

    // ---- helpers ----

    /** Lets a few paint cycles run so queued repaints reach the surface. */
    private void settle() throws Exception {
        for (int iter = 0; iter < 5; iter++) {
            runOnCn1AndWait(new Runnable() {
                @Override
                public void run() {
                }
            });
            Thread.sleep(60);
        }
    }

    private void runOnCn1AndWait(final Runnable r) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        Display.getInstance().callSerially(new Runnable() {
            @Override
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
