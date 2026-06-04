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
package com.codename1.impl.windows;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.WebSocketImpl;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Native Windows (Win32, desktop / tablet) implementation of the Codename One
 * platform layer. The runtime is produced by ParparVM's "windows" clean-target
 * build and linked into a standalone executable; graphics go through Direct2D,
 * text through DirectWrite, image decode through WIC and networking through
 * WinHTTP, all reached via the {@link WindowsNative} bridge.
 *
 * <p>Peers (graphics targets, fonts, images) are opaque native {@code long}
 * pointers boxed as {@link Long}. A handful of hooks that need richer desktop
 * UX (native text editing / IME, native peer components) are intentionally
 * minimal in this first cut and grow in later phases.</p>
 *
 * @author Codename One
 */
public class WindowsImplementation extends CodenameOneImplementation {
    private static WindowsImplementation INSTANCE;

    // Event type codes; must mirror the CN1EventType enum in cn1_windows.h.
    private static final int EVENT_POINTER_PRESSED = 1;
    private static final int EVENT_POINTER_RELEASED = 2;
    private static final int EVENT_POINTER_DRAGGED = 3;
    private static final int EVENT_KEY_PRESSED = 4;
    private static final int EVENT_KEY_RELEASED = 5;
    private static final int EVENT_SIZE_CHANGED = 6;
    private static final int EVENT_CLOSE = 7;

    private long windowGraphicsPeer;
    private Long windowGraphics;
    private Long defaultFont;
    private L10NManager l10n;
    private com.codename1.ui.util.ImageIO imageIO;
    private final int[] eventScratch = new int[4];

    /**
     * Registers the singleton so the Win32 native bootstrap and message loop can
     * route events back into the EDT through {@link #getInstance()}.
     */
    public WindowsImplementation() {
        INSTANCE = this;
    }

    /**
     * The single live implementation instance, or {@code null} before the port
     * has been constructed.
     */
    public static WindowsImplementation getInstance() {
        return INSTANCE;
    }

    /* -------------------------------------------------------------- helpers */

    private static long peer(Object o) {
        return o == null ? 0L : ((Long) o).longValue();
    }

    private String storagePath(String name) {
        return WindowsNative.storageDir() + getFileSystemSeparator() + name;
    }

    private static String stripFileUrl(String path) {
        if (path.startsWith("file://")) {
            return path.substring("file://".length());
        }
        return path;
    }

