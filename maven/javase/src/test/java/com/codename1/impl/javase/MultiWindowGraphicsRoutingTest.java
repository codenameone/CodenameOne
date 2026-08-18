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
import com.codename1.ui.Image;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Guards the riskiest edit in the desktop-window work: the JavaSE port used to resolve
 * <em>all</em> screen graphics to a single canvas.
 *
 * <p>{@code getGraphics(Object)} fell through to the primary canvas's buffer for any
 * screen graphics, so a second window would have drawn into the first window's pixels.
 * {@code isScreenGraphics} was an identity comparison against that one buffer, and
 * {@code drawNativePeerImpl} uses its answer to decide whether to undo the zoom scale --
 * so a wrong answer for a second window mis-scales its peer components.</p>
 *
 * <p>Neither had any test coverage before, and both are in the paint path where a
 * regression shows up as wrong pixels rather than an exception.</p>
 *
 * @author Shai Almog
 */
@CodenameOneTest
class MultiWindowGraphicsRoutingTest {

    @Test
    void eachCanvasResolvesItsOwnScreenGraphics() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port, "the port should be booted by CodenameOneTest");

        JavaSEPort.C first = port.createWindowCanvas(1);
        JavaSEPort.C second = port.createWindowCanvas(2);
        first.setSize(320, 240);
        second.setSize(400, 300);

        Object gFirst = port.getNativeGraphics(first);
        Object gSecond = port.getNativeGraphics(second);
        assertNotNull(gFirst);
        assertNotNull(gSecond);

        Graphics2D awtFirst = port.getGraphics(gFirst);
        Graphics2D awtSecond = port.getGraphics(gSecond);
        assertNotNull(awtFirst);
        assertNotNull(awtSecond);
        assertNotSame(awtFirst, awtSecond,
                "two canvases must not share one screen buffer, or a second window "
                        + "would draw into the first window's pixels");
    }

    @Test
    void screenGraphicsIsRecognisedForEveryCanvasButNotForAMutableImage() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);

        JavaSEPort.C canvas = port.createWindowCanvas(3);
        canvas.setSize(200, 150);
        Graphics2D windowGraphics = port.getGraphics(port.getNativeGraphics(canvas));
        assertTrue(port.isScreenGraphics(windowGraphics),
                "a secondary window's buffer is still a screen buffer; answering false "
                        + "here mis-scales its native peers");

        // The primary canvas must keep answering true -- that is the behaviour the old
        // identity comparison had, and every existing baseline depends on it.
        Graphics2D primaryGraphics = port.getGraphics(port.getNativeGraphics());
        assertTrue(port.isScreenGraphics(primaryGraphics),
                "the primary canvas's buffer must still be recognised");

        // A mutable image is not a screen buffer and must not be mistaken for one.
        Image mutable = Image.createImage(64, 64);
        Graphics2D imageGraphics = port.getGraphics(port.getNativeGraphics(mutable.getImage()));
        assertFalse(port.isScreenGraphics(imageGraphics),
                "a mutable image's graphics must never be treated as a screen buffer");
    }

    @Test
    void layingOutASecondaryCanvasDoesNotResizeTheMainSurface() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);

        // C.setBounds and both branches of ancestorResized are primary-canvas logic
        // that a secondary canvas runs too, being the same class and the same
        // listener. They funnel into queueSizeChangeEvent, which resizes the *main*
        // surface -- so merely laying out a secondary frame resized the main form's
        // hierarchy to the secondary canvas's dimensions.
        JavaSEPort.C secondary = port.createWindowCanvas(7);

        // Driven through the funnel rather than through setBounds. setBounds only
        // reaches it when no skin is loaded, so a setBounds-based test passes with
        // the guard removed and proves nothing -- which is exactly what the first
        // version of this test did.
        java.lang.reflect.Method queue = JavaSEPort.C.class.getDeclaredMethod(
                "queueSizeChangeEvent", int.class, int.class,
                boolean.class, boolean.class, boolean.class, boolean.class);
        queue.setAccessible(true);
        queue.invoke(secondary, 137, 91, false, false, false, false);

        // The queued flag is cleared again once the queued runnable runs, so it is
        // not a reliable probe from here. The recorded width is not cleared, so it
        // still shows whether the main-surface resize was staged at all.
        java.lang.reflect.Field width =
                JavaSEPort.C.class.getDeclaredField("pendingSizeChangeWidth");
        width.setAccessible(true);
        // -1 is the field's initial value, i.e. nothing was ever staged.
        assertEquals(-1, width.getInt(secondary),
                "a secondary canvas must not stage a main-surface resize; its own size "
                        + "is reported window-tagged through componentResized");
    }
}
