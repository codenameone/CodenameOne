/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codenameone.fidelity;

import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.system.NativeLookup;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codenameone.fidelity.render.Cn1WidgetRenderer;
import com.codenameone.fidelity.spec.ComponentSpec;
import com.codenameone.fidelity.spec.FidelitySpec;
import com.codenameone.fidelity.spec.FidelitySpecParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Drives the fidelity suite on device. For every component that applies to the
 * current platform, for every appearance and state, it renders the CN1 widget in
 * a fixed-size tile, screenshots the form, crops to the tile, and ships it to the
 * host as "&lt;id&gt;_&lt;state&gt;_&lt;appearance&gt;_cn1.png". In golden-regen
 * mode (-Dcn1ss.fidelity.captureNative=true) it additionally renders the real
 * native widget through {@link NativeWidgetFactory} and ships "..._native.png".
 *
 * The two renders share the exact same tile pixel dimensions, so the host can
 * diff them directly without cropping/alignment math.
 */
public class FidelityDeviceRunner {
    private static final String SPEC_RESOURCE = "/fidelity-tests.yaml";
    private static final long SETTLE_MS = 700;
    private static final long SCREENSHOT_TIMEOUT_MS = 15000;

    private FidelitySpec spec;
    private String platform;
    private boolean captureNative;
    private NativeWidgetFactory nativeFactory;

    public void runSuite() {
        try {
            runSuiteImpl();
        } catch (Throwable t) {
            println("CN1SS:ERR:fidelity suite crashed: " + t);
            Log.e(t);
        } finally {
            println("CN1SS:SUITE:FINISHED");
            Log.p("CN1SS:SUITE:FINISHED");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            try {
                Display.getInstance().exitApplication();
            } catch (Throwable ignored) {
            }
        }
    }

    private void runSuiteImpl() {
        platform = resolvePlatform();
        // Default ON: every run also renders the native reference so the host
        // can (re)generate goldens and report fidelity without separate device
        // property plumbing. Set cn1ss.fidelity.captureNative=false to skip.
        captureNative = !"false".equals(Display.getInstance().getProperty("cn1ss.fidelity.captureNative", "true"));
        println("CN1SS:INFO:fidelity platform=" + platform + " captureNative=" + captureNative);
        spec = loadSpec();
        if (spec == null) {
            println("CN1SS:ERR:fidelity spec failed to load from " + SPEC_RESOURCE);
            return;
        }
        if (captureNative) {
            try {
                NativeWidgetFactory f = (NativeWidgetFactory) NativeLookup.create(NativeWidgetFactory.class);
                if (f != null && f.isSupported()) {
                    nativeFactory = f;
                } else {
                    println("CN1SS:WARN:fidelity native factory unavailable on " + platform);
                }
            } catch (Throwable t) {
                println("CN1SS:WARN:fidelity native factory lookup failed: " + t);
            }
        }
        installNativeTheme();

        List components = spec.getComponents();
        List appearances = spec.getAppearances();
        for (int i = 0; i < components.size(); i++) {
            ComponentSpec c = (ComponentSpec) components.get(i);
            if (!c.appliesToPlatform(platform)) {
                println("CN1SS:INFO:fidelity skip " + c.getId() + " (not applicable on " + platform + ")");
                continue;
            }
            if (!Cn1WidgetRenderer.isSupported(c.getId())) {
                println("CN1SS:INFO:fidelity skip " + c.getId() + " (CN1 renderer not implemented yet)");
                continue;
            }
            for (int a = 0; a < appearances.size(); a++) {
                String appearance = (String) appearances.get(a);
                // Animation-frame test: capture the component once per declared
                // progress value with its animation frozen (deterministic frames,
                // no native reference), instead of the regular per-state render.
                if (c.getFrames() != null && !c.getFrames().isEmpty()) {
                    try {
                        renderCn1Frames(c, appearance);
                    } catch (Throwable t) {
                        println("CN1SS:ERR:fidelity frame render failed " + c.getId() + " " + appearance + " " + t);
                    }
                    continue;
                }
                // Isolate each component+appearance so one bad render (e.g. a
                // native bridge failure) cannot abort the whole suite.
                try {
                    renderCn1(c, appearance);
                } catch (Throwable t) {
                    println("CN1SS:ERR:fidelity cn1 render failed " + c.getId() + " " + appearance + " " + t);
                }
                if (nativeFactory != null) {
                    try {
                        renderNative(c, appearance);
                    } catch (Throwable t) {
                        println("CN1SS:ERR:fidelity native render failed " + c.getId() + " " + appearance + " " + t);
                        StackTraceElement[] st = t.getStackTrace();
                        for (int k = 0; k < st.length && k < 10; k++) {
                            println("CN1SS:ERR:fidelity   at " + st[k]);
                        }
                    }
                }
            }
        }
    }

