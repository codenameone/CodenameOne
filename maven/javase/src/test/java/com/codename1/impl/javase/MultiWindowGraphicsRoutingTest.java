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
    void disposingAWindowReleasesItsGlobalGestureListener() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JavaSEPort port = JavaSEPort.instance;
        assertNotNull(port);

        java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
        int before = toolkit.getAWTEventListeners(java.awt.AWTEvent.MOUSE_WHEEL_EVENT_MASK).length;

        JavaSEPort.C canvas = port.createWindowCanvas(11);
        int during = toolkit.getAWTEventListeners(java.awt.AWTEvent.MOUSE_WHEEL_EVENT_MASK).length;
        canvas.disposeGestureListeners();
        int after = toolkit.getAWTEventListeners(java.awt.AWTEvent.MOUSE_WHEEL_EVENT_MASK).length;

        // The Toolkit holds its listeners for the life of the VM, so a window that
        // never releases one leaks the canvas and its whole hierarchy, and keeps
        // inspecting every wheel event in the application.
        assertTrue(during >= before, "the canvas registers a global wheel listener");
        assertEquals(before, after, "disposing must hand that listener back");
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
}
