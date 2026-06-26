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

import com.codename1.impl.ios.RenderBridgeImpl;
import com.codename1.impl.ios.sim.bridge.BridgeRegistry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Boots the isolated simulator: the parent universe owns the native window
 * and paints the device skin through the native pipeline, while the user's
 * app runs in a child-first classloader (its own Codename One universe)
 * rendering into the skin's screen rectangle through the shared
 * {@link com.codename1.impl.ios.sim.bridge.RenderBridge}.
 */
public class IsolatedAppRunner {
    private long skinPeer;
    private byte[] skinPngBytes;
    private int skinW;
    private int skinH;
    private int screenX;
    private int screenY;
    private int screenW;
    private int screenH;
    private RenderBridgeImpl bridge;

    /**
     * Parses the .skin file: decodes skin.png through the native pipeline and
     * derives the screen rectangle from skin.properties (round skins) or the
     * black region of skin_map.png (classic skins, read back natively - no
     * java.desktop ImageIO involved).
     */
    /**
     * Resource bridge for decoding skin assets: the registered shell bridge
     * when one exists (RPC mode), the JNI bridge otherwise.
     */
    private com.codename1.impl.ios.sim.bridge.RenderBridge decoder() {
        com.codename1.impl.ios.sim.bridge.RenderBridge b = BridgeRegistry.getShellBridge();
        return b != null ? b : new RenderBridgeImpl(0, 0, 1, 1);
    }