    // ---- CN1 render ----

    private void renderCn1(final ComponentSpec c, final String appearance) {
        final int w = pixels(spec.tileWidthMm(c), true);
        final int h = pixels(spec.tileHeightMm(c), false);
        final List states = c.getStates();
        final List wrappers = new ArrayList();
        final List names = new ArrayList();
        runOnEdtSync(new Runnable() {
            public void run() {
                applyAppearance(appearance);
                Form form = new Form("fidelity", BoxLayout.y());
                form.getAllStyles().setBgColor(bgColor(appearance));
                form.getAllStyles().setBgTransparency(255);
                form.getContentPane().getAllStyles().setBgColor(bgColor(appearance));
                form.getContentPane().getAllStyles().setBgTransparency(255);
                for (int s = 0; s < states.size(); s++) {
                    String state = (String) states.get(s);
                    Component comp = Cn1WidgetRenderer.build(c, state, appearance);
                    if (comp == null) {
                        continue;
                    }
                    com.codename1.ui.Display disp = com.codename1.ui.Display.getInstance();
                    com.codename1.ui.plaf.Style st = comp.getAllStyles();
                    st.setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                            com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                            com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                            com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
                    if ("ios".equals(platform)) {
                        // iOS: the native reference renders full-width widgets filling
                        // the tile (handled by newTile's BorderLayout) and content-sized
                        // controls pinned top-left at frame (0,0). Slider/progress are
                        // full-width but thin, vertically centred (newTile uses a
                        // LEFT/CENTRE flow). There is no Material 48dp touch-target inset
                        // on iOS, so every widget anchors at the top-left with no margin.
                        if (isGlassPanelKind(c.getId())) {
                            // The native UIVisualEffectView glass panel is inset ~1mm
                            // inside the tile; match that with an equivalent pixel margin
                            // (reinterpreted in pixels by the unit switch above) so the
                            // CN1 GlassPanel fills the tile minus the same inset.
                            int inset = disp.convertToPixels(1f);
                            st.setMargin(inset, inset, inset, inset);
                        } else {
                            if ("Toolbar".equals(c.getId())) {
                                // Match the native nav bar's covered height (~7.3mm); the
                                // bar is NORTH-anchored so the sharp backdrop fills below.
                                comp.setPreferredH(disp.convertToPixels(7.3f));
                            } else if ("Slider".equals(c.getId())) {
                                comp.setPreferredW(w);
                                // Height = the knob's height (the painter draws the thumb at
                                // the component height); taller so the knob is a tall vertical
                                // capsule, not a short horizontal oval.
                                comp.setPreferredH(disp.convertToPixels(5.5f));
                            } else if ("ProgressBar".equals(c.getId())) {
                                comp.setPreferredW(w);
                                comp.setPreferredH(Math.max(8, disp.convertToPixels(0.8f))); // ~2x thicker bar
                            }
                            st.setMargin(0, 0, 0, 0);
                        }
                    } else {
                        // Android Material: size full-width widgets and land the visible
                        // part where native lands it -- centred vertically inside a 48dp
                        // minimum touch target (an 88px switch sits 22px down in a 132px
                        // view, a 110px button 11px down). CheckBox/Radio and Slider also
                        // carry a horizontal box/track inset on the native side.
                        if ("Slider".equals(c.getId())) {
                            comp.setPreferredW(w * 3 / 5 - disp.convertToPixels(0.4f)); // ~616px drawn width
                            comp.setPreferredH(disp.convertToPixels(7f)); // M3 slider is tall (~122px)
                        } else if ("ProgressBar".equals(c.getId())) {
                            comp.setPreferredW(w * 2 / 3);
                            comp.setPreferredH(Math.max(6, h / 21)); // thin line (~11px)
                        } else if ("Tabs".equals(c.getId()) || "Toolbar".equals(c.getId())) {
                            comp.setPreferredW(w);
                        }
                        int band = disp.convertToPixels(7.62f);     // ~48dp = 132px
                        int prefH = comp.getPreferredH();
                        int topInset;
                        if ("TextField".equals(c.getId())) {
                            topInset = disp.convertToPixels(0.87f);  // ~15px outlined-field top inset
                        } else if ("ProgressBar".equals(c.getId())) {
                            topInset = disp.convertToPixels(0.93f);  // ~16px; no 48dp touch target
                        } else if ("Tabs".equals(c.getId()) || "Toolbar".equals(c.getId()) || "Dialog".equals(c.getId())) {
                            topInset = 0;             // app bar / dialog card anchor top-left
                        } else {
                            topInset = Math.max(0, (band - prefH) / 2); // centre in 48dp target
                        }
                        int leftInset = 0;
                        if ("Slider".equals(c.getId())) {
                            leftInset = disp.convertToPixels(2.2f);  // ~38px track side padding
                        }
                        st.setMargin(topInset, 0, leftInset, 0);
                    }
                    Container tile = newTile(comp, c.getId(), w, h, appearance, resolveBackdrop(c));
                    form.add(centerRow(tile));
                    wrappers.add(tile);
                    names.add(c.getId() + "_" + state + "_" + appearance + "_cn1");
                }
                // Switch forms instantly: the default slide transition would
                // otherwise be captured mid-animation, bleeding the previous
                // (e.g. light) form into this one's screenshot.
                form.setTransitionInAnimator(CommonTransitions.createEmpty());
                form.setTransitionOutAnimator(CommonTransitions.createEmpty());
                form.show();
            }
        });
        settle();
        // HONEST capture: screenshot the REAL on-screen render (what users actually see),
        // not an offscreen paintComponent re-render. The offscreen path ran CSS
        // backdrop-filter:blur via a mutable-image blur the LIVE Metal screen cannot do, so
        // it passed while the running app showed no glass -- a false green. Capturing the live
        // screen makes the suite tell the truth: glass widgets go red until the live-screen
        // glass actually works. (emitTiles, the old offscreen path, is kept for reference.)
        cropAndEmit(captureScreen(), wrappers, names, w, h);
    }