    private static byte[] readFully(InputStream i) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read = i.read(buffer);
        while (read > 0) {
            bo.write(buffer, 0, read);
            read = i.read(buffer);
        }
        return bo.toByteArray();
    }

    /* ----------------------------------------------------------- lifecycle */

    /** The port's bundled native (material) theme, shipped next to the exe. */
    private static final String NATIVE_THEME_RES = "windowsNativeTheme.res";

    private int screenDpi = 96;

    /**
     * The live implementation instance, captured for {@link #runMainEventLoop()}
     * which is static (called straight from the app's main thread). There is only
     * ever one display, so a single static handle is sufficient.
     */
    private static WindowsImplementation mainLoopInstance;

    @Override
    public void init(Object m) {
        WindowsNative.initDisplay("Codename One", 800, 600);
        windowGraphicsPeer = WindowsNative.getWindowGraphics();
        windowGraphics = Long.valueOf(windowGraphicsPeer);
        defaultFont = Long.valueOf(WindowsNative.getDefaultFont());
        screenDpi = WindowsNative.screenDpi();
        if (screenDpi <= 0) {
            screenDpi = 96;
        }
        mainLoopInstance = this;
        installNativeTheme();
    }

    /**
     * The app's main thread calls this after {@code Display.init()} to keep the
     * process alive and own the Win32 message pump. Window messages are delivered
     * to the thread that created the window (this one), so input must be pumped
     * and dispatched here -- not on the EDT, which is a separate thread that
     * sleeps on the Display lock. Each pumped batch is drained into Codename One
     * via {@link #drainInput()} (pointerPressed/keyPressed/sizeChanged), and those
     * calls notify the Display lock, waking the EDT to lay out and repaint.
     *
     * <p>This mirrors how every desktop Codename One port feeds events from the
     * native UI thread to the EDT; it returns only when the window closes.</p>
     */
    public static void runMainEventLoop() {
        WindowsImplementation impl = mainLoopInstance;
        // Flush anything queued while the window was being created (e.g. the
        // initial WM_SIZE) before we start blocking for new messages.
        if (impl != null) {
            impl.drainInput();
        }
        while (WindowsNative.pumpMessages()) {
            if (impl != null) {
                impl.drainInput();
            }
        }
    }

    /* Desktop pixel conversion uses the real screen DPI rather than the mobile
     * density buckets, so mm-based theme metrics are sized correctly. dipCount
     * is in thousandths of a millimetre (see Display.convertToPixels). */
    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
        return Math.round(dipCount * (screenDpi / 25.4f));
    }

    @Override
    public int getDeviceDensity() {
        if (screenDpi >= 240) {
            return Display.DENSITY_VERY_HIGH;
        }
        if (screenDpi >= 180) {
            return Display.DENSITY_HIGH;
        }
        if (screenDpi >= 120) {
            return Display.DENSITY_MEDIUM;
        }
        return Display.DENSITY_LOW;
    }

    @Override
    public boolean hasNativeTheme() {
        InputStream in = getResourceAsStream(WindowsImplementation.class, "/" + NATIVE_THEME_RES);
        if (in == null) {
            return false;
        }
        try {
            in.close();
        } catch (IOException ignore) {
        }
        return true;
    }

    /**
     * Applies the port's bundled native theme (a material theme shipped next to
     * the executable) as the base look, mirroring how the iOS port installs
     * iOSModernTheme.res. An app that loads its own theme layers over this.
     * Silently does nothing when the theme resource is absent. This is the
     * framework hook (UIManager calls it for {@code @includeNativeBool} themes);
     * init() also calls it so an app with no theme of its own still gets it.
     */
    private boolean installingNativeTheme;

    @Override
    public void installNativeTheme() {
        // Re-entrancy guard. UIManager.buildTheme() calls installNativeTheme()
        // for any theme carrying @includeNativeBool: true. The bundled native
        // theme can itself carry that flag -- a CSS theme staged as the native
        // theme keeps `includeNativeBool: true` from the app's #Constants -- so
        // setThemeProps() below would recurse straight back into this method and
        // re-open/re-apply the resource forever: the EDT never returns and the
        // heap is exhausted before the first frame (observed as the native theme
        // resource being loaded hundreds of times until a GC-thread crash). The
        // native theme only needs to install once; the nested request is a no-op.
        if (installingNativeTheme) {
            return;
        }
        InputStream in = getResourceAsStream(WindowsImplementation.class, "/" + NATIVE_THEME_RES);
        if (in == null) {
            return;
        }
        installingNativeTheme = true;
        try {
            com.codename1.ui.util.Resources r = com.codename1.ui.util.Resources.open(in);
            String[] names = r.getThemeResourceNames();
            if (names != null && names.length > 0) {
                com.codename1.ui.plaf.UIManager.getInstance().setThemeProps(r.getTheme(names[0]));
            }
        } catch (Throwable t) {
            // A bad/absent theme must not stop the app from starting.
        } finally {
            installingNativeTheme = false;
            try {
                in.close();
            } catch (IOException ignore) {
            }
        }
    }

    /* ------------------------------------------- BrowserComponent (WebView2) */

    @Override
    public boolean isNativeBrowserComponentSupported() {
        return WindowsNative.browserSupported();
    }

    @Override
    public com.codename1.ui.PeerComponent createBrowserComponent(Object browserComponent) {
        return new WindowsBrowserComponent((com.codename1.ui.BrowserComponent) browserComponent);
    }

    @Override
    public void setBrowserPage(com.codename1.ui.PeerComponent browserPeer, String html, String baseUrl) {
        ((WindowsBrowserComponent) browserPeer).setHtml(html);
    }

    @Override
    public void setBrowserURL(com.codename1.ui.PeerComponent browserPeer, String url) {
        ((WindowsBrowserComponent) browserPeer).setUrl(url);
    }

    @Override
    public void browserExecute(com.codename1.ui.PeerComponent browserPeer, String javaScript) {
        ((WindowsBrowserComponent) browserPeer).execute(javaScript);
    }

    @Override
    public int getDisplayWidth() {
        return WindowsNative.getDisplayWidth();
    }

    @Override
    public int getDisplayHeight() {
        return WindowsNative.getDisplayHeight();
    }

    @Override
    public void flushGraphics() {
        WindowsNative.flushGraphics(windowGraphicsPeer, 0, 0, getDisplayWidth(), getDisplayHeight());
    }

    @Override
    public void flushGraphics(int x, int y, int width, int height) {
        WindowsNative.flushGraphics(windowGraphicsPeer, x, y, width, height);
    }

    @Override
    public void edtIdle(boolean enter) {
        // Intentionally empty: the Win32 message pump and input dispatch run on the
        // main thread (see runMainEventLoop), and the EDT is woken by the Display
        // lock notifications those dispatches trigger. The EDT therefore idles by
        // sleeping on the lock like every other Codename One platform -- it must
        // not pump or drain on its own (it is not the window's owning thread).
    }

    private void drainInput() {
        while (WindowsNative.pollEvent(eventScratch)) {
            int type = eventScratch[0];
            int x = eventScratch[1];
            int y = eventScratch[2];
            int key = eventScratch[3];
            switch (type) {
                case EVENT_POINTER_PRESSED:
                    pointerPressed(x, y);
                    break;
                case EVENT_POINTER_RELEASED:
                    pointerReleased(x, y);
                    break;
                case EVENT_POINTER_DRAGGED:
                    pointerDragged(x, y);
                    break;
                case EVENT_KEY_PRESSED:
                    keyPressed(key);
                    break;
                case EVENT_KEY_RELEASED:
                    keyReleased(key);
                    break;
                case EVENT_SIZE_CHANGED:
                    sizeChanged(x, y);
                    break;
                case EVENT_CLOSE:
                    Display.getInstance().exitApplication();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public Object getNativeGraphics() {
        return windowGraphics;
    }

    @Override
    public Object getNativeGraphics(Object image) {
        return Long.valueOf(WindowsNative.getImageGraphics(peer(image)));
    }

    /* ------------------------------------------------------------- transforms
     * Direct2D supports 2D affine transforms natively, so the port implements
     * the Transform SPI (charts, rotated/scaled drawing, transition effects rely
     * on it -- the default impl throws "Transforms not supported"). The native
     * transform object is a 6-element double affine [m00,m10,m01,m11,m02,m12]:
     * x' = m00*x + m01*y + m02, y' = m10*x + m11*y + m12. 3D/perspective is not
     * supported. The current per-graphics transform is tracked here and pushed to
     * the render target via WindowsNative.setTransform. */
    private final java.util.HashMap<Long, com.codename1.ui.Transform> graphicsTransforms =
            new java.util.HashMap<Long, com.codename1.ui.Transform>();

    @Override
    public boolean isTransformSupported() {
        return true;
    }

    @Override
    public boolean isPerspectiveTransformSupported() {
        return false;
    }

    @Override
    public boolean isTransformSupported(Object graphics) {
        return true;
    }

    @Override
    public boolean isPerspectiveTransformSupported(Object graphics) {
        return false;
    }

    @Override
    public Object makeTransformAffine(double m00, double m10, double m01, double m11, double m02, double m12) {
        return new double[] { m00, m10, m01, m11, m02, m12 };
    }

    @Override
    public void setTransformAffine(Object nt, double m00, double m10, double m01, double m11, double m02, double m12) {
        setAff((double[]) nt, m00, m10, m01, m11, m02, m12);
    }

    @Override
    public Object makeTransformTranslation(float x, float y, float z) {
        return new double[] { 1, 0, 0, 1, x, y };
    }

    @Override
    public void setTransformTranslation(Object nt, float x, float y, float z) {
        setAff((double[]) nt, 1, 0, 0, 1, x, y);
    }

    @Override
    public Object makeTransformScale(float sx, float sy, float sz) {
        return new double[] { sx, 0, 0, sy, 0, 0 };
    }

    @Override
    public void setTransformScale(Object nt, float sx, float sy, float sz) {
        setAff((double[]) nt, sx, 0, 0, sy, 0, 0);
    }

    @Override
    public Object makeTransformRotation(float angle, float x, float y, float z) {
        return rot(angle, x, y);
    }

    @Override
    public void setTransformRotation(Object nt, float angle, float x, float y, float z) {
        System.arraycopy(rot(angle, x, y), 0, (double[]) nt, 0, 6);
    }

    @Override
    public Object makeTransformIdentity() {
        return new double[] { 1, 0, 0, 1, 0, 0 };
    }

    @Override
    public void setTransformIdentity(Object nt) {
        setAff((double[]) nt, 1, 0, 0, 1, 0, 0);
    }

    @Override
    public Object makeTransformInverse(Object nt) {
        return invert((double[]) nt);
    }

    @Override
    public void setTransformInverse(Object nt) throws com.codename1.ui.Transform.NotInvertibleException {
        double[] r = invert((double[]) nt);
        if (r == null) {
            throw new com.codename1.ui.Transform.NotInvertibleException();
        }
        System.arraycopy(r, 0, (double[]) nt, 0, 6);
    }

    @Override
    public void copyTransform(Object src, Object dest) {
        System.arraycopy((double[]) src, 0, (double[]) dest, 0, 6);
    }

    @Override
    public void concatenateTransform(Object t1, Object t2) {
        System.arraycopy(mul((double[]) t1, (double[]) t2), 0, (double[]) t1, 0, 6);
    }

    @Override
    public boolean transformNativeEqualsImpl(Object t1, Object t2) {
        if (t1 == null) {
            return t2 == null;
        }
        if (t2 == null) {
            return false;
        }
        return java.util.Arrays.equals((double[]) t1, (double[]) t2);
    }

    // Apply the stored 2D affine [m00,m10,m01,m11,m02,m12] to a point. Without
    // this (and transformPoints) the base implementation throws "Transforms not
    // supported", which the lightweight Picker's 3D Scene hits while projecting
    // its wheel bounds -- the throw propagates through the paint pipeline and
    // wedges the EDT in a repaint loop. z (and any further coords) pass through
    // unchanged since the port is affine-only (no perspective).
    @Override
    public void transformPoint(Object nativeTransform, float[] in, float[] out) {
        double[] a = (double[]) nativeTransform;
        float x = in[0];
        float y = in.length > 1 ? in[1] : 0;
        out[0] = (float) (a[0] * x + a[2] * y + a[4]);
        if (out.length > 1) {
            out[1] = (float) (a[1] * x + a[3] * y + a[5]);
        }
        for (int k = 2; k < out.length; k++) {
            out[k] = k < in.length ? in[k] : 0;
        }
    }

    @Override
    public void transformPoints(Object nativeTransform, int pointSize, float[] in, int srcPos, float[] out, int destPos, int numPoints) {
        double[] a = (double[]) nativeTransform;
        for (int i = 0; i < numPoints; i++) {
            int si = srcPos + i * pointSize;
            int di = destPos + i * pointSize;
            float x = in[si];
            float y = pointSize > 1 ? in[si + 1] : 0;
            out[di] = (float) (a[0] * x + a[2] * y + a[4]);
            if (pointSize > 1) {
                out[di + 1] = (float) (a[1] * x + a[3] * y + a[5]);
            }
            for (int k = 2; k < pointSize; k++) {
                out[di + k] = in[si + k];
            }
        }
    }

    @Override
    public void setTransform(Object graphics, com.codename1.ui.Transform transform) {
        long g = peer(graphics);
        if (transform == null) {
            graphicsTransforms.remove(Long.valueOf(g));
            WindowsNative.setTransform(g, 1, 0, 0, 1, 0, 0);
            return;
        }
        graphicsTransforms.put(Long.valueOf(g), transform.copy());
        double[] a = (double[]) transform.getNativeTransform();
        WindowsNative.setTransform(g, (float) a[0], (float) a[1], (float) a[2], (float) a[3], (float) a[4], (float) a[5]);
    }

    @Override
    public com.codename1.ui.Transform getTransform(Object graphics) {
        com.codename1.ui.Transform t = graphicsTransforms.get(Long.valueOf(peer(graphics)));
        return t == null ? com.codename1.ui.Transform.makeIdentity() : t.copy();
    }

    @Override
    public void getTransform(Object graphics, com.codename1.ui.Transform t) {
        com.codename1.ui.Transform cur = graphicsTransforms.get(Long.valueOf(peer(graphics)));
        if (cur == null) {
            t.setIdentity();
        } else {
            t.setTransform(cur);
        }
    }

    private static void setAff(double[] a, double m00, double m10, double m01, double m11, double m02, double m12) {
        a[0] = m00; a[1] = m10; a[2] = m01; a[3] = m11; a[4] = m02; a[5] = m12;
    }

    /** Rotation by {@code angle} radians about pivot (px,py). */
    private static double[] rot(float angle, float px, float py) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double[] r = { cos, sin, -sin, cos, 0, 0 };
        if (px != 0 || py != 0) {
            return mul(mul(new double[] { 1, 0, 0, 1, px, py }, r), new double[] { 1, 0, 0, 1, -px, -py });
        }
        return r;
    }

    /** Returns A*B (B applied first, then A). */
    private static double[] mul(double[] a, double[] b) {
        return new double[] {
            a[0] * b[0] + a[2] * b[1],
            a[1] * b[0] + a[3] * b[1],
            a[0] * b[2] + a[2] * b[3],
            a[1] * b[2] + a[3] * b[3],
            a[0] * b[4] + a[2] * b[5] + a[4],
            a[1] * b[4] + a[3] * b[5] + a[5]
        };
    }

    /** Inverse of a 2D affine, or null if singular. */
    private static double[] invert(double[] m) {
        double det = m[0] * m[3] - m[1] * m[2];
        if (Math.abs(det) < 1e-12) {
            return null;
        }
        double id = 1.0 / det;
        double na = m[3] * id, nb = -m[1] * id, nc = -m[2] * id, nd = m[0] * id;
        double ne = -(m[4] * na + m[5] * nc);
        double nf = -(m[4] * nb + m[5] * nd);
        return new double[] { na, nb, nc, nd, ne, nf };
    }

    /* ------------------------------------------------------- graphics state */

    @Override
    public int getColor(Object graphics) {
        return WindowsNative.getColor(peer(graphics));
    }

    @Override
    public void setColor(Object graphics, int rgb) {
        WindowsNative.setColor(peer(graphics), rgb);
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        WindowsNative.setAlpha(peer(graphics), alpha);
    }

    @Override
    public int getAlpha(Object graphics) {
        return WindowsNative.getAlpha(peer(graphics));
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
        WindowsNative.setNativeFont(peer(graphics), peer(font));
    }

    @Override
    public int getClipX(Object graphics) {
        return WindowsNative.getClipX(peer(graphics));
    }

    @Override
    public int getClipY(Object graphics) {
        return WindowsNative.getClipY(peer(graphics));
    }

    @Override
    public int getClipWidth(Object graphics) {
        return WindowsNative.getClipWidth(peer(graphics));
    }

    @Override
    public int getClipHeight(Object graphics) {
        return WindowsNative.getClipHeight(peer(graphics));
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        WindowsNative.setClip(peer(graphics), x, y, width, height);
    }

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        WindowsNative.clipRect(peer(graphics), x, y, width, height);
    }

    /* ------------------------------------------------------------- drawing */

    @Override
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        WindowsNative.drawLine(peer(graphics), x1, y1, x2, y2);
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int width, int height) {
        WindowsNative.fillRect(peer(graphics), x, y, width, height);
    }

    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        WindowsNative.drawRect(peer(graphics), x, y, width, height);
    }

    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        WindowsNative.drawRoundRect(peer(graphics), x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        WindowsNative.fillRoundRect(peer(graphics), x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        WindowsNative.fillArc(peer(graphics), x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        WindowsNative.drawArc(peer(graphics), x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
        WindowsNative.drawString(peer(graphics), str, x, y);
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
        WindowsNative.drawImage(peer(graphics), peer(img), x, y);
    }

    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        WindowsNative.drawRGB(peer(graphics), rgbData, offset, x, y, w, h, processAlpha);
    }

    /* -------------------------------------------------------------- fonts */

    @Override
    public Object createFont(int face, int style, int size) {
        return Long.valueOf(WindowsNative.createFont(face, style, size));
    }

    @Override
    public Object getDefaultFont() {
        return defaultFont;
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        return WindowsNative.stringWidth(peer(nativeFont == null ? defaultFont : nativeFont), str);
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        return WindowsNative.charWidth(peer(nativeFont == null ? defaultFont : nativeFont), ch);
    }

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return WindowsNative.charsWidth(peer(nativeFont == null ? defaultFont : nativeFont), ch, offset, length);
    }

    @Override
    public int getHeight(Object nativeFont) {
        return WindowsNative.fontHeight(peer(nativeFont == null ? defaultFont : nativeFont));
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
        long font = WindowsNative.loadTrueTypeFont(fontName, fileName);
        if (font == 0) {
            return null;
        }
        return Long.valueOf(font);
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        return Long.valueOf(WindowsNative.deriveTrueTypeFont(peer(font), size, weight));
    }

    /* -------------------------------------------------------------- images */

    @Override
    public Object createImage(int[] rgb, int width, int height) {
        return Long.valueOf(WindowsNative.createImageFromARGB(rgb, width, height));
    }

    @Override
    public Object createImage(String path) throws IOException {
        return Long.valueOf(WindowsNative.createImageFromFile(stripFileUrl(path)));
    }

    @Override
    public Object createImage(InputStream i) throws IOException {
        byte[] data = readFully(i);
        return Long.valueOf(WindowsNative.createImageFromBytes(data, 0, data.length));
    }

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        return Long.valueOf(WindowsNative.createImageFromBytes(bytes, offset, len));
    }

    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        return Long.valueOf(WindowsNative.createMutableImage(width, height, fillColor));
    }

    @Override
    public int getImageWidth(Object i) {
        return WindowsNative.imageWidth(peer(i));
    }

    @Override
    public int getImageHeight(Object i) {
        return WindowsNative.imageHeight(peer(i));
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        return Long.valueOf(WindowsNative.scaleImage(peer(nativeImage), width, height));
    }

    @Override
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        WindowsNative.imageGetRGB(peer(nativeImage), arr, offset, x, y, width, height);
    }

    /* ---------------------------------------------------------- input keys */

    @Override
    public void editString(Component cmp, int maxSize, int constraint, String text, int initiatingKeycode) {
        // Native text editing / IME is a later-phase enhancement; the lightweight
        // editing path remains usable in the meantime.
        WindowsNative.nativeLog("WindowsImplementation.editString: native editing not wired yet");
    }

    @Override
    public boolean isTouchDevice() {
        return false;
    }

    @Override
    public int getSoftkeyCount() {
        return 0;
    }

    @Override
    public int[] getSoftkeyCode(int index) {
        return null;
    }

    @Override
    public int getClearKeyCode() {
        return -1;
    }

    @Override
    public int getBackspaceKeyCode() {
        return 8;
    }

    @Override
    public int getBackKeyCode() {
        return -1;
    }

    @Override
    public int getGameAction(int keyCode) {
        return 0;
    }

    @Override
    public int getKeyCode(int gameAction) {
        return 0;
    }

    /* ------------------------------------------------------------ network */

    @Override
    public Object connect(String url, boolean read, boolean write) throws IOException {
        long c = WindowsNative.httpOpen(url, read, write);
        if (c == 0) {
            throw new IOException("Unable to open connection to " + url);
        }
        return new WindowsHttpConnection(c);
    }

    @Override
    public void setHeader(Object connection, String key, String val) {
        WindowsNative.httpSetHeader(((WindowsHttpConnection) connection).peer, key, val);
    }

    @Override
    public void setPostRequest(Object connection, boolean p) {
        WindowsNative.httpSetMethod(((WindowsHttpConnection) connection).peer, p);
    }

    @Override
    public int getContentLength(Object connection) {
        return WindowsNative.httpContentLength(((WindowsHttpConnection) connection).peer);
    }

    @Override
    public OutputStream openOutputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            long h = WindowsNative.fileOpenWrite(stripFileUrl((String) connection), false);
            return new WindowsOutputStream(h, false);
        }
        return new WindowsOutputStream(((WindowsHttpConnection) connection).peer, true);
    }

    @Override
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        // offset-based writing maps to opening the file for append/seek; the
        // first cut appends, which covers the common resume-write case.
        long h = WindowsNative.fileOpenWrite(stripFileUrl((String) connection), true);
        return new WindowsOutputStream(h, false);
    }

    @Override
    public InputStream openInputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            long h = WindowsNative.fileOpenRead(stripFileUrl((String) connection));
            return new WindowsInputStream(h, false);
        }
        return new WindowsInputStream(((WindowsHttpConnection) connection).peer, true);
    }

    /**
     * Resolves a classpath-style resource (e.g. {@code /theme.res}) to a file
     * shipped next to the executable. The ParparVM clean target has no embedded
     * classpath resources, so app resources travel alongside the exe. Returns
     * null when the resource is absent.
     */
    @Override
    public InputStream getResourceAsStream(Class cls, String resource) {
        if (resource == null) {
            return null;
        }
        String dir = WindowsNative.executableDir();
        if (dir == null) {
            return null;
        }
        String name = resource.startsWith("/") ? resource.substring(1) : resource;
        String path = dir + "\\" + name.replace('/', '\\');
        long h = WindowsNative.fileOpenRead(path);
        if (h == 0) {
            return null;
        }
        return new WindowsInputStream(h, false);
    }

    @Override
    public int getResponseCode(Object connection) throws IOException {
        return WindowsNative.httpResponseCode(((WindowsHttpConnection) connection).peer);
    }

    @Override
    public String getResponseMessage(Object connection) throws IOException {
        return WindowsNative.httpResponseMessage(((WindowsHttpConnection) connection).peer);
    }

    @Override
    public String getHeaderField(String name, Object connection) throws IOException {
        return WindowsNative.httpHeaderField(((WindowsHttpConnection) connection).peer, name);
    }

    @Override
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        return WindowsNative.httpHeaderFieldNames(((WindowsHttpConnection) connection).peer);
    }

    @Override
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        String value = WindowsNative.httpHeaderField(((WindowsHttpConnection) connection).peer, name);
        if (value == null) {
            return null;
        }
        return new String[] { value };
    }

    @Override
    public void cleanup(Object o) {
        if (o instanceof WindowsHttpConnection) {
            WindowsNative.httpClose(((WindowsHttpConnection) o).peer);
            return;
        }
        super.cleanup(o);
    }

    /* ------------------------------------------------------------ sockets */

    @Override
    public boolean isSocketAvailable() {
        return true;
    }

    @Override
    public Object connectSocket(String host, int port, int connectTimeout) {
        WindowsSocket socket = new WindowsSocket(host, port, connectTimeout);
        return socket.isConnected() ? socket : null;
    }

    @Override
    public String getHostOrIP() {
        return WindowsNative.getHostOrIP();
    }

    @Override
    public void disconnectSocket(Object socket) {
        ((WindowsSocket) socket).close();
    }

    @Override
    public boolean isSocketConnected(Object socket) {
        return socket instanceof WindowsSocket && ((WindowsSocket) socket).isConnected();
    }

    @Override
    public int getSocketAvailableInput(Object socket) {
        return ((WindowsSocket) socket).available();
    }

    @Override
    public byte[] readFromSocketStream(Object socket) {
        return ((WindowsSocket) socket).readChunk();
    }

    @Override
    public void writeToSocketStream(Object socket, byte[] data) {
        ((WindowsSocket) socket).write(data, 0, data.length);
    }

    @Override
    public String getSocketErrorMessage(Object socket) {
        return ((WindowsSocket) socket).getErrorMessage();
    }

    @Override
    public int getSocketErrorCode(Object socket) {
        return ((WindowsSocket) socket).getErrorCode();
    }

    /* ---------------------------------------------------------- websocket */

    @Override
    public boolean isWebSocketSupported() {
        return true;
    }

    @Override
    public WebSocketImpl createWebSocketImpl(String url) {
        return new WindowsWebSocketImpl(url);
    }

    /* --------------------------------------------------------- image I/O */

    @Override
    public com.codename1.ui.util.ImageIO getImageIO() {
        if (imageIO == null) {
            imageIO = new com.codename1.ui.util.ImageIO() {
                @Override
                public void save(InputStream image, OutputStream response, String format,
                        int width, int height, float quality) throws IOException {
                    com.codename1.ui.Image img = com.codename1.ui.Image.createImage(image);
                    if (width > 0 && height > 0) {
                        img = img.scaled(width, height);
                    }
                    saveImage(img, response, format, quality);
                }

                @Override
                protected void saveImage(com.codename1.ui.Image img, OutputStream response,
                        String format, float quality) throws IOException {
                    int[] rgb = img.getRGB();
                    byte[] png = WindowsNative.encodeArgbToPng(rgb, img.getWidth(), img.getHeight());
                    if (png == null) {
                        throw new IOException("PNG encoding failed");
                    }
                    response.write(png);
                }

                @Override
                public boolean isFormatSupported(String format) {
                    return FORMAT_PNG.equals(format);
                }
            };
        }
        return imageIO;
    }

    /* ------------------------------------------------------- storage/files */

    @Override
    public void deleteStorageFile(String name) {
        WindowsNative.fileDelete(storagePath(name));
    }

    @Override
    public OutputStream createStorageOutputStream(String name) throws IOException {
        long h = WindowsNative.fileOpenWrite(storagePath(name), false);
        return new WindowsOutputStream(h, false);
    }

    @Override
    public InputStream createStorageInputStream(String name) throws IOException {
        long h = WindowsNative.fileOpenRead(storagePath(name));
        return new WindowsInputStream(h, false);
    }

    @Override
    public boolean storageFileExists(String name) {
        return WindowsNative.fileExists(storagePath(name));
    }

    @Override
    public String[] listStorageEntries() {
        return WindowsNative.fileList(WindowsNative.storageDir());
    }

    @Override
    public String[] listFilesystemRoots() {
        return WindowsNative.fileRoots();
    }

    @Override
    public String[] listFiles(String directory) throws IOException {
        return WindowsNative.fileList(stripFileUrl(directory));
    }

    @Override
    public long getRootSizeBytes(String root) {
        return WindowsNative.fileRootSize(root);
    }

    @Override
    public long getRootAvailableSpace(String root) {
        return WindowsNative.fileRootFree(root);
    }

    @Override
    public void mkdir(String directory) {
        WindowsNative.fileMkdir(stripFileUrl(directory));
    }

    @Override
    public void deleteFile(String file) {
        WindowsNative.fileDelete(stripFileUrl(file));
    }

    @Override
    public boolean isHidden(String file) {
        return WindowsNative.fileIsHidden(stripFileUrl(file));
    }

    @Override
    public void setHidden(String file, boolean h) {
        WindowsNative.fileSetHidden(stripFileUrl(file), h);
    }

    @Override
    public long getFileLength(String file) {
        return WindowsNative.fileLength(stripFileUrl(file));
    }

    @Override
    public boolean isDirectory(String file) {
        return WindowsNative.fileIsDirectory(stripFileUrl(file));
    }

    @Override
    public boolean exists(String file) {
        return WindowsNative.fileExists(stripFileUrl(file));
    }

    @Override
    public void rename(String file, String newName) {
        WindowsNative.fileRename(stripFileUrl(file), newName);
    }

    @Override
    public char getFileSystemSeparator() {
        return '\\';
    }

    /* ------------------------------------------------------------ platform */

    @Override
    public String getPlatformName() {
        return "win";
    }

    @Override
    public L10NManager getLocalizationManager() {
        if (l10n == null) {
            Locale l = Locale.getDefault();
            l10n = new L10NManager(l.getLanguage(), l.getCountry()) {
            };
        }
        return l10n;
    }
}
