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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
        try {
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
        } finally {
            // The canvas holds a Toolkit-global wheel listener for the life of the VM
            // unless it is released, so a test that drops one leaks into every test
            // after it.
            first.disposeGestureListeners();
            second.disposeGestureListeners();
        }
    }

    @Test
    void screenGraphicsIsRecognisedForEveryCanvasButNotForAMutableImage() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);

        JavaSEPort.C canvas = port.createWindowCanvas(3);
        try {
            canvas.setSize(200, 150);
            Graphics2D windowGraphics = port.getGraphics(port.getNativeGraphics(canvas));
            assertTrue(port.isScreenGraphics(windowGraphics),
                    "a secondary window's buffer is still a screen buffer; answering false "
                            + "here mis-scales its native peers");

            // The primary canvas must keep answering true -- that is the behaviour the
            // old identity comparison had, and every existing baseline depends on it.
            Graphics2D primaryGraphics = port.getGraphics(port.getNativeGraphics());
            assertTrue(port.isScreenGraphics(primaryGraphics),
                    "the primary canvas's buffer must still be recognised");

            // A mutable image is not a screen buffer and must not be mistaken for one.
            Image mutable = Image.createImage(64, 64);
            Graphics2D imageGraphics = port.getGraphics(port.getNativeGraphics(mutable.getImage()));
            assertFalse(port.isScreenGraphics(imageGraphics),
                    "a mutable image's graphics must never be treated as a screen buffer");
        } finally {
            canvas.disposeGestureListeners();
        }
    }

    @Test
    void disposingAWindowReleasesItsGlobalGestureListener() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);

        java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
        JavaSEPort.C canvas = port.createWindowCanvas(11);

        // The canvas's own listener, read off the field that holds it, rather than
        // whatever appeared in the Toolkit's list while this test ran. That list is
        // global to the VM and the simulator's event dispatch thread is live alongside
        // this test, so both a count and a before/after diff can attribute an unrelated
        // registration to this canvas and then demand that it be removed.
        java.lang.reflect.Field field =
                JavaSEPort.C.class.getDeclaredField("magnificationWheelFallbackListener");
        field.setAccessible(true);
        java.awt.event.AWTEventListener own = (java.awt.event.AWTEventListener) field.get(canvas);
        assertNotNull(own, "the canvas registers a global wheel listener");
        assertTrue(wheelListeners(toolkit).contains(own), "and hands it to the Toolkit");

        canvas.disposeGestureListeners();

        // The Toolkit holds its listeners for the life of the VM, so a window that
        // never releases one leaks the canvas and its whole hierarchy, and keeps
        // inspecting every wheel event in the application.
        assertFalse(wheelListeners(toolkit).contains(own),
                "disposing must hand that listener back");
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
        try {

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
        } finally {
            secondary.disposeGestureListeners();
        }
    }

    @Test
    void aPeerIsScaledForItsOwnWindowsMonitorNotTheMainDisplay() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);

        // A peer inside a secondary window used the global retinaScale -- the main
        // display's -- while everything positioning it around it used the owning
        // canvas's scale. On a desktop whose monitors have different backing scales
        // the peer was then offset and sized by the ratio between the two, so a
        // browser or a native editor drifted away from the component it belongs to.
        //
        // A single-monitor CI machine cannot show that by itself: every monitor has
        // the same scale, so the two agree by accident. Forcing the global scale to a
        // value the canvas's monitor does not have is what makes the difference
        // observable here, and it is exactly the disagreement a second monitor
        // produces on a real desktop.
        JavaSEPort.C canvas = port.createWindowCanvas(21);
        javax.swing.JFrame frame = new javax.swing.JFrame("peer scale");
        frame.getContentPane().add(canvas);
        frame.setSize(320, 240);
        // Displayable, so the canvas resolves a real GraphicsConfiguration and
        // canvasScale() answers from its monitor rather than falling back.
        frame.addNotify();

        double monitorScale = canvas.canvasScale();
        double originalRetina = JavaSEPort.retinaScale;
        try {
            JavaSEPort.retinaScale = monitorScale + 3.0;

            javax.swing.JPanel native1 = new javax.swing.JPanel();
            native1.setPreferredSize(new java.awt.Dimension(100, 50));
            JavaSEPort.Peer peer = new JavaSEPort.Peer(frame, native1);

            java.lang.reflect.Field owning =
                    JavaSEPort.Peer.class.getDeclaredField("owningCanvas");
            owning.setAccessible(true);
            owning.set(peer, canvas);

            com.codename1.ui.geom.Dimension pref = peer.calcPreferredSize();

            int expected = (int) (100 * monitorScale / port.zoomLevel);
            int ifItUsedTheMainDisplay = (int) (100 * (monitorScale + 3.0) / port.zoomLevel);
            assertNotEquals(expected, ifItUsedTheMainDisplay,
                    "the two scales have to differ or this test proves nothing");
            assertEquals(expected, pref.getWidth(),
                    "a peer must be sized by its own window's monitor scale; sizing it "
                            + "by the main display's stretches it by the ratio between "
                            + "the two monitors");
        } finally {
            JavaSEPort.retinaScale = originalRetina;
            frame.dispose();
            canvas.disposeGestureListeners();
        }
    }

    @Test
    void theUtilityWindowTypeStillChangesAfterTheWindowIsShown() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);

        // Swing only allows the window type to change while the frame is
        // undisplayable, and the setter used to skip the change outright once the
        // window was up. Window.isUtilityWindow() reported the requested value while
        // the platform stayed on the old taskbar behaviour, so the setter silently did
        // nothing for the only case that matters -- a palette toggled at runtime.
        JavaSEWindowManager wm = new JavaSEWindowManager(port);
        Object peerObj = wm.createWindow(31, "utility", 40, 40, 300, 200,
                true, true, null, false, false);
        assertNotNull(peerObj);
        try {
            wm.show(peerObj);
            flushAwt();

            wm.setUtilityWindow(peerObj, true);
            flushAwt();
            assertEquals(java.awt.Window.Type.UTILITY, frameOf(peerObj).getType(),
                    "a shown window must still be able to become a utility window");
            assertTrue(frameOf(peerObj).isVisible(),
                    "and must still be on screen afterwards");

            wm.setUtilityWindow(peerObj, false);
            flushAwt();
            assertEquals(java.awt.Window.Type.NORMAL, frameOf(peerObj).getType(),
                    "and must be able to change back");
        } finally {
            wm.dispose(peerObj);
            flushAwt();
        }
    }

    private static java.awt.Window frameOf(Object peerObj) throws Exception {
        java.lang.reflect.Field f =
                JavaSEWindowManager.Peer.class.getDeclaredField("frame");
        f.setAccessible(true);
        return (java.awt.Window) f.get(peerObj);
    }

    private static void flushAwt() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @Test
    void aWindowMenuDispatchesThroughItsWindowAndOmitsTheMcpMenu() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);

        final boolean[] listenerSaw = new boolean[1];
        final boolean[] commandRan = new boolean[1];
        com.codename1.ui.Command cmd = new com.codename1.ui.Command("Save") {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                commandRan[0] = true;
            }
        };

        // A stand-in for the owning window: the builder only needs something whose
        // dispatchCommand it can call, and building a real native window here would
        // drag in the whole show() path for what is a menu-wiring question.
        com.codename1.ui.Window owner = new com.codename1.ui.Window("owner");
        owner.addCommandListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                listenerSaw[0] = true;
            }
        });

        java.util.List<com.codename1.ui.Command> cmds =
                new java.util.ArrayList<com.codename1.ui.Command>();
        cmds.add(cmd);
        javax.swing.JMenuBar bar = port.buildWindowMenuBar(cmds, owner);
        assertNotNull(bar);

        // The MCP menu carries development-only controls ("Expose This Tool To Agents",
        // host installation). It belongs to the application's main frame, not to every
        // window that happens to carry a command.
        for (int iter = 0; iter < bar.getMenuCount(); iter++) {
            assertNotEquals("MCP", bar.getMenu(iter).getText(),
                    "a secondary window's menu must not carry the MCP tooling menu");
        }

        // Activating the item must go through the window, so listeners registered with
        // addCommandListener see it -- invoking the command directly bypasses them.
        javax.swing.JMenuItem item = bar.getMenu(0).getItem(0);
        assertEquals("Save", item.getText());
        item.doClick();
        com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });

        assertTrue(commandRan[0], "the command itself must run");
        assertTrue(listenerSaw[0],
                "and the window's command listeners must be notified");
    }

    @Test
    void aCanvasResolvesHitTestsAndEditorFocusFromItsOwnWindow() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);

        // The main canvas answers with the current form, exactly as before.
        JavaSEPort.C main = port.canvas;
        if (main != null) {
            java.lang.reflect.Method top =
                    JavaSEPort.C.class.getDeclaredMethod("canvasTopLevel");
            top.setAccessible(true);
            assertSame(com.codename1.ui.CN.getCurrentForm(), top.invoke(main),
                    "the main canvas must still resolve the current form");
        }

        // A secondary canvas must answer with the window it renders, not with whatever
        // form happens to be current. Resolving the current form here is what made a
        // peer in a window fail its hit test -- an unrelated main-form component at
        // those window-local coordinates set cn1GrabbedDrag and swallowed the event --
        // and made an editor focused in a window invisible to isPureEditorFocused().
        assumeTrue(com.codename1.ui.Desktop.isSupported(), "needs a windowing system");
        com.codename1.ui.Window w = new com.codename1.ui.Window("hit test");
        w.setWindowSize(300, 200);
        w.show();
        // The desktop registry is populated on the event dispatch thread, so the id is
        // not resolvable until it has run.
        com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
        JavaSEPort.C secondary = port.createWindowCanvas(w.getWindowId());
        try {
            java.lang.reflect.Method top =
                    JavaSEPort.C.class.getDeclaredMethod("canvasTopLevel");
            top.setAccessible(true);
            Object resolved = top.invoke(secondary);
            assertSame(w, resolved,
                    "a secondary canvas must resolve the window it renders");
            assertNotSame(com.codename1.ui.CN.getCurrentForm(), resolved,
                    "and not the main form");
        } finally {
            secondary.disposeGestureListeners();
            w.dispose();
        }
    }

    @Test
    void theMonitorFingerprintSeesTheWorkArea() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        // A taskbar or dock that moves edge, changes size or toggles auto-hide
        // reconfigures the work area while leaving the monitor's bounds and scale
        // identical. A fingerprint built only from bounds and scale is byte-identical
        // across that change, so monitorsChanged() never fires: windows keep a stale
        // work area and centerOnDesktop() can place one under the taskbar that just
        // appeared.
        java.lang.reflect.Method sig =
                JavaSEWindowManager.class.getDeclaredMethod("topologySignature");
        sig.setAccessible(true);
        String actual = (String) sig.invoke(null);
        assumeFalse("unavailable".equals(actual), "display was mid-reconfiguration");

        // Rebuild the bounds-and-scale-only fingerprint the code used to produce, and
        // require that the real one carries strictly more than it -- otherwise a
        // work-area change is invisible to the poller.
        StringBuilder boundsOnly = new StringBuilder();
        for (java.awt.GraphicsDevice device
                : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            java.awt.GraphicsConfiguration cfg = device.getDefaultConfiguration();
            java.awt.Rectangle b = cfg.getBounds();
            boundsOnly.append(device.getIDstring()).append(':')
                    .append(b.x).append(',').append(b.y).append(',')
                    .append(b.width).append('x').append(b.height).append('@')
                    .append(cfg.getDefaultTransform().getScaleX()).append(';');
        }
        assertNotEquals(boundsOnly.toString(), actual,
                "the fingerprint must carry more than bounds and scale, or a taskbar "
                        + "change never reaches monitorsChanged()");

        java.awt.Insets in = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration());
        assertTrue(actual.contains(in.top + "," + in.left + "," + in.bottom + "," + in.right),
                "the primary display's screen insets must appear in the fingerprint");
    }

    @Test
    void aSecondaryCanvasResizeLeavesTheMainCanvasAlone() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);
        JavaSEPort.C main = port.canvas;
        assumeFalse(main == null, "needs a booted primary canvas");

        java.lang.reflect.Field forced =
                JavaSEPort.C.class.getDeclaredField("forcedSize");
        forced.setAccessible(true);
        Object before = forced.get(main);

        // ancestorResized is primary-surface logic that a secondary canvas also runs,
        // being the same class on the same listener. Its body reaches
        // canvas.setForcedSize() -- the *port's* canvas, not the one the event arrived
        // on -- so a secondary window's resize stamped the main canvas with the
        // secondary window's dimensions and a later Swing layout could resize or clip
        // the main surface. queueSizeChangeEvent's own guard is too late: by then the
        // main canvas has already been mutated.
        JavaSEPort.C secondary = port.createWindowCanvas(51);
        // In a real frame, so the handler runs the same path it would in production
        // rather than tripping over a null ancestor -- otherwise the test fails for the
        // wrong reason and never reaches the assertion that matters.
        javax.swing.JFrame frame = new javax.swing.JFrame("secondary");
        frame.getContentPane().setLayout(new java.awt.BorderLayout());
        frame.getContentPane().add(java.awt.BorderLayout.CENTER, secondary);
        frame.setSize(137, 91);
        frame.addNotify();
        try {
            secondary.ancestorResized(new java.awt.event.HierarchyEvent(
                    secondary, java.awt.event.HierarchyEvent.ANCESTOR_RESIZED,
                    secondary, secondary.getParent()));
            assertSame(before, forced.get(main),
                    "a secondary canvas's resize must not stamp the main canvas's "
                            + "forced size");
        } finally {
            frame.dispose();
            secondary.disposeGestureListeners();
        }
    }

    @Test
    void aPeerHitTestConvertsWithItsOwnCanvasScale() {
        // The companion to aPeerIsScaledForItsOwnWindowsMonitorNotTheMainDisplay: the
        // peer is positioned with peerScale(), so the hit test that decides whether a
        // mouse event belongs to it has to use the same scale. Converting with the
        // global retinaScale instead tests a different point on a mixed-DPI desktop,
        // and the lookup can then find an unrelated component, set cn1GrabbedDrag and
        // swallow input meant for a browser or native editor.
        //
        // The conversion is isolated in a helper precisely so this can be checked
        // without showing a real window on a second monitor.
        int screenCoordinate = 400;
        int canvasOriginOnScreen = 100;
        int canvasOffset = 10;
        int screenCoordsOffset = 5;
        double zoom = 1.0;
        double canvasMonitorScale = 2.0;
        double mainDisplayScale = 1.0;

        int withOwnCanvas = JavaSEPort.CN1JPanel.toCn1Coordinate(screenCoordinate,
                canvasOriginOnScreen, canvasOffset, screenCoordsOffset, zoom, canvasMonitorScale);
        int withMainDisplay = JavaSEPort.CN1JPanel.toCn1Coordinate(screenCoordinate,
                canvasOriginOnScreen, canvasOffset, screenCoordsOffset, zoom, mainDisplayScale);

        assertNotEquals(withMainDisplay, withOwnCanvas,
                "the two scales have to disagree or this test proves nothing");
        assertEquals((int) ((screenCoordinate - canvasOriginOnScreen
                        - (canvasOffset + screenCoordsOffset) * zoom / canvasMonitorScale)
                        / zoom * canvasMonitorScale),
                withOwnCanvas,
                "a hit test must convert with the owning canvas's backing scale");
    }

    /// The Toolkit's wheel listeners, unwrapped from the proxies it hands out.
    ///
    /// `getAWTEventListeners(mask)` builds a fresh `AWTEventListenerProxy` per call, so
    /// comparing the returned objects by identity never matches. The listener inside
    /// the proxy is the stable one.
    private static java.util.List<java.awt.event.AWTEventListener> wheelListeners(
            java.awt.Toolkit toolkit) {
        java.util.List<java.awt.event.AWTEventListener> out =
                new java.util.ArrayList<java.awt.event.AWTEventListener>();
        for (java.awt.event.AWTEventListener l
                : toolkit.getAWTEventListeners(java.awt.AWTEvent.MOUSE_WHEEL_EVENT_MASK)) {
            out.add(l instanceof java.awt.event.AWTEventListenerProxy
                    ? ((java.awt.event.AWTEventListenerProxy) l).getListener() : l);
        }
        return out;
    }

    @Test
    void aFrameReportsAutomaticVisibilityChangesToTheFramework() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        assumeTrue(com.codename1.ui.Desktop.isSupported(), "needs a windowing system");

        com.codename1.ui.Window w = new com.codename1.ui.Window("owned visibility");
        w.setWindowSize(320, 240);
        w.show();
        com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
        try {
            java.awt.Window frame = findAwtWindowTitled("owned visibility");
            assertNotNull(frame, "the window manager should have created a native frame");

            // AWT hides a window's owned dialogs with it and shows them again with it,
            // reporting only componentHidden/componentShown on the child -- no window
            // event. Listening for those is what tells the framework; without it an
            // owned window kept reporting itself shown with no surface behind it.
            java.awt.event.ComponentEvent hidden = new java.awt.event.ComponentEvent(
                    frame, java.awt.event.ComponentEvent.COMPONENT_HIDDEN);
            boolean delivered = false;
            for (java.awt.event.ComponentListener l : frame.getComponentListeners()) {
                l.componentHidden(hidden);
                delivered = true;
            }
            assertTrue(delivered, "the frame must carry a component listener");
            // The notification is marshalled to the event dispatch thread.
            com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                }
            });
            assertFalse(w.isWindowShowing(),
                    "a componentHidden from the platform must reach the framework");

            java.awt.event.ComponentEvent shown = new java.awt.event.ComponentEvent(
                    frame, java.awt.event.ComponentEvent.COMPONENT_SHOWN);
            for (java.awt.event.ComponentListener l : frame.getComponentListeners()) {
                l.componentShown(shown);
            }
            com.codename1.ui.Display.getInstance().callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                }
            });
            assertTrue(w.isWindowShowing(), "and componentShown must bring it back");
        } finally {
            w.dispose();
        }
    }

    /// The AWT window with the given title, or null.
    private static java.awt.Window findAwtWindowTitled(String title) {
        for (java.awt.Window each : java.awt.Window.getWindows()) {
            if (each instanceof java.awt.Frame
                    && title.equals(((java.awt.Frame) each).getTitle())) {
                return each;
            }
            if (each instanceof java.awt.Dialog
                    && title.equals(((java.awt.Dialog) each).getTitle())) {
                return each;
            }
        }
        return null;
    }

    /**
     * The registry that answers {@code isScreenGraphics} keys a {@code Graphics2D} to
     * its owning canvas strongly, so a disposed window that never unregisters keeps its
     * canvas and its {@code BufferedImage}s reachable for the life of the application.
     * At a large window size that is tens of megabytes per window ever opened.
     */
    @Test
    void disposingACanvasReleasesItsScreenGraphics() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port, "the port should be booted by CodenameOneTest");

        JavaSEWindowManager wm = new JavaSEWindowManager(port);
        Object peer = wm.createWindow(41, "leak", 0, 0, 320, 240, true, true, null,
                false, false);
        assumeTrue(peer != null, "needs a native window");
        JavaSEPort.C canvas = ((JavaSEWindowManager.Peer) peer).canvas;
        assertNotNull(canvas, "the window should have a canvas");
        canvas.setSize(320, 240);
        Graphics2D g = port.getGraphics(port.getNativeGraphics(canvas));
        assertNotNull(g);
        assertTrue(port.isScreenGraphics(g),
                "a painted canvas registers its screen graphics");

        // Through the real dispose path, not the release method directly: what this
        // guards is that disposal is wired to it at all.
        wm.dispose(peer);
        for (int i = 0; i < 100 && port.isScreenGraphics(g); i++) {
            try {
                java.awt.EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                    }
                });
            } catch (Exception err) {
                break;
            }
        }

        assertFalse(port.isScreenGraphics(g),
                "a disposed window must not leave its canvas in the registry: the "
                        + "registry holds it strongly, so it would never be collected");
    }

    /**
     * The blit and paint transforms use {@code canvasScale()} rather than the global
     * {@code retinaScale}, so a window on a display of a different scale is not
     * stretched by the ratio between the two. That is only safe for the main window
     * because its {@code canvasScale()} is {@code retinaScale} by definition -- if
     * that stops being true, the main window's rendering changes with it, which no
     * screenshot in the suite would attribute to this.
     */
    @Test
    void theMainCanvasScaleIsTheGlobalRetinaScale() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port, "the port should be booted by CodenameOneTest");

        JavaSEPort.C main = port.createWindowCanvas(0);

        assertEquals(JavaSEPort.retinaScale, main.canvasScale(), 0.0001,
                "window 0 is the main window and must keep using the global scale");
    }

    /**
     * hide() has to complete before it returns, exactly as show() does. Queued, a
     * hide followed by a show in the same event dispatch thread turn ran after the
     * show had already put the window back, so the frame's componentHidden arrived
     * with the window visible and was reported as a minimize -- firing minimize
     * listeners for a window that is on screen.
     */
    @Test
    void hidingAWindowCompletesBeforeItReturns() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        assumeFalse(java.awt.EventQueue.isDispatchThread(),
                "the point of this is the cross-thread hand-off");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port, "the port should be booted by CodenameOneTest");

        JavaSEWindowManager wm = new JavaSEWindowManager(port);
        Object peer = wm.createWindow(57, "sync hide", 0, 0, 320, 240, true, true, null,
                false, false);
        assumeTrue(peer != null, "needs a native window");
        try {
            wm.show(peer);
            java.awt.Window frame = ((JavaSEWindowManager.Peer) peer).frame;
            assumeTrue(frame.isVisible(), "the window has to be up for this to mean anything");

            wm.hide(peer);

            assertFalse(frame.isVisible(),
                    "hide must have taken effect by the time it returns, or a show in "
                            + "the same turn races the queued hide");
        } finally {
            wm.dispose(peer);
        }
    }
}