    // ---- animation-frame render ----
    //
    // Deterministic animation-frame captures (review: static screenshots cannot
    // catch a wrong motion path, overshoot, lens size or tint timing). One tile
    // per declared progress value, all on one form; each tile's Tabs has its
    // selection morph FROZEN at exactly that progress via the setMorphTestState
    // probe (travelling first tab -> last tab), so each capture is a pure
    // function of (theme, progress) and stable across runs. The single
    // screenshot is cropped per tile and shipped as
    // "<id>_t<value>_<appearance>_cn1.png"; the host regression-compares the
    // frames against committed CN1 frame goldens and property-validates the
    // motion (MorphFrameValidator). The same progress points are pinned
    // numerically against the TabSelectionMorph model in TabSelectionMorphTest.

    private void renderCn1Frames(final ComponentSpec c, final String appearance) {
        final int w = pixels(spec.tileWidthMm(c), true);
        final int h = pixels(spec.tileHeightMm(c), false);
        final List frames = c.getFrames();
        final List tabsComponents = new ArrayList();
        final List wrappers = new ArrayList();
        final List names = new ArrayList();
        runOnEdtSync(new Runnable() {
            public void run() {
                applyAppearance(appearance);
                Form form = new Form("fidelity", BoxLayout.y());
                form.getAllStyles().setBgColor(bgColor(appearance));
                form.getAllStyles().setBgTransparency(255);
                form.getContentPane().getAllStyles().setBgColor(bgColor(appearance));
                form.getContentPane().getAllStyles().setBgTransparency(255);
                for (int i = 0; i < frames.size(); i++) {
                    String frame = ((String) frames.get(i)).trim();
                    Component comp = Cn1WidgetRenderer.build(c, "normal", appearance);
                    if (comp == null) {
                        continue;
                    }
                    com.codename1.ui.plaf.Style st = comp.getAllStyles();
                    st.setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                            com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                            com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                            com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
                    st.setMargin(0, 0, 0, 0);
                    Container tile = newTile(comp, c.getId(), w, h, appearance, resolveBackdrop(c));
                    form.add(centerRow(tile));
                    tabsComponents.add(comp);
                    wrappers.add(tile);
                    names.add(c.getId() + "_t" + pad3(frame) + "_" + appearance + "_cn1");
                }
                form.setTransitionInAnimator(CommonTransitions.createEmpty());
                form.setTransitionOutAnimator(CommonTransitions.createEmpty());
                form.show();
            }
        });
        // Freeze each tile's morph AFTER layout (the probe resolves the real laid-out
        // cell bounds), then let the frozen frames paint before the capture.
        settle();
        runOnEdtSync(new Runnable() {
            public void run() {
                for (int i = 0; i < tabsComponents.size(); i++) {
                    Component comp = (Component) tabsComponents.get(i);
                    String frame = ((String) frames.get(i)).trim();
                    int value = parseFrameValue(frame);
                    if (comp instanceof com.codename1.ui.Tabs) {
                        com.codename1.ui.Tabs tabs = (com.codename1.ui.Tabs) comp;
                        int last = Math.max(0, tabs.getTabCount() - 1);
                        tabs.setMorphTestState(0, last, value);
                    } else if (comp instanceof com.codename1.components.Switch) {
                        ((com.codename1.components.Switch) comp).setMorphTestProgress(value / 100f);
                    }
                }
            }
        });
        settle();
        cropAndEmit(captureScreen(), wrappers, names, w, h);
    }

