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
import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import com.codename1.ui.layouts.BorderLayout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The semantic tree describes its nodes in the device pixels Codename One lays out in,
 * while Swing accessibility asks and answers in the canvas's own coordinates. On a
 * display with a backing scale those are not the same, so a reader was told each element
 * sat at twice its position and was twice its size, and the point it handed back for hit
 * testing landed at half the intended place.
 */
@CodenameOneTest
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
public class JavaSEAccessibilityScaleTest {

    @Test
    public void boundsAndHitTestingAreInCanvasCoordinates() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assumeTrue(port != null, "needs the JavaSE port");

        final Form form = new Form("accessible", new BorderLayout());
        final Button b = new Button("press me");
        final AtomicReference<AccessibilityTreeSnapshot> built =
                new AtomicReference<AccessibilityTreeSnapshot>();
        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                form.add(BorderLayout.CENTER, b);
                form.show();
            }
        });
        // Built on the event thread, which is the only place a miss is filled in: asked
        // for from here it would come back empty and the test would quietly skip.
        runOnCn1AndWait(new Runnable() {
            @Override
            public void run() {
                built.set(com.codename1.ui.accessibility.AccessibilityInspector.snapshot(form));
            }
        });

        double original = JavaSEPort.retinaScale;
        try {
            JavaSEPort.retinaScale = 2.0;
            JavaSEAccessibility bridge =
                    new JavaSEAccessibility((javax.swing.JPanel) port.getCanvas(), port, 0);

            AccessibilityTreeSnapshot tree = built.get();
            assertNotNull(tree, "precondition: there is a tree");
            AccessibilityNodeSnapshot node = findByLabel(tree, "press me");
            assertNotNull(node, "precondition: the button is described in the tree");

            // The node's own bounds, which are the pixels the layout used.
            com.codename1.ui.geom.Rectangle device = node.getBounds();
            assertTrue(device.getWidth() > 1 && device.getHeight() > 1,
                    "precondition: the button has real bounds");

            AccessibleComponent root = (AccessibleComponent) bridge.getContext();
            // Asked for in canvas coordinates, which at this scale are half the pixels.
            Point inCanvas = new Point(
                    (int) ((device.getX() + device.getWidth() / 2) / 2.0),
                    (int) ((device.getY() + device.getHeight() / 2) / 2.0));
            Accessible found = root.getAccessibleAt(inCanvas);
            assertNotNull(found, "the point the reader uses has to find the element under it");

            AccessibleComponent c = (AccessibleComponent) found.getAccessibleContext();
            java.awt.Rectangle reported = c.getBounds();
            assertEquals((int) Math.round(device.getWidth() / 2.0), reported.width,
                    "the width a reader is told has to be in its own coordinates");
            assertTrue(reported.height > 0);
        } finally {
            JavaSEPort.retinaScale = original;
        }
    }

    private AccessibilityNodeSnapshot findByLabel(AccessibilityTreeSnapshot tree, String label) {
        for (Long id : tree.getNodes().keySet()) {
            AccessibilityNodeSnapshot n = tree.getNode(id.longValue());
            if (n != null && label.equals(n.getLabel())) {
                return n;
            }
        }
        return null;
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