    public void loadSkin(String skinPath) throws Exception {
        byte[] skinPng = null;
        byte[] mapPng = null;
        byte[] themeRes = null;
        // reloadable for the Skins menu - a skin without a landscape variant
        // must not inherit the previous one's
        landscapeSkinPngBytes = null;
        landscapeMapPng = null;
        if (skinPeer != 0) {
            decoder().releasePeer(skinPeer);
            skinPeer = 0;
        }
        Properties props = new Properties();
        ZipFile zip = new ZipFile(skinPath);
        try {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.getName().equals("skin.png")) {
                    skinPng = readAll(zip.getInputStream(e));
                } else if (e.getName().equals("skin_map.png")) {
                    mapPng = readAll(zip.getInputStream(e));
                } else if (e.getName().equals("skin_l.png")) {
                    landscapeSkinPngBytes = readAll(zip.getInputStream(e));
                } else if (e.getName().equals("skin_map_l.png")) {
                    landscapeMapPng = readAll(zip.getInputStream(e));
                } else if (e.getName().equals("skin.properties")) {
                    props.load(zip.getInputStream(e));
                } else if (e.getName().endsWith(".res") && themeRes == null) {
                    // the skin's embedded native theme
                    themeRes = readAll(zip.getInputStream(e));
                }
            }
        } finally {
            zip.close();
        }
        if (skinPng == null) {
            throw new IllegalStateException("No skin.png in " + skinPath);
        }
        skinPngBytes = skinPng;

        // a throwaway bridge for decoding; offsets do not matter yet
        com.codename1.impl.ios.sim.bridge.RenderBridge decode = decoder();
        long[] skin = decode.createImage(skinPng);
        skinPeer = skin[0];
        skinW = (int) skin[1];
        skinH = (int) skin[2];

        if (props.getProperty("roundScreen", "false").equalsIgnoreCase("true")) {
            screenX = Integer.parseInt(props.getProperty("displayX"));
            screenY = Integer.parseInt(props.getProperty("displayY"));
            screenW = Integer.parseInt(props.getProperty("displayWidth"));
            screenH = Integer.parseInt(props.getProperty("displayHeight"));
        } else if (mapPng != null) {
            // black pixels mark the screen region (same rule as DeviceSkin)
            int[] rect = parseScreenRect(decode, mapPng);
            screenX = rect[0];
            screenY = rect[1];
            screenW = rect[2];
            screenH = rect[3];
        } else {
            throw new IllegalStateException("Skin has neither roundScreen properties nor skin_map.png");
        }
        System.out.println("[cn1sim] skin " + skinW + "x" + skinH
                + " screen " + screenX + "," + screenY + " " + screenW + "x" + screenH);

        // skin metadata the app universe consumes: simulated platform, the
        // embedded native theme and the safe areas (unscaled screen coords)
        com.codename1.impl.ios.sim.bridge.BridgeRegistry.setSkinInfo(
                props.getProperty("platformName", "ios"),
                themeRes,
                parseSafeArea(props, "safePortrait", screenW, screenH),
                parseSafeArea(props, "safeLandscape", screenH, screenW),
                screenW, screenH);
        com.codename1.impl.ios.sim.bridge.BridgeRegistry.setFontMetrics(
                Integer.parseInt(props.getProperty("smallFontSize", "0")),
                Integer.parseInt(props.getProperty("mediumFontSize", "0")),
                Integer.parseInt(props.getProperty("largeFontSize", "0")),
                Integer.parseInt(props.getProperty("ppi", "0")));

        if (landscapeSkinPngBytes != null && landscapeMapPng != null) {
            long[] lskin = decode.createImage(landscapeSkinPngBytes);
            landscapeSkinW = (int) lskin[1];
            landscapeSkinH = (int) lskin[2];
            decode.releasePeer(lskin[0]);
            int[] lrect = parseScreenRect(decode, landscapeMapPng);
            landscapeScreenX = lrect[0];
            landscapeScreenY = lrect[1];
            landscapeScreenW = lrect[2];
            landscapeScreenH = lrect[3];
            System.out.println("[cn1sim] landscape skin " + landscapeSkinW + "x" + landscapeSkinH
                    + " screen " + landscapeScreenX + "," + landscapeScreenY
                    + " " + landscapeScreenW + "x" + landscapeScreenH);
        }
    }

    /**
     * Reads a safe-area rectangle from skin.properties; null when the skin
     * defines none (the whole screen is safe).
     */
    private static int[] parseSafeArea(Properties props, String prefix, int defW, int defH) {
        if (props.getProperty(prefix + "X") == null && props.getProperty(prefix + "Y") == null
                && props.getProperty(prefix + "Width") == null
                && props.getProperty(prefix + "Height") == null) {
            return null;
        }
        return new int[]{
                Integer.parseInt(props.getProperty(prefix + "X", "0")),
                Integer.parseInt(props.getProperty(prefix + "Y", "0")),
                Integer.parseInt(props.getProperty(prefix + "Width", String.valueOf(defW))),
                Integer.parseInt(props.getProperty(prefix + "Height", String.valueOf(defH)))
        };
    }

    /**
     * Decodes a skin map natively and returns the black-region bounding box.
     */
    private int[] parseScreenRect(com.codename1.impl.ios.sim.bridge.RenderBridge decode, byte[] mapPng) {
        // Decode the skin map LOCALLY on the host JVM (ImageIO) instead of
        // round-tripping a multi-megabyte getRGB through the relay. That socket
        // transfer was the intermittent boot hang (OP_GET_RGB on the ~3M-pixel
        // map could stall the relay's serve thread while it also tried to read
        // the host's render batches). The relay never needs the map pixels.
        try {
            java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(mapPng));
            if (bi == null) {
                return new int[]{0, 0, 0, 0};
            }
            int mw = bi.getWidth();
            int mh = bi.getHeight();
            int[] px = bi.getRGB(0, 0, mw, mh, null, 0, mw);
            int x1 = Integer.MAX_VALUE, y1 = Integer.MAX_VALUE, x2 = 0, y2 = 0;
            for (int i = 0; i < px.length; i++) {
                if ((px[i] & 0xffffff) == 0 && (px[i] >>> 24) == 0xff) {
                    int x = i % mw;
                    int y = i / mw;
                    if (x < x1) {
                        x1 = x;
                    }
                    if (y < y1) {
                        y1 = y;
                    }
                    if (x > x2) {
                        x2 = x;
                    }
                    if (y > y2) {
                        y2 = y;
                    }
                }
            }
            if (x1 == Integer.MAX_VALUE) {
                return new int[]{0, 0, 0, 0};
            }
            return new int[]{x1, y1, x2 - x1 + 1, y2 - y1 + 1};
        } catch (Exception ex) {
            ex.printStackTrace();
            return new int[]{0, 0, 0, 0};
        }
    }

    private byte[] landscapeSkinPngBytes;
    private byte[] landscapeMapPng;
    private int landscapeSkinW, landscapeSkinH;
    private int landscapeScreenX, landscapeScreenY, landscapeScreenW, landscapeScreenH;

    /** @return true when the skin ships a landscape variant */
    public boolean hasLandscape() {
        return landscapeSkinPngBytes != null && landscapeSkinW > 0;
    }

    public byte[] getLandscapeSkinPngBytes() {
        return landscapeSkinPngBytes;
    }

    public int getLandscapeSkinW() {
        return landscapeSkinW;
    }

    public int getLandscapeSkinH() {
        return landscapeSkinH;
    }

    public int getLandscapeScreenX() {
        return landscapeScreenX;
    }

    public int getLandscapeScreenY() {
        return landscapeScreenY;
    }

    public int getLandscapeScreenW() {
        return landscapeScreenW;
    }

    public int getLandscapeScreenH() {
        return landscapeScreenH;
    }

    public int getWindowWidth() {
        return skinW;
    }

    public int getWindowHeight() {
        return skinH;
    }

    /**
     * Paints the skin frame around the screen rectangle. Called once after
     * the window is up - the persistent screen texture keeps it.
     */
    public void drawSkinFrame() {
        // a full-window bridge paints the frame; the app's bridge is
        // confined to the screen rectangle
        RenderBridgeImpl whole = new RenderBridgeImpl(0, 0, skinW, skinH);
        whole.setClip(0, 0, skinW, skinH);
        whole.fillRect(0xffffff, 255, 0, 0, skinW, skinH);
        whole.drawImage(skinPeer, 255, 0, 0, skinW, skinH);
        whole.flush();

        bridge = new RenderBridgeImpl(screenX, screenY, screenW, screenH);
        BridgeRegistry.setBridge(bridge);
    }

    /** routing rectangle in window coordinates (shell mode overrides it) */
    private int routeX = -1, routeY, routeW, routeH;
    /** viewport limiting routing in zoom mode (the app rect overflows it) */
    private int viewX, viewY, viewW = Integer.MAX_VALUE, viewH = Integer.MAX_VALUE;

    /**
     * Overrides where the app's screen lives in window coordinates - used in
     * shell mode where the skin is laid out (and scaled) by the shell UI.
     */
    public void setRouteRect(int x, int y, int w, int h) {
        routeX = x;
        routeY = y;
        routeW = w;
        routeH = h;
    }

    /**
     * Window region pointer routing is limited to - in zoom mode the screen
     * rectangle extends beyond the scroller, and clicks on the surrounding
     * chrome (sidebar) must stay with the shell.
     */
    public void setRouteViewport(int x, int y, int w, int h) {
        viewX = x;
        viewY = y;
        viewW = w;
        viewH = h;
    }

    /**
     * Routes a window-coordinate pointer event into the app universe when it
     * falls inside the screen rectangle.
     */
    public boolean routePointer(int type, int windowX, int windowY) {
        com.codename1.impl.ios.sim.bridge.InputSink sink = BridgeRegistry.getInputSink();
        if (sink == null) {
            return false;
        }
        boolean debug = System.getenv("CN1_SIM_DEBUG") != null;
        if (windowX < viewX || windowY < viewY
                || windowX >= viewX + viewW || windowY >= viewY + viewH) {
            if (debug && type == 1) {
                System.err.println("cn1sim: route REJECT(viewport) " + windowX + "," + windowY
                        + " vp=" + viewX + "," + viewY + " " + viewW + "x" + viewH);
            }
            return false;
        }
        int rx = routeX >= 0 ? routeX : screenX;
        int ry = routeX >= 0 ? routeY : screenY;
        int rw = routeX >= 0 ? routeW : screenW;
        int rh = routeX >= 0 ? routeH : screenH;
        int x = windowX - rx;
        int y = windowY - ry;
        if (x < 0 || y < 0 || x >= rw || y >= rh) {
            if (debug && type == 1) {
                System.err.println("cn1sim: route REJECT(rect) " + windowX + "," + windowY
                        + " rect=" + rx + "," + ry + " " + rw + "x" + rh);
            }
            return false;
        }
        if (debug && type == 1) {
            System.err.println("cn1sim: route APP " + windowX + "," + windowY
                    + " -> " + x + "," + y);
        }
        sink.pointerEvent(type, x, y);
        return true;
    }

    /**
     * @return the raw skin.png bytes (available after loadSkin)
     */
    public byte[] getSkinPngBytes() {
        return skinPngBytes;
    }

    public int getScreenX() {
        return screenX;
    }

    public int getScreenY() {
        return screenY;
    }

    public int getScreenW() {
        return screenW;
    }

    public int getScreenH() {
        return screenH;
    }

    /**
     * Boots the user's app inside a fresh Codename One universe.
     *
     * @param mainClass the app's main class name
     */
    public void runApp(final String mainClass) throws Exception {
        String cp = System.getProperty("java.class.path");
        String[] parts = cp.split(File.pathSeparator);
        URL[] urls = new URL[parts.length];
        for (int i = 0; i < parts.length; i++) {
            urls[i] = new File(parts[i]).toURI().toURL();
        }
        final ChildFirstClassLoader child = new ChildFirstClassLoader(
                urls, IsolatedAppRunner.class.getClassLoader(), appPackagePrefixes(mainClass));

        // install the bridged factory into the child universe
        Class<?> factoryCls = Class.forName("com.codename1.impl.ImplementationFactory", true, child);
        Class<?> bridgedFactoryCls = Class.forName(
                "com.codename1.impl.ios.sim.child.BridgedImplementationFactory", true, child);
        Method setInstance = factoryCls.getMethod("setInstance", factoryCls);
        setInstance.invoke(null, bridgedFactoryCls.getDeclaredConstructor().newInstance());

        // child Display.init + app lifecycle, all inside the child universe
        Class<?> displayCls = Class.forName("com.codename1.ui.Display", true, child);
        Method init = displayCls.getMethod("init", Object.class);
        init.invoke(null, (Object) null);

        final Object app = Class.forName(mainClass, true, child).getDeclaredConstructor().newInstance();
        invokeOptional(app, "init", new Class[]{Object.class}, new Object[]{null});

        final Object display = displayCls.getMethod("getInstance").invoke(null);
        Method callSerially = displayCls.getMethod("callSerially", Runnable.class);
        // device parity: the platform native theme underlies whatever theme
        // the app itself installs
        final Method installTheme = displayCls.getMethod("installNativeTheme");
        callSerially.invoke(display, new Runnable() {
            public void run() {
                try {
                    installTheme.invoke(display);
                    disableFormTransitions(child);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        callSerially.invoke(display, new Runnable() {
            public void run() {
                try {
                    invokeOptional(app, "start", new Class[0], new Object[0]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        appInstance = app;
        childDisplay = display;
        childCallSerially = callSerially;
    }

    private Object appInstance;
    private Object childDisplay;
    private Method childCallSerially;

    /**
     * The Push Simulation tool: delivers a payload to the app's
     * push(String) callback on the child EDT, mirroring how a device build
     * surfaces an incoming push to a PushCallback main class.
     */
    public void invokePush(final String payload) {
        if (appInstance == null || childCallSerially == null) {
            return;
        }
        try {
            childCallSerially.invoke(childDisplay, new Runnable() {
                public void run() {
                    try {
                        invokeOptional(appInstance, "push",
                                new Class[]{String.class}, new Object[]{payload});
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * The Pause App / Resume App tool: fires the app's stop() or start()
     * lifecycle method on the child universe's EDT, the same contract a
     * device backgrounding cycle follows.
     */
    public void invokeAppLifecycle(final String method) {
        if (appInstance == null || childCallSerially == null) {
            return;
        }
        try {
            childCallSerially.invoke(childDisplay, new Runnable() {
                public void run() {
                    try {
                        invokeOptional(appInstance, method, new Class[0], new Object[0]);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String[] appPackagePrefixes(String mainClass) {
        int idx = mainClass.lastIndexOf('.');
        if (idx < 0) {
            return new String[0];
        }
        return new String[]{mainClass.substring(0, idx + 1)};
    }

    /**
     * Turns off form-change transitions for the simulated app. CN1's slide
     * transitions snapshot forms into mutable images and composite them; over
     * the RPC relay that mutable-image path renders vertically flipped and can
     * stick mid-animation (a Metal mutable-texture orientation issue in the
     * out-of-process pipeline). Until that's resolved, a stable upright UI is
     * worth far more than animated transitions, so default both directions to
     * an empty transition. Apps that set their own per-form transition still
     * win - this only changes the look-and-feel default.
     */
    private static void disableFormTransitions(ClassLoader child) {
        try {
            Class<?> uiMgr = Class.forName("com.codename1.ui.plaf.UIManager", true, child);
            Object mgr = uiMgr.getMethod("getInstance").invoke(null);
            Object laf = uiMgr.getMethod("getLookAndFeel").invoke(mgr);
            Class<?> lafCls = Class.forName("com.codename1.ui.plaf.LookAndFeel", true, child);
            Class<?> trans = Class.forName("com.codename1.ui.animations.Transition", true, child);
            Class<?> common = Class.forName("com.codename1.ui.animations.CommonTransitions", true, child);
            Method createEmpty = common.getMethod("createEmpty");
            lafCls.getMethod("setDefaultFormTransitionOut", trans)
                    .invoke(laf, createEmpty.invoke(null));
            lafCls.getMethod("setDefaultFormTransitionIn", trans)
                    .invoke(laf, createEmpty.invoke(null));
            System.out.println("[cn1sim] form transitions disabled (RPC mutable-image limitation)");
        } catch (Exception ex) {
            System.err.println("[cn1sim] could not disable transitions: " + ex);
        }
    }

    private static void invokeOptional(Object target, String name, Class[] sig, Object[] args) throws Exception {
        try {
            Method m = target.getClass().getMethod(name, sig);
            m.invoke(target, args);
        } catch (NoSuchMethodException ignored) {
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        in.close();
        return bo.toByteArray();
    }
}