    /// Frame value "0".."100" -> int, defensively clamped.
    private int parseFrameValue(String frame) {
        int v;
        try {
            v = Integer.parseInt(frame);
        } catch (NumberFormatException nfe) {
            v = 0;
        }
        if (v < 0) {
            v = 0;
        }
        if (v > 100) {
            v = 100;
        }
        return v;
    }

    /// Zero-pads a frame value to three digits so file names sort in progress order.
    private String pad3(String frame) {
        String v = frame;
        while (v.length() < 3) {
            v = "0" + v;
        }
        return v;
    }

    // Render each tile into its OWN mutable Image via paintComponent, rather than
    // screenshotting the live form. This is what makes CSS backdrop-filter:blur work
    // for the glass tiles: the blur hook (Component.internalPaintImpl) calls
    // impl.blurRegion, and the iOS/Android/JavaSE ports can blur a mutable image's
    // backing buffer in place -- which they cannot do for the live screen drawable.
    // It also sidesteps screen retina-scale / peer-compositing entirely.
    private void emitTiles(List wrappers, List names) {
        final int n = wrappers.size();
        final Image[] imgs = new Image[n];
        runOnEdtSync(new Runnable() {
            public void run() {
                for (int i = 0; i < n; i++) {
                    Container tile = (Container) wrappers.get(i);
                    int cw = tile.getWidth() > 0 ? tile.getWidth() : 1;
                    int ch = tile.getHeight() > 0 ? tile.getHeight() : 1;
                    Image img = Image.createImage(cw, ch, 0xffffffff);
                    com.codename1.ui.Graphics g = img.getGraphics();
                    g.translate(-tile.getAbsoluteX(), -tile.getAbsoluteY());
                    tile.paintComponent(g, true);
                    imgs[i] = img;
                }
            }
        });
        for (int i = 0; i < n; i++) {
            if (imgs[i] != null) {
                Cn1ssDeviceRunnerHelper.emitImage(imgs[i], (String) names.get(i), null);
            }
        }
    }

    // ---- Native render ----
    //
    // The native reference is rasterized OFF-SCREEN by the factory (returns PNG
    // bytes), so there is no form, no peer, and no window screenshot. This is
    // synchronous and GPU-independent, which is what makes it reliable on a
    // headless emulator/simulator. The bytes are decoded into a CN1 Image and
    // shipped through the same WebSocket path as the CN1 tiles.

