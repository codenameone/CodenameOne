/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.impl.ios.sim;

import com.codename1.ui.Display;

import java.lang.reflect.Method;

/**
 * Pure Codename One simulator launcher: no AWT, no Swing - the dylib owns the
 * window, AppKit input flows straight into the CN1 event pipeline, and
 * everything on screen is rendered by Codename One through the native
 * backend.
 *
 * <p>Launch with:</p>
 * <pre>
 * java -XstartOnFirstThread -Djava.awt.headless=true \
 *      -Dcn1.sim.native.path=/path/to/libcn1sim.dylib \
 *      com.codename1.impl.ios.sim.CN1PureSimulator com.mycompany.MyApp
 * </pre>
 *
 * <p>The process main thread runs the AppKit event loop (the
 * -XstartOnFirstThread requirement, same as SWT); the Codename One lifecycle
 * runs on the CN1 EDT. The app class follows the standard CN1 lifecycle
 * contract (init/start/stop/destroy).</p>
 */
public class CN1PureSimulator {
    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: CN1PureSimulator <main-class>");
            System.exit(1);
        }
        System.setProperty("cn1.simulator.backend", "ios");
        if (System.getProperty("cn1.simulator.decorators") == null) {
            System.setProperty("cn1.simulator.decorators", "true");
        }
        // make sure the implementation factory selects the native backend in
        // pure mode (no AWT window creation inside the backend)
        System.setProperty("cn1.sim.pure", "true");

        final String mainClass = args[0];

        if (System.getProperty("cn1.sim.rpc") != null) {
            int rpcPort = Integer.getInteger("cn1.sim.rpc", 17995).intValue();
            if (Boolean.getBoolean("cn1.sim.shell")) {
                runShellRpc(mainClass, rpcPort);
                return;
            }
            runRpc(mainClass, rpcPort);
            return;
        }

        CN1SimHost.load();

        if (Boolean.getBoolean("cn1.sim.shell")) {
            runShell(mainClass);
            return;
        }
        if (Boolean.getBoolean("cn1.sim.isolate")) {
            runIsolated(mainClass);
            return;
        }
        Thread appThread = new Thread("CN1Sim-App") {
            public void run() {
                try {
                    Display.init(null);
                    final Object app = Class.forName(mainClass).getDeclaredConstructor().newInstance();
                    invokeLifecycle(app, "init", new Class[]{Object.class}, new Object[]{null});
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            try {
                                invokeLifecycle(app, "start", new Class[0], new Object[0]);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                System.exit(1);
                            }
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(1);
                }
            }
        };
        appThread.setDaemon(true);
        appThread.start();

        // dedicates the process main thread to AppKit; never returns
        CN1SimHost.runEventLoop();
    }

    /**
     * RPC mode - the architecture pivot: the native layer is the REAL Mac
     * Catalyst build of Codename One running as a separate relay process
     * (full UIKit, real bundle identity). We listen, it connects, and the
     * same bridge interface that fronted JNI now fronts the wire. No JNI,
     * no dylib, no reimplemented natives in this process at all.
     */
    /**
     * The full simulator shell over the RPC relay: skin chrome, sidebar
     * tools and the two-universe app isolation all run exactly as in the
     * in-process mode - the relay window stands in for the native window and
     * two region facades over one connection stand in for the JNI bridges.
     */
    private static void dumpTree(com.codename1.ui.Component c, int depth) {
        StringBuilder b = new StringBuilder("[tree] ");
        for (int i = 0; i < depth; i++) {
            b.append("  ");
        }
        b.append(c.getClass().getSimpleName()).append(" abs=").append(c.getAbsoluteX())
                .append(",").append(c.getAbsoluteY()).append(" ").append(c.getWidth())
                .append("x").append(c.getHeight());
        System.out.println(b);
        if (c instanceof com.codename1.ui.Container) {
            com.codename1.ui.Container ct = (com.codename1.ui.Container) c;
            for (int i = 0; i < ct.getComponentCount(); i++) {
                dumpTree(ct.getComponentAt(i), depth + 1);
            }
        }
    }

    private static void runShellRpc(final String mainClass, int port) throws Exception {
        String skin = System.getProperty("cn1.sim.skin");
        if (skin == null) {
            System.err.println("Shell mode requires -Dcn1.sim.skin=/path/to/device.skin");
            System.exit(1);
        }
        final com.codename1.impl.ios.sim.rpc.RpcRenderBridge conn =
                new com.codename1.impl.ios.sim.rpc.RpcRenderBridge(port);
        final int winW = conn.getAppWidth();
        final int winH = conn.getAppHeight();

        // shell universe = the whole relay surface; registered before
        // loadSkin so skin decoding rides the RPC connection
        com.codename1.impl.ios.sim.bridge.BridgeRegistry.setShellBridge(
                new com.codename1.impl.ios.sim.rpc.RpcRegionBridge(conn, 0, 0, winW, winH));

        final IsolatedAppRunner runner = new IsolatedAppRunner();
        runner.loadSkin(skin);
        final int maxSkinH = Integer.getInteger("cn1.sim.maxHeight", winH - 40).intValue();
        final double scale = Math.min(1.0, maxSkinH / (double) runner.getWindowHeight());
        com.codename1.impl.ios.sim.bridge.BridgeRegistry.setRenderScale(scale);
        final com.codename1.impl.ios.sim.child.BridgedSimImplementation[] shellImplHolder =
                new com.codename1.impl.ios.sim.child.BridgedSimImplementation[1];
        com.codename1.impl.ImplementationFactory.setInstance(
                new com.codename1.impl.ImplementationFactory() {
                    public Object createImplementation() {
                        shellImplHolder[0] = new com.codename1.impl.ios.sim.child.BridgedSimImplementation(
                                com.codename1.impl.ios.sim.bridge.BridgeRegistry.getShellBridge());
                        return shellImplHolder[0];
                    }
                });

        // installed by the boot block; rescales the chrome + rebinds the app
        // when the relay window resizes
        final Runnable[] resizeHook = new Runnable[1];

        // the canonical input dispatch: live relay input AND Test Recorder
        // replays both route through this, so a replay reproduces exactly
        final InputRecorder recorder = new InputRecorder(new InputRecorder.Dispatcher() {
            public void pointer(int type, int x, int y) {
                if (runner.routePointer(type, x, y)) {
                    return;
                }
                if (!Display.isInitialized()) {
                    return;
                }
                Display d = Display.getInstance();
                switch (type) {
                    case 1:
                        d.pointerPressed(new int[]{x}, new int[]{y});
                        break;
                    case 2:
                        d.pointerReleased(new int[]{x}, new int[]{y});
                        break;
                    case 3:
                        d.pointerDragged(new int[]{x}, new int[]{y});
                        break;
                    default:
                        break;
                }
            }

            public void key(int type, int code) {
                com.codename1.impl.ios.sim.bridge.InputSink sink =
                        com.codename1.impl.ios.sim.bridge.BridgeRegistry.getInputSink();
                if (sink != null) {
                    sink.keyEvent(type, code);
                    return;
                }
                if (!Display.isInitialized()) {
                    return;
                }
                if (type == 1) {
                    Display.getInstance().keyPressed(code);
                } else {
                    Display.getInstance().keyReleased(code);
                }
            }
        });
        com.codename1.impl.ios.sim.bridge.BridgeRegistry.setRecorderControl(recorder);

        // relay input: record (when armed) then route through the dispatcher
        conn.setRelayEvents(new com.codename1.impl.ios.sim.rpc.RpcRenderBridge.RelayEvents() {
            public void pointerEvent(int type, int x, int y) {
                recorder.record(0, type, x, y);
                recorder.inject(0, type, x, y);
            }

            public void keyEvent(int type, int code) {
                recorder.record(1, type, code, 0);
                recorder.inject(1, type, code, 0);
            }

            public void sizeChanged(int w, int h) {
                com.codename1.impl.ios.sim.bridge.BridgeRegistry.setShellBridge(
                        new com.codename1.impl.ios.sim.rpc.RpcRegionBridge(conn, 0, 0, w, h));
                if (shellImplHolder[0] != null) {
                    shellImplHolder[0].notifySizeChanged(w, h);
                }
                if (resizeHook[0] != null && Display.isInitialized()) {
                    Display.getInstance().callSerially(resizeHook[0]);
                }
            }
        });

        Display.init(null);
        if (Display.getInstance().hasNativeTheme()) {
            Display.getInstance().installNativeTheme();
        }

        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                try {
                    com.codename1.ui.Image skinImg = com.codename1.ui.Image.createImage(
                            new java.io.ByteArrayInputStream(runner.getSkinPngBytes()));
                    // The overlay crop is only consumed by the native
                    // rounded-corner overlay (setScreenOverlay), which is a
                    // no-op in RPC mode. Computing it forces a multi-megabyte
                    // getRGB round-trip over the socket that races with the
                    // child universe's concurrent font/image calls - skip it.
                    com.codename1.ui.Image portraitCrop = null;
                    if (com.codename1.impl.ios.sim.CN1SimHost.isLoaded()) {
                        portraitCrop = skinImg.subImage(
                                runner.getScreenX(), runner.getScreenY(),
                                runner.getScreenW(), runner.getScreenH(), true);
                    }
                    if (scale < 1.0) {
                        skinImg = skinImg.scaled(
                                (int) Math.round(runner.getWindowWidth() * scale),
                                (int) Math.round(runner.getWindowHeight() * scale));
                    }
                    final com.codename1.impl.ios.sim.shell.SimulatorShell shell =
                            new com.codename1.impl.ios.sim.shell.SimulatorShell(skinImg,
                                    (int) Math.round(runner.getScreenX() * scale),
                                    (int) Math.round(runner.getScreenY() * scale),
                                    (int) Math.round(runner.getScreenW() * scale),
                                    (int) Math.round(runner.getScreenH() * scale));
                    // push the menu straight to the live shell impl: the Form's
                    // auto-push doesn't reach it across the classloader boundary
                    shell.setMenuPusher(
                            new com.codename1.impl.ios.sim.shell.SimulatorShell.MenuPusher() {
                                public void push(java.util.Vector commands) {
                                    if (shellImplHolder[0] != null) {
                                        shellImplHolder[0].pushShellMenu(commands);
                                    }
                                }
                            });
                    com.codename1.ui.Image landscapeCrop = null;
                    if (runner.hasLandscape()) {
                        com.codename1.ui.Image lskin = com.codename1.ui.Image.createImage(
                                new java.io.ByteArrayInputStream(runner.getLandscapeSkinPngBytes()));
                        if (com.codename1.impl.ios.sim.CN1SimHost.isLoaded()) {
                            landscapeCrop = lskin.subImage(
                                    runner.getLandscapeScreenX(), runner.getLandscapeScreenY(),
                                    runner.getLandscapeScreenW(), runner.getLandscapeScreenH(), true);
                        }
                        if (scale < 1.0) {
                            lskin = lskin.scaled(
                                    (int) Math.round(runner.getLandscapeSkinW() * scale),
                                    (int) Math.round(runner.getLandscapeSkinH() * scale));
                        }
                        shell.setLandscapeSkin(lskin,
                                (int) Math.round(runner.getLandscapeScreenX() * scale),
                                (int) Math.round(runner.getLandscapeScreenY() * scale),
                                (int) Math.round(runner.getLandscapeScreenW() * scale),
                                (int) Math.round(runner.getLandscapeScreenH() * scale));
                    }
                    shell.setOverlayCrops(portraitCrop, landscapeCrop);
                    // a fixed ~1/4-window sidebar (NOT the leftover beside a
                    // tall narrow phone skin, which made the panel huge and
                    // overlap the skin); the skin re-scales to fit the rest
                    int toolsW = Math.max(280, winW / 4);
                    shell.setToolsWidth(toolsW);
                    final String skinPath = new java.io.File(
                            System.getProperty("cn1.sim.skin")).getAbsolutePath();
                    final String[] currentSkin = {skinPath};
                    final int[] maxH = {maxSkinH};
                    // the last rect/viewport we rebound to, so a scroll that does
                    // not actually move the app screen skips the rebind entirely
                    // (idempotent - kills redundant bridge churn / bounce)
                    final int[] lastBind = {Integer.MIN_VALUE, 0, 0, 0, 0, 0, 0, 0};
                    final Runnable rebindApp = new Runnable() {
                        public void run() {
                            int[] rect = shell.getAppScreenRect();
                            int[] vp = shell.getViewportRect();
                            if (lastBind[0] == rect[0] && lastBind[1] == rect[1]
                                    && lastBind[2] == rect[2] && lastBind[3] == rect[3]
                                    && lastBind[4] == vp[0] && lastBind[5] == vp[1]
                                    && lastBind[6] == vp[2] && lastBind[7] == vp[3]) {
                                return;
                            }
                            lastBind[0] = rect[0]; lastBind[1] = rect[1];
                            lastBind[2] = rect[2]; lastBind[3] = rect[3];
                            lastBind[4] = vp[0]; lastBind[5] = vp[1];
                            lastBind[6] = vp[2]; lastBind[7] = vp[3];
                            runner.setRouteRect(rect[0], rect[1], rect[2], rect[3]);
                            runner.setRouteViewport(vp[0], vp[1], vp[2], vp[3]);
                            com.codename1.impl.ios.sim.bridge.BridgeRegistry.setBridge(
                                    new com.codename1.impl.ios.sim.rpc.RpcRegionBridge(conn,
                                            rect[0], rect[1], rect[2], rect[3],
                                            vp[0], vp[1], vp[2], vp[3]));
                            shell.updateScreenOverlay();
                            // force the app to repaint at the new offset NOW
                            // instead of on its next idle frame, so the moved
                            // screen hole does not show a stale/chrome frame
                            // (the scroll flicker) until the app catches up
                            com.codename1.impl.ios.sim.bridge.InputSink sink =
                                    com.codename1.impl.ios.sim.bridge.BridgeRegistry.getInputSink();
                            if (sink != null) {
                                sink.repaintRequest();
                            }
                        }
                    };
                    shell.setSkinContext(new java.io.File(skinPath).getParent(),
                            skinPath,
                            new com.codename1.impl.ios.sim.shell.SimulatorShell.SkinSwitcher() {
                                public void switchTo(String path) {
                                    currentSkin[0] = path;
                                    switchSkin(runner, shell, maxH[0], path);
                                }
                            });
                    shell.setZoomToggle(new Runnable() {
                        public void run() {
                            maxH[0] = shell.isZoomed()
                                    ? runner.getWindowHeight() : maxSkinH;
                            applySkinScaled(runner, shell, maxH[0], currentSkin[0]);
                            com.codename1.impl.ios.sim.bridge.ChildControl cc =
                                    com.codename1.impl.ios.sim.bridge.BridgeRegistry.getChildControl();
                            if (cc != null) {
                                cc.control("nativeTheme:"
                                        + com.codename1.impl.ios.sim.bridge.BridgeRegistry.getNativeThemePref());
                            }
                        }
                    });
                    shell.setScrollRebind(rebindApp);
                    resizeHook[0] = new Runnable() {
                        public void run() {
                            // the connection's dims are always current even
                            // when a resize raced the boot sequence
                            int curW = conn.getAppWidth();
                            int curH = conn.getAppHeight();
                            com.codename1.impl.ios.sim.bridge.BridgeRegistry.setShellBridge(
                                    new com.codename1.impl.ios.sim.rpc.RpcRegionBridge(conn, 0, 0, curW, curH));
                            if (shellImplHolder[0] != null) {
                                shellImplHolder[0].notifySizeChanged(curW, curH);
                            }
                            maxH[0] = Math.max(300, curH - 40);
                            applySkinScaled(runner, shell, maxH[0], currentSkin[0]);
                            rebindApp.run();
                            com.codename1.impl.ios.sim.bridge.InputSink sink =
                                    com.codename1.impl.ios.sim.bridge.BridgeRegistry.getInputSink();
                            if (sink != null) {
                                int[] r = shell.getAppScreenRect();
                                sink.sizeChanged(r[2], r[3]);
                                sink.repaintRequest();
                            }
                            com.codename1.ui.Form cur = Display.getInstance().getCurrent();
                            if (cur != null) {
                                cur.forceRevalidate();
                                cur.repaint();
                            }
                        }
                    };
                    final Runnable relayout = new Runnable() {
                        public void run() {
                            // the relay window keeps its size; re-scale the skin
                            // to the width now left beside the sidebar (so it
                            // never overflows under the panel), relayout the
                            // chrome and rebind the app universe
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    com.codename1.ui.Form current = Display.getInstance().getCurrent();
                                    if (current != null) {
                                        current.forceRevalidate();
                                    }
                                    rebindApp.run();
                                    com.codename1.impl.ios.sim.bridge.InputSink sink =
                                            com.codename1.impl.ios.sim.bridge.BridgeRegistry.getInputSink();
                                    if (sink != null) {
                                        int[] rect = shell.getAppScreenRect();
                                        sink.sizeChanged(rect[2], rect[3]);
                                        sink.repaintRequest();
                                    }
                                    if (current != null) {
                                        current.repaint();
                                    }
                                }
                            });
                        }
                    };
                    shell.setOrientationChangedCallback(relayout);
                    shell.setPushSender(
                            new com.codename1.impl.ios.sim.shell.SimulatorShell.PushSender() {
                                public void sendPush(String payload) {
                                    runner.invokePush(payload);
                                }
                            });
                    shell.setAppController(
                            new com.codename1.impl.ios.sim.shell.SimulatorShell.AppController() {
                                public void pauseApp() {
                                    runner.invokeAppLifecycle("stop");
                                }

                                public void resumeApp() {
                                    runner.invokeAppLifecycle("start");
                                }
                            });
                    shell.setWindowControl(
                            new com.codename1.impl.ios.sim.shell.SimulatorShell.WindowControl() {
                                public void setAlwaysOnTop(boolean onTop) {
                                    conn.setAlwaysOnTop(onTop);
                                }

                                public void setWindowSize(int width, int height) {
                                    conn.setWindowSize(width, height);
                                }
                            });
                    shell.show();
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            try {
                                rebindApp.run();
                                runner.runApp(mainClass);
                                // a relay resize during boot leaves a stale
                                // chrome scale; re-derive ONLY when the live
                                // dims differ (re-applying the skin on the
                                // good path destabilizes the layout)
                                if (conn.getAppWidth() != winW || conn.getAppHeight() != winH) {
                                    Display.getInstance().callSerially(resizeHook[0]);
                                }
                                // A Catalyst window frequently opens at a default
                                // size and only restores its saved frame a beat
                                // later. That late E_SIZE_CHANGED can arrive before
                                // resizeHook is installed (dropped) or before the
                                // app is up (no effect), leaving the app laid out
                                // for the stale boot size - it renders off the skin
                                // hole (blank screen). Re-sync once the app is
                                // running, but ONLY when the live dims still differ
                                // from boot, so the good path is untouched.
                                new Thread("CN1Sim-BootResync") {
                                    public void run() {
                                        for (int i = 0; i < 6; i++) {
                                            try {
                                                Thread.sleep(400);
                                            } catch (InterruptedException e) {
                                                return;
                                            }
                                            if (conn.getAppWidth() != winW
                                                    || conn.getAppHeight() != winH) {
                                                Display.getInstance().callSerially(resizeHook[0]);
                                                return;
                                            }
                                        }
                                    }
                                }.start();
                                // -Dcn1.sim.testZoomScroll=stepPx,count drives a
                                // zoom + repeated viewport scroll so the scroll
                                // rendering can be verified headlessly (synthetic
                                // drags don't reach the Catalyst window)
                                final String tzs = System.getProperty("cn1.sim.testZoomScroll");
                                if (tzs != null) {
                                    final String[] zp = tzs.split(",");
                                    new Thread("CN1Sim-TestZoomScroll") {
                                        public void run() {
                                            try {
                                                Thread.sleep(3500);
                                                Display.getInstance().callSerially(new Runnable() {
                                                    public void run() {
                                                        shell.toggleZoom();
                                                    }
                                                });
                                                Thread.sleep(1500);
                                                int step = Integer.parseInt(zp[0]);
                                                int count = zp.length > 1
                                                        ? Integer.parseInt(zp[1]) : 8;
                                                for (int i = 0; i < count; i++) {
                                                    Display.getInstance().callSerially(new Runnable() {
                                                        public void run() {
                                                            shell.devScrollBy(0, step);
                                                        }
                                                    });
                                                    Thread.sleep(250);
                                                }
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }.start();
                                }
                                // -Dcn1.sim.testRecorder records a tap on the
                                // top button, replays it, and logs the counts -
                                // a headless end-to-end check of record/replay
                                if (System.getProperty("cn1.sim.testRecorder") != null) {
                                    new Thread("CN1Sim-TestRec") {
                                        public void run() {
                                            try {
                                                Thread.sleep(4500);
                                                int[] r = shell.getAppScreenRect();
                                                int cx = r[0] + r[2] / 2;
                                                int cy = r[1] + 110 * r[3] / 1287;
                                                recorder.start();
                                                recorder.record(0, 1, cx, cy);
                                                recorder.inject(0, 1, cx, cy);
                                                Thread.sleep(60);
                                                recorder.record(0, 2, cx, cy);
                                                recorder.inject(0, 2, cx, cy);
                                                recorder.stop();
                                                System.out.println("[cn1sim] REC captured="
                                                        + recorder.count());
                                                Thread.sleep(900);
                                                recorder.play();
                                                Thread.sleep(900);
                                                System.out.println("[cn1sim] REC replay done");
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }.start();
                                }
                                // -Dcn1.sim.testTap=x,y,delayMs injects one
                                // pointer press/release through the same
                                // InputSink the window router uses - verifies
                                // the child dispatch without synthetic mouse
                                // input on a machine that's in use
                                final String testTap = System.getProperty("cn1.sim.testTap");
                                if (testTap != null) {
                                    final String[] parts = testTap.split(",");
                                    new Thread("CN1Sim-TestTap") {
                                        public void run() {
                                            try {
                                                Thread.sleep(Long.parseLong(parts[2]));
                                                com.codename1.impl.ios.sim.bridge.InputSink sink =
                                                        com.codename1.impl.ios.sim.bridge.BridgeRegistry.getInputSink();
                                                int tx = Integer.parseInt(parts[0]);
                                                int ty = Integer.parseInt(parts[1]);
                                                if (sink != null) {
                                                    sink.pointerEvent(1, tx, ty);
                                                    Thread.sleep(80);
                                                    sink.pointerEvent(2, tx, ty);
                                                    System.out.println("[cn1sim-rpc] testTap fired " + tx + "," + ty);
                                                } else {
                                                    System.out.println("[cn1sim-rpc] testTap: no sink");
                                                }
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }.start();
                                }
                                // one full-window sweep after the chrome
                                // settles: layout passes before the first
                                // rebind can leave stale pixels in the
                                // relay's persistent framebuffer
                                new com.codename1.ui.util.UITimer(new Runnable() {
                                    public void run() {
                                        com.codename1.ui.Form cur = Display.getInstance().getCurrent();
                                        if (cur != null) {
                                            dumpTree(cur, 0);
                                            cur.repaint();
                                        }
                                    }
                                }).schedule(1200, false, Display.getInstance().getCurrent());
                                // the chrome can settle a few layout passes
                                // after boot (tools panel, fonts); track the
                                // skin rect and rebind whenever it moves
                                // BOUNDED settle check: the chrome can shift a
                                // couple of layout passes after boot. Poll only
                                // for the first few seconds, and STOP once the
                                // rect has held steady - a forever-poller turns
                                // any rebind-induced relayout jitter into a
                                // permanent bounce feedback loop.
                                final int[] last = shell.getAppScreenRect();
                                final int[] ticks = {0};
                                final int[] stable = {0};
                                new com.codename1.ui.util.UITimer(new Runnable() {
                                    public void run() {
                                        ticks[0]++;
                                        int[] now = shell.getAppScreenRect();
                                        boolean moved = now[0] != last[0] || now[1] != last[1]
                                                || now[2] != last[2] || now[3] != last[3];
                                        if (moved) {
                                            stable[0] = 0;
                                            System.arraycopy(now, 0, last, 0, 4);
                                            rebindApp.run();
                                            com.codename1.impl.ios.sim.bridge.InputSink sink =
                                                    com.codename1.impl.ios.sim.bridge.BridgeRegistry.getInputSink();
                                            if (sink != null) {
                                                sink.sizeChanged(now[2], now[3]);
                                                sink.repaintRequest();
                                            }
                                            com.codename1.ui.Form cur = Display.getInstance().getCurrent();
                                            if (cur != null) {
                                                cur.repaint();
                                            }
                                        } else {
                                            stable[0]++;
                                        }
                                        // stop once settled (2 stable ticks) or
                                        // after ~4s, whichever comes first
                                        if (stable[0] < 2 && ticks[0] < 8) {
                                            new com.codename1.ui.util.UITimer(this)
                                                    .schedule(500, false, Display.getInstance().getCurrent());
                                        } else {
                                            System.out.println("[cn1sim-rpc] settle poller stopped after "
                                                    + ticks[0] + " ticks");
                                        }
                                    }
                                }).schedule(500, false, Display.getInstance().getCurrent());
                            } catch (Throwable t) {
                                t.printStackTrace();
                                System.exit(1);
                            }
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(1);
                }
            }
        });

        Object lock = new Object();
        synchronized (lock) {
            lock.wait();
        }
    }

    private static void runRpc(final String mainClass, int port) throws Exception {
        final com.codename1.impl.ios.sim.rpc.RpcRenderBridge bridge =
                new com.codename1.impl.ios.sim.rpc.RpcRenderBridge(port);
        com.codename1.impl.ios.sim.bridge.BridgeRegistry.setBridge(bridge);
        com.codename1.impl.ImplementationFactory.setInstance(
                new com.codename1.impl.ImplementationFactory() {
                    public Object createImplementation() {
                        return new com.codename1.impl.ios.sim.child.BridgedSimImplementation(bridge) {
                            // RPC pure mode: a single universe over the relay
                            public int getDisplayWidth() {
                                return bridge.getAppWidth();
                            }

                            public int getDisplayHeight() {
                                return bridge.getAppHeight();
                            }
                        };
                    }
                });

        bridge.setRelayEvents(new com.codename1.impl.ios.sim.rpc.RpcRenderBridge.RelayEvents() {
            public void pointerEvent(int type, int x, int y) {
                Display d = Display.getInstance();
                if (System.getProperty("cn1.sim.rpc.debug") != null) {
                    final int fx = x, fy = y;
                    d.callSerially(new Runnable() {
                        public void run() {
                            com.codename1.ui.Form cur = Display.getInstance().getCurrent();
                            com.codename1.ui.Component c = cur == null ? null : cur.getComponentAt(fx, fy);
                            System.out.println("[cn1sim-rpc] componentAt " + fx + "," + fy + " = "
                                    + (c == null ? "null" : c.getClass().getSimpleName() + " abs=" + c.getAbsoluteX() + "," + c.getAbsoluteY() + " " + c.getWidth() + "x" + c.getHeight()));
                        }
                    });
                }
                switch (type) {
                    case 1:
                        d.pointerPressed(new int[]{x}, new int[]{y});
                        break;
                    case 2:
                        d.pointerReleased(new int[]{x}, new int[]{y});
                        break;
                    case 3:
                        d.pointerDragged(new int[]{x}, new int[]{y});
                        break;
                    default:
                        break;
                }
            }

            public void keyEvent(int type, int code) {
                if (type == 1) {
                    Display.getInstance().keyPressed(code);
                } else {
                    Display.getInstance().keyReleased(code);
                }
            }

            public void sizeChanged(int w, int h) {
                // the relay can resize before Display.init runs; the bridge
                // already recorded the dims so init picks them up
                if (Display.isInitialized()) {
                    Display.getInstance().sizeChanged(w, h);
                }
            }
        });

        Display.init(null);
        if (Display.getInstance().hasNativeTheme()) {
            Display.getInstance().installNativeTheme();
        }
        final Object app = Class.forName(mainClass).getDeclaredConstructor().newInstance();
        invokeLifecycle(app, "init", new Class[]{Object.class}, new Object[]{null});
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                try {
                    invokeLifecycle(app, "start", new Class[0], new Object[0]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        });
        // keep the process alive; the relay connection drives everything
        Object lock = new Object();
        synchronized (lock) {
            lock.wait();
        }
    }

    private static void invokeLifecycle(Object app, String name, Class[] sig, Object[] args) throws Exception {
        try {
            Method m = app.getClass().getMethod(name, sig);
            m.invoke(app, args);
        } catch (NoSuchMethodException ignored) {
            // lifecycle methods are optional
        }
    }

    /**
     * Isolated mode: the parent paints the device skin natively and the
     * user's app runs in its own Codename One universe (child-first
     * classloader) rendering into the skin's screen rectangle.
     * Requires -Dcn1.sim.skin=/path/to/device.skin
     */
    private static void runIsolated(final String mainClass) throws Exception {
        String skin = System.getProperty("cn1.sim.skin");
        if (skin == null) {
            System.err.println("Isolated mode requires -Dcn1.sim.skin=/path/to/device.skin");
            System.exit(1);
        }
        final IsolatedAppRunner runner = new IsolatedAppRunner();
        runner.loadSkin(skin);
        CN1SimHost.isolatedRunner = runner;
        CN1SimHost.createNativeWindow("Codename One Simulator", runner.getWindowWidth(), runner.getWindowHeight());

        Thread appThread = new Thread("CN1Sim-IsolatedApp") {
            public void run() {
                try {
                    // wait for the window surface, then paint the skin before
                    // the app's first flush
                    for (int i = 0; i < 100 && !CN1SimHost.isSurfaceReady(); i++) {
                        Thread.sleep(100);
                    }
                    runner.drawSkinFrame();
                    runner.runApp(mainClass);
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(1);
                }
            }
        };
        appThread.setDaemon(true);
        appThread.start();

        CN1SimHost.runEventLoop();
    }

    /**
     * Shell mode: the simulator chrome itself is a Codename One app in the
     * parent universe (skin frame + tool panels, the network monitor first),
     * with the user's app isolated in a child universe rendering into the
     * skin's screen rectangle. Requires -Dcn1.sim.skin.
     */
    private static void runShell(final String mainClass) throws Exception {
        String skin = System.getProperty("cn1.sim.skin");
        if (skin == null) {
            System.err.println("Shell mode requires -Dcn1.sim.skin=/path/to/device.skin");
            System.exit(1);
        }
        final IsolatedAppRunner runner = new IsolatedAppRunner();
        runner.loadSkin(skin);

        // scale the skin to a workable window height, tools panel beside it
        final int maxSkinH = Integer.getInteger("cn1.sim.maxHeight", 900);
        final double scale = Math.min(1.0, maxSkinH / (double) runner.getWindowHeight());
        com.codename1.impl.ios.sim.bridge.BridgeRegistry.setRenderScale(scale);
        final int skinW = (int) Math.round(runner.getWindowWidth() * scale);
        final int skinH = (int) Math.round(runner.getWindowHeight() * scale);
        int toolsW = Math.max(260, skinW / 2);
        final int winW = skinW + toolsW + 16;
        final int winH = skinH + 16;

        CN1SimHost.isolatedRunner = runner;
        CN1SimHost.createNativeWindow("Codename One Simulator", winW, winH);

        // test driver: -Dcn1.sim.testMenu="delayMs:Label;delayMs:Label;..."
        // fires native-menu commands programmatically, the exact path a user
        // selection takes minus the NSMenu click itself
        final String testMenu = System.getProperty("cn1.sim.testMenu");
        if (testMenu != null) {
            new Thread("CN1Sim-MenuTestDriver") {
                public void run() {
                    try {
                        for (String step : testMenu.split(";")) {
                            int colon = step.indexOf(':');
                            long delay = Long.parseLong(step.substring(0, colon));
                            String label = step.substring(colon + 1);
                            Thread.sleep(delay);
                            if (label.startsWith("click=")) {
                                // "click=x,y" - a synthetic window tap
                                String[] xy = label.substring("click=".length()).split(",");
                                int cx = Integer.parseInt(xy[0]);
                                int cy = Integer.parseInt(xy[1]);
                                CN1SimHost.nativePointerEvent(1, cx, cy);
                                CN1SimHost.nativePointerEvent(2, cx, cy);
                                System.out.println("[cn1sim] test-menu clicked " + cx + "," + cy);
                                continue;
                            }
                            boolean ok = CN1SimHost.fireMenuCommandByLabel(label);
                            System.out.println("[cn1sim] test-menu fired '" + label + "' ok=" + ok);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }.start();
        }

        Thread bootThread = new Thread("CN1Sim-ShellBoot") {
            public void run() {
                try {
                    for (int i = 0; i < 100 && !CN1SimHost.isSurfaceReady(); i++) {
                        Thread.sleep(100);
                    }
                    // the parent universe gets its own CN1 Display over the
                    // whole window - the simulator chrome is CN1 UI
                    com.codename1.impl.ios.sim.bridge.BridgeRegistry.setShellBridge(
                            new com.codename1.impl.ios.RenderBridgeImpl(0, 0, winW, winH));
                    final com.codename1.impl.ios.sim.child.BridgedSimImplementation[] shellImplHolder =
                            new com.codename1.impl.ios.sim.child.BridgedSimImplementation[1];
                    com.codename1.impl.ImplementationFactory.setInstance(
                            new com.codename1.impl.ImplementationFactory() {
                                public Object createImplementation() {
                                    shellImplHolder[0] = new com.codename1.impl.ios.sim.child.BridgedSimImplementation(
                                            com.codename1.impl.ios.sim.bridge.BridgeRegistry.getShellBridge());
                                    return shellImplHolder[0];
                                }
                            });
                    Display.init(null);
                    CN1SimHost.parentDisplayActive = true;
                    final int toolsWf = toolsW;

                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            try {
                                com.codename1.ui.Image skinImg = com.codename1.ui.Image.createImage(
                                        new java.io.ByteArrayInputStream(runner.getSkinPngBytes()));
                                // bezel crop from the UNSCALED image - the
                                // native overlay scales while drawing
                                com.codename1.ui.Image portraitCrop = skinImg.subImage(
                                        runner.getScreenX(), runner.getScreenY(),
                                        runner.getScreenW(), runner.getScreenH(), true);
                                if (scale < 1.0) {
                                    skinImg = skinImg.scaled(skinW, skinH);
                                }
                                final com.codename1.impl.ios.sim.shell.SimulatorShell shell =
                                        new com.codename1.impl.ios.sim.shell.SimulatorShell(skinImg,
                                                (int) Math.round(runner.getScreenX() * scale),
                                                (int) Math.round(runner.getScreenY() * scale),
                                                (int) Math.round(runner.getScreenW() * scale),
                                                (int) Math.round(runner.getScreenH() * scale));
                                com.codename1.ui.Image landscapeCrop = null;
                                if (runner.hasLandscape()) {
                                    com.codename1.ui.Image lskin = com.codename1.ui.Image.createImage(
                                            new java.io.ByteArrayInputStream(runner.getLandscapeSkinPngBytes()));
                                    landscapeCrop = lskin.subImage(
                                            runner.getLandscapeScreenX(), runner.getLandscapeScreenY(),
                                            runner.getLandscapeScreenW(), runner.getLandscapeScreenH(), true);
                                    if (scale < 1.0) {
                                        lskin = lskin.scaled(
                                                (int) Math.round(runner.getLandscapeSkinW() * scale),
                                                (int) Math.round(runner.getLandscapeSkinH() * scale));
                                    }
                                    shell.setLandscapeSkin(lskin,
                                            (int) Math.round(runner.getLandscapeScreenX() * scale),
                                            (int) Math.round(runner.getLandscapeScreenY() * scale),
                                            (int) Math.round(runner.getLandscapeScreenW() * scale),
                                            (int) Math.round(runner.getLandscapeScreenH() * scale));
                                }
                                shell.setOverlayCrops(portraitCrop, landscapeCrop);
                                shell.setToolsWidth(toolsWf);
                                final String skinPath = new java.io.File(
                                        System.getProperty("cn1.sim.skin")).getAbsolutePath();
                                final String[] currentSkin = {skinPath};
                                final int[] maxH = {maxSkinH};
                                shell.setSkinContext(new java.io.File(skinPath).getParent(),
                                        skinPath,
                                        new com.codename1.impl.ios.sim.shell.SimulatorShell.SkinSwitcher() {
                                            public void switchTo(String path) {
                                                currentSkin[0] = path;
                                                switchSkin(runner, shell, maxH[0], path);
                                            }
                                        });
                                shell.setZoomToggle(new Runnable() {
                                    public void run() {
                                        // zoom = the skin at 100% scale inside
                                        // the unchanged window, scrollable
                                        maxH[0] = shell.isZoomed()
                                                ? runner.getWindowHeight() : maxSkinH;
                                        applySkinScaled(runner, shell, maxH[0], currentSkin[0]);
                                        // rebuild the app's theme fonts at the
                                        // new render scale
                                        com.codename1.impl.ios.sim.bridge.ChildControl cc =
                                                com.codename1.impl.ios.sim.bridge.BridgeRegistry.getChildControl();
                                        if (cc != null) {
                                            cc.control("nativeTheme:"
                                                    + com.codename1.impl.ios.sim.bridge.BridgeRegistry.getNativeThemePref());
                                        }
                                    }
                                });
                                shell.setScrollRebind(new Runnable() {
                                    public void run() {
                                        // zoom-mode scrolling moved the app
                                        // rectangle - rebind offsets only
                                        int[] rect = shell.getAppScreenRect();
                                        int[] vp = shell.getViewportRect();
                                        runner.setRouteRect(rect[0], rect[1], rect[2], rect[3]);
                                        runner.setRouteViewport(vp[0], vp[1], vp[2], vp[3]);
                                        com.codename1.impl.ios.sim.bridge.BridgeRegistry.setBridge(
                                                new com.codename1.impl.ios.RenderBridgeImpl(
                                                        rect[0], rect[1], rect[2], rect[3],
                                                        vp[0], vp[1], vp[2], vp[3]));
                                        shell.updateScreenOverlay();
                                    }
                                });
                                // trackpad / mouse-wheel pans the zoom viewport
                                CN1SimHost.scrollWheelHandler =
                                        new CN1SimHost.ScrollWheelHandler() {
                                            public void scrollWheel(final int dx, final int dy) {
                                                Display.getInstance().callSerially(new Runnable() {
                                                    public void run() {
                                                        shell.scrollViewport(dx, dy);
                                                    }
                                                });
                                            }
                                        };
                                // user window resizes rescale the chrome to
                                // the dragged height; the window then snaps
                                // to the fitted content size
                                final int[] fitDims = {winW, winH};
                                final Runnable[] relayoutHolder = new Runnable[1];
                                relayoutHolder[0] = new Runnable() {
                                    public void run() {
                                        // resize the window for the new
                                        // orientation/skin/panel layout,
                                        // relayout the shell, then rebind
                                        // the app universe. resizeWindow is
                                        // synchronous - once it returns the
                                        // fresh surface is live and every
                                        // repaint below lands on it. In zoom
                                        // mode the window keeps its fitted
                                        // size and the viewport scrolls
                                        final int newW;
                                        final int newH;
                                        if (shell.isZoomed()) {
                                            newW = fitDims[0];
                                            newH = fitDims[1];
                                        } else {
                                            newW = shell.getActiveSkinWidth()
                                                    + shell.getDesiredToolsWidth() + 16;
                                            newH = shell.getActiveSkinHeight() + 16
                                                    + shell.getChromeBottomHeight();
                                            fitDims[0] = newW;
                                            fitDims[1] = newH;
                                        }
                                        CN1SimHost.resizeWindow(newW, newH);
                                        com.codename1.impl.ios.sim.bridge.BridgeRegistry.setShellBridge(
                                                new com.codename1.impl.ios.RenderBridgeImpl(0, 0, newW, newH));
                                        shellImplHolder[0].notifySizeChanged(newW, newH);
                                        Display.getInstance().callSerially(new Runnable() {
                                            public void run() {
                                                // runs after the shell's size
                                                // event finished its relayout;
                                                // force one anyway for
                                                // same-size changes (monitor
                                                // toggle, equal-size skins)
                                                com.codename1.ui.Form current = Display.getInstance().getCurrent();
                                                if (current != null) {
                                                    current.forceRevalidate();
                                                }
                                                int[] rect = shell.getAppScreenRect();
                                                int[] vp = shell.getViewportRect();
                                                runner.setRouteRect(rect[0], rect[1], rect[2], rect[3]);
                                                runner.setRouteViewport(vp[0], vp[1], vp[2], vp[3]);
                                                com.codename1.impl.ios.sim.bridge.BridgeRegistry.setBridge(
                                                        new com.codename1.impl.ios.RenderBridgeImpl(
                                                                rect[0], rect[1], rect[2], rect[3],
                                                                vp[0], vp[1], vp[2], vp[3]));
                                                com.codename1.impl.ios.sim.bridge.InputSink sink =
                                                        com.codename1.impl.ios.sim.bridge.BridgeRegistry.getInputSink();
                                                if (sink != null) {
                                                    sink.sizeChanged(rect[2], rect[3]);
                                                    // same-size relayouts drop
                                                    // the size event - request
                                                    // an explicit child repaint
                                                    sink.repaintRequest();
                                                }
                                                shell.updateScreenOverlay();
                                                // final chrome sweep erases any
                                                // child pixels painted at the
                                                // old rectangle; its flush
                                                // pokes the child recomposite
                                                if (current != null) {
                                                    current.repaint();
                                                }
                                            }
                                        });
                                    }
                                };
                                shell.setOrientationChangedCallback(relayoutHolder[0]);
                                CN1SimHost.windowResizeHandler =
                                        new CN1SimHost.WindowResizeHandler() {
                                            public void windowResized(final int w, final int h) {
                                                Display.getInstance().callSerially(new Runnable() {
                                                    public void run() {
                                                        if (shell.isZoomed()) {
                                                            // zoom mode: the drag
                                                            // resizes the viewport,
                                                            // the 100% scale stays
                                                            fitDims[0] = w;
                                                            fitDims[1] = h;
                                                            relayoutHolder[0].run();
                                                        } else {
                                                            maxH[0] = Math.max(300,
                                                                    h - 16 - shell.getChromeBottomHeight());
                                                            applySkinScaled(runner, shell,
                                                                    maxH[0], currentSkin[0]);
                                                        }
                                                    }
                                                });
                                            }
                                        };
                                shell.setPushSender(
                                        new com.codename1.impl.ios.sim.shell.SimulatorShell.PushSender() {
                                            public void sendPush(String payload) {
                                                runner.invokePush(payload);
                                            }
                                        });
                                shell.setAppController(
                                        new com.codename1.impl.ios.sim.shell.SimulatorShell.AppController() {
                                            public void pauseApp() {
                                                runner.invokeAppLifecycle("stop");
                                            }

                                            public void resumeApp() {
                                                runner.invokeAppLifecycle("start");
                                            }
                                        });
                                shell.show();
                                // after the first layout the skin's window
                                // position is known: boot the isolated app
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        try {
                                            int[] rect = shell.getAppScreenRect();
                                            int[] vp = shell.getViewportRect();
                                            runner.setRouteRect(rect[0], rect[1], rect[2], rect[3]);
                                            runner.setRouteViewport(vp[0], vp[1], vp[2], vp[3]);
                                            com.codename1.impl.ios.sim.bridge.BridgeRegistry.setBridge(
                                                    new com.codename1.impl.ios.RenderBridgeImpl(
                                                            rect[0], rect[1], rect[2], rect[3],
                                                            vp[0], vp[1], vp[2], vp[3]));
                                            shell.updateScreenOverlay();
                                            runner.runApp(mainClass);
                                            // one relayout pass after boot:
                                            // shrinks the window when no
                                            // sidebar is enabled and settles
                                            // skin/app alignment
                                            Display.getInstance().callSerially(new Runnable() {
                                                public void run() {
                                                    relayoutHolder[0].run();
                                                }
                                            });
                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                            System.exit(1);
                                        }
                                    }
                                });
                            } catch (Throwable t) {
                                t.printStackTrace();
                                System.exit(1);
                            }
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(1);
                }
            }
        };
        bootThread.setDaemon(true);
        bootThread.start();

        CN1SimHost.runEventLoop();
    }

    /**
     * The Skins menu action: reloads the skin file, rebuilds the scaled
     * images and applies them to the shell, which triggers the same
     * window-resize + app-rebind path as a rotation. Runs on the shell EDT.
     */
    private static void switchSkin(IsolatedAppRunner runner,
            com.codename1.impl.ios.sim.shell.SimulatorShell shell, int maxSkinH, String path) {
        try {
            runner.loadSkin(path);
            applySkinScaled(runner, shell, maxSkinH, path);
            System.out.println("[cn1sim] switched skin to " + path);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Rebuilds the shell's skin images at the given target height from the
     * runner's already-loaded skin - shared by skin switching, the Zoom
     * toggle and user window resizes.
     */
    private static void applySkinScaled(IsolatedAppRunner runner,
            com.codename1.impl.ios.sim.shell.SimulatorShell shell, int maxSkinH, String path) {
        try {
            double sc = Math.min(1.0, maxSkinH / (double) runner.getWindowHeight());
            // also fit the width left beside the sidebar so the skin never
            // overflows under the tool panel (the relay window can't widen)
            int availW = shell.getAvailableSkinWidth();
            if (availW > 0 && runner.getWindowWidth() > 0) {
                sc = Math.min(sc, availW / (double) runner.getWindowWidth());
            }
            com.codename1.impl.ios.sim.bridge.BridgeRegistry.setRenderScale(sc);
            // overlay crops feed the native rounded-corner overlay only
            // (no-op in RPC mode); skip their expensive getRGB round-trips
            boolean overlayCrops = com.codename1.impl.ios.sim.CN1SimHost.isLoaded();
            com.codename1.ui.Image skinImg = com.codename1.ui.Image.createImage(
                    new java.io.ByteArrayInputStream(runner.getSkinPngBytes()));
            com.codename1.ui.Image portraitCrop = overlayCrops ? skinImg.subImage(
                    runner.getScreenX(), runner.getScreenY(),
                    runner.getScreenW(), runner.getScreenH(), true) : null;
            if (sc < 1.0) {
                skinImg = skinImg.scaled(
                        (int) Math.round(runner.getWindowWidth() * sc),
                        (int) Math.round(runner.getWindowHeight() * sc));
            }
            com.codename1.ui.Image lskin = null;
            com.codename1.ui.Image landscapeCrop = null;
            int lx = 0, ly = 0, lw = 0, lh = 0;
            if (runner.hasLandscape()) {
                lskin = com.codename1.ui.Image.createImage(
                        new java.io.ByteArrayInputStream(runner.getLandscapeSkinPngBytes()));
                landscapeCrop = overlayCrops ? lskin.subImage(
                        runner.getLandscapeScreenX(), runner.getLandscapeScreenY(),
                        runner.getLandscapeScreenW(), runner.getLandscapeScreenH(), true) : null;
                if (sc < 1.0) {
                    lskin = lskin.scaled(
                            (int) Math.round(runner.getLandscapeSkinW() * sc),
                            (int) Math.round(runner.getLandscapeSkinH() * sc));
                }
                lx = (int) Math.round(runner.getLandscapeScreenX() * sc);
                ly = (int) Math.round(runner.getLandscapeScreenY() * sc);
                lw = (int) Math.round(runner.getLandscapeScreenW() * sc);
                lh = (int) Math.round(runner.getLandscapeScreenH() * sc);
            }
            shell.setOverlayCrops(portraitCrop, landscapeCrop);
            shell.applySkin(path, skinImg,
                    (int) Math.round(runner.getScreenX() * sc),
                    (int) Math.round(runner.getScreenY() * sc),
                    (int) Math.round(runner.getScreenW() * sc),
                    (int) Math.round(runner.getScreenH() * sc),
                    lskin, lx, ly, lw, lh);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
