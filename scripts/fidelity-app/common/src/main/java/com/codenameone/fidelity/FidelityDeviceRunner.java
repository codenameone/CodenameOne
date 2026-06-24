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
                    Component comp = Cn1WidgetRenderer.build(c, state);
                    if (comp == null) {
                        continue;
                    }
                    // Sliders/progress bars are full-width; size them to the same
                    // fraction of the tile the native side uses (2/3) so the two
                    // are comparable rather than the CN1 one collapsing small.
                    if ("Slider".equals(c.getId())) {
                        comp.setPreferredW(w * 3 / 5 - com.codename1.ui.Display.getInstance().convertToPixels(0.4f)); // match the native slider's drawn width (~616px)
                        comp.setPreferredH(com.codename1.ui.Display.getInstance().convertToPixels(7f)); // M3 slider is tall (~122px); thumb spans the height
                    } else if ("ProgressBar".equals(c.getId())) {
                        comp.setPreferredW(w * 2 / 3);
                        comp.setPreferredH(Math.max(6, h / 21)); // native progress bar is a thin line (~11px)
                    } else if ("Tabs".equals(c.getId()) || "Toolbar".equals(c.getId())) {
                        comp.setPreferredW(w);    // tab strip / app bar are full-width, like native
                    }
                    // Land the visible widget where native lands it: Material widgets
                    // centre their visible part vertically inside a 48dp minimum touch
                    // target (an 88px switch sits 22px down in a 132px view, a 110px
                    // button 11px down). Apply that same inset as a top margin so the
                    // absolute-position metric sees them aligned. CheckBox/Radio and
                    // Slider also carry a horizontal box/track inset on the native side.
                    com.codename1.ui.Display disp = com.codename1.ui.Display.getInstance();
                    int band = disp.convertToPixels(7.62f);     // ~48dp = 132px
                    int prefH = comp.getPreferredH();
                    int topInset;
                    if ("TextField".equals(c.getId())) {
                        topInset = disp.convertToPixels(0.87f);  // ~15px outlined-field top inset (taller than 48dp band)
                    } else if ("ProgressBar".equals(c.getId())) {
                        topInset = disp.convertToPixels(0.93f);  // ~16px; the linear indicator has no 48dp touch target
                    } else if ("Tabs".equals(c.getId()) || "Toolbar".equals(c.getId()) || "Dialog".equals(c.getId())) {
                        topInset = 0;             // tab strip / app bar / dialog card anchor at the top-left, like native
                    } else {
                        topInset = Math.max(0, (band - prefH) / 2); // centre the visible widget in the 48dp touch target
                    }
                    int leftInset = 0;
                    if ("Slider".equals(c.getId())) {
                        leftInset = disp.convertToPixels(2.2f);  // ~38px track side padding native shows
                    }
                    com.codename1.ui.plaf.Style st = comp.getAllStyles();
                    st.setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                            com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                            com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS,
                            com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
                    st.setMargin(topInset, 0, leftInset, 0);
                    Container tile = newTile(comp, c.getId(), w, h, appearance);
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
        Image screen = captureScreen();
        cropAndEmit(screen, wrappers, names, w, h);
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

    private Container newTile(Component comp, String compId, int w, int h, String appearance) {
        // Android Material widgets centre their VISIBLE part inside a 48dp minimum
        // touch target (e.g. an 88px switch track sits 22px down inside a 132px
        // view; a 110px button 11px down). The native reference render carries that
        // inset, so to land the CN1 widget at the SAME absolute position we place it
        // left-aligned but vertically centred inside an identical 48dp band at the
        // top of the tile. Component theme margin is neutralized (it is external
        // spacing, absent on the native side) so only the touch-target inset places
        // the widget.
        Container tile = new Container(new FlowLayout(Component.LEFT, Component.TOP));
        Image backdrop = isGlassKind(compId) ? getGlassBackdrop() : null;
        if (backdrop != null) {
            // Liquid Glass needs content behind it. The iOS native reference renders
            // these widgets over the SAME committed backdrop PNG, so CN1 must too --
            // the only difference that should remain is how each renders the glass.
            tile.getAllStyles().setBgImage(backdrop);
            tile.getAllStyles().setBackgroundType(com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_SCALED_FILL);
            tile.getAllStyles().setBgTransparency(255);
        } else {
            tile.getAllStyles().setBgColor(bgColor(appearance));
            tile.getAllStyles().setBgTransparency(255);
        }
        tile.getAllStyles().setPadding(0, 0, 0, 0);
        tile.getAllStyles().setMargin(0, 0, 0, 0);
        tile.setPreferredW(w);
        tile.setPreferredH(h);
        tile.add(comp);
        return tile;
    }

    /// Glass-styled iOS widgets that are rendered over the shared backdrop (the iOS
    /// native reference uses iOS 26 Liquid Glass for these). iOS only -- Android
    /// Material does not use glass, so its tiles stay on the plain background.
    private boolean isGlassKind(String compId) {
        if (!"ios".equals(platform) || compId == null) {
            return false;
        }
        return "Button".equals(compId) || "RaisedButton".equals(compId) || "FlatButton".equals(compId)
                || "Toolbar".equals(compId) || "Tabs".equals(compId);
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