    private void renderNative(final ComponentSpec c, final String appearance) {
        int w = pixels(spec.tileWidthMm(c), true);
        int h = pixels(spec.tileHeightMm(c), false);
        String kind = c.getNativeKind(platform);
        String text = c.getText();
        if (text == null) {
            text = "";   // never pass null across the native bridge (toNSString(null) is unsafe)
        }
        List states = c.getStates();
        if (kind == null || !nativeFactory.isWidgetSupported(kind)) {
            println("CN1SS:INFO:fidelity native skip " + c.getId() + " kind=" + kind);
            return;
        }
        final int fw = w;
        final int fh = h;
        final String fkind = kind;
        final String ftext = text;
        for (int s = 0; s < states.size(); s++) {
            final String state = (String) states.get(s);
            final String appr = appearance;
            String name = c.getId() + "_" + state + "_" + appearance + "_native";
            // Hand the native side a writable absolute path as a String ARGUMENT
            // (which marshals cleanly on iOS) and get only a boolean back -- no
            // object crosses the return boundary, sidestepping the fromNSString /
            // nsDataToByteArr return-marshaling NPE in this ParparVM build.
            com.codename1.io.FileSystemStorage fs0 = com.codename1.io.FileSystemStorage.getInstance();
            String home = fs0.getAppHomePath();
            if (home == null) {
                home = "";
            }
            final String outPath = home + (home.endsWith("/") ? "" : "/")
                    + "cn1ss_native_" + c.getId() + "_" + state + "_" + appearance + ".png";
            final boolean[] holder = new boolean[1];
            // UIKit construction must happen on the iOS main thread; the .m hops
            // there itself. We call on the EDT so the native invocation carries a
            // valid CN1 thread context. The off-EDT WS emit stays on this thread.
            runOnEdtSync(new Runnable() {
                public void run() {
                    try {
                        holder[0] = nativeFactory.renderWidgetToFile(fkind, state, appr, ftext, outPath, fw, fh);
                    } catch (Throwable t) {
                        println("CN1SS:ERR:fidelity native render threw " + state + " " + appr + " " + t);
                        StackTraceElement[] st = t.getStackTrace();
                        for (int k = 0; k < st.length && k < 8; k++) {
                            println("CN1SS:ERR:fidelity   at " + st[k]);
                        }
                    }
                }
            });
            if (!holder[0]) {
                println("CN1SS:WARN:fidelity native render returned false " + name);
                continue;
            }
            // The factory wrote the PNG to outPath; read it back and ship the bytes.
            byte[] png = readFileBytes(outPath);
            if (png == null || png.length == 0) {
                println("CN1SS:WARN:fidelity native unreadable " + name + " path=" + outPath);
                continue;
            }
            Cn1ssDeviceRunnerHelper.emitPngBytes(png, name);
        }
    }

    /** Reads a PNG file the native factory wrote (path returned across the bridge). */
    private byte[] readFileBytes(String path) {
        try {
            com.codename1.io.FileSystemStorage fs = com.codename1.io.FileSystemStorage.getInstance();
            java.io.InputStream is = fs.openInputStream(path);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            is.close();
            return bos.toByteArray();
        } catch (Throwable t) {
            println("CN1SS:ERR:fidelity read native file failed " + path + " " + t);
            return null;
        }
    }

    // ---- shared capture/crop/emit ----

    private void cropAndEmit(Image screen, List wrappers, List names, int w, int h) {
        if (screen == null) {
            println("CN1SS:ERR:fidelity screenshot returned null");
            return;
        }
        int sw = screen.getWidth();
        int sh = screen.getHeight();
        for (int i = 0; i < wrappers.size(); i++) {
            Container tile = (Container) wrappers.get(i);
            String name = (String) names.get(i);
            int ax = tile.getAbsoluteX();
            int ay = tile.getAbsoluteY();
            int cw = tile.getWidth() > 0 ? tile.getWidth() : w;
            int ch = tile.getHeight() > 0 ? tile.getHeight() : h;
            // Clamp the crop rectangle inside the screenshot bounds.
            if (ax < 0) {
                ax = 0;
            }
            if (ay < 0) {
                ay = 0;
            }
            if (ax + cw > sw) {
                cw = sw - ax;
            }
            if (ay + ch > sh) {
                ch = sh - ay;
            }
            if (cw <= 0 || ch <= 0) {
                println("CN1SS:ERR:fidelity bad crop " + name + " ax=" + ax + " ay=" + ay + " cw=" + cw + " ch=" + ch);
                continue;
            }
            Image tileImage = screen.subImage(ax, ay, cw, ch, true);
            Cn1ssDeviceRunnerHelper.emitImage(tileImage, name, null);
        }
    }

