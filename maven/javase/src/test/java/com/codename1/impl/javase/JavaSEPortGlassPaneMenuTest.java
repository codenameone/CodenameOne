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

import java.awt.Point;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for simulator menu mouse routing through the HiDPI glass pane:
 * <a href="https://github.com/codenameone/CodenameOne/issues/2686">#2686</a> and
 * <a href="https://github.com/codenameone/CodenameOne/issues/5430">#5430</a>.
 */
public class JavaSEPortGlassPaneMenuTest {

    @Test
    public void glassPaneDoesNotInterceptEmbeddedMenuBar() throws Exception {
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Fixture fixture = new Fixture();
                    Point point = SwingUtilities.convertPoint(
                            fixture.deviceMenu, 10, 10, fixture.glassPane);
                    Point layeredPoint = SwingUtilities.convertPoint(
                            fixture.glassPane, point, fixture.rootPane.getLayeredPane());

                    assertSame(fixture.deviceMenu, SwingUtilities.getDeepestComponentAt(
                            fixture.rootPane.getLayeredPane(), layeredPoint.x, layeredPoint.y),
                            "the regression fixture must target the embedded JMenu");

                    assertFalse(JavaSEPort.shouldGlassPaneInterceptMouseEvent(
                            fixture.glassPane, fixture.canvas, point.x, point.y),
                            "the HiDPI glass pane must leave embedded JMenu clicks to Swing");
                } catch (Throwable t) {
                    failure.set(t);
                }
            }
        });
        rethrow(failure.get());
    }

    @Test
    public void glassPaneDoesNotInterceptPopupItemOverCanvas() throws Exception {
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Fixture fixture = new Fixture();
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem item = new JMenuItem("Rotate");
                    popup.add(item);
                    popup.setBounds(0, 24, 140, 30);
                    item.setBounds(1, 1, 138, 28);
                    fixture.rootPane.getLayeredPane().add(
                            popup, JLayeredPane.POPUP_LAYER);
                    popup.setVisible(true);

                    Point point = SwingUtilities.convertPoint(
                            item, 10, 10, fixture.glassPane);
                    Point canvasPoint = SwingUtilities.convertPoint(
                            fixture.glassPane, point, fixture.canvas);
                    Point layeredPoint = SwingUtilities.convertPoint(
                            fixture.glassPane, point, fixture.rootPane.getLayeredPane());

                    assertTrue(fixture.canvas.getVisibleRect().contains(canvasPoint),
                            "the regression fixture must place the popup item geometrically over the canvas");
                    assertSame(item, SwingUtilities.getDeepestComponentAt(
                            fixture.rootPane.getLayeredPane(), layeredPoint.x, layeredPoint.y),
                            "the regression fixture must target the popup JMenuItem above the canvas");

                    assertFalse(JavaSEPort.shouldGlassPaneInterceptMouseEvent(
                            fixture.glassPane, fixture.canvas, point.x, point.y),
                            "a popup JMenuItem over the canvas must receive its own click");
                } catch (Throwable t) {
                    failure.set(t);
                }
            }
        });
        rethrow(failure.get());
    }

    @Test
    public void glassPaneStillInterceptsCanvasEvents() throws Exception {
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Fixture fixture = new Fixture();
                    Point point = SwingUtilities.convertPoint(
                            fixture.canvas, 200, 200, fixture.glassPane);

                    assertTrue(JavaSEPort.shouldGlassPaneInterceptMouseEvent(
                            fixture.glassPane, fixture.canvas, point.x, point.y),
                            "ordinary simulator-canvas events must still pass through the HiDPI dispatcher");
                } catch (Throwable t) {
                    failure.set(t);
                }
            }
        });
        rethrow(failure.get());
    }

    private static void rethrow(Throwable failure) throws Exception {
        if (failure == null) {
            return;
        }
        if (failure instanceof Exception) {
            throw (Exception) failure;
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        throw new RuntimeException(failure);
    }

    private static final class Fixture {
        private final JRootPane rootPane = new JRootPane();
        private final JPanel canvas = new JPanel();
        private final JComponent glassPane = new JComponent() {
        };
        private final JMenu deviceMenu = new JMenu("Device");

        private Fixture() {
            rootPane.setSize(400, 400);
            rootPane.getLayeredPane().setSize(400, 400);

            JPanel contentPane = new JPanel(null);
            contentPane.setBounds(0, 24, 400, 376);
            canvas.setBounds(0, 0, 400, 376);
            contentPane.add(canvas);
            rootPane.setContentPane(contentPane);

            JMenuBar menuBar = new JMenuBar();
            menuBar.setBounds(0, 0, 400, 24);
            deviceMenu.setBounds(0, 0, 80, 24);
            menuBar.add(deviceMenu);
            rootPane.setJMenuBar(menuBar);

            rootPane.setGlassPane(glassPane);
            glassPane.setBounds(0, 0, 400, 400);
            glassPane.setVisible(true);
        }
    }
}
