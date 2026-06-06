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
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.media.Media;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
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

    /** The port's bundled native (material) theme, embedded in the executable. */
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

    /*
     * Rasterize linear gradients on the fly (direct drawLine strips) rather than
     * caching them in a mutable Image, matching iOS / Android / JavaScript. The
     * cached path draws the gradient via impl.drawImage, which -- unlike the core
     * drawLine primitives -- does NOT pre-add the graphics' xTranslate to the
     * blit position. LinearGradientPaint.paint sets a rotation matrix that
     * Graphics.setTransform conjugates with T(xTranslate) (since
     * isTranslationSupported() is false here), so the un-translated drawImage
     * lands off-cell: the SVG gradient_circle / clipped_badge fills rendered
     * outside their clip (an empty disc) while solid fills under the same clip
     * were fine. Disabling the cache routes the fill through drawLine, which
     * positions correctly.
     */
    @Override
    protected boolean cacheLinearGradients() {
        return false;
    }

    /* --------------------------------------------------------------- camera */

    // No camera backend is provided: the port does not yet access the host's
    // webcam, so createCameraImpl() inherits the base null (Camera.isSupported()
    // returns false) rather than returning fabricated frames. A real device port
    // must surface real hardware or report unsupported -- never synthetic data
    // that could reach a shipping app. Real Media Foundation webcam capture is a
    // tracked gap (see Ports/WindowsPort/status.md).

    @Override
    public void flushGraphics(int x, int y, int width, int height) {
        WindowsNative.flushGraphics(windowGraphicsPeer, x, y, width, height);
    }

    /*
     * Capture the already-rendered window instead of the base behaviour, which
     * re-paints the current form into a fresh mutable image
     * (current.paintComponent(img.getGraphics(), true)). Re-painting a *heavy*
     * form into a mutable-image target drops every grid cell after the first --
     * the screenshot suite's draw-arc (100 concentric arcs/cell) and
     * draw-image-rect came out with only the top-left quadrant filled while the
     * rest stayed form background. The live window target painted the same form
     * correctly through the normal flush path, so read it back directly. In CI
     * the window is the headless WIC bitmap, so captureWindowToPngBytes returns
     * the current frame; if it is not WIC-backed (on-screen HWND target) it
     * returns null and we fall back to the base mutable-image capture.
     */
    @Override
    public void screenshot(com.codename1.util.SuccessCallback<Image> callback) {
        byte[] png = WindowsNative.captureWindowToPngBytes();
        if (png != null && png.length > 0) {
            try {
                callback.onSucess(Image.createImage(new java.io.ByteArrayInputStream(png)));
                return;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        super.screenshot(callback);
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

    /* The port fully supports 2D affine transforms (setTransform + the legacy
     * scale/rotate below). The base default is false, which made every test that
     * guards on g.isAffineSupported() (Rotate, Scale, AffineScale, ...) bail out
     * and draw "Affine unsupported" instead of the transformed content. */
    @Override
    public boolean isAffineSupported() {
        return true;
    }

    @Override
    public void resetAffine(Object graphics) {
        setTransform(graphics, com.codename1.ui.Transform.makeIdentity());
    }

    /*
     * Software Gaussian blur. Direct2D has a blur effect but it needs the
     * ID2D1DeviceContext/effects pipeline the port's plain render targets don't
     * use; a pure-pixel blur (the same approach iOS/Android fall back to) is
     * simpler and correct. Three box-blur passes approximate a Gaussian; alpha is
     * premultiplied so transparent edges (drop shadows -- the Switch thumb, etc.)
     * blur without dark halos.
     */
    @Override
    public boolean isGaussianBlurSupported() {
        return true;
    }

    @Override
    public Image gaussianBlurImage(Image image, float radius) {
        if (image == null) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int rad = Math.round(radius);
        if (w <= 0 || h <= 0 || rad <= 0) {
            return image;
        }
        int[] px = image.getRGB();
        if (px == null || px.length != w * h) {
            return image;
        }
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int a = p >>> 24;
            int r = ((p >> 16) & 0xff) * a / 255;
            int g = ((p >> 8) & 0xff) * a / 255;
            int b = (p & 0xff) * a / 255;
            px[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        int[] tmp = new int[px.length];
        for (int pass = 0; pass < 3; pass++) {
            boxBlur(px, tmp, w, h, rad, true);
            boxBlur(tmp, px, w, h, rad, false);
        }
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int a = p >>> 24;
            if (a == 0) {
                px[i] = 0;
                continue;
            }
            int r = Math.min(255, ((p >> 16) & 0xff) * 255 / a);
            int g = Math.min(255, ((p >> 8) & 0xff) * 255 / a);
            int b = Math.min(255, (p & 0xff) * 255 / a);
            px[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return Image.createImage(px, w, h);
    }

    /** One separable box-blur pass over premultiplied ARGB; edges clamp. */
    private static void boxBlur(int[] src, int[] dst, int w, int h, int rad, boolean horizontal) {
        int div = 2 * rad + 1;
        if (horizontal) {
            for (int y = 0; y < h; y++) {
                int row = y * w;
                for (int x = 0; x < w; x++) {
                    int aSum = 0, rSum = 0, gSum = 0, bSum = 0;
                    for (int k = -rad; k <= rad; k++) {
                        int xx = x + k;
                        if (xx < 0) {
                            xx = 0;
                        } else if (xx >= w) {
                            xx = w - 1;
                        }
                        int p = src[row + xx];
                        aSum += p >>> 24;
                        rSum += (p >> 16) & 0xff;
                        gSum += (p >> 8) & 0xff;
                        bSum += p & 0xff;
                    }
                    dst[row + x] = ((aSum / div) << 24) | ((rSum / div) << 16) | ((gSum / div) << 8) | (bSum / div);
                }
            }
        } else {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int aSum = 0, rSum = 0, gSum = 0, bSum = 0;
                    for (int k = -rad; k <= rad; k++) {
                        int yy = y + k;
                        if (yy < 0) {
                            yy = 0;
                        } else if (yy >= h) {
                            yy = h - 1;
                        }
                        int p = src[yy * w + x];
                        aSum += p >>> 24;
                        rSum += (p >> 16) & 0xff;
                        gSum += (p >> 8) & 0xff;
                        bSum += p & 0xff;
                    }
                    dst[y * w + x] = ((aSum / div) << 24) | ((rSum / div) << 16) | ((gSum / div) << 8) | (bSum / div);
                }
            }
        }
    }

    /*
     * Two different questions share this name. The no-arg form (reached via
     * Transform.isPerspectiveSupported()) asks whether the transform MATH does the
     * homogeneous w-divide -- it does (Matrix), so graphics-transform-perspective
     * can project its own corners. The Object form (reached via
     * Graphics.isPerspectiveTransformSupported()) asks whether the GRAPHICS can
     * RENDER a primitive under a perspective matrix -- it cannot: the Direct2D
     * target is 2D-affine and setTransform only keeps the affine sub-matrix. So it
     * must return false, otherwise FlipTransition / ComponentReplace(flip) draw the
     * form image under a perspective transform whose perspective term is dropped,
     * collapsing the mid-flip frames to nothing (black). Returning false makes the
     * flip use its 2D scaled-drawImage fallback, which renders.
     */
    @Override
    public boolean isPerspectiveTransformSupported() {
        return true;
    }

    @Override
    public boolean isTransformSupported(Object graphics) {
        return true;
    }

    @Override
    public boolean isPerspectiveTransformSupported(Object graphics) {
        return false;
    }

    // The transform SPI is backed by Matrix (a pure-Java 4x4, shared with the iOS
    // port). A 4x4 is required because the 3D test content (graphics-transform-
    // perspective / -camera) composes makeIdentity -> translate/scale/rotate over
    // a real z axis -> concatenate(makePerspective) and projects the model corners
    // through transformPoint, which performs the homogeneous (w) divide. The
    // Direct2D render target itself stays a 2D affine: setTransform(graphics, ...)
    // extracts the affine sub-matrix for the native layer, while any perspective
    // projection happens here in Java (transformPoint) before a primitive is drawn.
    @Override
    public Object makeTransformAffine(double m00, double m10, double m01, double m11, double m02, double m12) {
        return Matrix.make(new float[] {
            (float) m00, (float) m10, 0, 0,
            (float) m01, (float) m11, 0, 0,
            0, 0, 1, 0,
            (float) m02, (float) m12, 0, 1
        });
    }

    @Override
    public void setTransformAffine(Object nt, double m00, double m10, double m01, double m11, double m02, double m12) {
        ((Matrix) nt).setData(new float[] {
            (float) m00, (float) m10, 0, 0,
            (float) m01, (float) m11, 0, 0,
            0, 0, 1, 0,
            (float) m02, (float) m12, 0, 1
        });
    }

    @Override
    public Object makeTransformTranslation(float x, float y, float z) {
        return Matrix.makeTranslation(x, y, z);
    }

    @Override
    public void setTransformTranslation(Object nt, float x, float y, float z) {
        ((Matrix) nt).setTranslation(x, y, z);
    }

    @Override
    public Object makeTransformScale(float sx, float sy, float sz) {
        Matrix out = Matrix.makeIdentity();
        out.scale(sx, sy, sz);
        return out;
    }

    @Override
    public void setTransformScale(Object nt, float sx, float sy, float sz) {
        Matrix out = (Matrix) nt;
        out.setIdentity();
        out.scale(sx, sy, sz);
    }

    @Override
    public Object makeTransformRotation(float angle, float x, float y, float z) {
        return Matrix.makeRotation(angle, x, y, z);
    }

    @Override
    public void setTransformRotation(Object nt, float angle, float x, float y, float z) {
        Matrix m = (Matrix) nt;
        m.setIdentity();
        m.rotate(angle, x, y, z);
    }

    @Override
    public Object makeTransformPerspective(float fovy, float aspect, float zNear, float zFar) {
        return Matrix.makePerspective(fovy, aspect, zNear, zFar);
    }

    @Override
    public void setTransformPerspective(Object nt, float fovy, float aspect, float zNear, float zFar) {
        ((Matrix) nt).setPerspective(fovy, aspect, zNear, zFar);
    }

    @Override
    public Object makeTransformOrtho(float left, float right, float bottom, float top, float near, float far) {
        return Matrix.makeOrtho(left, right, bottom, top, near, far);
    }

    @Override
    public void setTransformOrtho(Object nt, float left, float right, float bottom, float top, float near, float far) {
        ((Matrix) nt).setOrtho(left, right, bottom, top, near, far);
    }

    @Override
    public Object makeTransformCamera(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        return Matrix.makeCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    @Override
    public void setTransformCamera(Object nt, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        ((Matrix) nt).setCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    @Override
    public void transformRotate(Object nt, float angle, float x, float y, float z) {
        ((Matrix) nt).rotate(angle, x, y, z);
    }

    @Override
    public void transformTranslate(Object nt, float x, float y, float z) {
        ((Matrix) nt).translate(x, y, z);
    }

    @Override
    public void transformScale(Object nt, float x, float y, float z) {
        ((Matrix) nt).scale(x, y, z);
    }

    @Override
    public Object makeTransformIdentity() {
        return Matrix.makeIdentity();
    }

    @Override
    public void setTransformIdentity(Object nt) {
        ((Matrix) nt).setIdentity();
    }

    @Override
    public Object makeTransformInverse(Object nt) {
        Matrix copy = ((Matrix) nt).copy();
        return copy.invert() ? copy : null;
    }

    @Override
    public void setTransformInverse(Object nt) throws com.codename1.ui.Transform.NotInvertibleException {
        if (!((Matrix) nt).invert()) {
            throw new com.codename1.ui.Transform.NotInvertibleException();
        }
    }

    @Override
    public void copyTransform(Object src, Object dest) {
        ((Matrix) dest).setData(((Matrix) src).getData());
    }

    @Override
    public void concatenateTransform(Object t1, Object t2) {
        ((Matrix) t1).concatenate((Matrix) t2);
    }

    @Override
    public boolean transformNativeEqualsImpl(Object t1, Object t2) {
        if (t1 == null) {
            return t2 == null;
        }
        if (t2 == null) {
            return false;
        }
        return java.util.Arrays.equals(((Matrix) t1).getData(), ((Matrix) t2).getData());
    }

    // Project a point through the 4x4, performing the homogeneous (w) divide so a
    // perspective transform foreshortens correctly. The base implementation throws
    // "Transforms not supported", which the lightweight Picker's 3D Scene hits
    // while projecting its wheel bounds -- the throw propagates through paint and
    // wedges the EDT in a repaint loop.
    @Override
    public void transformPoint(Object nativeTransform, float[] in, float[] out) {
        ((Matrix) nativeTransform).transformPoints(Math.min(3, in.length), in, 0, out, 0, 1);
    }

    @Override
    public void transformPoints(Object nativeTransform, int pointSize, float[] in, int srcPos, float[] out, int destPos, int numPoints) {
        ((Matrix) nativeTransform).transformPoints(pointSize, in, srcPos, out, destPos, numPoints);
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
        // Pull the 2D affine sub-matrix out of the 4x4 (column-major: m00,m10 at
        // [0],[1]; m01,m11 at [4],[5]; m02,m12 -- the translation -- at [12],[13]).
        // The render target is affine-only; any perspective term is applied in Java
        // (transformPoint) before drawing, never pushed down to Direct2D.
        float[] d = ((Matrix) transform.getNativeTransform()).getData();
        WindowsNative.setTransform(g, d[0], d[1], d[4], d[5], d[12], d[13]);
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

    /*
     * Legacy affine API (g.scale / g.rotate / g.rotateRadians). The base impl is a
     * no-op ("Affine unsupported"), so these did nothing -- graphics-rotate and any
     * g.scale-based drawing rendered untransformed. Compose onto the current
     * transform exactly as the JavaSE/iOS ports do; getTransform/setTransform
     * already handle the component cell offset.
     */
    @Override
    public void scale(Object graphics, float x, float y) {
        com.codename1.ui.Transform t = getTransform(graphics);
        t.scale(x, y);
        setTransform(graphics, t);
    }

    @Override
    public void rotate(Object graphics, float angle) {
        rotate(graphics, angle, 0, 0);
    }

    @Override
    public void rotate(Object graphics, float angle, int pivotX, int pivotY) {
        com.codename1.ui.Transform t = getTransform(graphics);
        t.rotate(angle, pivotX, pivotY);
        setTransform(graphics, t);
    }

    /*
     * translateMatrix composes the translation directly onto the affine matrix --
     * the matrix-correct counterpart of the integer translate accumulator -- so it
     * pairs with scale()/rotate() exactly as on iOS/JavaSE/Android. Without this
     * the base falls back to translate(int,int): a following g.scale() then
     * multiplies that integer cell anchor, which threw the inscribed-triangle-grid
     * cells off-panel (the sy=2 row rendered below the panel and vanished). Since
     * the port already drives all of scale/rotate/setTransform through one Matrix,
     * advertise support and compose here too.
     */
    @Override
    public boolean isTranslateMatrixSupported() {
        return true;
    }

    @Override
    public void translateMatrix(Object graphics, float x, float y) {
        com.codename1.ui.Transform t = getTransform(graphics);
        t.translate(x, y);
        setTransform(graphics, t);
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
        com.codename1.ui.Transform t = getTransform(graphics);
        if (t == null || t.isIdentity()) {
            WindowsNative.clipRect(peer(graphics), x, y, width, height);
            return;
        }
        /* Under a rotation/scale the clip rect becomes a transformed quad (the
         * clip-under-rotation case). Pass the RAW rect corners; the native layer
         * applies the transform captured at setClip time as the mask transform, so
         * the clip lands exactly where the (equally transformed) drawing does. */
        float[] coords = {
            x,         y,
            x + width, y,
            x + width, y + height,
            x,         y + height
        };
        int[] types = { 0, 1, 1, 1, 4 };
        WindowsNative.setClipShape(peer(graphics), coords, types, 5, 1);
    }

    @Override
    public boolean isShapeClipSupported(Object graphics) {
        return true;
    }

    @Override
    public void setClip(Object graphics, Shape shape) {
        if (shape == null) {
            return;
        }
        FlatPath fp = flattenShape(shape);
        /* The reference renderer clips a straight-edge polygon to its bounding box but
         * tessellates a curved clip precisely -- pixel-verified against the goldens:
         * graphics-clip's triangle fills its whole bbox, while the SVG gradient_circle
         * and clipped_badge clip to the exact curve. Mirror that: MOVE/LINE/CLOSE only
         * -> bbox; any QUAD/CUBIC -> precise geometry. */
        boolean curved = false;
        for (int i = 0; i < fp.typeCount; i++) {
            if (fp.types[i] == 2 || fp.types[i] == 3) {
                curved = true;
                break;
            }
        }
        if (curved) {
            /* Pass the RAW path. cn1WinPushClip applies the world transform captured
             * at setClip time as the layer maskTransform, so a curved clip lands
             * exactly where drawShape draws the same path -- correct under scale/
             * rotate. Pre-transforming the points here instead double-counted the cell
             * offset under a GeneratedSVGImage's viewBox scale and pushed the gradient
             * fill off its clip disc. */
            WindowsNative.setClipShape(peer(graphics), fp.coords, fp.types, fp.typeCount, fp.windingRule);
            return;
        }
        /* Polygon: clip to the screen-space bounding box (axis-aligned rect clip).
         * Transform the corners through the current affine for the bbox since the
         * rect-clip path carries no transform of its own. */
        float[] c = fp.coords;
        com.codename1.ui.Transform t = getTransform(graphics);
        if (t != null && !t.isIdentity()) {
            float[] in = new float[2];
            float[] out = new float[2];
            for (int i = 0; i + 1 < c.length; i += 2) {
                in[0] = c[i];
                in[1] = c[i + 1];
                t.transformPoint(in, out);
                c[i] = out[0];
                c[i + 1] = out[1];
            }
        }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i + 1 < c.length; i += 2) {
            minX = Math.min(minX, c[i]);
            maxX = Math.max(maxX, c[i]);
            minY = Math.min(minY, c[i + 1]);
            maxY = Math.max(maxY, c[i + 1]);
        }
        if (maxX >= minX) {
            WindowsNative.setClip(peer(graphics), (int) Math.floor(minX), (int) Math.floor(minY),
                    (int) Math.ceil(maxX - minX), (int) Math.ceil(maxY - minY));
        }
    }

    /* Clip stack. The base pushClip/popClip are unimplemented no-ops, so a
     * clipRect inside push/pop never restored -- the narrowed clip leaked to
     * everything drawn afterwards (visible as the clip test's quadrants clipping
     * each other). Save/restore the rect clip explicitly. */
    private final java.util.HashMap<Long, java.util.ArrayList<int[]>> clipStacks =
            new java.util.HashMap<Long, java.util.ArrayList<int[]>>();

    @Override
    public void pushClip(Object graphics) {
        Long g = Long.valueOf(peer(graphics));
        java.util.ArrayList<int[]> stack = clipStacks.get(g);
        if (stack == null) {
            stack = new java.util.ArrayList<int[]>();
            clipStacks.put(g, stack);
        }
        stack.add(new int[] {
                getClipX(graphics), getClipY(graphics), getClipWidth(graphics), getClipHeight(graphics)
        });
    }

    @Override
    public void popClip(Object graphics) {
        java.util.ArrayList<int[]> stack = clipStacks.get(Long.valueOf(peer(graphics)));
        if (stack != null && !stack.isEmpty()) {
            int[] c = stack.remove(stack.size() - 1);
            setClip(graphics, c[0], c[1], c[2], c[3]);
        }
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

    /*
     * Direct2D fills/strokes path geometry natively, so the port supports arbitrary
     * shapes. This is what makes RoundBorder / RoundRectBorder (material pill
     * buttons, rounded dialogs, chat bubbles) and Graphics.fillShape/drawShape
     * render -- the base impl returns false and those backgrounds stay blank.
     */
    @Override
    public boolean isShapeSupported(Object graphics) {
        return true;
    }

    @Override
    public void fillShape(Object graphics, Shape shape) {
        FlatPath fp = flattenShape(shape);
        WindowsNative.fillShape(peer(graphics), fp.coords, fp.types, fp.typeCount, fp.windingRule);
    }

    @Override
    public void drawShape(Object graphics, Shape shape, Stroke stroke) {
        FlatPath fp = flattenShape(shape);
        float lineWidth = stroke != null ? stroke.getLineWidth() : 1f;
        WindowsNative.drawShape(peer(graphics), fp.coords, fp.types, fp.typeCount, fp.windingRule, lineWidth);
    }

    /*
     * The base fillPolygon / drawPolygon are software scanline fills built on
     * drawLine -- aliased, and slow (one drawLine per raster row). Route them
     * through the Direct2D path geometry instead so polygons (e.g. the projected
     * 3D quads in graphics-transform-perspective / -camera, which transformPoint
     * has already projected to screen-space corners) fill anti-aliased and match
     * the AA goldens the other ports produce.
     */
    @Override
    public void fillPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints <= 0) {
            return;
        }
        com.codename1.ui.geom.GeneralPath p = new com.codename1.ui.geom.GeneralPath();
        p.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            p.lineTo(xPoints[i], yPoints[i]);
        }
        p.closePath();
        fillShape(graphics, p);
    }

    @Override
    public void drawPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints <= 0) {
            return;
        }
        com.codename1.ui.geom.GeneralPath p = new com.codename1.ui.geom.GeneralPath();
        p.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            p.lineTo(xPoints[i], yPoints[i]);
        }
        p.closePath();
        drawShape(graphics, p, new Stroke(1f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f));
    }

    /** Flattened path data handed to the native geometry builder. */
    private static final class FlatPath {
        final float[] coords;
        final int[] types;
        final int typeCount;
        final int windingRule;

        FlatPath(float[] coords, int[] types, int typeCount, int windingRule) {
            this.coords = coords;
            this.types = types;
            this.typeCount = typeCount;
            this.windingRule = windingRule;
        }
    }

    /** Walks a Shape's PathIterator into the (coords, types) arrays the native
     *  Direct2D path-geometry builder consumes. Segment ops map to
     *  0=move,1=line,2=quad,3=cubic,4=close. */
    private FlatPath flattenShape(Shape shape) {
        PathIterator it = shape.getPathIterator();
        int windingRule = it.getWindingRule();
        float[] coords = new float[64];
        int[] types = new int[32];
        int ci = 0;
        int ti = 0;
        float[] seg = new float[6];
        while (!it.isDone()) {
            int type = it.currentSegment(seg);
            int mapped;
            int n;
            switch (type) {
                case PathIterator.SEG_MOVETO: mapped = 0; n = 2; break;
                case PathIterator.SEG_LINETO: mapped = 1; n = 2; break;
                case PathIterator.SEG_QUADTO: mapped = 2; n = 4; break;
                case PathIterator.SEG_CUBICTO: mapped = 3; n = 6; break;
                case PathIterator.SEG_CLOSE: mapped = 4; n = 0; break;
                default: mapped = 1; n = 2; break;
            }
            if (ti >= types.length) {
                int[] nt = new int[types.length * 2];
                System.arraycopy(types, 0, nt, 0, types.length);
                types = nt;
            }
            types[ti++] = mapped;
            for (int k = 0; k < n; k++) {
                if (ci >= coords.length) {
                    float[] nc = new float[coords.length * 2];
                    System.arraycopy(coords, 0, nc, 0, coords.length);
                    coords = nc;
                }
                coords[ci++] = seg[k];
            }
            it.next();
        }
        float[] trimmedCoords = new float[ci];
        System.arraycopy(coords, 0, trimmedCoords, 0, ci);
        int[] trimmedTypes = new int[ti];
        System.arraycopy(types, 0, trimmedTypes, 0, ti);
        return new FlatPath(trimmedCoords, trimmedTypes, ti, windingRule);
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
        long font = 0;
        // Bundled TTFs (material-design-font.ttf and any app font) ship as
        // classpath resources embedded in the exe -- load them straight from the
        // executable via the DirectWrite in-memory loader so there is no file
        // next to the exe. Falls back to the file-based loader (a font staged
        // beside the exe) when the resource isn't embedded.
        if (fileName != null && fileName.toLowerCase().endsWith(".ttf")) {
            byte[] data = readResourceFully("/" + fileName);
            if (data != null) {
                font = WindowsNative.loadTrueTypeFontFromMemory(fontName, data);
            }
        }
        if (font == 0) {
            font = WindowsNative.loadTrueTypeFont(fontName, fileName);
        }
        if (font == 0) {
            return null;
        }
        return Long.valueOf(font);
    }

    /** Reads an embedded classpath resource fully into a byte[], or null. */
    private byte[] readResourceFully(String resource) {
        InputStream in = getResourceAsStream(WindowsImplementation.class, resource);
        if (in == null) {
            return null;
        }
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException ex) {
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {
            }
        }
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
     * Resolves a classpath-style resource (e.g. {@code /theme.res}). The ParparVM
     * windows target embeds the app's classpath resources into the executable's PE
     * resource section, so they are served straight from the exe -- a single
     * self-contained binary, the Windows analog of the iOS .app bundle. Falls back
     * to a file shipped next to the executable (a dev/debug convenience for
     * resources that were staged rather than embedded). Returns null when absent.
     */
    @Override
    public InputStream getResourceAsStream(Class cls, String resource) {
        if (resource == null) {
            return null;
        }
        byte[] embedded = WindowsNative.resourceBytes(resource);
        if (embedded != null) {
            return new ByteArrayInputStream(embedded);
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

    /**
     * Plays audio (and decodes video) through Media Foundation. The stream is
     * read into memory and handed to the native engine, which spools it to a
     * temp file and resolves the source. Returns null only if the engine can't
     * be created.
     */
    @Override
    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        if (stream == null) {
            return null;
        }
        byte[] data = Util.readInputStream(stream);
        long peer = WindowsNative.mediaCreate(data, data.length, mimeType);
        if (peer == 0) {
            return null;
        }
        return new WindowsMedia(peer, onCompletion);
    }

    /** English long month names; index 0 = January. */
    private static final String[] LONG_MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    @Override
    public L10NManager getLocalizationManager() {
        if (l10n == null) {
            Locale l = Locale.getDefault();
            l10n = new L10NManager(l.getLanguage(), l.getCountry()) {
                /*
                 * The base L10NManager resolves month names by formatting the
                 * date with formatDateLongStyle() (which here returns
                 * Date.toString()) and scanning for the first word -- on the
                 * clean target that yields the weekday or no word at all, so the
                 * date Picker's month wheel renders "null". Worse, the fallback
                 * routes back through DateFormatSymbols.getMonths() -> this same
                 * method, risking unbounded recursion. Resolve the month name
                 * directly from the calendar instead; getShortMonthName() derives
                 * from this, so the abbreviated form is fixed too.
                 */
                @Override
                public String getLongMonthName(Date date) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);
                    int m = c.get(Calendar.MONTH);
                    if (m < 0 || m > 11) {
                        m = 0;
                    }
                    return LONG_MONTHS[m];
                }
            };
        }
        return l10n;
    }
}