    private Image captureScreen() {
        final Image[] out = new Image[1];
        final Object lock = new Object();
        final boolean[] done = new boolean[1];
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                try {
                    Display.getInstance().screenshot(new com.codename1.util.SuccessCallback<Image>() {
                        public void onSucess(Image value) {
                            synchronized (lock) {
                                out[0] = value;
                                done[0] = true;
                                lock.notifyAll();
                            }
                        }
                    });
                } catch (Throwable t) {
                    println("CN1SS:ERR:fidelity screenshot threw " + t);
                    synchronized (lock) {
                        done[0] = true;
                        lock.notifyAll();
                    }
                }
            }
        });
        synchronized (lock) {
            long deadline = System.currentTimeMillis() + SCREENSHOT_TIMEOUT_MS;
            while (!done[0]) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
                try {
                    lock.wait(remaining);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        return out[0];
    }

    // ---- helpers ----

    private Container newTile(Component comp, String compId, int w, int h, String appearance, String backdropSpec) {
        // Android Material widgets centre their VISIBLE part inside a 48dp minimum
        // touch target (e.g. an 88px switch track sits 22px down inside a 132px
        // view; a 110px button 11px down). The native reference render carries that
        // inset, so to land the CN1 widget at the SAME absolute position we place it
        // left-aligned but vertically centred inside an identical 48dp band at the
        // top of the tile. Component theme margin is neutralized (it is external
        // spacing, absent on the native side) so only the touch-target inset places
        // the widget.
        // Genuinely full-width widgets (text field, bars, toolbar, tabs, dialog)
        // span the whole tile in a real app, so we stretch them edge-to-edge with
        // BorderLayout.CENTER -- matching the native reference, which fills the tile
        // for these kinds. Content-sized controls (buttons, switch, checkbox/radio)
        // keep their preferred size pinned top-left.
        boolean fullWidth = isFullWidthKind(compId);
        boolean widthCenter = isWidthCenterKind(compId);
        // The iOS 26 tab bar is a floating glass PILL, content-sized and CENTRED
        // horizontally near the top -- not stretched to the tile width. TabsGeom is
        // the same widget over a flat backdrop (geometry-isolation), so it lays out
        // identically.
        boolean centered = "ios".equals(platform) && ("Tabs".equals(compId) || "TabsGeom".equals(compId)
                || "TabsMorph".equals(compId) || "TabOne".equals(compId));
        Container tile;
        if (centered) {
            tile = new Container(new FlowLayout(Component.CENTER, Component.TOP));
        } else if (fullWidth) {
            tile = new Container(new BorderLayout());
        } else if (widthCenter) {
            // Full-width but thin. The slider track floats vertically centred; the
            // progress bar sits at the TOP of the tile (the native linear bar is a
            // top-anchored hairline), so progress is top-aligned, slider centred.
            int valign = "ProgressBar".equals(compId) ? Component.TOP : Component.CENTER;
            tile = new Container(new FlowLayout(Component.LEFT, valign));
        } else {
            tile = new Container(new FlowLayout(Component.LEFT, Component.TOP));
        }
        applyBackdrop(tile, backdropSpec, appearance);
        tile.getAllStyles().setPadding(0, 0, 0, 0);
        tile.getAllStyles().setMargin(0, 0, 0, 0);
        tile.setPreferredW(w);
        tile.setPreferredH(h);
        if (fullWidth) {
            if ("ios".equals(platform) && "Toolbar".equals(compId)) {
                // The native nav bar covers only the top ~7mm of the tile (its blurred
                // glass); the rest of the tile shows the SHARP backdrop. Anchor the CN1
                // bar NORTH at its natural height so the tile's (sharp) backdrop shows
                // below it, matching the native golden -- not a full-height blurred bar.
                tile.add(BorderLayout.NORTH, comp);
            } else {
                tile.add(BorderLayout.CENTER, comp);
            }
        } else {
            tile.add(comp);
        }
        return tile;
    }

    /// Full-width widgets that fill the whole tile (both CN1 and the native
    /// reference stretch them edge-to-edge). iOS only -- on Android the tuned
    /// preferred-size + 48dp inset path handles layout, so this stays false there
    /// to preserve the committed Android baseline. Buttons/switch/checkbox/radio
    /// are content-sized and excluded on every platform.
    private boolean isFullWidthKind(String compId) {
        if (!"ios".equals(platform) || compId == null) {
            return false;
        }
        return "TextField".equals(compId)
                || "Toolbar".equals(compId) || "Dialog".equals(compId)
                || "Spinner".equals(compId)   // picker wheel fills the tile, like UIPickerView
                || isGlassPanelKind(compId);  // glass panel fills the tile (minus its margin)
    }

    /// The glass-panel isolation widgets (plain GlassPanel-UIID containers that
    /// fill the tile minus a 1mm inset, each over a different backdrop). iOS only.
    private boolean isGlassPanelKind(String compId) {
        return "ios".equals(platform) && compId != null
                && (compId.startsWith("GlassPanel") || "GlassText".equals(compId) || "GlassIcon".equals(compId));
    }

    /// Full-width-but-thin iOS widgets (slider, progress) that span the tile width
    /// and float vertically centred, like the native track. iOS only.
    private boolean isWidthCenterKind(String compId) {
        if (!"ios".equals(platform) || compId == null) {
            return false;
        }
        return "Slider".equals(compId) || "ProgressBar".equals(compId);
    }

    /// Resolves the tile backdrop for a component. The backdrop (solid colour,
    /// gradient or photo) is an iOS-only concept driven by the iOS 26 Liquid Glass
    /// blend; Android Material tiles always stay on the plain appearance background,
    /// preserving the committed Android baseline. Returns null for "no backdrop".
    private String resolveBackdrop(ComponentSpec c) {
        if (!"ios".equals(platform)) {
            return null;
        }
        return c.getBackdrop();
    }

    /// Paints the resolved backdrop behind a tile. Mirrors the native reference
    /// (NativeRef.swift) exactly: a 6-hex value is a solid fill, "gradient" is a
    /// vertical blue (#1e64ff top) to green (#28c850 bottom) ramp, and "photo" is
    /// the shared glass-backdrop.png. Anything else (incl. null) is a plain tile.
    private void applyBackdrop(Container tile, String backdropSpec, String appearance) {
        com.codename1.ui.plaf.Style s = tile.getAllStyles();
        if ("photo".equals(backdropSpec)) {
            // Liquid Glass needs content behind it. The iOS native reference renders
            // these widgets over the SAME committed backdrop PNG, so CN1 must too --
            // the only difference that should remain is how each renders the glass.
            // STRETCH (SCALED, ignore aspect) to match the native ref's .scaleToFill
            // so the two backdrops are pixel-for-pixel the same gradient; the
            // comparator then masks that shared backdrop out and scores only the
            // widget.
            Image backdrop = getGlassBackdrop();
            if (backdrop != null) {
                s.setBgImage(backdrop);
                s.setBackgroundType(com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_SCALED);
                s.setBgTransparency(255);
                return;
            }
            // Asset missing: fall through to the plain background.
        } else if ("gradient".equals(backdropSpec)) {
            // Vertical linear gradient, start colour at the top, end at the bottom
            // (matches CAGradientLayer with startPoint y=0, endPoint y=1).
            s.setBackgroundType(com.codename1.ui.plaf.Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL);
            s.setBackgroundGradientStartColor(0x1e64ff);
            s.setBackgroundGradientEndColor(0x28c850);
            s.setBgTransparency(255);
            return;
        } else if (isHexColor(backdropSpec)) {
            s.setBgColor(parseHexColor(backdropSpec));
            s.setBgTransparency(255);
            return;
        }
        s.setBgColor(bgColor(appearance));
        s.setBgTransparency(255);
    }

    /// True when the string is exactly six hexadecimal digits (an RGB colour).
    private boolean isHexColor(String value) {
        if (value == null || value.length() != 6) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            char ch = value.charAt(i);
            boolean hex = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    /// Parses a 6-hex RGB string into a 0xRRGGBB int. Caller guarantees validity.
    private int parseHexColor(String value) {
        return Integer.parseInt(value, 16) & 0xffffff;
    }

    private Image glassBackdrop;
    private boolean glassBackdropLoaded;

    /// The shared glass backdrop PNG (same asset the native reference uses), loaded
    /// from the app resources once. Null if absent.
    private Image getGlassBackdrop() {
        if (!glassBackdropLoaded) {
            glassBackdropLoaded = true;
            InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/glass-backdrop.png");
            if (in == null) {
                in = getClass().getResourceAsStream("/glass-backdrop.png");
            }
            if (in != null) {
                try {
                    glassBackdrop = Image.createImage(in);
                } catch (Throwable t) {
                    println("CN1SS:WARN:fidelity glass backdrop load failed " + t);
                } finally {
                    Util.cleanup(in);
                }
            }
        }
        return glassBackdrop;
    }

    private Container centerRow(Container tile) {
        Container row = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        row.getAllStyles().setPadding(1, 1, 0, 0);
        row.getAllStyles().setMargin(0, 0, 0, 0);
        row.add(tile);
        return row;
    }

    private int bgColor(String appearance) {
        return "dark".equals(appearance) ? 0x000000 : 0xffffff;
    }

    private void applyAppearance(String appearance) {
        boolean dark = "dark".equals(appearance);
        try {
            Display.getInstance().setDarkMode(Boolean.valueOf(dark));
        } catch (Throwable ignored) {
        }
        try {
            UIManager.getInstance().refreshTheme();
        } catch (Throwable ignored) {
        }
    }

    private int pixels(int mm, boolean horizontal) {
        int px = Display.getInstance().convertToPixels(mm, horizontal);
        return px > 0 ? px : mm;
    }

    private void settle() {
        try {
            Thread.sleep(SETTLE_MS);
        } catch (InterruptedException ignored) {
        }
    }

    private void runOnEdtSync(Runnable r) {
        Display d = Display.getInstance();
        if (d.isEdt()) {
            r.run();
        } else {
            d.callSeriallyAndWait(r);
        }
    }

    private FidelitySpec loadSpec() {
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), SPEC_RESOURCE);
        if (in == null) {
            in = getClass().getResourceAsStream(SPEC_RESOURCE);
        }
        if (in == null) {
            return null;
        }
        try {
            return FidelitySpecParser.parse(in);
        } catch (Throwable t) {
            println("CN1SS:ERR:fidelity spec parse failed " + t);
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    private InputStream openTheme(String name) {
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), name);
        if (in == null) {
            in = getClass().getResourceAsStream(name);
        }
        return in;
    }

    private void installNativeTheme() {
        String resourceName = resolveThemeResource();
        if (resourceName == null) {
            println("CN1SS:WARN:fidelity no native theme resource for platform=" + platform);
            return;
        }
        // Prefer a bundled dev override (e.g. /AndroidMaterialThemeDev.res) when
        // present, so theme-development iterations can ship a freshly-compiled
        // theme inside the app without rebuilding the platform port. Falls back
        // to the port's shipped theme otherwise.
        String devName = resourceName.substring(0, resourceName.length() - 4) + "Dev.res";
        InputStream in = openTheme(devName);
        if (in != null) {
            println("CN1SS:INFO:fidelity using dev theme override " + devName);
            resourceName = devName;
        } else {
            in = openTheme(resourceName);
        }
        if (in == null) {
            println("CN1SS:WARN:fidelity native theme resource missing: " + resourceName);
            return;
        }
        try {
            Resources r = Resources.open(in);
            String[] names = r.getThemeResourceNames();
            if (names == null || names.length == 0) {
                println("CN1SS:ERR:fidelity native theme has no themes: " + resourceName);
                return;
            }
            UIManager.getInstance().setThemeProps(r.getTheme(names[0]));
            println("CN1SS:INFO:fidelity installed theme " + resourceName + " name=" + names[0]);
        } catch (Throwable ex) {
            println("CN1SS:ERR:fidelity native theme load failed: " + ex + " resource=" + resourceName);
        } finally {
            Util.cleanup(in);
        }
    }

    private String resolveThemeResource() {
        String forced = Display.getInstance().getProperty("cn1ss.fidelity.themeResource", null);
        if (forced != null && forced.length() > 0) {
            return forced;
        }
        if ("ios".equals(platform)) {
            return "/iOSModernTheme.res";
        }
        if (platform != null && platform.startsWith("and")) {
            return "/AndroidMaterialTheme.res";
        }
        return Display.getInstance().getProperty("cn1.modernThemeResource", null);
    }

    private String resolvePlatform() {
        String forced = Display.getInstance().getProperty("cn1ss.fidelity.platform", null);
        if (forced != null && forced.length() > 0) {
            return forced;
        }
        return Display.getInstance().getPlatformName();
    }

    private static void println(String line) {
        System.out.println(line);
    }
}
