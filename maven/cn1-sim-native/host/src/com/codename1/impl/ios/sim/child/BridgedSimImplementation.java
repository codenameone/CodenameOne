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
package com.codename1.impl.ios.sim.child;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.ios.sim.bridge.BridgeRegistry;
import com.codename1.impl.ios.sim.bridge.InputSink;
import com.codename1.impl.ios.sim.bridge.RenderBridge;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Codename One implementation of the ISOLATED app universe: this class
 * (and the entire com.codename1 core it extends) is loaded by the child-first
 * classloader, giving the user's app its own Display/EDT, while every
 * rendering primitive crosses the shared {@link RenderBridge} into the parent
 * universe's native pipeline. Storage and networking are pure java.io /
 * java.net and need no bridge().
 *
 * <p>Graphics state (clip, color, alpha, font) is tracked per graphics
 * context Java-side - the same model the device implementation uses - and
 * each emitted op carries its color/alpha, matching the native ExecutableOp
 * pipeline.</p>
 */
public class BridgedSimImplementation extends CodenameOneImplementation {
    private final RenderBridge fixedBridge;
    private final boolean appUniverse;

    /**
     * App universes resolve their bridge dynamically from the registry on
     * every call: the shell swaps in a new bridge (with a different region)
     * when the device rotates or the skin changes. The shell universe uses a
     * fixed full-window bridge().
     */
    private RenderBridge bridge() {
        if (appUniverse) {
            return BridgeRegistry.getBridge();
        }
        RenderBridge shell = BridgeRegistry.getShellBridge();
        return shell != null ? shell : fixedBridge;
    }

    /**
     * Child-universe path: the bridge comes from the shared registry, where
     * the parent installed it before booting this universe. Registers the
     * input sink so window events inside the screen rect reach this universe.
     */
    public BridgedSimImplementation() {
        this.fixedBridge = null;
        this.appUniverse = true;
    }

    /**
     * Shell-universe path: the parent constructs this directly with the
     * full-window bridge(). Input arrives through the normal parent dispatch,
     * so no sink is registered.
     *
     * @param bridge the render bridge for this universe's region
     */
    public BridgedSimImplementation(RenderBridge bridge) {
        this.fixedBridge = bridge;
        this.appUniverse = false;
    }

    /** graphics context state */
    static class G {
        int color;
        int alpha = 255;
        int clipX, clipY, clipW, clipH;
        BridgedFont font;
        boolean clipDirty = true;
        /** non-null when this context draws into a mutable image */
        BridgedImage target;

        G(int w, int h) {
            clipW = w;
            clipH = h;
        }
    }

    /**
     * Faux-pointer cleanup: a JVM-side native-object wrapper (image/font) maps
     * 1:1 to a remote peer on the relay. When the wrapper is GC'd, the remote
     * peer must be released too - same lifecycle as a native peer freed when
     * its Java object dies. Registered with a Cleaner; the cleanup action
     * captures only (bridge, peer) - never the wrapper - so it cannot pin it.
     */
    private static final java.lang.ref.Cleaner PEER_CLEANER = java.lang.ref.Cleaner.create();

    /** Cleanup action: holds (bridge, peer), not the wrapper. */
    private static final class PeerRelease implements Runnable {
        private final RenderBridge bridge;
        private final long peer;
        PeerRelease(RenderBridge bridge, long peer) {
            this.bridge = bridge;
            this.peer = peer;
        }
        public void run() {
            try {
                bridge.releasePeer(peer);
            } catch (Throwable t) {
                // connection may be down at shutdown; releasing is best-effort
            }
        }
    }

    /** font peer wrapper */
    static class BridgedFont {
        final long peer;
        final int face, style, size;
        java.lang.ref.Cleaner.Cleanable cleanable;

        BridgedFont(long peer, int face, int style, int size) {
            this.peer = peer;
            this.face = face;
            this.style = style;
            this.size = size;
        }
    }

    /** image peer wrapper */
    static class BridgedImage {
        final long peer;
        final int width, height;
        /** mutable image with unpublished edits (finishMutable pending) */
        volatile boolean dirty;
        java.lang.ref.Cleaner.Cleanable cleanable;

        BridgedImage(long peer, int width, int height) {
            this.peer = peer;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Register a freshly created image wrapper for GC-driven remote release,
     * capturing the current universe's bridge. Returns the same wrapper for
     * call-site chaining. Reused wrappers (e.g. scale() returning the same
     * BridgedImage) must NOT be re-tracked.
     */
    private BridgedImage track(BridgedImage bi) {
        if (bi != null && bi.peer != 0 && bi.cleanable == null) {
            bi.cleanable = PEER_CLEANER.register(bi, new PeerRelease(bridge(), bi.peer));
        }
        return bi;
    }

    private BridgedFont track(BridgedFont bf) {
        if (bf != null && bf.peer != 0 && bf.cleanable == null) {
            bf.cleanable = PEER_CLEANER.register(bf, new PeerRelease(bridge(), bf.peer));
        }
        return bf;
    }

    /**
     * The native peer behind a CN1 image's native-image object - the shell
     * uses it to hand the skin's bezel crop to the screen-overlay native.
     *
     * @param nativeImage the value of {@code Image.getImage()}
     * @return the peer handle or 0
     */
    public static long imagePeer(Object nativeImage) {
        return nativeImage instanceof BridgedImage ? ((BridgedImage) nativeImage).peer : 0;
    }

    private G globalGraphics;
    private BridgedFont defaultFont;
    private File storageRoot;
    private File fsRoot;

    @Override
    public void init(Object m) {
        String home = System.getProperty("cn1.sim.home",
                System.getProperty("user.home") + File.separator + ".cn1sim");
        fsRoot = new File(home, "isolated-app");
        storageRoot = new File(fsRoot, "storage");
        storageRoot.mkdirs();

        if (!appUniverse) {
            return;
        }
        BridgeRegistry.setInputSink(new InputSink() {
            public void pointerEvent(int type, int x, int y) {
                Display d = Display.getInstance();
                if (type == POINTER_PRESSED && System.getenv("CN1_SIM_DEBUG") != null) {
                    com.codename1.ui.Form f = d.getCurrent();
                    com.codename1.ui.Component c = f == null ? null : f.getComponentAt(x, y);
                    System.out.println("cn1sim: child componentAt " + x + "," + y + " = "
                            + (c == null ? "null" : c.getClass().getSimpleName()
                            + " abs=" + c.getAbsoluteX() + "," + c.getAbsoluteY()
                            + " " + c.getWidth() + "x" + c.getHeight())
                            + " display=" + d.getDisplayWidth() + "x" + d.getDisplayHeight());
                }
                switch (type) {
                    case POINTER_PRESSED:
                        d.pointerPressed(new int[]{x}, new int[]{y});
                        break;
                    case POINTER_RELEASED:
                        d.pointerReleased(new int[]{x}, new int[]{y});
                        break;
                    case POINTER_DRAGGED:
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

            public void repaintRequest() {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        com.codename1.ui.Form current = Display.getInstance().getCurrent();
                        if (current != null) {
                            current.repaint();
                        }
                    }
                });
            }

            public void sizeChanged(int w, int h) {
                BridgedSimImplementation.this.sizeChanged(w, h);
            }
        });
        BridgeRegistry.setChildControl(new com.codename1.impl.ios.sim.bridge.ChildControl() {
            public void control(String command) {
                handleControlCommand(command);
            }
        });
    }

