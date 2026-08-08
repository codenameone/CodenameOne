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

package com.codename1.guibuilder;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
import java.awt.Desktop;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;

public final class CodenameOneGUIBuilderStub implements Runnable, WindowListener {
    static final String APP_DISPLAY_NAME = "Codename One GUI Builder";
    private static final String APP_STORAGE_NAME = "CodenameOneGUIBuilder";
    private static final int APP_WIDTH = 1440;
    private static final int APP_HEIGHT = 900;
    private static JFrame frame;
    private CodenameOneGUIBuilder app;

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", APP_DISPLAY_NAME);
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_DISPLAY_NAME);
        System.setProperty("sun.awt.application.name", APP_DISPLAY_NAME);
        System.setProperty("sun.awt.X11.XWMClass", APP_STORAGE_NAME);
        JavaSEPort.setNativeTheme("/NativeTheme.res");
        JavaSEPort.blockMonitors();
        JavaSEPort.setAppHomeDir("." + APP_STORAGE_NAME);
        JavaSEPort.setExposeFilesystem(true);
        JavaSEPort.setTablet(true);
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setShowEDTWarnings(false);
        JavaSEPort.setDesktopTitleBarMode("native");
        JavaSEPort.setDesktopInteractiveScrollbars(true);
        JavaSEPort.setFontFaces(File.separatorChar == '\\' ? "ArialUnicodeMS" : "Arial", "SansSerif", "Monospaced");
        frame = new JFrame(APP_DISPLAY_NAME);
        JavaSEPort.setDefaultPixelMilliRatio(Toolkit.getDefaultToolkit().getScreenResolution() / 25.4 * JavaSEPort.getRetinaScale());
        Display.init(frame.getContentPane());
        Display.getInstance().setProperty("AppName", APP_DISPLAY_NAME);
        SwingUtilities.invokeLater(new CodenameOneGUIBuilderStub());
    }

    @Override
    public void run() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        installMenus();
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        frame.setLocationByPlatform(true);
        frame.setResizable(true);
        frame.getContentPane().setPreferredSize(new java.awt.Dimension(APP_WIDTH, APP_HEIGHT));
        frame.getContentPane().setMinimumSize(new java.awt.Dimension(1000, 650));
        frame.pack();
        frame.setVisible(true);
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() {
                app = new CodenameOneGUIBuilder();
                app.init(this);
                app.start();
                SwingUtilities.invokeLater(() -> {
                    installDesignerPointerBridge();
                    installMenus();
                    scheduleOpenCss();
                    scheduleScreenshot();
                    scheduleEditorSelfTest();
                    scheduleGuidedLayoutSelfTest();
                    scheduleInteractionSelfTest();
                });
            }
        });
    }

    private static void scheduleOpenCss() {
        if (!Boolean.getBoolean("guibuilder.openCss")) return;
        Timer open = new Timer(700, e -> Display.getInstance().callSerially(CodenameOneGUIBuilder::openActiveCssForTest));
        open.setRepeats(false);
        open.start();
    }

    private static void installDesignerPointerBridge() {
        java.awt.Container canvas = JavaSEPort.instance.getCanvas();
        canvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseDragged(java.awt.event.MouseEvent event) {
                int x = (int) Math.round(event.getX() * JavaSEPort.getRetinaScale());
                int y = (int) Math.round(event.getY() * JavaSEPort.getRetinaScale());
                Display.getInstance().callSerially(() -> CodenameOneGUIBuilder.activeDesktopPointerDragged(x, y));
            }
        });
        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseReleased(java.awt.event.MouseEvent event) {
                int x = (int) Math.round(event.getX() * JavaSEPort.getRetinaScale());
                int y = (int) Math.round(event.getY() * JavaSEPort.getRetinaScale());
                Display.getInstance().callSerially(() -> CodenameOneGUIBuilder.activeDesktopPointerReleased(x, y));
            }
        });
    }

    private static void scheduleEditorSelfTest() {
        if (!Boolean.getBoolean("guibuilder.selfTest")) return;
        Timer open = new Timer(900, e -> Display.getInstance().callSerially(CodenameOneGUIBuilder::openActiveCssForTest));
        open.setRepeats(false);
        open.start();
        Timer exercise = new Timer(2200, e -> new Thread(() -> {
            try {
                final JTextPane[] editor = new JTextPane[1];
                SwingUtilities.invokeAndWait(() -> {
                    frame.setAlwaysOnTop(true);
                    frame.toFront();
                    frame.requestFocus();
                    editor[0] = findTextPane(frame.getLayeredPane());
                });
                if (editor[0] == null) throw new AssertionError("No live JTextPane was mounted by theme.css");
                final javax.swing.JScrollPane[] scroll = new javax.swing.JScrollPane[1];
                SwingUtilities.invokeAndWait(() -> {
                    java.awt.Container parent = editor[0].getParent();
                    while (parent != null && !(parent instanceof javax.swing.JScrollPane)) parent = parent.getParent();
                    scroll[0] = (javax.swing.JScrollPane) parent;
                    if (scroll[0] == null || editor[0].getMargin().left < 40
                            || !scroll[0].getVerticalScrollBar().isVisible()
                            || scroll[0].getVerticalScrollBar().getWidth() < 10) {
                        throw new AssertionError("CSS editor chrome is missing: editorMargin="
                                + editor[0].getMargin() + ", verticalBar="
                                + (scroll[0] == null ? null : scroll[0].getVerticalScrollBar()));
                    }
                });
                final java.awt.Point[] point = new java.awt.Point[1];
                final int[] initialLength = new int[1];
                SwingUtilities.invokeAndWait(() -> {
                    initialLength[0] = editor[0].getDocument().getLength();
                    editor[0].setCaretPosition(Math.min(8, initialLength[0]));
                    point[0] = editor[0].getLocationOnScreen();
                });
                Robot robot = new Robot();
                robot.setAutoDelay(45);
                robot.mouseMove(frame.getX() + frame.getWidth() / 2, frame.getY() + 10);
                robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseMove(point[0].x + 40, point[0].y + 24);
                robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                SwingUtilities.invokeAndWait(() -> {
                    frame.toFront();
                    frame.requestFocus();
                    editor[0].requestFocus();
                });
                long focusDeadline = System.currentTimeMillis() + 1800;
                while (!editor[0].isFocusOwner() && System.currentTimeMillis() < focusDeadline) {
                    robot.mouseMove(point[0].x + 40, point[0].y + 24);
                    robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                    SwingUtilities.invokeAndWait(() -> {
                        frame.toFront();
                        editor[0].requestFocus();
                    });
                    Thread.sleep(120);
                }
                if (!editor[0].isFocusOwner()) throw new AssertionError("macOS did not focus the mounted CSS editor");
                int[] keys = {KeyEvent.VK_Z, KeyEvent.VK_Z, KeyEvent.VK_T, KeyEvent.VK_E, KeyEvent.VK_S, KeyEvent.VK_T};
                for (int key : keys) { robot.keyPress(key); robot.keyRelease(key); }
                robot.waitForIdle();
                final String[] text = new String[1];
                final String[] editorState = new String[1];
                SwingUtilities.invokeAndWait(() -> {
                    text[0] = editor[0].getText();
                    java.awt.Component focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                    editorState[0] = "editable=" + editor[0].isEditable() + ", focusOwner=" + focus
                            + ", editorBounds=" + editor[0].getBounds() + ", showing=" + editor[0].isShowing()
                            + ", glassVisible=" + frame.getGlassPane().isVisible();
                });
                if (text[0].length() <= initialLength[0] || text[0].toLowerCase().indexOf("zztest") < 0) {
                    throw new AssertionError("Physical typing did not mutate the actual theme.css editor: " + editorState[0]);
                }
                int typedLength = text[0].length();
                int shortcutKey = (menuShortcutMask()
                        & java.awt.event.InputEvent.META_DOWN_MASK) != 0 ? KeyEvent.VK_META : KeyEvent.VK_CONTROL;
                robot.keyPress(shortcutKey);
                robot.keyPress(KeyEvent.VK_Z);
                robot.keyRelease(KeyEvent.VK_Z);
                robot.keyRelease(shortcutKey);
                robot.waitForIdle();
                final int[] undoLength = new int[1];
                final JTextPane[] stillMounted = new JTextPane[1];
                SwingUtilities.invokeAndWait(() -> {
                    undoLength[0] = editor[0].getDocument().getLength();
                    stillMounted[0] = findTextPane(frame.getLayeredPane());
                });
                if (stillMounted[0] != editor[0] || undoLength[0] >= typedLength) {
                    throw new AssertionError("Command-Z escaped the focused CSS editor instead of using its undo buffer");
                }
                SwingUtilities.invokeAndWait(() -> editor[0].setText("Button { color: #e11919; }"));
                Thread.sleep(450);
                java.util.concurrent.atomic.AtomicInteger previewColor = new java.util.concurrent.atomic.AtomicInteger(-1);
                Display.getInstance().callSerially(() -> previewColor.set(
                        CodenameOneGUIBuilder.activePreviewForegroundForTest(null)));
                long deadline = System.currentTimeMillis() + 2500;
                while (previewColor.get() == -1 && System.currentTimeMillis() < deadline) Thread.sleep(25);
                if (previewColor.get() != 0xe11919) {
                    throw new AssertionError("CSS document changed but live preview color was 0x"
                            + Integer.toHexString(previewColor.get()));
                }
                System.out.println("GUIBUILDER_EDITOR_SELF_TEST_PASS");
                if (!"false".equals(System.getProperty("guibuilder.selfTest.exit"))) System.exit(0);
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.err.println("GUIBUILDER_EDITOR_SELF_TEST_FAIL");
                System.exit(2);
            }
        }, "GUIBuilder editor self-test").start());
        exercise.setRepeats(false);
        exercise.start();
    }

    private static JTextPane findTextPane(java.awt.Container parent) {
        for (java.awt.Component child : parent.getComponents()) {
            if (child instanceof JTextPane) return (JTextPane) child;
            if (child instanceof java.awt.Container) {
                JTextPane found = findTextPane((java.awt.Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void scheduleGuidedLayoutSelfTest() {
        if (!Boolean.getBoolean("guibuilder.guidedSelfTest")) return;
        Timer exercise = new Timer(1800, e -> new Thread(() -> {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    frame.setAlwaysOnTop(true);
                    frame.toFront();
                    frame.requestFocus();
                });
                final int[][] initial = new int[1][];
                final int[][] sameWidth = new int[1][];
                final int[] baselines = new int[2];
                Display.getInstance().callSerially(() -> {
                    initial[0] = CodenameOneGUIBuilder.activePreviewBoundsForTest("primary");
                    sameWidth[0] = CodenameOneGUIBuilder.activePreviewBoundsForTest("secondary");
                    baselines[0] = CodenameOneGUIBuilder.activePreviewBaselineForTest("baselineLabel");
                    baselines[1] = CodenameOneGUIBuilder.activePreviewBaselineForTest("baselineAction");
                });
                waitFor(() -> initial[0] != null && sameWidth[0] != null && baselines[0] >= 0 && baselines[1] >= 0, 2500);
                if (initial[0][2] != sameWidth[0][2]) throw new AssertionError("Initial same-width relationship is not rendered");
                if (Math.abs(baselines[0] - baselines[1]) > 1) throw new AssertionError("Baseline relationship differs by "
                        + Math.abs(baselines[0] - baselines[1]) + "px");

                Robot robot = new Robot();
                robot.setAutoDelay(55);
                java.awt.Container canvas = JavaSEPort.instance.getCanvas();
                java.awt.Point canvasPoint = canvas.getLocationOnScreen();
                double scale = JavaSEPort.getRetinaScale();
                int startX = canvasPoint.x + (int) Math.round((initial[0][0] + initial[0][2] / 2.0) / scale);
                int startY = canvasPoint.y + (int) Math.round((initial[0][1] + initial[0][3] / 2.0) / scale);
                System.out.println("GUIDED_SELF_TEST_COORDS frame=" + frame.getBounds() + " canvas=" + canvas.getBounds()
                        + " canvasScreen=" + canvasPoint + " scale=" + scale + " initial="
                        + java.util.Arrays.toString(initial[0]) + " robotStart=" + startX + "," + startY);
                drag(robot, startX, startY, startX + (int) Math.round(70 / scale), startY + (int) Math.round(60 / scale));
                Thread.sleep(650);

                final int[][] moved = new int[1][];
                long moveDeadline = System.currentTimeMillis() + 2500;
                while (System.currentTimeMillis() < moveDeadline) {
                    moved[0] = activeBounds("primary");
                    if (moved[0] != null && (moved[0][0] != initial[0][0] || moved[0][1] != initial[0][1])) break;
                    Thread.sleep(60);
                }
                if (moved[0] == null || moved[0][0] == initial[0][0] && moved[0][1] == initial[0][1]) {
                    throw new AssertionError("Physical body drag did not change bounds: initial="
                            + java.util.Arrays.toString(initial[0]) + " last=" + java.util.Arrays.toString(moved[0])
                            + " designer=" + activeDesignerState());
                }
                if (Math.abs(moved[0][0] - initial[0][0]) < 30 || Math.abs(moved[0][1] - initial[0][1]) < 25) {
                    throw new AssertionError("Physical body drag did not commit near the guide: initial="
                            + java.util.Arrays.toString(initial[0]) + " moved=" + java.util.Arrays.toString(moved[0]));
                }

                int resizeX = canvasPoint.x + (int) Math.round((moved[0][0] + moved[0][2]) / scale);
                int resizeY = canvasPoint.y + (int) Math.round((moved[0][1] + moved[0][3] / 2.0) / scale);
                drag(robot, resizeX, resizeY, resizeX + (int) Math.round(100 / scale), resizeY);
                Thread.sleep(650);

                final int[][] resized = new int[1][];
                final int[][] linked = new int[1][];
                final String[] policy = new String[1];
                long resizeDeadline = System.currentTimeMillis() + 2500;
                while (System.currentTimeMillis() < resizeDeadline) {
                    resized[0] = activeBounds("primary");
                    linked[0] = activeBounds("secondary");
                    policy[0] = activeAttribute("primary", "guidedHorizontalSize");
                    if (resized[0] != null && linked[0] != null && resized[0][2] > moved[0][2] + 50) break;
                    Thread.sleep(60);
                }
                if (resized[0] == null || resized[0][2] <= moved[0][2] + 50) {
                    throw new AssertionError("Physical edge resize did not grow the width: moved="
                            + java.util.Arrays.toString(moved[0]) + " last=" + java.util.Arrays.toString(resized[0]));
                }
                if (Math.abs(resized[0][0] - moved[0][0]) > 2 || Math.abs(resized[0][1] - moved[0][1]) > 2) {
                    throw new AssertionError("Right-edge resize moved an untouched edge: moved="
                            + java.util.Arrays.toString(moved[0]) + " resized=" + java.util.Arrays.toString(resized[0]));
                }
                if (!"fixed".equals(policy[0])) throw new AssertionError("Resize did not persist the fixed horizontal policy: " + policy[0]);
                if (resized[0][2] != linked[0][2]) throw new AssertionError("Linked width did not update after physical resize: "
                        + resized[0][2] + " vs " + linked[0][2]);
                System.out.println("GUIBUILDER_GUIDED_LAYOUT_SELF_TEST_PASS moved="
                        + java.util.Arrays.toString(moved[0]) + " resized=" + java.util.Arrays.toString(resized[0]));
                if (!"false".equals(System.getProperty("guibuilder.guidedSelfTest.exit"))) System.exit(0);
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.err.println("GUIBUILDER_GUIDED_LAYOUT_SELF_TEST_FAIL");
                System.exit(3);
            }
        }, "GUIBuilder guided-layout self-test").start());
        exercise.setRepeats(false);
        exercise.start();
    }

    private static void scheduleInteractionSelfTest() {
        if (!Boolean.getBoolean("guibuilder.interactionSelfTest")) return;
        Timer exercise = new Timer(1800, e -> new Thread(() -> {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    frame.setAlwaysOnTop(true);
                    frame.toFront();
                    frame.requestFocus();
                });
                int[] primary = activeBounds("primary");
                int[] secondary = activeBounds("secondary");
                if (primary == null || secondary == null) throw new AssertionError("Guided fixture components are missing");
                Robot robot = new Robot();
                robot.setAutoDelay(55);
                clickCn1(robot, secondary[0] + secondary[2] / 2, secondary[1] + secondary[3] / 2);
                waitFor(() -> "secondary".equals(CodenameOneGUIBuilder.activeSelectedNameForTest()), 2000);
                clickCn1(robot, primary[0] + primary[2] / 2, primary[1] + primary[3] / 2);
                waitFor(() -> "primary".equals(CodenameOneGUIBuilder.activeSelectedNameForTest()), 2000);

                int[] desktop = activeNamedUiBounds("Desktop — full canvas");
                if (desktop == null) throw new AssertionError("Desktop canvas-mode button is missing");
                System.out.println("INTERACTION_SELF_TEST_DESKTOP bounds=" + java.util.Arrays.toString(desktop)
                        + " designer=" + activeDesignerState() + " mode=" + CodenameOneGUIBuilder.activeCanvasModeForTest());
                clickCn1(robot, desktop[0] + desktop[2] / 2, desktop[1] + desktop[3] / 2);
                try {
                    waitFor(() -> "desktop".equals(CodenameOneGUIBuilder.activeCanvasModeForTest()), 2500);
                } catch (AssertionError timeout) {
                    throw new AssertionError("Desktop mode click failed: bounds=" + java.util.Arrays.toString(desktop)
                            + " designer=" + activeDesignerState() + " mode="
                            + CodenameOneGUIBuilder.activeCanvasModeForTest(), timeout);
                }
                int[] landscape = activeNamedUiBounds("Phone landscape");
                if (landscape == null) throw new AssertionError("Phone landscape button is missing after refresh");
                clickCn1(robot, landscape[0] + landscape[2] / 2, landscape[1] + landscape[3] / 2);
                waitFor(() -> "phoneLandscape".equals(CodenameOneGUIBuilder.activeCanvasModeForTest()), 2500);
                System.out.println("GUIBUILDER_INTERACTION_SELF_TEST_PASS selection=primary mode=phoneLandscape");
                System.exit(0);
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.err.println("GUIBUILDER_INTERACTION_SELF_TEST_FAIL");
                System.exit(4);
            }
        }, "GUIBuilder interaction self-test").start());
        exercise.setRepeats(false);
        exercise.start();
    }

    private static int[] activeNamedUiBounds(String name) throws Exception {
        final int[][] value = new int[1][];
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Display.getInstance().callSerially(() -> {
            value[0] = CodenameOneGUIBuilder.activeNamedUiBoundsForTest(name);
            latch.countDown();
        });
        if (!latch.await(1, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("EDT did not return UI bounds");
        return value[0];
    }

    private static void clickCn1(Robot robot, int cn1X, int cn1Y) throws Exception {
        java.awt.Container canvas = JavaSEPort.instance.getCanvas();
        java.awt.Point canvasPoint = canvas.getLocationOnScreen();
        double scale = JavaSEPort.getRetinaScale();
        robot.mouseMove(canvasPoint.x + (int) Math.round(cn1X / scale),
                canvasPoint.y + (int) Math.round(cn1Y / scale));
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(120);
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }

    private static void drag(Robot robot, int fromX, int fromY, int toX, int toY) {
        robot.mouseMove(fromX, fromY);
        robot.delay(180);
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(220);
        int steps = 12;
        for (int i = 1; i <= steps; i++) {
            robot.mouseMove(fromX + (toX - fromX) * i / steps, fromY + (toY - fromY) * i / steps);
            robot.delay(35);
        }
        robot.delay(180);
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }

    private static void waitFor(java.util.function.BooleanSupplier condition, long timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) Thread.sleep(30);
        if (!condition.getAsBoolean()) throw new AssertionError("Timed out waiting for Guided Layout UI state");
    }

    private static int[] activeBounds(String name) throws Exception {
        final int[][] value = new int[1][];
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Display.getInstance().callSerially(() -> {
            value[0] = CodenameOneGUIBuilder.activePreviewBoundsForTest(name);
            latch.countDown();
        });
        if (!latch.await(1, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("EDT did not return preview bounds");
        return value[0];
    }

    private static String activeAttribute(String name, String attribute) throws Exception {
        final String[] value = new String[1];
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Display.getInstance().callSerially(() -> {
            value[0] = CodenameOneGUIBuilder.activeDocumentAttributeForTest(name, attribute);
            latch.countDown();
        });
        if (!latch.await(1, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("EDT did not return document attribute");
        return value[0];
    }

    private static String activeDesignerState() throws Exception {
        final String[] value = new String[1];
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Display.getInstance().callSerially(() -> {
            value[0] = CodenameOneGUIBuilder.activeDesignerStateForTest();
            latch.countDown();
        });
        latch.await(1, java.util.concurrent.TimeUnit.SECONDS);
        return value[0];
    }

    private static void installMenus() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(item("Save Form", KeyEvent.VK_S, true, onCn1Edt(CodenameOneGUIBuilder::saveActiveDocument)));
        file.add(item("Reload Project Forms", KeyEvent.VK_R, true, onCn1Edt(CodenameOneGUIBuilder::refreshActiveProject)));
        file.addSeparator();
        file.add(item("Close GUI Builder", KeyEvent.VK_W, true, onCn1Edt(CodenameOneGUIBuilderStub::exitWithConfirmation)));

        JMenu edit = new JMenu("Edit");
        edit.add(item("Undo", KeyEvent.VK_Z, true, onCn1Edt(CodenameOneGUIBuilderStub::undoFocusedEditorOrForm)));
        JMenuItem redo = item("Redo", 0, false, onCn1Edt(CodenameOneGUIBuilderStub::redoFocusedEditorOrForm));
        redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                menuShortcutMask() | KeyEvent.SHIFT_DOWN_MASK));
        edit.add(redo);
        edit.addSeparator();
        edit.add(item("Cut", KeyEvent.VK_X, true, () -> editFocusedTextOrForm("cut")));
        edit.add(item("Copy", KeyEvent.VK_C, true, () -> editFocusedTextOrForm("copy")));
        edit.add(item("Paste", KeyEvent.VK_V, true, () -> editFocusedTextOrForm("paste")));
        edit.addSeparator();
        // Bare Backspace, so it competes with typing: guard it, or deleting a character in the code
        // editor deletes the selected component from the design instead.
        edit.add(item("Delete Component", KeyEvent.VK_BACK_SPACE, false,
                onCn1Edt(() -> { if (!isCodeEditorFocused()) CodenameOneGUIBuilder.deleteActiveSelection(); })));

        JMenu view = new JMenu("View");
        JCheckBoxMenuItem dark = new JCheckBoxMenuItem("Dark Mode", CodenameOneGUIBuilder.isActiveDarkMode());
        dark.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, menuShortcutMask()));
        dark.addActionListener(e -> com.codename1.ui.CN.callSerially(CodenameOneGUIBuilder::toggleActiveDarkMode));
        view.add(dark);
        view.addSeparator();
        view.add(item("Refresh Canvas", KeyEvent.VK_0, true, onCn1Edt(CodenameOneGUIBuilder::refreshActiveProject)));

        JMenu forms = new JMenu("Forms");
        String[] names = CodenameOneGUIBuilder.activeFormNames();
        for (int i = 0; i < names.length; i++) {
            final int formIndex = i;
            String name = names[i];
            String simple = name.substring(name.lastIndexOf('.') + 1);
            JMenuItem form = item(simple, i < 9 ? KeyEvent.VK_1 + i : 0, i < 9,
                    onCn1Edt(() -> CodenameOneGUIBuilder.openActiveForm(formIndex)));
            form.setToolTipText(name);
            forms.add(form);
        }

        JMenu code = new JMenu("Editors");
        code.add(item("Edit Companion Java Source", KeyEvent.VK_J, true, onCn1Edt(CodenameOneGUIBuilder::openActiveSource)));
        code.add(item("Edit Binding Model", KeyEvent.VK_K, true, onCn1Edt(CodenameOneGUIBuilder::openActiveModel)));
        code.add(item("Edit theme.css with Live Preview", KeyEvent.VK_T, true, onCn1Edt(CodenameOneGUIBuilder::openActiveCss)));
        bar.add(file);
        bar.add(edit);
        bar.add(view);
        bar.add(forms);
        bar.add(code);
        frame.setJMenuBar(bar);
        installAboutHandler(frame);
    }

    /**
     * The menu shortcut modifier. {@code getMenuShortcutKeyMaskEx} arrived in Java 10 and this
     * editor is a Java 8 artifact, so the older accessor is used; it reports the legacy mask
     * constants, which {@code KeyStroke} and {@code InputEvent} still understand.
     */
    @SuppressWarnings("deprecation")
    private static int menuShortcutMask() {
        // Converted to the extended constants the call sites use. Java 8 only offers the legacy
        // accessor, and mixing a legacy mask into an accelerator built from *_DOWN_MASK values
        // yields a modifier combination that never matches.
        int legacy = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        if ((legacy & java.awt.event.InputEvent.META_MASK) != 0) return java.awt.event.InputEvent.META_DOWN_MASK;
        if ((legacy & java.awt.event.InputEvent.ALT_MASK) != 0) return java.awt.event.InputEvent.ALT_DOWN_MASK;
        if ((legacy & java.awt.event.InputEvent.SHIFT_MASK) != 0) return java.awt.event.InputEvent.SHIFT_DOWN_MASK;
        return java.awt.event.InputEvent.CTRL_DOWN_MASK;
    }

    /**
     * Installs the About entry in the application menu. {@code Desktop.Action.APP_ABOUT} and
     * {@code setAboutHandler} are Java 9 additions, so they are invoked reflectively: on 8 the menu
     * simply has no About entry rather than the whole editor failing to build.
     *
     * @param frame the window the dialog is shown over
     */
    private static void installAboutHandler(final JFrame frame) {
        try {
            Class<?> actionClass = Class.forName("java.awt.Desktop$Action");
            Object appAbout = actionClass.getField("APP_ABOUT").get(null);
            if (!Desktop.isDesktopSupported()) return;
            Desktop desktop = Desktop.getDesktop();
            java.lang.reflect.Method supported = Desktop.class.getMethod("isSupported", actionClass);
            if (!Boolean.TRUE.equals(supported.invoke(desktop, appAbout))) return;
            Class<?> handlerClass = Class.forName("java.awt.desktop.AboutHandler");
            Object handler = java.lang.reflect.Proxy.newProxyInstance(
                    CodenameOneGUIBuilderStub.class.getClassLoader(), new Class<?>[]{handlerClass},
                    (proxy, method, args) -> {
                        if ("handleAbout".equals(method.getName())) {
                            javax.swing.JOptionPane.showMessageDialog(frame,
                                    "Modern Maven-first visual editor for Codename One GUI forms.",
                                    APP_DISPLAY_NAME, javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        }
                        return null;
                    });
            Desktop.class.getMethod("setAboutHandler", handlerClass).invoke(desktop, handler);
        } catch (Throwable olderJdkOrHeadless) {
            // Java 8, or a desktop that does not offer an application menu.
        }
    }

    /**
     * Routes undo or redo to a focused Codename One editor, exactly as the clipboard bridge routes
     * cut and copy.
     *
     * <p>The Swing focus owner stays on the canvas while a pure {@code EditorView} has the Codename
     * One focus, and nothing installs the {@code cn1.codeEditorUndoManager} client property that
     * was queried here, so the accelerator always reached the fallback: Cmd/Ctrl+Z rebuilt the
     * canvas by undoing the GUI document while the source, CSS or model edit it was aimed at stayed
     * exactly as it was.
     *
     * @param redo true to redo, false to undo
     * @return true when a focused editor handled it
     */
    private static boolean undoFocusedCodeEditor(final boolean redo) {
        com.codename1.ui.Form form = com.codename1.ui.CN.getCurrentForm();
        com.codename1.ui.Component focused = form == null ? null : form.getFocused();
        if (!(focused instanceof com.codename1.ui.editor.EditorView)) return false;
        final com.codename1.ui.editor.EditorView view = (com.codename1.ui.editor.EditorView) focused;
        if (view.getComponentForm() == null) return false;
        com.codename1.ui.CN.callSerially(new Runnable() {
            @Override public void run() {
                if (redo) view.performRedo(); else view.performUndo();
            }
        });
        return true;
    }

    private static void undoFocusedEditorOrForm() {
        if (undoFocusedCodeEditor(false)) return;
        CodenameOneGUIBuilder.undoActiveEdit();
    }

    /** Exits only once the editor has confirmed there is nothing unsaved to lose. */
    private static void exitWithConfirmation() {
        if (!CodenameOneGUIBuilder.confirmActiveExit()) return;
        Display.getInstance().exitApplication();
    }

    private static void redoFocusedEditorOrForm() {
        if (undoFocusedCodeEditor(true)) return;
        CodenameOneGUIBuilder.redoActiveEdit();
    }

    private static void editFocusedTextOrForm(final String operation) {
        // A menu accelerator consumes the keystroke before Codename One ever sees it, and the AWT
        // focus owner is always the canvas rather than a Swing text component, so without this the
        // Edit menu treated Cmd+V inside the code editor as "paste a component into the design".
        // That rebuilt the canvas and tore down the split pane the editor was living in.
        if (editFocusedCodeEditor(operation)) return;
        java.awt.Component focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focus instanceof JTextComponent) {
            JTextComponent text = (JTextComponent) focus;
            if ("cut".equals(operation)) text.cut();
            else if ("copy".equals(operation)) text.copy();
            else text.paste();
            return;
        }
        // Only the Swing branch above has to stay on the AWT thread. This fallback mutates the
        // GuiDocument and rebuilds Codename One components, so it belongs on the Codename One EDT
        // like every other menu command; excluding the whole clipboard entry from onCn1Edt left it
        // running here on the wrong thread.
        com.codename1.ui.CN.callSerially(new Runnable() {
            @Override public void run() {
                if ("cut".equals(operation)) CodenameOneGUIBuilder.cutActiveSelection();
                else if ("copy".equals(operation)) CodenameOneGUIBuilder.copyActiveSelection();
                else CodenameOneGUIBuilder.pasteActiveSelection();
            }
        });
    }

    /** True while a Codename One code editor holds the focus and should own the keyboard. */
    private static boolean isCodeEditorFocused() {
        com.codename1.ui.Form form = com.codename1.ui.CN.getCurrentForm();
        return form != null && form.getFocused() instanceof com.codename1.ui.editor.EditorView;
    }

    /** Routes a clipboard operation to the Codename One code editor when it holds the focus. */
    private static boolean editFocusedCodeEditor(String operation) {
        com.codename1.ui.Form form = com.codename1.ui.CN.getCurrentForm();
        com.codename1.ui.Component focused = form == null ? null : form.getFocused();
        if (!(focused instanceof com.codename1.ui.editor.EditorView)) return false;
        final com.codename1.ui.editor.EditorView view = (com.codename1.ui.editor.EditorView) focused;
        if (view.getComponentForm() == null) return false;
        com.codename1.ui.CN.callSerially(new Runnable() {
            @Override public void run() {
                if ("cut".equals(operation)) view.cutSelection();
                else if ("copy".equals(operation)) view.copySelection();
                else view.pasteClipboard();
            }
        });
        return true;
    }

    /**
     * Wraps an action that mutates Codename One state so it runs on the Codename One EDT. Swing
     * delivers menu actions on the AWT event thread, and the pointer bridge and test timers already
     * hop across; the menu commands did not, so ordinary menu use raced painting and input.
     *
     * <p>Deliberately not applied to the clipboard items: those first inspect the AWT focus owner
     * and may drive a Swing {@code JTextComponent}, which has to stay on the AWT thread. They hand
     * off to the Codename One EDT themselves once they know a Codename One editor is focused.
     *
     * @param action the Codename One work to run
     * @return a runnable safe to hand to Swing
     */
    private static Runnable onCn1Edt(final Runnable action) {
        return new Runnable() {
            @Override public void run() { com.codename1.ui.CN.callSerially(action); }
        };
    }

    private static JMenuItem item(String label, int key, boolean menuShortcut, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        if (key > 0) {
            int mask = menuShortcut ? menuShortcutMask() : 0;
            item.setAccelerator(KeyStroke.getKeyStroke(key, mask));
        }
        item.addActionListener(e -> action.run());
        return item;
    }

    private static void scheduleScreenshot() {
        String output = System.getProperty("guibuilder.screenshot");
        if (output == null || output.length() == 0) return;
        Timer timer = new Timer(1600, e -> {
            try {
                frame.setAlwaysOnTop(true);
                frame.toFront();
                BufferedImage image = new Robot().createScreenCapture(frame.getBounds());
                ImageIO.write(image, "png", new File(output));
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (!"false".equals(System.getProperty("guibuilder.screenshot.exit"))) Display.getInstance().exitApplication();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Override public void windowClosing(WindowEvent e) {
        // exitApplication() terminates the JVM without any save or lifecycle callback, so both this
        // and the Cmd/Ctrl+W item have to ask first or ordinary closure discards the work.
        com.codename1.ui.CN.callSerially(CodenameOneGUIBuilderStub::exitWithConfirmation);
    }
    @Override public void windowOpened(WindowEvent e) { }
    @Override public void windowClosed(WindowEvent e) { }
    @Override public void windowIconified(WindowEvent e) { }
    @Override public void windowDeiconified(WindowEvent e) { }
    @Override public void windowActivated(WindowEvent e) { }
    @Override public void windowDeactivated(WindowEvent e) { }
}