    /**
     * Executes shell tool-menu commands inside this (app) universe.
     */
    private void handleControlCommand(String command) {
        if ("cleanStorage".equals(command)) {
            String[] entries = storageRoot.list();
            if (entries != null) {
                for (String e : entries) {
                    new File(storageRoot, e).delete();
                }
            }
            System.out.println("[cn1sim] storage cleaned: " + storageRoot);
        } else if (command.startsWith("nativeTheme:")) {
            BridgeRegistry.setNativeThemePref(command.substring("nativeTheme:".length()));
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    installNativeTheme();
                    com.codename1.ui.Form f = Display.getInstance().getCurrent();
                    if (f != null) {
                        f.refreshTheme();
                        f.forceRevalidate();
                        f.repaint();
                    }
                }
            });
        } else if ("perfMonitor:on".equals(command)) {
            startPerfMonitor();
        } else if ("perfMonitor:off".equals(command)) {
            perfRunning = false;
        } else if (command.startsWith("dark:")) {
            BridgeRegistry.setDarkMode(command.substring("dark:".length()));
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    com.codename1.ui.Form f = Display.getInstance().getCurrent();
                    if (f != null) {
                        f.refreshTheme();
                        f.forceRevalidate();
                        f.repaint();
                    }
                }
            });
        } else if (command.startsWith("largerText:")) {
            float scale = 1.0f;
            try {
                scale = Float.parseFloat(command.substring("largerText:".length()));
            } catch (NumberFormatException ex) {
                scale = 1.0f;
            }
            BridgeRegistry.setLargerTextScale(scale);
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    // refreshTheme re-derives fonts, which re-read getLargerTextScale()
                    com.codename1.ui.Form f = Display.getInstance().getCurrent();
                    if (f != null) {
                        f.refreshTheme();
                        f.forceRevalidate();
                        f.repaint();
                    }
                }
            });
        } else if (command.startsWith("shareText:")) {
            final String text = command.substring("shareText:".length());
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    fireSharedContentReceived(
                            com.codename1.share.SharedContent.builder().addText(text).build());
                }
            });
        } else if (command.startsWith("shareUrl:")) {
            final String url = command.substring("shareUrl:".length());
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    fireSharedContentReceived(
                            com.codename1.share.SharedContent.builder().addUrl(url).build());
                }
            });
        } else if (command.startsWith("shareFile:")) {
            final String path = command.substring("shareFile:".length());
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    String lower = path.toLowerCase();
                    boolean image = lower.endsWith(".png") || lower.endsWith(".jpg")
                            || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                            || lower.endsWith(".webp");
                    String name = path.replace('\\', '/');
                    int slash = name.lastIndexOf('/');
                    if (slash >= 0) {
                        name = name.substring(slash + 1);
                    }
                    com.codename1.share.SharedContent.Builder b =
                            com.codename1.share.SharedContent.builder();
                    if (image) {
                        b.addImage(null, "file://" + path, name);
                    } else {
                        b.addFile(null, "file://" + path, name);
                    }
                    fireSharedContentReceived(b.build());
                }
            });
        } else if ("inspect".equals(command)) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    com.codename1.ui.Form f = Display.getInstance().getCurrent();
                    StringBuilder sb = new StringBuilder();
                    inspectIdMap.clear();
                    nextInspectId = 0;
                    if (f != null) {
                        appendComponentTree(sb, f, 0);
                    }
                    com.codename1.impl.ios.sim.bridge.ToolsBridge tb =
                            BridgeRegistry.getToolsBridge();
                    if (tb != null) {
                        tb.inspectorTree(sb.toString());
                    }
                }
            });
        } else if (command.startsWith("inspectSelect:")) {
            final String idStr = command.substring("inspectSelect:".length());
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    highlightInspected(idStr);
                }
            });
        } else if (command.startsWith("inspectSet:")) {
            // inspectSet:<property>=<value> - live-edit the selected component
            final String body = command.substring("inspectSet:".length());
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    applyInspectorEdit(body);
                }
            });
        }
    }

    private final java.util.Map<Integer, com.codename1.ui.Component> inspectIdMap =
            new java.util.HashMap<Integer, com.codename1.ui.Component>();
    private int nextInspectId;
    private com.codename1.ui.Component inspectSelected;

    /**
     * Applies a live property edit from the inspector detail pane to the
     * currently selected component, then re-pushes its description so the
     * detail pane reflects the change. {@code body} is "property=value".
     */
    private void applyInspectorEdit(String body) {
        com.codename1.ui.Component c = inspectSelected;
        if (c == null) {
            return;
        }
        int eq = body.indexOf('=');
        if (eq < 0) {
            return;
        }
        String prop = body.substring(0, eq);
        String value = body.substring(eq + 1);
        if ("uiid".equals(prop)) {
            c.setUIID(value.length() == 0 ? "Container" : value);
        } else if ("text".equals(prop)) {
            if (c instanceof com.codename1.ui.Label) {
                ((com.codename1.ui.Label) c).setText(value);
            } else if (c instanceof com.codename1.ui.TextArea) {
                ((com.codename1.ui.TextArea) c).setText(value);
            }
        } else {
            return;
        }
        com.codename1.ui.Form f = c.getComponentForm();
        if (f != null) {
            f.revalidate();
            f.repaint();
        }
        com.codename1.impl.ios.sim.bridge.ToolsBridge tb = BridgeRegistry.getToolsBridge();
        if (tb != null) {
            tb.inspectorDetail(describeComponent(c));
        }
    }

    /**
     * Serializes the component tree for the inspector, one node per line as
     * "id\tdepth\tlabel", and records id -> live Component so the shell can
     * select a node to highlight it.
     */
    private void appendComponentTree(StringBuilder sb, com.codename1.ui.Component c, int depth) {
        int id = nextInspectId++;
        inspectIdMap.put(Integer.valueOf(id), c);
        sb.append(id).append('\t').append(depth).append('\t');
        sb.append(c.getClass().getSimpleName());
        String uiid = c.getUIID();
        if (uiid != null && uiid.length() > 0) {
            sb.append(" #").append(uiid);
        }
        sb.append(" [").append(c.getX()).append(',').append(c.getY())
                .append(' ').append(c.getWidth()).append('x').append(c.getHeight())
                .append(']');
        String txt = null;
        if (c instanceof com.codename1.ui.Label) {
            txt = ((com.codename1.ui.Label) c).getText();
        } else if (c instanceof com.codename1.ui.TextArea) {
            txt = ((com.codename1.ui.TextArea) c).getText();
        }
        if (txt != null && txt.length() > 0) {
            if (txt.length() > 24) {
                txt = txt.substring(0, 24) + "...";
            }
            sb.append(" \"").append(txt).append('"');
        }
        sb.append('\n');
        if (c instanceof com.codename1.ui.Container) {
            com.codename1.ui.Container ct = (com.codename1.ui.Container) c;
            for (int i = 0; i < ct.getComponentCount(); i++) {
                appendComponentTree(sb, ct.getComponentAt(i), depth + 1);
            }
        }
    }

    /**
     * Highlights the selected component on the live form (a translucent overlay
     * on the glass pane) and reports its properties back to the inspector. id
     * "none" clears the highlight.
     */
    private void highlightInspected(String idStr) {
        com.codename1.ui.Form f = Display.getInstance().getCurrent();
        if (f == null) {
            return;
        }
        if ("none".equals(idStr)) {
            f.setGlassPane(null);
            f.repaint();
            return;
        }
        com.codename1.ui.Component target;
        try {
            target = inspectIdMap.get(Integer.valueOf(Integer.parseInt(idStr)));
        } catch (NumberFormatException ex) {
            target = null;
        }
        if (target == null) {
            inspectSelected = null;
            f.setGlassPane(null);
            f.repaint();
            return;
        }
        inspectSelected = target;
        final com.codename1.ui.Component sel = target;
        f.setGlassPane(new com.codename1.ui.Painter() {
            public void paint(com.codename1.ui.Graphics g, com.codename1.ui.geom.Rectangle rect) {
                int x = sel.getAbsoluteX();
                int y = sel.getAbsoluteY();
                int w = sel.getWidth();
                int h = sel.getHeight();
                g.setColor(0x3a7bd5);
                g.setAlpha(70);
                g.fillRect(x, y, w, h);
                g.setAlpha(255);
                g.drawRect(x, y, Math.max(0, w - 1), Math.max(0, h - 1));
            }
        });
        f.repaint();
        com.codename1.impl.ios.sim.bridge.ToolsBridge tb = BridgeRegistry.getToolsBridge();
        if (tb != null) {
            tb.inspectorDetail(describeComponent(sel));
        }
    }

    /** A multi-line property readout for the inspector detail pane. */
    private String describeComponent(com.codename1.ui.Component c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.getClass().getName()).append('\n');
        String uiid = c.getUIID();
        sb.append("UIID: ").append(uiid == null ? "" : uiid).append('\n');
        if (c instanceof com.codename1.ui.Label) {
            String t = ((com.codename1.ui.Label) c).getText();
            sb.append("Text: ").append(t == null ? "" : t).append('\n');
        } else if (c instanceof com.codename1.ui.TextArea) {
            String t = ((com.codename1.ui.TextArea) c).getText();
            sb.append("Text: ").append(t == null ? "" : t).append('\n');
        }
        sb.append("Bounds: ").append(c.getX()).append(',').append(c.getY())
                .append(' ').append(c.getWidth()).append('x').append(c.getHeight()).append('\n');
        sb.append("Abs: ").append(c.getAbsoluteX()).append(',').append(c.getAbsoluteY()).append('\n');
        sb.append("Preferred: ").append(c.getPreferredW()).append('x')
                .append(c.getPreferredH()).append('\n');
        com.codename1.ui.plaf.Style s = c.getStyle();
        sb.append("Fg/Bg: ").append(Integer.toHexString(s.getFgColor()))
                .append(" / ").append(Integer.toHexString(s.getBgColor())).append('\n');
        sb.append("Padding: ").append(s.getPaddingTop()).append(',').append(s.getPaddingBottom())
                .append(',').append(s.getPaddingLeft(false)).append(',')
                .append(s.getPaddingRight(false)).append('\n');
        sb.append("Margin: ").append(s.getMarginTop()).append(',').append(s.getMarginBottom())
                .append(',').append(s.getMarginLeft(false)).append(',')
                .append(s.getMarginRight(false));
        return sb.toString();
    }

    /* ---- native theme + safe area (device parity from the skin) ------------- */

    @Override
    public boolean hasNativeTheme() {
        return true;
    }

    @Override
    public Boolean isDarkMode() {
        // honor the Simulate > Dark/Light Mode override; "auto" defers to default
        String m = BridgeRegistry.getDarkMode();
        if ("dark".equals(m)) {
            return Boolean.TRUE;
        }
        if ("light".equals(m)) {
            return Boolean.FALSE;
        }
        return super.isDarkMode();
    }

    @Override
    public float getLargerTextScale() {
        // Simulate > Larger Text: core multiplies font sizes by this globally
        return BridgeRegistry.getLargerTextScale();
    }

    private com.codename1.security.Biometrics simBiometrics;

    @Override
    public com.codename1.security.Biometrics getBiometrics() {
        // Simulate > Biometric Simulation - parity with JavaSEBiometrics
        if (simBiometrics == null) {
            simBiometrics = new SimBiometrics();
        }
        return simBiometrics;
    }

    private com.codename1.payment.Purchase simPurchase;

    @Override
    public com.codename1.payment.Purchase getInAppPurchase() {
        // Simulate > In App Purchase - parity with the JavaSEPort IAP stub
        if (simPurchase == null) {
            simPurchase = new SimPurchase();
        }
        return simPurchase;
    }

    @Override
    public void installNativeTheme() {
        try {
            byte[] data = resolveNativeThemeBytes();
            if (data == null) {
                return;
            }
            com.codename1.ui.util.Resources r = com.codename1.ui.util.Resources.open(
                    new java.io.ByteArrayInputStream(data));
            com.codename1.ui.plaf.UIManager.getInstance().setThemeProps(
                    r.getTheme(r.getThemeResourceNames()[0]));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Resolves the theme the same way the classic simulator does: the Native
     * Theme menu selection wins; "auto" maps the skin's platform to the
     * framework's default native theme; "embedded" (and any unresolvable
     * name) falls back to the theme bundled inside the .skin file.
     */
    private byte[] resolveNativeThemeBytes() {
        String key = BridgeRegistry.getNativeThemePref();
        if (key == null || key.length() == 0 || "auto".equals(key)) {
            String platform = BridgeRegistry.getSkinPlatformName();
            if ("ios".equals(platform)) {
                key = "iOSModernTheme";
            } else if ("win".equals(platform)) {
                key = "winTheme";
            } else {
                key = "AndroidMaterialTheme";
            }
        }
        if (!"embedded".equals(key)) {
            java.io.InputStream in = BridgedSimImplementation.class
                    .getResourceAsStream("/" + key + ".res");
            if (in != null) {
                try {
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        bo.write(buf, 0, n);
                    }
                    return bo.toByteArray();
                } catch (java.io.IOException ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (java.io.IOException ignored) {
                    }
                }
            } else {
                System.err.println("[cn1sim] native theme " + key
                        + ".res not on classpath; using the skin's embedded theme");
            }
        }
        return BridgeRegistry.getEmbeddedThemeRes();
    }

    @Override
    public com.codename1.ui.geom.Rectangle getDisplaySafeArea(com.codename1.ui.geom.Rectangle rect) {
        if (rect == null) {
            rect = new com.codename1.ui.geom.Rectangle();
        }
        int w = getDisplayWidth();
        int h = getDisplayHeight();
        boolean portrait = w <= h;
        int[] safe = portrait ? BridgeRegistry.getSafeAreaPortrait()
                : BridgeRegistry.getSafeAreaLandscape();
        // safe areas come in unscaled screen coordinates; this universe runs
        // at the scaled size, so scale them by the current display size
        int unscaledW = portrait ? BridgeRegistry.getUnscaledScreenWidth()
                : BridgeRegistry.getUnscaledScreenHeight();
        int unscaledH = portrait ? BridgeRegistry.getUnscaledScreenHeight()
                : BridgeRegistry.getUnscaledScreenWidth();
        if (safe == null || unscaledW <= 0 || unscaledH <= 0) {
            rect.setBounds(0, 0, w, h);
            return rect;
        }
        double sx = w / (double) unscaledW;
        double sy = h / (double) unscaledH;
        rect.setBounds((int) Math.round(safe[0] * sx), (int) Math.round(safe[1] * sy),
                (int) Math.round(safe[2] * sx), (int) Math.round(safe[3] * sy));
        return rect;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        if ("AppArg".equals(key)) {
            String arg = BridgeRegistry.getAppArg();
            if (arg != null) {
                return arg;
            }
        }
        return super.getProperty(key, defaultValue);
    }

    @Override
    public int getDisplayWidth() {
        return bridge().getAppWidth();
    }

    @Override
    public int getDisplayHeight() {
        return bridge().getAppHeight();
    }

    @Override
    public boolean isTouchDevice() {
        return true;
    }

    @Override
    public int getCommandBehavior() {
        // the shell's commands live in the native menu bar, never in-form;
        // the app universe keeps device-style command rendering
        if (!appUniverse) {
            return Display.COMMAND_BEHAVIOR_NATIVE;
        }
        int b = super.getCommandBehavior();
        if (b == Display.COMMAND_BEHAVIOR_NATIVE) {
            // native themes may request the desktop menu bar, but the shell
            // owns it in the simulator - render in-form like a phone build
            return Display.COMMAND_BEHAVIOR_ICS;
        }
        return b;
    }

    /**
     * Public trigger for the launcher: the window was resized (rotation),
     * relayout this universe.
     */
    public void notifySizeChanged(int w, int h) {
        sizeChanged(w, h);
    }

    @Override
    public String getPlatformName() {
        return "ios";
    }

    /* ---- graphics ---------------------------------------------------------- */

    @Override
    public Object getNativeGraphics() {
        if (globalGraphics == null) {
            globalGraphics = new G(getDisplayWidth(), getDisplayHeight());
        }
        return globalGraphics;
    }

    @Override
    public Object getNativeGraphics(Object image) {
        BridgedImage bi = (BridgedImage) image;
        G g = new G(bi.width, bi.height);
        g.target = bi;
        return g;
    }

    private void applyClip(G g) {
        if (g.target != null) {
            if (g.clipDirty) {
                bridge().mutableClip(g.target.peer, g.clipX, g.clipY, g.clipW, g.clipH);
                g.clipDirty = false;
            }
            return;
        }
        if (g == globalGraphics && g.clipDirty) {
            bridge().setClip(g.clipX, g.clipY, g.clipW, g.clipH);
            g.clipDirty = false;
        }
    }

    private boolean drops(Object graphics) {
        G g = (G) graphics;
        return g != globalGraphics && g.target == null;
    }

    /**
     * Publishes pending mutable edits so the peer can be consumed as a
     * regular image (drawn, scaled, read back).
     */
    private long drawablePeer(BridgedImage bi) {
        if (bi.dirty) {
            bridge().finishMutable(bi.peer);
            bi.dirty = false;
        }
        return bi.peer;
    }

    @Override
    public void setColor(Object graphics, int RGB) {
        ((G) graphics).color = RGB & 0xffffff;
    }

    @Override
    public int getColor(Object graphics) {
        return ((G) graphics).color;
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        ((G) graphics).alpha = alpha;
    }

    @Override
    public int getAlpha(Object graphics) {
        return ((G) graphics).alpha;
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        G g = (G) graphics;
        g.clipX = x;
        g.clipY = y;
        g.clipW = width;
        g.clipH = height;
        g.clipDirty = true;
    }

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        G g = (G) graphics;
        int x1 = Math.max(g.clipX, x);
        int y1 = Math.max(g.clipY, y);
        int x2 = Math.min(g.clipX + g.clipW, x + width);
        int y2 = Math.min(g.clipY + g.clipH, y + height);
        g.clipX = x1;
        g.clipY = y1;
        g.clipW = Math.max(0, x2 - x1);
        g.clipH = Math.max(0, y2 - y1);
        g.clipDirty = true;
    }

    @Override
    public int getClipX(Object graphics) {
        return ((G) graphics).clipX;
    }

    @Override
    public int getClipY(Object graphics) {
        return ((G) graphics).clipY;
    }

    @Override
    public int getClipWidth(Object graphics) {
        return ((G) graphics).clipW;
    }

    @Override
    public int getClipHeight(Object graphics) {
        return ((G) graphics).clipH;
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int w, int h) {
        if (drops(graphics)) {
            return;
        }
        G g = (G) graphics;
        applyClip(g);
        if (g.target != null) {
            g.target.dirty = true;
            bridge().mutableFillRect(g.target.peer, g.color, g.alpha, x, y, w, h);
            return;
        }
        bridge().fillRect(g.color, g.alpha, x, y, w, h);
    }

    /* ---- native peers: browser, media, camera/gallery ----------------------- */

    /**
     * A Codename One component backed by a real native view (WKWebView,
     * AVPlayerView) floated over the app's window region; the frame follows
     * the component through layout changes.
     */
    class SimPeer extends com.codename1.ui.PeerComponent {
        final long peer;

        SimPeer(long peer) {
            super(Long.valueOf(peer));
            this.peer = peer;
        }

        private void updateFrame() {
            bridge().peerSetFrame(peer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
        }

        protected void onPositionSizeChange() {
            super.onPositionSizeChange();
            updateFrame();
        }

        protected void initComponent() {
            super.initComponent();
            updateFrame();
        }

        protected void deinitialize() {
            bridge().peerRemove(peer);
            super.deinitialize();
        }

        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(320, 240);
        }
    }

    /**
     * Camera backend over the bridge: the relay opens a REAL AVCaptureSession
     * and floats its preview where our SimPeer reports its frame.
     */
    @Override
    public com.codename1.impl.CameraImpl createCameraImpl() {
        return new com.codename1.impl.CameraImpl() {
            private long peer;

            public com.codename1.camera.CameraInfo[] enumerateCameras() {
                return new com.codename1.camera.CameraInfo[]{
                    new com.codename1.camera.CameraInfo("relay-default",
                            com.codename1.camera.CameraFacing.FRONT, null, null, false, false)
                };
            }

            public void open(String cameraId, com.codename1.camera.CameraSessionOptions opts)
                    throws java.io.IOException {
                System.out.println("[cn1sim-rpc] cameraImpl.open " + cameraId);
                peer = bridge().peerCreateCamera();
                System.out.println("[cn1sim-rpc] cameraImpl.open -> peer " + peer);
                if (peer == 0) {
                    throw new java.io.IOException("relay reported no camera");
                }
            }

            public com.codename1.ui.PeerComponent createPreviewPeer() {
                return new SimPeer(peer);
            }

            public void takePhoto(com.codename1.camera.PhotoCaptureOptions opts,
                    com.codename1.util.AsyncResource<com.codename1.camera.CapturedPhoto> result) {
                result.error(new RuntimeException("photo capture not bridged yet"));
            }

            public void startVideoRecording(String filePath, boolean audio)
                    throws java.io.IOException {
                throw new java.io.IOException("video recording not bridged yet");
            }

            public void stopVideoRecording(
                    com.codename1.util.AsyncResource<String> result) {
                result.error(new RuntimeException("video recording not bridged yet"));
            }

            public void setFrameListener(com.codename1.camera.FrameListener listener,
                    com.codename1.camera.FrameFormat format, int maxFps) {
            }

            public void setFlashMode(com.codename1.camera.FlashMode mode) {
            }

            public void setZoom(float ratio) {
            }

            public void focus(float xNorm, float yNorm) {
            }

            public void pause() {
            }

            public void resume() {
            }

            public void close() {
                if (peer != 0) {
                    bridge().peerRelease(peer);
                    peer = 0;
                }
            }
        };
    }

    @Override
    public boolean isNativeBrowserComponentSupported() {
        return true;
    }

    @Override
    public com.codename1.ui.PeerComponent createBrowserComponent(Object browserComponent) {
        return new SimPeer(bridge().peerCreateWebView());
    }

    @Override
    public void setBrowserURL(com.codename1.ui.PeerComponent browserPeer, String url) {
        bridge().peerWebLoadURL(((SimPeer) browserPeer).peer, url);
    }

    @Override
    public void setBrowserPage(com.codename1.ui.PeerComponent browserPeer, String html,
            String baseUrl) {
        bridge().peerWebLoadHTML(((SimPeer) browserPeer).peer, html, baseUrl);
    }

    /** AVPlayer-backed media; video exposes an AVPlayerView peer component */
    class BridgedMedia implements com.codename1.media.Media {
        final long peer;
        final boolean video;
        private SimPeer videoComponent;
        private final java.util.Map<String, Object> vars =
                new java.util.HashMap<String, Object>();

        BridgedMedia(long peer, boolean video) {
            this.peer = peer;
            this.video = video;
        }

        public void play() {
            bridge().mediaControl(peer, 0, 0);
        }

        public void pause() {
            bridge().mediaControl(peer, 1, 0);
        }

        public void prepare() {
        }

        public void cleanup() {
            pause();
            bridge().peerRelease(peer);
        }

        public int getTime() {
            return bridge().mediaQuery(peer, 0);
        }

        public void setTime(int time) {
            bridge().mediaControl(peer, 2, time);
        }

        public int getDuration() {
            return bridge().mediaQuery(peer, 1);
        }

        public int getVolume() {
            return 100;
        }

        public void setVolume(int vol) {
        }

        public boolean isPlaying() {
            return bridge().mediaQuery(peer, 2) != 0;
        }

        public Component getVideoComponent() {
            if (!video) {
                return null;
            }
            if (videoComponent == null) {
                videoComponent = new SimPeer(peer);
            }
            return videoComponent;
        }

        public boolean isVideo() {
            return video;
        }

        public boolean isFullScreen() {
            return false;
        }

        public void setFullScreen(boolean fullScreen) {
        }

        public boolean isNativePlayerMode() {
            return false;
        }

        public void setNativePlayerMode(boolean nativePlayer) {
        }

        public void setVariable(String key, Object value) {
            vars.put(key, value);
        }

        public Object getVariable(String key) {
            return vars.get(key);
        }
    }

    @Override
    public com.codename1.media.Media createMedia(String uri, boolean isVideo,
            Runnable onCompletion) throws IOException {
        long peer = bridge().mediaCreate(uri, isVideo);
        if (peer == 0) {
            throw new IOException("Unable to create media for " + uri);
        }
        return new BridgedMedia(peer, isVideo);
    }

    /**
     * The simulator's camera and gallery: a native file panel, mirroring the
     * classic simulator's behavior.
     */
    @Override
    public void capturePhoto(final com.codename1.ui.events.ActionListener response) {
        // Use the relay's NATIVE camera (not a file picker). The relay opens the
        // platform camera, saves the still to a shared path, and replies through
        // the PickCallback. On cancel or a denied camera permission the path is
        // null and we deliver null so the app's callback runs normally instead
        // of hanging -- the deny-safe fallback.
        BridgeRegistry.setPickCallback(new com.codename1.impl.ios.sim.bridge.PickCallback() {
            public void picked(final String path) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (path == null) {
                            response.actionPerformed(null);
                        } else {
                            response.actionPerformed(
                                    new com.codename1.ui.events.ActionEvent("file://" + path));
                        }
                    }
                });
            }
        });
        bridge().capturePhoto();
    }

    @Override
    public void openGallery(final com.codename1.ui.events.ActionListener response, int type) {
        pickWithPanel(response);
    }

    private void pickWithPanel(final com.codename1.ui.events.ActionListener response) {
        BridgeRegistry.setPickCallback(new com.codename1.impl.ios.sim.bridge.PickCallback() {
            public void picked(final String path) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (path == null) {
                            response.actionPerformed(null);
                        } else {
                            response.actionPerformed(
                                    new com.codename1.ui.events.ActionEvent("file://" + path));
                        }
                    }
                });
            }
        });
        bridge().pickFile();
    }

    /* ---- transform objects (geometry math only) ------------------------------
     * The core Transform class and GeneralPath.setShape need a working
     * transform OBJECT (identity checks, point mapping); rendering with a
     * graphics-level transform remains unsupported for now. */

    static class SimTransform {
        // affine: [m00 m01 tx; m10 m11 ty]
        double m00 = 1, m01, tx, m10, m11 = 1, ty;

        boolean isIdentity() {
            return m00 == 1 && m01 == 0 && tx == 0 && m10 == 0 && m11 == 1 && ty == 0;
        }

        void set(double a, double b, double c, double d, double e, double f) {
            m00 = a;
            m01 = b;
            tx = c;
            m10 = d;
            m11 = e;
            ty = f;
        }

        void concat(SimTransform o) {
            double a = m00 * o.m00 + m01 * o.m10;
            double b = m00 * o.m01 + m01 * o.m11;
            double c = m00 * o.tx + m01 * o.ty + tx;
            double d = m10 * o.m00 + m11 * o.m10;
            double e = m10 * o.m01 + m11 * o.m11;
            double f = m10 * o.tx + m11 * o.ty + ty;
            set(a, b, c, d, e, f);
        }
    }

    @Override
    public boolean isTransformSupported() {
        return true;
    }

    @Override
    public boolean isPerspectiveTransformSupported() {
        return false;
    }

    @Override
    public Object makeTransformIdentity() {
        return new SimTransform();
    }

    @Override
    public void setTransformIdentity(Object t) {
        ((SimTransform) t).set(1, 0, 0, 0, 1, 0);
    }

    @Override
    public Object makeTransformTranslation(float x, float y, float z) {
        SimTransform t = new SimTransform();
        t.set(1, 0, x, 0, 1, y);
        return t;
    }

    @Override
    public void setTransformTranslation(Object t, float x, float y, float z) {
        ((SimTransform) t).set(1, 0, x, 0, 1, y);
    }

    @Override
    public Object makeTransformScale(float sx, float sy, float sz) {
        SimTransform t = new SimTransform();
        t.set(sx, 0, 0, 0, sy, 0);
        return t;
    }

    @Override
    public void setTransformScale(Object t, float sx, float sy, float sz) {
        ((SimTransform) t).set(sx, 0, 0, 0, sy, 0);
    }

    @Override
    public Object makeTransformRotation(float angle, float x, float y, float z) {
        SimTransform t = new SimTransform();
        setTransformRotation(t, angle, x, y, z);
        return t;
    }

    @Override
    public void setTransformRotation(Object t, float angle, float x, float y, float z) {
        // 2D rotation about the z axis only
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        ((SimTransform) t).set(c, -s, 0, s, c, 0);
    }

    @Override
    public Object makeTransformAffine(double m00, double m10, double m01, double m11,
            double m02, double m12) {
        SimTransform t = new SimTransform();
        t.set(m00, m01, m02, m10, m11, m12);
        return t;
    }

    @Override
    public void setTransformAffine(Object t, double m00, double m10, double m01,
            double m11, double m02, double m12) {
        ((SimTransform) t).set(m00, m01, m02, m10, m11, m12);
    }

    @Override
    public void transformTranslate(Object t, float x, float y, float z) {
        SimTransform o = new SimTransform();
        o.set(1, 0, x, 0, 1, y);
        ((SimTransform) t).concat(o);
    }

    @Override
    public void transformScale(Object t, float x, float y, float z) {
        SimTransform o = new SimTransform();
        o.set(x, 0, 0, 0, y, 0);
        ((SimTransform) t).concat(o);
    }

    @Override
    public void transformRotate(Object t, float angle, float x, float y, float z) {
        SimTransform o = (SimTransform) makeTransformRotation(angle, x, y, z);
        ((SimTransform) t).concat(o);
    }

    @Override
    public Object makeTransformInverse(Object t) {
        SimTransform r = new SimTransform();
        copyTransform(t, r);
        try {
            setTransformInverse(r);
        } catch (com.codename1.ui.Transform.NotInvertibleException ex) {
            return null;
        }
        return r;
    }

    @Override
    public void setTransformInverse(Object t)
            throws com.codename1.ui.Transform.NotInvertibleException {
        SimTransform s = (SimTransform) t;
        double det = s.m00 * s.m11 - s.m01 * s.m10;
        if (det == 0) {
            throw new com.codename1.ui.Transform.NotInvertibleException();
        }
        double a = s.m11 / det;
        double b = -s.m01 / det;
        double d = -s.m10 / det;
        double e = s.m00 / det;
        double c = -(a * s.tx + b * s.ty);
        double f = -(d * s.tx + e * s.ty);
        s.set(a, b, c, d, e, f);
    }

    @Override
    public void copyTransform(Object src, Object dest) {
        SimTransform s = (SimTransform) src;
        ((SimTransform) dest).set(s.m00, s.m01, s.tx, s.m10, s.m11, s.ty);
    }

    @Override
    public void concatenateTransform(Object t1, Object t2) {
        ((SimTransform) t1).concat((SimTransform) t2);
    }

    @Override
    public void transformPoint(Object t, float[] in, float[] out) {
        SimTransform s = (SimTransform) t;
        float x = in[0];
        float y = in[1];
        out[0] = (float) (s.m00 * x + s.m01 * y + s.tx);
        out[1] = (float) (s.m10 * x + s.m11 * y + s.ty);
        if (out.length > 2) {
            out[2] = in.length > 2 ? in[2] : 0;
        }
    }

    @Override
    public void transformPoints(Object t, int pointSize, float[] in, int srcPos,
            float[] out, int destPos, int numPoints) {
        float[] pin = new float[pointSize];
        float[] pout = new float[pointSize];
        for (int i = 0; i < numPoints; i++) {
            System.arraycopy(in, srcPos + i * pointSize, pin, 0, pointSize);
            transformPoint(t, pin, pout);
            System.arraycopy(pout, 0, out, destPos + i * pointSize, pointSize);
        }
    }

    @Override
    public boolean transformNativeEqualsImpl(Object t1, Object t2) {
        SimTransform a = (SimTransform) t1;
        SimTransform b = (SimTransform) t2;
        return a.m00 == b.m00 && a.m01 == b.m01 && a.tx == b.tx
                && a.m10 == b.m10 && a.m11 == b.m11 && a.ty == b.ty;
    }

    /* ---- shapes (RoundRectBorder, themed dialogs and borders) --------------- */

    @Override
    public boolean isShapeSupported(Object graphics) {
        return true;
    }

    @Override
    public void fillShape(Object graphics, com.codename1.ui.geom.Shape shape) {
        shapeImpl(graphics, shape, false, 1, 0, 0, 1);
    }

    @Override
    public void drawShape(Object graphics, com.codename1.ui.geom.Shape shape,
            com.codename1.ui.Stroke stroke) {
        shapeImpl(graphics, shape, true, stroke.getLineWidth(), stroke.getCapStyle(),
                stroke.getJoinStyle(), stroke.getMiterLimit());
    }

    private void shapeImpl(Object graphics, com.codename1.ui.geom.Shape shape,
            boolean stroke, float lineWidth, int capStyle, int joinStyle, float miterLimit) {
        if (drops(graphics)) {
            return;
        }
        G g = (G) graphics;
        applyClip(g);
        com.codename1.ui.geom.GeneralPath p;
        if (shape instanceof com.codename1.ui.geom.GeneralPath) {
            p = (com.codename1.ui.geom.GeneralPath) shape;
        } else {
            p = new com.codename1.ui.geom.GeneralPath();
            p.setShape(shape, null);
        }
        int commandsLen = p.getTypesSize();
        int pointsLen = p.getPointsSize();
        byte[] commands = new byte[commandsLen];
        float[] points = new float[pointsLen];
        p.getTypes(commands);
        p.getPoints(points);
        if (g.target != null) {
            g.target.dirty = true;
            bridge().mutableShape(g.target.peer, commands, commandsLen, points, pointsLen,
                    g.color, g.alpha, stroke, lineWidth, capStyle, joinStyle, miterLimit);
            return;
        }
        bridge().shape(commands, commandsLen, points, pointsLen, g.color, g.alpha,
                stroke, lineWidth, capStyle, joinStyle, miterLimit);
    }

    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        if (drops(graphics)) {
            return;
        }
        G g = (G) graphics;
        applyClip(g);
        bridge().drawLine(g.color, g.alpha, x, y, x + width, y);
        bridge().drawLine(g.color, g.alpha, x + width, y, x + width, y + height);
        bridge().drawLine(g.color, g.alpha, x + width, y + height, x, y + height);
        bridge().drawLine(g.color, g.alpha, x, y + height, x, y);
    }

    @Override
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        if (drops(graphics)) {
            return;
        }
        G g = (G) graphics;
        applyClip(g);
        if (g.target != null) {
            g.target.dirty = true;
            bridge().mutableDrawLine(g.target.peer, g.color, g.alpha, x1, y1, x2, y2);
            return;
        }
        bridge().drawLine(g.color, g.alpha, x1, y1, x2, y2);
    }

    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        // v1 approximation: plain rectangle outline
        drawRect(graphics, x, y, width, height);
    }

    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        // v1 approximation: plain fill
        fillRect(graphics, x, y, width, height);
    }

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        // arcs are not bridged yet
    }

    @Override
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        // v1 approximation for full circles used by themes: filled rect
        if (arcAngle >= 360 || arcAngle <= -360) {
            fillRect(graphics, x, y, width, height);
        }
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
        if (drops(graphics) || str == null || str.length() == 0) {
            return;
        }
        G g = (G) graphics;
        applyClip(g);
        BridgedFont f = g.font != null ? g.font : defaultFont();
        if (g.target != null) {
            g.target.dirty = true;
            bridge().mutableDrawString(g.target.peer, f.peer, g.color, g.alpha, str, x, y);
            return;
        }
        bridge().drawString(g.color, g.alpha, f.peer, str, x, y);
    }

    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        if (drops(graphics)) {
            return;
        }
        G g = (G) graphics;
        applyClip(g);
        int[] pixels = rgbData;
        if (offset != 0 || pixels.length != w * h) {
            pixels = new int[w * h];
            System.arraycopy(rgbData, offset, pixels, 0, w * h);
        }
        long peer = bridge().createImageFromARGB(pixels, w, h);
        if (peer != 0) {
            bridge().drawImage(peer, g.alpha, x, y, w, h);
            bridge().releasePeer(peer);
        }
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
        BridgedImage bi = (BridgedImage) img;
        drawImage(graphics, img, x, y, bi.width, bi.height);
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y, int w, int h) {
        if (drops(graphics)) {
            return;
        }
        G g = (G) graphics;
        applyClip(g);
        long srcPeer = drawablePeer((BridgedImage) img);
        if (g.target != null) {
            g.target.dirty = true;
            bridge().mutableDrawImage(g.target.peer, srcPeer, g.alpha, x, y, w, h);
            return;
        }
        bridge().drawImage(srcPeer, g.alpha, x, y, w, h);
    }

    @Override
    public boolean isScaledImageDrawingSupported() {
        return true;
    }

    @Override
    public void tileImage(Object graphics, Object img, int x, int y, int w, int h) {
        if (drops(graphics)) {
            return;
        }
        G g = (G) graphics;
        applyClip(g);
        BridgedImage bi = (BridgedImage) img;
        long srcPeer = drawablePeer(bi);
        if (g.target != null) {
            g.target.dirty = true;
            for (int ty = y; ty < y + h; ty += bi.height) {
                for (int tx = x; tx < x + w; tx += bi.width) {
                    bridge().mutableDrawImage(g.target.peer, srcPeer, g.alpha,
                            tx, ty, bi.width, bi.height);
                }
            }
            return;
        }
        bridge().tileImage(srcPeer, g.alpha, x, y, w, h);
    }

    @Override
    public void flushGraphics() {
        perfFrames++;
        bridge().flush();
        pokeAppUniverse();
    }

    @Override
    public void flushGraphics(int x, int y, int width, int height) {
        perfFrames++;
        bridge().flush();
        pokeAppUniverse();
    }

    private volatile int perfFrames;
    private volatile boolean perfRunning;

    /** Background sampler for the Performance Monitor tool (host-side memory + fps). */
    private void startPerfMonitor() {
        if (perfRunning) {
            return;
        }
        perfRunning = true;
        Thread t = new Thread("cn1sim-perf") {
            public void run() {
                int last = perfFrames;
                while (perfRunning) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    int now = perfFrames;
                    int fps = now - last;
                    last = now;
                    Runtime rt = Runtime.getRuntime();
                    int usedKb = (int) ((rt.totalMemory() - rt.freeMemory()) / 1024);
                    int totalKb = (int) (rt.totalMemory() / 1024);
                    com.codename1.impl.ios.sim.bridge.ToolsBridge tb =
                            BridgeRegistry.getToolsBridge();
                    if (tb != null) {
                        tb.perfStats(fps, usedKb, totalKb);
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /**
     * Shell universe only: a shell repaint may have painted chrome background
     * over the app's region of the shared screen texture; ask the app to
     * recomposite. The app universe never pokes anyone (no loops).
     */
    private void pokeAppUniverse() {
        if (appUniverse || BridgeRegistry.isAppPaused()) {
            // while paused the shell's pause overlay owns the screen region
            return;
        }
        com.codename1.impl.ios.sim.bridge.InputSink sink = BridgeRegistry.getInputSink();
        if (sink != null) {
            sink.repaintRequest();
        }
    }

    @Override
    public boolean isNativeInputSupported() {
        // editString floats a real NSTextField - without this the core falls
        // back to the legacy lightweight (T9) editor
        return true;
    }

    @Override
    public boolean isNativeInputImmediate() {
        // a tap starts editString() directly - the desktop has no virtual
        // keyboard for the touch path to summon
        return true;
    }

    private volatile Component editingComponent;

    @Override
    public boolean isEditingText() {
        return editingComponent != null;
    }

    @Override
    public boolean isEditingText(Component c) {
        return c == editingComponent;
    }

    @Override
    public boolean isNativeEditorVisible(Component c) {
        // the component skips painting its own text/cursor while the native
        // NSTextField floats above it
        return c == editingComponent;
    }

    @Override
    public void editString(final Component cmp, int maxSize, int constraint, String text,
            int initiatingKeycode) {
        // native NSTextField overlay; the commit (Enter / focus loss) flows
        // back through the registry's editing callback
        if (System.getenv("CN1_SIM_DEBUG") != null) {
            System.err.println("cn1sim: editString universe=" + (appUniverse ? "app" : "shell")
                    + " cmp=" + cmp.getClass().getSimpleName()
                    + " reentrant=" + (cmp == editingComponent)
                    + " at " + cmp.getAbsoluteX() + "," + cmp.getAbsoluteY()
                    + " " + cmp.getWidth() + "x" + cmp.getHeight());
        }
        // A re-layout mid-edit (the per-keystroke setText below revalidates the
        // sidebar) re-issues editString for the SAME live field. Tearing down
        // the native editor and re-placing it - sometimes at a stale 0,0 while
        // the relayout is in flight - is exactly the "one char then dead, field
        // jumps to 0,0" failure. Treat a re-entrant call for the component we
        // are already editing as a no-op: the native field is alive and the
        // text is already mirrored.
        if (cmp == editingComponent) {
            return;
        }
        editingComponent = cmp;
        com.codename1.ui.plaf.Style st = cmp.getStyle();
        // Resolve the component's CONCRETE style here - the relay has no copy of
        // the app's theme, so we never send a component (it would resolve to
        // unstyled defaults on the far side). Send the pixel height + style so
        // the relay sizes the native font correctly even when the app font isn't
        // a BridgedFont (the old fontPeer path fell back to a tiny system font).
        com.codename1.ui.Font f = st.getFont();
        // the derive() pixel size reproduces the font exactly; fall back to ~0.8
        // of the line height for system fonts that report no pixel size
        int fontHeightPx = 0;
        if (f != null) {
            fontHeightPx = (int) f.getPixelSize();
            if (fontHeightPx <= 0) {
                fontHeightPx = (int) Math.round(f.getHeight() * 0.8);
            }
        }
        if (fontHeightPx <= 0) {
            fontHeightPx = (int) Math.round(
                    com.codename1.ui.Font.getDefaultFont().getHeight() * 0.8);
        }
        int fontStyle = f != null ? f.getStyle() : com.codename1.ui.Font.STYLE_PLAIN;
        int fgColor = st.getFgColor();
        int bgColor = st.getBgColor();
        int bgTransparency = st.getBgTransparency() & 0xff;
        boolean multiline = cmp instanceof com.codename1.ui.TextArea
                && !(cmp instanceof com.codename1.ui.TextField)
                && ((com.codename1.ui.TextArea) cmp).getRows() > 1;
        // inset by the style padding so the native text lines up with where
        // the component renders its own text
        int padL = st.getPaddingLeft(false);
        int padR = st.getPaddingRight(false);
        int padT = st.getPaddingTop();
        int padB = st.getPaddingBottom();
        BridgeRegistry.setEditingCallback(new com.codename1.impl.ios.sim.bridge.EditingCallback() {
            public void editingDone(final String committed) {
                editingComponent = null;
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        Display.getInstance().onEditingComplete(cmp, committed);
                        cmp.repaint();
                    }
                });
            }

            public void editingUpdate(final String text) {
                // mirror per-keystroke so grow-by-content and listeners
                // behave like a device mid-edit
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (cmp instanceof com.codename1.ui.TextArea) {
                            ((com.codename1.ui.TextArea) cmp).setText(text);
                            cmp.repaint();
                        }
                    }
                });
            }
        });
        cmp.repaint();
        bridge().editString(text == null ? "" : text,
                cmp.getAbsoluteX() + padL, cmp.getAbsoluteY() + padT,
                Math.max(8, cmp.getWidth() - padL - padR),
                Math.max(8, cmp.getHeight() - padT - padB),
                // full resolved style: pixel font height + style, fg/bg colors and
                // bg alpha, plus the input constraint and alignment - everything
                // the native field needs, with no theme lookup on the relay side
                fontHeightPx, fontStyle, fgColor, bgColor, bgTransparency, multiline,
                constraint, st.getAlignment());
    }

    /* ---- fonts -------------------------------------------------------------- */

    private BridgedFont defaultFont() {
        if (defaultFont == null) {
            defaultFont = (BridgedFont) createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        }
        return defaultFont;
    }

    @Override
    public Object getDefaultFont() {
        return defaultFont();
    }

    @Override
    public Object createFont(int face, int style, int size) {
        long peer = bridge().createSystemFont(face, style, size);
        // the app universe sizes system fonts from the skin's device metrics
        // (scaled to the render resolution) so themes proportion like the
        // device; the shell chrome keeps the plain desktop sizes
        int px = scaledSystemFontSize(size);
        if (px > 0) {
            long derived = bridge().deriveTruetypeFont(peer,
                    (style & Font.STYLE_BOLD) != 0, (style & Font.STYLE_ITALIC) != 0, px);
            if (derived != 0) {
                peer = derived;
            }
        }
        return track(new BridgedFont(peer, face, style, size));
    }

    private int scaledSystemFontSize(int size) {
        if (!appUniverse) {
            return 0;
        }
        int unscaled;
        if (size == Font.SIZE_SMALL) {
            unscaled = BridgeRegistry.getFontSizeSmall();
        } else if (size == Font.SIZE_LARGE) {
            unscaled = BridgeRegistry.getFontSizeLarge();
        } else {
            unscaled = BridgeRegistry.getFontSizeMedium();
        }
        if (unscaled <= 0) {
            return 0;
        }
        return Math.max(9, (int) Math.round(unscaled * BridgeRegistry.getRenderScale()));
    }

    @Override
    public int getDeviceDensity() {
        if (!appUniverse) {
            return super.getDeviceDensity();
        }
        int ppi = BridgeRegistry.getSkinPpi();
        if (ppi <= 0) {
            return super.getDeviceDensity();
        }
        // the effective density of the SCALED rendering surface
        double effective = ppi * BridgeRegistry.getRenderScale();
        if (effective < 140) {
            return Display.DENSITY_LOW;
        }
        if (effective < 200) {
            return Display.DENSITY_MEDIUM;
        }
        if (effective < 280) {
            return Display.DENSITY_HIGH;
        }
        if (effective < 400) {
            return Display.DENSITY_VERY_HIGH;
        }
        return Display.DENSITY_HD;
    }

    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
        if (appUniverse) {
            int ppi = BridgeRegistry.getSkinPpi();
            if (ppi > 0) {
                // dips are millimeters; the effective ppi accounts for the
                // render scale
                double effective = ppi * BridgeRegistry.getRenderScale();
                return (int) Math.round(dipCount * effective / 25.4);
            }
        }
        return super.convertToPixels(dipCount, horizontal);
    }

    @Override
    public boolean isTrueTypeSupported() {
        return true;
    }

    @Override
    public boolean isNativeFontSchemeSupported() {
        return true;
    }

    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        return track(new BridgedFont(bridge().createTruetypeFont(fontName), 0, 0, 0));
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        BridgedFont f = (BridgedFont) font;
        long derived = bridge().deriveTruetypeFont(f.peer,
                (weight & Font.STYLE_BOLD) != 0, (weight & Font.STYLE_ITALIC) != 0, size);
        return track(new BridgedFont(derived, f.face, weight, f.size));
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
        ((G) graphics).font = (BridgedFont) font;
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        if (str == null) {
            return 0;
        }
        return bridge().stringWidth(peerOf(nativeFont), str);
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        return bridge().charWidth(peerOf(nativeFont), ch);
    }

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return stringWidth(nativeFont, new String(ch, offset, length));
    }

    @Override
    public int getHeight(Object nativeFont) {
        return bridge().fontHeight(peerOf(nativeFont));
    }

    @Override
    public int getFontAscent(Object nativeFont) {
        return bridge().fontAscent(peerOf(nativeFont));
    }

    @Override
    public int getFontDescent(Object nativeFont) {
        return bridge().fontDescent(peerOf(nativeFont));
    }

    private long peerOf(Object nativeFont) {
        BridgedFont f = nativeFont instanceof BridgedFont ? (BridgedFont) nativeFont : defaultFont();
        return f.peer;
    }

    /* ---- images ------------------------------------------------------------- */

    @Override
    public Object createImage(InputStream i) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = i.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        return createImage(bo.toByteArray(), 0, bo.size());
    }

    @Override
    public Object createImage(String path) throws IOException {
        InputStream is;
        if (exists(path)) {
            is = openFileInputStream(path);
        } else {
            is = getResourceAsStream(getClass(), path);
        }
        if (is == null) {
            throw new IOException("Image not found: " + path);
        }
        try {
            return createImage(is);
        } finally {
            is.close();
        }
    }

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        byte[] data = bytes;
        if (offset != 0 || len != bytes.length) {
            data = new byte[len];
            System.arraycopy(bytes, offset, data, 0, len);
        }
        long[] r = bridge().createImage(data);
        if (r == null) {
            return null;
        }
        return track(new BridgedImage(r[0], (int) r[1], (int) r[2]));
    }

    @Override
    public Object createImage(int[] rgb, int width, int height) {
        return track(new BridgedImage(bridge().createImageFromARGB(rgb, width, height), width, height));
    }

    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        long peer = bridge().createMutableImage(width, height, fillColor);
        return track(new BridgedImage(peer, width, height));
    }


    @Override
    public int getImageWidth(Object i) {
        return i != null ? ((BridgedImage) i).width : 0;
    }

    @Override
    public int getImageHeight(Object i) {
        return i != null ? ((BridgedImage) i).height : 0;
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        BridgedImage bi = (BridgedImage) nativeImage;
        if (bi.width == width && bi.height == height) {
            return bi;
        }
        return track(new BridgedImage(bridge().scaleImage(drawablePeer(bi), width, height), width, height));
    }

    @Override
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        BridgedImage bi = (BridgedImage) nativeImage;
        int[] target = arr;
        if (offset != 0) {
            target = new int[width * height];
        }
        bridge().imageRgbToIntArray(drawablePeer(bi), target, x, y, width, height, bi.width, bi.height);
        if (offset != 0) {
            System.arraycopy(target, 0, arr, offset, width * height);
        }
    }

    @Override
    public void releaseImage(Object image) {
        if (image instanceof BridgedImage) {
            BridgedImage bi = (BridgedImage) image;
            // Explicit dispose: run the registered cleanup exactly once and
            // deregister it from the Cleaner so GC of the wrapper won't send a
            // second (stale) release for a peer id the relay may have reused.
            if (bi.cleanable != null) {
                bi.cleanable.clean();
            } else {
                bridge().releasePeer(bi.peer);
            }
        }
    }

    /* ---- keys --------------------------------------------------------------- */

    @Override
    public int getSoftkeyCount() {
        return 0;
    }

    @Override
    public int[] getSoftkeyCode(int index) {
        return new int[]{0};
    }

    @Override
    public int getClearKeyCode() {
        return -8;
    }

    @Override
    public int getBackspaceKeyCode() {
        return -8;
    }

    @Override
    public int getBackKeyCode() {
        return -11;
    }

    @Override
    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case -1:
                return Display.GAME_UP;
            case -2:
                return Display.GAME_DOWN;
            case -3:
                return Display.GAME_LEFT;
            case -4:
                return Display.GAME_RIGHT;
            case -5:
                return Display.GAME_FIRE;
            default:
                return 0;
        }
    }

    @Override
    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case Display.GAME_UP:
                return -1;
            case Display.GAME_DOWN:
                return -2;
            case Display.GAME_LEFT:
                return -3;
            case Display.GAME_RIGHT:
                return -4;
            case Display.GAME_FIRE:
                return -5;
            default:
                return 0;
        }
    }

    /* ---- native menu (built from Codename One commands) ------------------------ */

    /**
     * Commands currently in the native menu, index-aligned with the encoded
     * rows; selections come back through the MenuDispatcher.
     */
    private java.util.List<com.codename1.ui.Command> nativeMenuCommands;

    @Override
    public void setNativeCommands(java.util.Vector commands) {
        // The Form auto-pushes once per addCommand; for a 90+ item shell menu
        // that floods the relay with a full menu rebuild per command. The shell
        // owns its menu bar and pushes it ONCE, after the whole list is built,
        // through pushShellMenu(). The app universe renders commands in-form
        // (device style), never in the menu bar. So this auto-push is ignored.
    }

    /**
     * Installs the shell's native menu bar from its complete command list:
     * encodes the rows, registers the dispatcher that routes a native-menu
     * selection back to the matching command, and pushes once to the relay.
     */
    public void pushShellMenu(java.util.Vector commands) {
        java.util.ArrayList<com.codename1.ui.Command> filtered =
                new java.util.ArrayList<com.codename1.ui.Command>();
        StringBuilder sb = new StringBuilder();
        if (commands != null) {
            for (int i = 0; i < commands.size(); i++) {
                Object o = commands.elementAt(i);
                if (!(o instanceof com.codename1.ui.Command)) {
                    continue;
                }
                com.codename1.ui.Command c = (com.codename1.ui.Command) o;
                String name = c.getCommandName();
                if (name == null || name.length() == 0) {
                    continue;
                }
                String hint = c.getDesktopMenu();
                if (hint == null) {
                    hint = "";
                }
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(hint).append('\t').append(name).append('\t')
                        .append(c.getDesktopShortcutKeyChar()).append('\t')
                        .append(c.getDesktopShortcutModifiers()).append('\t');
                // simulator extension column: 'c' renders the item checked
                if (Boolean.TRUE.equals(c.getClientProperty("cn1.sim.checked"))) {
                    sb.append('c');
                }
                filtered.add(c);
            }
        }
        nativeMenuCommands = filtered;
        BridgeRegistry.setMenuDispatcher(new com.codename1.impl.ios.sim.bridge.MenuDispatcher() {
            public void fireMenuCommand(int index) {
                final java.util.List<com.codename1.ui.Command> cmds = nativeMenuCommands;
                if (cmds == null || index < 0 || index >= cmds.size()) {
                    return;
                }
                final com.codename1.ui.Command c = cmds.get(index);
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        c.actionPerformed(new com.codename1.ui.events.ActionEvent(c));
                    }
                });
            }

            public int indexOfLabel(String label) {
                java.util.List<com.codename1.ui.Command> cmds = nativeMenuCommands;
                if (cmds != null) {
                    for (int i = 0; i < cmds.size(); i++) {
                        if (label.equals(cmds.get(i).getCommandName())) {
                            return i;
                        }
                    }
                }
                return -1;
            }
        });
        bridge().setNativeMenu(sb.toString());
    }

    /* ---- localization -------------------------------------------------------- */

    @Override
    public L10NManager getLocalizationManager() {
        return new L10NManager("en", "US") {
        };
    }

    /* ---- storage -------------------------------------------------------------- */

    private File storageFile(String name) {
        return new File(storageRoot, name.replace('/', '_'));
    }

    @Override
    public OutputStream createStorageOutputStream(String name) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(storageFile(name)));
    }

    @Override
    public InputStream createStorageInputStream(String name) throws IOException {
        return new BufferedInputStream(new FileInputStream(storageFile(name)));
    }

    @Override
    public boolean storageFileExists(String name) {
        return storageFile(name).exists();
    }

    @Override
    public void deleteStorageFile(String name) {
        storageFile(name).delete();
    }

    @Override
    public String[] listStorageEntries() {
        String[] entries = storageRoot.list();
        return entries != null ? entries : new String[0];
    }

    /* ---- file system ------------------------------------------------------------ */

    private File unfile(String path) {
        if (path.startsWith("file://")) {
            path = path.substring(7);
        }
        return new File(path);
    }

    @Override
    public String[] listFilesystemRoots() {
        fsRoot.mkdirs();
        return new String[]{"file://" + fsRoot.getAbsolutePath() + "/"};
    }

    @Override
    public String getAppHomePath() {
        fsRoot.mkdirs();
        return "file://" + fsRoot.getAbsolutePath() + "/";
    }

    @Override
    public String[] listFiles(String directory) throws IOException {
        String[] files = unfile(directory).list();
        return files != null ? files : new String[0];
    }

    @Override
    public long getRootSizeBytes(String root) {
        return unfile(root).getTotalSpace();
    }

    @Override
    public long getRootAvailableSpace(String root) {
        return unfile(root).getUsableSpace();
    }

    @Override
    public void mkdir(String directory) {
        unfile(directory).mkdirs();
    }

    @Override
    public void deleteFile(String file) {
        unfile(file).delete();
    }

    @Override
    public boolean isHidden(String file) {
        return unfile(file).isHidden();
    }

    @Override
    public void setHidden(String file, boolean h) {
    }

    @Override
    public long getFileLength(String file) {
        return unfile(file).length();
    }

    @Override
    public boolean isDirectory(String file) {
        return unfile(file).isDirectory();
    }

    @Override
    public boolean exists(String file) {
        if (file == null || !(file.startsWith("file:") || file.startsWith("/"))) {
            return false;
        }
        return unfile(file).exists();
    }

    @Override
    public void rename(String file, String newName) {
        File f = unfile(file);
        f.renameTo(new File(f.getParentFile(), newName));
    }

    @Override
    public char getFileSystemSeparator() {
        return '/';
    }

    @Override
    public OutputStream openFileOutputStream(String file) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(unfile(file)));
    }

    @Override
    public InputStream openFileInputStream(String file) throws IOException {
        return new BufferedInputStream(new FileInputStream(unfile(file)));
    }

    /* ---- networking (pure java.net) ----------------------------------------------- */

    /** monotonically increasing connection ids reported to the tools bridge */
    private static int nextConnectionId;
    private final java.util.Map<Object, Integer> connectionIds =
            java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<Object, Integer>());

    private int toolsId(Object connection) {
        Integer id = connectionIds.get(connection);
        return id != null ? id.intValue() : -1;
    }

    @Override
    public Object connect(String url, boolean read, boolean write) throws IOException {
        if (BridgeRegistry.getNetworkCondition() == BridgeRegistry.NETWORK_DISCONNECTED
                && url.toLowerCase().startsWith("http")) {
            throw new IOException("Unreachable");
        }
        URL u = new URL(url);
        URLConnection con = u.openConnection();
        if (con instanceof HttpURLConnection) {
            HttpURLConnection c = (HttpURLConnection) con;
            c.setUseCaches(false);
            c.setInstanceFollowRedirects(false);
        }
        con.setDoInput(read);
        con.setDoOutput(write);
        com.codename1.impl.ios.sim.bridge.ToolsBridge tools = BridgeRegistry.getToolsBridge();
        if (tools != null) {
            int id;
            synchronized (BridgedSimImplementation.class) {
                id = nextConnectionId++;
            }
            connectionIds.put(con, id);
            tools.networkConnect(id, url);
        }
        return con;
    }

    @Override
    public void setHeader(Object connection, String key, String val) {
        ((URLConnection) connection).setRequestProperty(key, val);
    }

    @Override
    public OutputStream openOutputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            return openFileOutputStream((String) connection);
        }
        return new BufferedOutputStream(((URLConnection) connection).getOutputStream());
    }

    @Override
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        RandomAccessFile rf = new RandomAccessFile(unfile((String) connection), "rw");
        rf.seek(offset);
        return new BufferedOutputStream(new FileOutputStream(rf.getFD()));
    }

    @Override
    public InputStream openInputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            return openFileInputStream((String) connection);
        }
        int condition = BridgeRegistry.getNetworkCondition();
        if (condition == BridgeRegistry.NETWORK_DISCONNECTED) {
            throw new IOException("Unreachable");
        }
        if (condition == BridgeRegistry.NETWORK_SLOW) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection ht = (HttpURLConnection) connection;
            if (ht.getResponseCode() < 400) {
                return new BufferedInputStream(ht.getInputStream());
            }
            return new BufferedInputStream(ht.getErrorStream());
        }
        return new BufferedInputStream(((URLConnection) connection).getInputStream());
    }

    /* ---- location (driven by the shell's location tool) ------------------------- */

    @Override
    public com.codename1.location.LocationManager getLocationManager() {
        return BridgedLocationManager.getInstance();
    }

    static class BridgedLocationManager extends com.codename1.location.LocationManager {
        private static BridgedLocationManager instance;

        static synchronized BridgedLocationManager getInstance() {
            if (instance == null) {
                instance = new BridgedLocationManager();
            }
            return instance;
        }

        private com.codename1.location.Location current() {
            com.codename1.location.Location l = new com.codename1.location.Location();
            l.setLatitude(BridgeRegistry.getSimulatedLatitude());
            l.setLongitude(BridgeRegistry.getSimulatedLongitude());
            l.setAccuracy(10);
            l.setTimeStamp(System.currentTimeMillis());
            l.setStatus(AVAILABLE);
            return l;
        }

        public com.codename1.location.Location getCurrentLocation() {
            return current();
        }

        public com.codename1.location.Location getLastKnownLocation() {
            return current();
        }

        protected void bindListener() {
        }

        protected void clearListener() {
        }
    }

    @Override
    public void setPostRequest(Object connection, boolean p) {
        try {
            ((HttpURLConnection) connection).setRequestMethod(p ? "POST" : "GET");
        } catch (IOException err) {
            err.printStackTrace();
        }
        com.codename1.impl.ios.sim.bridge.ToolsBridge tools = BridgeRegistry.getToolsBridge();
        if (tools != null) {
            tools.networkMethod(toolsId(connection), p ? "POST" : "GET");
        }
    }

    @Override
    public int getResponseCode(Object connection) throws IOException {
        int code = ((HttpURLConnection) connection).getResponseCode();
        com.codename1.impl.ios.sim.bridge.ToolsBridge tools = BridgeRegistry.getToolsBridge();
        if (tools != null) {
            tools.networkResponse(toolsId(connection), code,
                    ((HttpURLConnection) connection).getContentLength());
        }
        return code;
    }

    @Override
    public String getResponseMessage(Object connection) throws IOException {
        return ((HttpURLConnection) connection).getResponseMessage();
    }

    @Override
    public int getContentLength(Object connection) {
        return ((URLConnection) connection).getContentLength();
    }

    @Override
    public String getHeaderField(String name, Object connection) throws IOException {
        return ((URLConnection) connection).getHeaderField(name);
    }

    @Override
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        Map<String, List<String>> fields = ((URLConnection) connection).getHeaderFields();
        return fields.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        List<String> headers = new ArrayList<String>();
        Map<String, List<String>> fields = ((URLConnection) connection).getHeaderFields();
        for (String key : fields.keySet()) {
            if (key != null && key.equalsIgnoreCase(name)) {
                headers.addAll(fields.get(key));
            }
        }
        return headers.toArray(new String[0]);
    }
}
