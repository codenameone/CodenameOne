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
package com.codename1.impl.linux;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.WebSocketImpl;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.media.Media;
import com.codename1.printing.PrintResult;
import com.codename1.printing.PrintResultListener;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Native Linux (GTK3, desktop) implementation of the Codename One platform
 * layer. The runtime is produced by ParparVM's "linux" clean-target build and
 * linked into a standalone ELF executable; graphics go through Cairo, text
 * through Pango, image decode through GdkPixbuf and networking through libcurl,
 * all reached via the {@link LinuxNative} bridge.
 *
 * <p>Peers (graphics targets, fonts, images) are opaque native {@code long}
 * pointers boxed as {@link Long}. A handful of hooks that need richer desktop
 * UX (native text editing / IME, native peer components) are intentionally
 * minimal in this first cut and grow in later phases.</p>
 *
 * <p><b>Note:</b> this port was seeded from the proven native Windows port and
 * shares its structure; some inline comments still reference the Windows
 * rendering stack (Direct2D/DirectWrite/WIC/WinHTTP) and are being migrated to
 * their Cairo/Pango/GdkPixbuf/libcurl equivalents. The {@link LinuxNative}
 * bridge contract is platform-neutral and is fully implemented (no stubs) in
 * GTK/Cairo/Pango/GdkPixbuf/WebKitGTK/GStreamer/EGL C under
 * {@code Ports/LinuxPort/nativeSources}; the 2D and GLES 3D paths have been run
 * headless (see {@code Ports/LinuxPort/status.md}).</p>
 */
public class LinuxImplementation extends CodenameOneImplementation {

    @Override
    public boolean isHighContrastEnabled() {
        return LinuxNative.isHighContrastEnabled();
    }

    @Override
    public boolean isReduceMotionEnabled() {
        return LinuxNative.isReduceMotionEnabled();
    }

    @Override
    public boolean isScreenReaderEnabled() {
        return LinuxNative.isScreenReaderEnabled();
    }
    private static LinuxImplementation INSTANCE;

    // Event type codes; must mirror the CN1EventType enum in cn1_linux.h.
    private static final int EVENT_POINTER_PRESSED = 1;
    private static final int EVENT_POINTER_RELEASED = 2;
    private static final int EVENT_POINTER_DRAGGED = 3;
    private static final int EVENT_KEY_PRESSED = 4;
    private static final int EVENT_KEY_RELEASED = 5;
    private static final int EVENT_SIZE_CHANGED = 6;
    private static final int EVENT_CLOSE = 7;
    private static final int EVENT_MOUSE_WHEEL = 8;
    private static final int EVENT_MOUSE_HWHEEL = 9;
    private static final int EVENT_PINCH = 10;
    private static final int EVENT_ROTATE = 11;
    private static final int EVENT_ACCESSIBILITY_ACTION = 12;

    // The native gesture events encode their float (incremental scale / radians) as
    // an int in 1/10000 units; see CN1_GESTURE_FIXED in cn1_linux.h.
    private static final float GESTURE_FIXED = 10000f;

    // One mouse-wheel notch is WHEEL_DELTA (120). Linux defaults to scrolling
    // three lines per notch; * 5 then converted through the screen DPI gives a
    // comfortable per-notch pixel travel that scales with the display, matching
    // the JavaSE desktop port's feel.
    private static final int WHEEL_DELTA = 120;

    private long windowGraphicsPeer;
    private Long windowGraphics;
    private Long defaultFont;
    private L10NManager l10n;
    private com.codename1.ui.util.ImageIO imageIO;
    private final int[] eventScratch = new int[4];
    private final Map<String, Integer> accessibilityActionTokens = new HashMap<String, Integer>();
    private final Map<Integer, AccessibilityActionTarget> accessibilityActionTargets =
            new HashMap<Integer, AccessibilityActionTarget>();
    private int nextAccessibilityActionToken = 1;

    /**
     * Registers the singleton so the Win32 native bootstrap and message loop can
     * route events back into the EDT through {@link #getInstance()}.
     */
    public LinuxImplementation() {
        INSTANCE = this;
    }

    /**
     * The single live implementation instance, or {@code null} before the port
     * has been constructed.
     */
    public static LinuxImplementation getInstance() {
        return INSTANCE;
    }

    /* -------------------------------------------------------------- helpers */

    private static long peer(Object o) {
        return o == null ? 0L : ((Long) o).longValue();
    }

    private String storagePath(String name) {
        return LinuxNative.storageDir() + getFileSystemSeparator() + name;
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
    private static final String NATIVE_THEME_RES = "linuxNativeTheme.res";

    private int screenDpi = 96;

    /**
     * The live implementation instance, captured for {@link #runMainEventLoop()}
     * which is static (called straight from the app's main thread). There is only
     * ever one display, so a single static handle is sufficient.
     */
    private static LinuxImplementation mainLoopInstance;

    @Override
    public void init(Object m) {
        LinuxNative.initDisplay("Codename One", 800, 600);
        windowGraphicsPeer = LinuxNative.getWindowGraphics();
        windowGraphics = Long.valueOf(windowGraphicsPeer);
        defaultFont = Long.valueOf(LinuxNative.getDefaultFont());
        screenDpi = LinuxNative.screenDpi();
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
        LinuxImplementation impl = mainLoopInstance;
        // Flush anything queued while the window was being created (e.g. the
        // initial WM_SIZE) before we start blocking for new messages.
        if (impl != null) {
            impl.drainInput();
        }
        while (LinuxNative.pumpMessages()) {
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
        InputStream in = getResourceAsStream(LinuxImplementation.class, "/" + NATIVE_THEME_RES);
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
        InputStream in = getResourceAsStream(LinuxImplementation.class, "/" + NATIVE_THEME_RES);
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
        return LinuxNative.browserSupported();
    }

    @Override
    public com.codename1.ui.PeerComponent createBrowserComponent(Object browserComponent) {
        return new LinuxBrowserComponent((com.codename1.ui.BrowserComponent) browserComponent);
    }

    @Override
    public void setBrowserPage(com.codename1.ui.PeerComponent browserPeer, String html, String baseUrl) {
        ((LinuxBrowserComponent) browserPeer).setHtml(html);
    }

    @Override
    public void setBrowserURL(com.codename1.ui.PeerComponent browserPeer, String url) {
        ((LinuxBrowserComponent) browserPeer).setUrl(url);
    }

    @Override
    public void browserExecute(com.codename1.ui.PeerComponent browserPeer, String javaScript) {
        ((LinuxBrowserComponent) browserPeer).execute(javaScript);
    }

    /* ----------------------------------------------- generic native peers */

    // Wraps an app @NativeInterface-returned native widget (a child HWND boxed as
    // a long[]) in a PeerComponent that places/sizes/shows the HWND over the
    // lightweight component -- the generic peer-placement path (the analog of iOS
    // NativeIPhoneView), used by native interfaces and the camera preview.
    @Override
    public com.codename1.ui.PeerComponent createNativePeer(Object nativeComponent) {
        if (nativeComponent instanceof long[]) {
            return new LinuxGenericPeer(nativeComponent);
        }
        return super.createNativePeer(nativeComponent);
    }

    // OpenGL ES 3.0 backend for the portable 3D API (com.codename1.gpu), rendered
    // offscreen via EGL (cn1_linux_gl.c). The surface reports unsupported
    // (createPeer returns null) when EGL/GLES cannot be initialized, matching the
    // port's "real data or unsupported" rule.
    private final com.codename1.impl.gpu.GpuImplementation gpuImpl =
            new com.codename1.impl.gpu.GpuImplementation() {
        @Override
        public com.codename1.ui.PeerComponent createPeer(com.codename1.gpu.RenderView view) {
            LinuxGLSurface surface = new LinuxGLSurface(view);
            if (surface.getContextPeer() == 0) {
                return null;
            }
            return surface;
        }

        @Override
        public void setContinuous(com.codename1.ui.PeerComponent peer, boolean continuous) {
            if (peer instanceof LinuxGLSurface) {
                ((LinuxGLSurface) peer).setContinuous(continuous);
            }
        }

        @Override
        public void requestRender(com.codename1.ui.PeerComponent peer) {
            if (peer instanceof LinuxGLSurface) {
                ((LinuxGLSurface) peer).requestRender();
            }
        }
    };

    @Override
    public com.codename1.impl.gpu.GpuImplementation getGpuImplementation() {
        return gpuImpl;
    }

    // SIMD acceleration: SSE2 (x64) / NEON (arm64) backed vector kernels, the
    // x86/ARM analog of the iOS IOSSimd/NEON layer. LinuxSimd.isSupported()
    // returns true and the hot-path operations run native intrinsics; ops it does
    // not override fall back to the portable Simd scalar loop (still correct).
    @Override
    public com.codename1.util.Simd createSimd() {
        return new LinuxSimd();
    }

    // DPAPI-backed secure storage (the desktop analog of the iOS keychain /
    // Android EncryptedSharedPreferences non-prompting store). Used by the
    // networking layer to read API keys / tokens at rest; see LinuxSecureStorage.
    private com.codename1.security.SecureStorage secureStorage;

    @Override
    public com.codename1.security.SecureStorage getSecureStorage() {
        if (secureStorage == null) {
            secureStorage = new LinuxSecureStorage();
        }
        return secureStorage;
    }

    // Linux Hello biometric authentication (WinRT UserConsentVerifier). Reports
    // unsupported honestly when no Hello hardware is present or WinRT is absent.
    private com.codename1.security.Biometrics biometrics;

    @Override
    public com.codename1.security.Biometrics getBiometrics() {
        if (biometrics == null) {
            biometrics = new LinuxBiometrics();
        }
        return biometrics;
    }

    // Real BLE central via the in-process libcn1ble engine (btleplug -> BlueZ),
    // reached through the native LinuxBleBridge. Peripheral / L2CAP / classic
    // report unsupported (btleplug is central-only).
    private com.codename1.bluetooth.Bluetooth bluetooth;

    @Override
    public com.codename1.bluetooth.Bluetooth getBluetooth() {
        if (bluetooth == null) {
            bluetooth = new com.codename1.impl.bluetooth.NativeBluetooth(
                    new LinuxBleBridge());
        }
        return bluetooth;
    }

    private com.codename1.health.Health health;

    /// Returns a local health store. There is no platform health provider
    /// on this port, so the store reports
    /// {@code HealthAvailability.LOCAL_ONLY}: reads and writes work and
    /// are this app's own, but nothing else writes into it. The Bluetooth
    /// sensor layer is unaffected and works fully.
    @Override
    public com.codename1.health.Health getHealth() {
        // Guarded because everything the store serializes is per-instance:
        // the authorization queue, the subscription registry, drain
        // coalescing and the persisted-cursor lock. Two threads racing this
        // getter each got their own store, and two stores coordinate on
        // nothing -- they would launch overlapping permission flows despite
        // the queue inside each one being correct.
        synchronized (LinuxImplementation.class) {
            if (health == null) {
                health = new com.codename1.impl.health.LocalHealth();
            }
            return health;
        }
    }

    private com.codename1.home.spi.HomeBridge homeBridge;

    private com.codename1.impl.nearby.LocalNearbyBridge nearbyBridge;

    /// Returns a local simulated home. There is no HomeKit or Google Home on
    /// this port, so the bridge reports
    /// {@code HomeAvailability.LOCAL_ONLY}: the accessories are furnished by
    /// {@code SyntheticHome}, reads and writes work and are durable, and
    /// nothing outside this app can see them.
    ///
    /// Worth having rather than answering unsupported, because almost all of a
    /// smart-home feature -- laying out a room, wiring a control to a write,
    /// rendering an unreachable accessory -- is code that has nothing to do
    /// with hardware, and a port that reported nothing would make all of it
    /// testable only on a phone.
    @Override
    public com.codename1.home.spi.HomeBridge getHomeBridge() {
        // Guarded because the bridge holds the graph, the current trait
        // values and the undelivered-change queues. Two threads racing this
        // getter would each get their own home, and two homes coordinate on
        // nothing -- a write through one would be invisible to a
        // subscription registered against the other.
        synchronized (LinuxImplementation.class) {
            if (homeBridge == null) {
                com.codename1.impl.home.LocalHomeBridge local =
                        new com.codename1.impl.home.LocalHomeBridge();
                com.codename1.impl.home.SyntheticHome.populate(local);
                homeBridge = local;
            }
            return homeBridge;
        }
    }

    /// The nearby bridge for the native Linux port: a simulated
    /// implementation rather than no implementation, for the same reason
    /// [#getHomeBridge()] carries one.
    /// Ranging UI, an association flow and a transport screen are almost
    /// entirely code with nothing to do with radios, and a port that reported
    /// nothing would make all of it testable only on a pair of phones.
    ///
    /// It reports `LOCAL_ONLY`, never `AVAILABLE`, so an app can tell the
    /// developer the peers it is tracking are not real.
    @Override
    public com.codename1.nearby.spi.NearbyBridge getNearbyBridge() {
        // Guarded for the reason the home bridge is: the bridge holds the
        // live sessions, the association store and the connection set, and
        // two threads racing this getter would each get their own -- a
        // session prepared through one would be invisible to the other.
        synchronized (LinuxImplementation.class) {
            if (nearbyBridge == null) {
                com.codename1.impl.nearby.LocalNearbyBridge local =
                        new com.codename1.impl.nearby.LocalNearbyBridge();
                com.codename1.impl.nearby.SyntheticNearby.populate(local);
                nearbyBridge = local;
            }
            return nearbyBridge;
        }
    }


    // WinRT Geolocator-backed location. getCurrentLocation reports OUT_OF_SERVICE
    // honestly when Linux location is disabled / denied.
    private com.codename1.location.LocationManager locationManager;

    @Override
    public com.codename1.location.LocationManager getLocationManager() {
        if (locationManager == null) {
            locationManager = new LinuxLocationManager();
        }
        return locationManager;
    }

    /* ------------------------------------------------------------ contacts
     * Backed by the WinRT ContactStore (cn1_linux_winrt.cpp). One native call
     * returns every contact as a delimited blob which is parsed and briefly
     * cached here so getAllContacts() + the per-id getContactById() the base
     * runs in a loop share a single store read. */
    private java.util.HashMap<String, String[]> contactCache;

    private java.util.HashMap<String, String[]> contacts() {
        if (contactCache == null) {
            contactCache = new java.util.HashMap<String, String[]>();
            String blob = LinuxNative.contactsGetAll();
            if (blob != null && blob.length() > 0) {
                String[] records = blob.split("");
                for (int i = 0; i < records.length; i++) {
                    String[] f = records[i].split("", -1);
                    if (f.length >= 1 && f[0].length() > 0) {
                        contactCache.put(f[0], f);
                    }
                }
            }
        }
        return contactCache;
    }

    @Override
    public String[] getAllContacts(boolean withNumbers) {
        java.util.HashMap<String, String[]> all = contacts();
        java.util.ArrayList<String> ids = new java.util.ArrayList<String>();
        for (String[] f : all.values()) {
            // withNumbers filters to contacts that have a phone number.
            if (!withNumbers || (f.length > 2 && f[2].length() > 0)) {
                ids.add(f[0]);
            }
        }
        return ids.toArray(new String[ids.size()]);
    }

    @Override
    public com.codename1.contacts.Contact getContactById(String id) {
        String[] f = contacts().get(id);
        if (f == null) {
            return null;
        }
        com.codename1.contacts.Contact c = new com.codename1.contacts.Contact();
        c.setId(f[0]);
        if (f.length > 1) {
            c.setDisplayName(f[1]);
        }
        if (f.length > 2 && f[2].length() > 0) {
            c.setPrimaryPhoneNumber(f[2]);
            java.util.Hashtable phones = new java.util.Hashtable();
            phones.put("mobile", f[2]);
            c.setPhoneNumbers(phones);
        }
        if (f.length > 3 && f[3].length() > 0) {
            c.setPrimaryEmail(f[3]);
            java.util.Hashtable emails = new java.util.Hashtable();
            emails.put("home", f[3]);
            c.setEmails(emails);
        }
        return c;
    }

    @Override
    public int getDisplayWidth() {
        return LinuxNative.getDisplayWidth();
    }

    @Override
    public int getDisplayHeight() {
        return LinuxNative.getDisplayHeight();
    }

    @Override
    public void flushGraphics() {
        LinuxNative.flushGraphics(windowGraphicsPeer, 0, 0, getDisplayWidth(), getDisplayHeight());
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

    // The richer com.codename1.camera CameraImpl (device camera API): a Media
    // Foundation capture session with an image-based live preview peer, stills and
    // a frame listener (LinuxCameraImpl). Video/flash/zoom/focus are honestly
    // reported unsupported there (a generic desktop webcam exposes none via the
    // source reader). The legacy Capture API capturePhoto below remains for the
    // simple "take a photo" path.
    @Override
    public com.codename1.impl.CameraImpl createCameraImpl() {
        return new LinuxCameraImpl();
    }

    // Legacy Capture API: capturePhoto grabs a single real frame from the default
    // webcam via Media Foundation (cn1_linux_camera.cpp) -- the honest desktop
    // snapshot, never synthetic.
    @Override
    public void capturePhoto(final com.codename1.ui.events.ActionListener response) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String result = null;
                try {
                    int[] dims = new int[2];
                    int[] argb = LinuxNative.cameraCaptureFrame(dims);
                    if (argb != null && dims[0] > 0 && dims[1] > 0) {
                        byte[] png = LinuxNative.encodeArgbToPng(argb, dims[0], dims[1]);
                        String path = LinuxNative.storageDir() + getFileSystemSeparator()
                                + "cn1photo" + System.currentTimeMillis() + ".png";
                        OutputStream os = openOutputStream(path);
                        os.write(png);
                        os.close();
                        result = "file://" + path.replace('\\', '/');
                    }
                } catch (Throwable err) {
                    err.printStackTrace();
                }
                final com.codename1.ui.events.ActionEvent ev = result != null
                        ? new com.codename1.ui.events.ActionEvent(result) : null;
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        response.actionPerformed(ev);
                    }
                });
            }
        }, "cn1-linux-capturephoto");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void flushGraphics(int x, int y, int width, int height) {
        LinuxNative.flushGraphics(windowGraphicsPeer, x, y, width, height);
    }

    /// Issue #5273: confine a clip set while a component paints to its flushed
    /// (dirty) region. The Linux port draws screen ops immediately into a
    /// persistent Cairo surface, so without this an oversized clip would escape
    /// the repainted sub-region and leave stale pixels until a full repaint.
    @Override
    protected void setPaintDirtyRegionClip(int x, int y, int width, int height) {
        if (windowGraphicsPeer != 0) {
            LinuxNative.setFlushRect(windowGraphicsPeer, x, y, width, height);
        }
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
        byte[] png = LinuxNative.captureWindowToPngBytes();
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

    // High bit the native layer ORs into a pointer event's key field to flag a touch
    // digitizer (see cn1_linux.h CN1_PE_TOUCH_FLAG); the low byte is the button
    // bitmask (PointerEvent.MASK_*).
    private static final int POINTER_BUTTON_BITS = 0xFF;
    private static final int POINTER_TOUCH_FLAG = 256;

    // Decodes the native pointer key field (button mask + touch flag) into the
    // cross-platform PointerEvent metadata for the next dispatched pointer event, so
    // the rich pointer / context-menu APIs report the real button and device type.
    private void markPointer(int keyField) {
        int mask = keyField & POINTER_BUTTON_BITS;
        int type = (keyField & POINTER_TOUCH_FLAG) != 0
                ? com.codename1.ui.events.PointerEvent.TYPE_TOUCH
                : com.codename1.ui.events.PointerEvent.TYPE_MOUSE;
        int button;
        if (mask == 0) {
            mask = com.codename1.ui.events.PointerEvent.MASK_PRIMARY;
            button = com.codename1.ui.events.PointerEvent.BUTTON_PRIMARY;
        } else if ((mask & com.codename1.ui.events.PointerEvent.MASK_PRIMARY) != 0) {
            button = com.codename1.ui.events.PointerEvent.BUTTON_PRIMARY;
        } else if ((mask & com.codename1.ui.events.PointerEvent.MASK_SECONDARY) != 0) {
            button = com.codename1.ui.events.PointerEvent.BUTTON_SECONDARY;
        } else if ((mask & com.codename1.ui.events.PointerEvent.MASK_MIDDLE) != 0) {
            button = com.codename1.ui.events.PointerEvent.BUTTON_MIDDLE;
        } else if ((mask & com.codename1.ui.events.PointerEvent.MASK_BACK) != 0) {
            button = com.codename1.ui.events.PointerEvent.BUTTON_BACK;
        } else {
            button = com.codename1.ui.events.PointerEvent.BUTTON_FORWARD;
        }
        setPointerEventMetadata(button, mask, type, 1f, 0, 0, 0, 0, false);
    }

    private void drainInput() {
        while (LinuxNative.pollEvent(eventScratch)) {
            int type = eventScratch[0];
            int x = eventScratch[1];
            int y = eventScratch[2];
            int key = eventScratch[3];
            switch (type) {
                case EVENT_POINTER_PRESSED:
                    markPointer(key);
                    pointerPressed(x, y);
                    break;
                case EVENT_POINTER_RELEASED:
                    markPointer(key);
                    pointerReleased(x, y);
                    break;
                case EVENT_POINTER_DRAGGED:
                    markPointer(key);
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
                case EVENT_MOUSE_WHEEL:
                    // key carries the signed wheel delta (multiple of WHEEL_DELTA).
                    // A forward (positive) notch reveals content above, i.e. drags
                    // the finger down -> positive scrollY. Map through the shared
                    // CodenameOneImplementation.pointerWheelMoved scroll gesture.
                    pointerWheelMoved(x, y, 0, wheelUnits(key));
                    break;
                case EVENT_MOUSE_HWHEEL:
                    // A positive horizontal notch tilts right (scrolls content
                    // left), i.e. drags the finger left -> negative scrollX.
                    pointerWheelMoved(x, y, -wheelUnits(key), 0);
                    break;
                case EVENT_PINCH:
                    // key is the incremental scale multiplier in 1/10000 units.
                    Display.getInstance().fireMagnifyGesture(x, y, key / GESTURE_FIXED);
                    break;
                case EVENT_ROTATE:
                    // key is the incremental rotation in 1/10000 radians.
                    Display.getInstance().fireRotationGesture(x, y, key / GESTURE_FIXED);
                    break;
                case EVENT_CLOSE:
                    Display.getInstance().exitApplication();
                    break;
                case EVENT_ACCESSIBILITY_ACTION:
                    AccessibilityActionTarget actionTarget = accessibilityActionTargets.get(Integer.valueOf(key));
                    if (actionTarget != null) {
                        performAccessibilityAction(actionTarget.nodeId, actionTarget.actionId, null);
                    }
                    break;
                default:
                    break;
            }
        }
        // A clicked notification balloon hands its id back here; deliver it to the
        // app's LocalNotificationCallback like the mobile ports do.
        String clicked = LinuxNative.notificationPollClicked();
        if (clicked != null) {
            dispatchLocalNotification(clicked);
        }
        // Applet widget windows queue their click / moved events on the same
        // native pump; the bridge routes clicks through Surfaces.dispatchAction.
        LinuxWidgetBridge bridge = widgetBridge;
        if (bridge != null) {
            bridge.drainNativeEvents();
        }
    }

    /* ------------------------------------------------------------ surfaces */

    /**
     * The bridge behind {@code com.codename1.surfaces} on this port: pinned widget
     * kinds render as frameless always-on-top GTK "applet" windows and a live
     * activity docks a pill window top-center (full behavior on X11/XWayland;
     * plain floating windows under Wayland -- see cn1_linux_widgets.c). Volatile
     * because it is created lazily on the EDT and read by the main pump thread's
     * {@link #drainInput()}.
     */
    private volatile LinuxWidgetBridge widgetBridge;

    @Override
    public com.codename1.surfaces.spi.SurfaceBridge getSurfaceBridge() {
        if (widgetBridge == null) {
            widgetBridge = new LinuxWidgetBridge();
        }
        return widgetBridge;
    }

    @Override
    public void accessibilityTreeChanged(int changeType) {
        AccessibilityTreeSnapshot tree = getAccessibilityTreeSnapshot();
        Set<String> liveActionKeys = new HashSet<String>();
        LinuxNative.accessibilityBegin();
        for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
            int flags = (node.isFocusable() ? 1 : 0) | (node.isFocused() ? 2 : 0)
                    | (Boolean.TRUE.equals(node.getEnabled()) ? 4 : 0)
                    | (Boolean.TRUE.equals(node.getSelected()) ? 8 : 0)
                    | (node.getChecked().ordinal() << 4)
                    | (Boolean.TRUE.equals(node.getExpanded()) ? 64 : 0)
                    | (Boolean.TRUE.equals(node.getInvalid()) ? 128 : 0);
            com.codename1.ui.geom.Rectangle b = node.getBounds();
            LinuxNative.accessibilityNode(node.getId(), node.getParentId(), node.getRole().name(), node.getLabel(),
                    joinDescription(node), node.getValue(), b.getX(), b.getY(), b.getWidth(), b.getHeight(), flags);
            for (AccessibilityAction action : node.getActions()) {
                if (action.isEnabled()) {
                    String actionKey = accessibilityActionKey(node.getId(), action.getId());
                    liveActionKeys.add(actionKey);
                    LinuxNative.accessibilityAction(node.getId(), action.getId(),
                            accessibilityActionToken(node.getId(), action.getId()),
                            action.getLabel() == null ? action.getId() : action.getLabel());
                }
            }
        }
        LinuxNative.accessibilityEnd(changeType);
        Iterator<Map.Entry<String, Integer>> iterator = accessibilityActionTokens.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (!liveActionKeys.contains(entry.getKey())) {
                accessibilityActionTargets.remove(entry.getValue());
                iterator.remove();
            }
        }
    }

    private int accessibilityActionToken(long nodeId, String actionId) {
        String key = accessibilityActionKey(nodeId, actionId);
        Integer existing = accessibilityActionTokens.get(key);
        if (existing != null) {
            accessibilityActionTargets.put(existing, new AccessibilityActionTarget(nodeId, actionId));
            return existing.intValue();
        }
        int candidate = nextAccessibilityActionToken;
        while (accessibilityActionTargets.containsKey(Integer.valueOf(candidate))) {
            candidate = nextAccessibilityActionToken(candidate);
        }
        nextAccessibilityActionToken = nextAccessibilityActionToken(candidate);
        Integer token = Integer.valueOf(candidate);
        accessibilityActionTokens.put(key, token);
        accessibilityActionTargets.put(token, new AccessibilityActionTarget(nodeId, actionId));
        return candidate;
    }

    private int nextAccessibilityActionToken(int current) {
        return current == Integer.MAX_VALUE ? 1 : current + 1;
    }

    private String accessibilityActionKey(long nodeId, String actionId) {
        return nodeId + "\n" + actionId;
    }

    private static final class AccessibilityActionTarget {
        final long nodeId;
        final String actionId;

        AccessibilityActionTarget(long nodeId, String actionId) {
            this.nodeId = nodeId;
            this.actionId = actionId;
        }
    }

    private String joinDescription(AccessibilityNodeSnapshot node) {
        StringBuilder out = new StringBuilder();
        if (node.getHint() != null) out.append(node.getHint());
        if (node.getDescription() != null) {
            if (out.length() > 0) out.append(". ");
            out.append(node.getDescription());
        }
        if (node.getValidationError() != null) {
            if (out.length() > 0) out.append(". ");
            out.append(node.getValidationError());
        }
        return out.length() == 0 ? null : out.toString();
    }

    @Override
    public boolean isAccessibilityTreeSupported() {
        return true;
    }

    /* --------------------------------------------------- local notifications
     * Desktop semantic (mirrors the JavaSE port): while the app runs, a Timer
     * fires the notification at its scheduled time and Shell_NotifyIcon shows a
     * tray balloon; clicking it dispatches the id to the app's
     * LocalNotificationCallback. */
    private java.util.Timer localNotificationsTimer;
    private final java.util.HashMap<String, java.util.TimerTask> localNotifications =
            new java.util.HashMap<String, java.util.TimerTask>();

    @Override
    public void scheduleLocalNotification(final com.codename1.notifications.LocalNotification notif,
            long firstTime, int repeat) {
        if (localNotificationsTimer == null) {
            localNotificationsTimer = new java.util.Timer();
        }
        java.util.TimerTask old = localNotifications.get(notif.getId());
        if (old != null) {
            old.cancel();
        }
        java.util.TimerTask task = new java.util.TimerTask() {
            @Override
            public void run() {
                LinuxNative.showNotification(notif.getId(), notif.getAlertTitle(), notif.getAlertBody());
            }
        };
        localNotifications.put(notif.getId(), task);
        long period = repeatPeriod(repeat);
        if (period <= 0) {
            localNotificationsTimer.schedule(task, new Date(firstTime));
        } else {
            localNotificationsTimer.schedule(task, new Date(firstTime), period);
        }
    }

    @Override
    public void cancelLocalNotification(String notificationId) {
        java.util.TimerTask task = localNotifications.remove(notificationId);
        if (task != null) {
            task.cancel();
        }
    }

    private static long repeatPeriod(int repeat) {
        switch (repeat) {
            case com.codename1.notifications.LocalNotification.REPEAT_MINUTE:
                return 60 * 1000L;
            case com.codename1.notifications.LocalNotification.REPEAT_HOUR:
                return 60 * 60 * 1000L;
            case com.codename1.notifications.LocalNotification.REPEAT_DAY:
                return 24 * 60 * 60 * 1000L;
            case com.codename1.notifications.LocalNotification.REPEAT_WEEK:
                return 7 * 24 * 60 * 60 * 1000L;
            default:
                return 0L;
        }
    }

    private void dispatchLocalNotification(final String notificationId) {
        Object app = getCurrentApplicationInstance();
        if (app instanceof com.codename1.notifications.LocalNotificationCallback) {
            final com.codename1.notifications.LocalNotificationCallback cb =
                    (com.codename1.notifications.LocalNotificationCallback) app;
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    cb.localNotificationReceived(notificationId);
                }
            });
        }
    }

    /**
     * Converts a raw Win32 wheel delta (a signed multiple of {@link #WHEEL_DELTA})
     * into a pixel scroll distance for {@link #pointerWheelMoved}. Scaled through
     * the real screen DPI so one notch travels a comparable physical distance at
     * any density.
     */
    private int wheelUnits(int delta) {
        int notches = delta / WHEEL_DELTA;
        if (notches == 0) {
            // Sub-notch high-resolution wheels still deliver a fraction of travel.
            notches = delta > 0 ? 1 : -1;
        }
        return notches * convertToPixels(15, true);
    }

    @Override
    public Object getNativeGraphics() {
        return windowGraphics;
    }

    @Override
    public Object getNativeGraphics(Object image) {
        return Long.valueOf(LinuxNative.getImageGraphics(peer(image)));
    }

    /* ------------------------------------------------------------- transforms
     * Direct2D supports 2D affine transforms natively, so the port implements
     * the Transform SPI (charts, rotated/scaled drawing, transition effects rely
     * on it -- the default impl throws "Transforms not supported"). The native
     * transform object is a 6-element double affine [m00,m10,m01,m11,m02,m12]:
     * x' = m00*x + m01*y + m02, y' = m10*x + m11*y + m12. 3D/perspective is not
     * supported. The current per-graphics transform is tracked here and pushed to
     * the render target via LinuxNative.setTransform. */
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
            LinuxNative.setTransform(g, 1, 0, 0, 1, 0, 0);
            return;
        }
        graphicsTransforms.put(Long.valueOf(g), transform.copy());
        // Pull the 2D affine sub-matrix out of the 4x4 (column-major: m00,m10 at
        // [0],[1]; m01,m11 at [4],[5]; m02,m12 -- the translation -- at [12],[13]).
        // The render target is affine-only; any perspective term is applied in Java
        // (transformPoint) before drawing, never pushed down to Direct2D.
        float[] d = ((Matrix) transform.getNativeTransform()).getData();
        LinuxNative.setTransform(g, d[0], d[1], d[4], d[5], d[12], d[13]);
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
        return LinuxNative.getColor(peer(graphics));
    }

    @Override
    public void setColor(Object graphics, int rgb) {
        LinuxNative.setColor(peer(graphics), rgb);
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        LinuxNative.setAlpha(peer(graphics), alpha);
    }

    @Override
    public int getAlpha(Object graphics) {
        return LinuxNative.getAlpha(peer(graphics));
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
        LinuxNative.setNativeFont(peer(graphics), peer(font));
    }

    @Override
    public int getClipX(Object graphics) {
        return LinuxNative.getClipX(peer(graphics));
    }

    @Override
    public int getClipY(Object graphics) {
        return LinuxNative.getClipY(peer(graphics));
    }

    @Override
    public int getClipWidth(Object graphics) {
        return LinuxNative.getClipWidth(peer(graphics));
    }

    @Override
    public int getClipHeight(Object graphics) {
        return LinuxNative.getClipHeight(peer(graphics));
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        LinuxNative.setClip(peer(graphics), x, y, width, height);
    }

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        com.codename1.ui.Transform t = getTransform(graphics);
        if (t == null || t.isIdentity()) {
            LinuxNative.clipRect(peer(graphics), x, y, width, height);
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
        LinuxNative.setClipShape(peer(graphics), coords, types, 5, 1);
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
            LinuxNative.setClipShape(peer(graphics), fp.coords, fp.types, fp.typeCount, fp.windingRule);
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
            LinuxNative.setClip(peer(graphics), (int) Math.floor(minX), (int) Math.floor(minY),
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
        LinuxNative.drawLine(peer(graphics), x1, y1, x2, y2);
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int width, int height) {
        LinuxNative.fillRect(peer(graphics), x, y, width, height);
    }

    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        LinuxNative.drawRect(peer(graphics), x, y, width, height);
    }

    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        LinuxNative.drawRoundRect(peer(graphics), x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        LinuxNative.fillRoundRect(peer(graphics), x, y, width, height, arcWidth, arcHeight);
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
        LinuxNative.fillShape(peer(graphics), fp.coords, fp.types, fp.typeCount, fp.windingRule);
    }

    @Override
    public void drawShape(Object graphics, Shape shape, Stroke stroke) {
        FlatPath fp = flattenShape(shape);
        float lineWidth = stroke != null ? stroke.getLineWidth() : 1f;
        LinuxNative.drawShape(peer(graphics), fp.coords, fp.types, fp.typeCount, fp.windingRule, lineWidth);
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
        LinuxNative.fillArc(peer(graphics), x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        LinuxNative.drawArc(peer(graphics), x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
        LinuxNative.drawString(peer(graphics), str, x, y);
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
        LinuxNative.drawImage(peer(graphics), peer(img), x, y);
    }

    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        LinuxNative.drawRGB(peer(graphics), rgbData, offset, x, y, w, h, processAlpha);
    }

    /* -------------------------------------------------------------- fonts */

    @Override
    public Object createFont(int face, int style, int size) {
        return Long.valueOf(LinuxNative.createFont(face, style, size));
    }

    @Override
    public Object getDefaultFont() {
        return defaultFont;
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        return LinuxNative.stringWidth(peer(nativeFont == null ? defaultFont : nativeFont), str);
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        return LinuxNative.charWidth(peer(nativeFont == null ? defaultFont : nativeFont), ch);
    }

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return LinuxNative.charsWidth(peer(nativeFont == null ? defaultFont : nativeFont), ch, offset, length);
    }

    @Override
    public int getHeight(Object nativeFont) {
        return LinuxNative.fontHeight(peer(nativeFont == null ? defaultFont : nativeFont));
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
        if (fileName != null && isBundledFontFile(fileName)) {
            byte[] data = readResourceFully("/" + fileName);
            if (data != null) {
                font = LinuxNative.loadTrueTypeFontFromMemory(fontName, data);
            }
        }
        if (font == 0) {
            font = LinuxNative.loadTrueTypeFont(fontName, fileName);
        }
        if (font == 0) {
            return null;
        }
        return Long.valueOf(font);
    }

    /**
     * True for a font the app bundles. FontConfig and FreeType read the SFNT
     * container whether the outlines are glyf or CFF, so OpenType is as
     * loadable as TrueType here.
     */
    private static boolean isBundledFontFile(String fileName) {
        return endsWithIgnoreCase(fileName, ".ttf") || endsWithIgnoreCase(fileName, ".otf");
    }

    /** Locale-independent case-insensitive suffix test. */
    private static boolean endsWithIgnoreCase(String value, String suffix) {
        return value != null && value.length() >= suffix.length()
                && value.regionMatches(true, value.length() - suffix.length(), suffix, 0, suffix.length());
    }

    /** Reads an embedded classpath resource fully into a byte[], or null. */
    private byte[] readResourceFully(String resource) {
        InputStream in = getResourceAsStream(LinuxImplementation.class, resource);
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
        return Long.valueOf(LinuxNative.deriveTrueTypeFont(peer(font), size, weight));
    }

    /* -------------------------------------------------------------- images */

    @Override
    public Object createImage(int[] rgb, int width, int height) {
        return Long.valueOf(LinuxNative.createImageFromARGB(rgb, width, height));
    }

    @Override
    public Object createImage(String path) throws IOException {
        return Long.valueOf(LinuxNative.createImageFromFile(stripFileUrl(path)));
    }

    @Override
    public Object createImage(InputStream i) throws IOException {
        byte[] data = readFully(i);
        return Long.valueOf(LinuxNative.createImageFromBytes(data, 0, data.length));
    }

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        return Long.valueOf(LinuxNative.createImageFromBytes(bytes, offset, len));
    }

    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        return Long.valueOf(LinuxNative.createMutableImage(width, height, fillColor));
    }

    @Override
    public int getImageWidth(Object i) {
        return LinuxNative.imageWidth(peer(i));
    }

    @Override
    public int getImageHeight(Object i) {
        return LinuxNative.imageHeight(peer(i));
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        return Long.valueOf(LinuxNative.scaleImage(peer(nativeImage), width, height));
    }

    @Override
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        LinuxNative.imageGetRGB(peer(nativeImage), arr, offset, x, y, width, height);
    }

    /* ---------------------------------------------------------- input keys */

    /** The native EDIT-control peer of the field currently being edited, else 0. */
    private long editPeer;
    /** The field currently being edited (the native EDIT stands in for it). */
    private Component editCmp;
    /** Polls the native control for commit (Enter / focus loss) on the EDT. */
    private com.codename1.ui.util.UITimer editPoller;

    /**
     * Native input is supported: while a TextField/TextArea is edited it is
     * overlaid by a real Win32 EDIT control with a native caret, selection,
     * keyboard and IME -- so {@code TextArea} routes here instead of the
     * lightweight (CN1-drawn) editor.
     */
    @Override
    public boolean isNativeInputSupported() {
        return true;
    }

    /**
     * Asynchronous edit mode. editString returns immediately and the native
     * control owns the keystrokes; a UITimer polls for commit and the framework
     * calls {@link #hideTextEditor()} when the user scrolls the editing field away
     * (Component.setScrollY) -- so the overlay never floats detached from its
     * field. (The sync invokeAndBlock alternative would freeze the EDT and miss
     * the scroll-hide hook.)
     */
    @Override
    public boolean isAsyncEditMode() {
        return true;
    }

    /**
     * Overlays a native Win32 EDIT control over the field's text area (inside the
     * field's padding so its border/background still show), styled to match the
     * field's font and colours, and focuses it. Returns immediately (async); the
     * value is delivered through {@code Display.onEditingComplete} when the user
     * commits (Enter on a single-line field) or the edit ends (focus loss, the
     * field scrolling away, or editing another field).
     */
    @Override
    public void editString(final Component cmp, int maxSize, int constraint, String text, int initiatingKeycode) {
        // Tapping straight from one field into another: finish the first cleanly
        // before overlaying the new one, so two controls never float at once.
        if (editPeer != 0) {
            commitEdit();
        }
        boolean singleLine = !(cmp instanceof com.codename1.ui.TextArea)
                || ((com.codename1.ui.TextArea) cmp).isSingleLineTextArea();

        com.codename1.ui.plaf.Style s = cmp.getStyle();
        int padL = s.getPaddingLeft(false);
        int padR = s.getPaddingRight(false);
        int padT = s.getPaddingTop();
        int padB = s.getPaddingBottom();
        int x = cmp.getAbsoluteX() + cmp.getScrollX() + padL;
        int y = cmp.getAbsoluteY() + cmp.getScrollY() + padT;
        int w = cmp.getWidth() - padL - padR;
        int h = cmp.getHeight() - padT - padB;

        long fontPeer = 0;
        com.codename1.ui.Font f = s.getFont();
        if (f != null && f.getNativeFont() instanceof Long) {
            fontPeer = ((Long) f.getNativeFont()).longValue();
        }

        long peer = LinuxNative.editStringAt(x, y, w, h, text == null ? "" : text,
                singleLine, maxSize, fontPeer, s.getFgColor(), s.getBgColor(), 0);
        if (peer == 0) {
            // No native window (headless) -> nothing to edit; complete with the
            // existing text so a caller awaiting the callback still proceeds.
            Display.getInstance().onEditingComplete(cmp, text);
            return;
        }
        editPeer = peer;
        editCmp = cmp;
        com.codename1.ui.Form form = cmp.getComponentForm();
        if (form != null) {
            editPoller = com.codename1.ui.util.UITimer.timer(30, true, form, new Runnable() {
                public void run() {
                    if (editPeer == 0) {
                        return;
                    }
                    if (LinuxNative.editIsDone(editPeer)) {
                        hideTextEditor();
                        return;
                    }
                    // Mirror the native control's text into the field as the user
                    // types: in async edit mode TextArea.onEditComplete does NOT
                    // write the text back (it assumes the port streams it live, as
                    // iOS does), so the field must be kept in sync here or the edits
                    // are lost. Fires the field's data-change listeners too.
                    syncEditText();
                }
            });
        }
    }

    /** Pushes the native control's current text into the editing field if it changed. */
    private void syncEditText() {
        if (editPeer == 0 || !(editCmp instanceof com.codename1.ui.TextArea)) {
            return;
        }
        String t = LinuxNative.editGetText(editPeer);
        if (t != null && !t.equals(((com.codename1.ui.TextArea) editCmp).getText())) {
            ((com.codename1.ui.TextArea) editCmp).setText(t);
        }
    }

    /**
     * Reads the native control's final text, writes it back to the field (async
     * onEditComplete does not), tears the control down, and fires the field's
     * action/editing-complete events. Does not touch the framework's editingText
     * flag (the caller does) -- used both to finish the current edit before
     * starting another and as the body of {@link #hideTextEditor()}.
     */
    private void commitEdit() {
        long p = editPeer;
        Component c = editCmp;
        if (p == 0) {
            return;
        }
        String txt = LinuxNative.editGetText(p);
        if (txt != null && c instanceof com.codename1.ui.TextArea) {
            ((com.codename1.ui.TextArea) c).setText(txt);
        }
        editPeer = 0;
        editCmp = null;
        if (editPoller != null) {
            editPoller.cancel();
            editPoller = null;
        }
        LinuxNative.editClose(p);
        if (c != null) {
            Display.getInstance().onEditingComplete(c, txt != null ? txt : "");
        }
    }

    /**
     * Framework hook: invoked when the editing field is scrolled away
     * (Component.setScrollY) or editing is ended. Commits and removes the native
     * control, then clears the framework editing state via super.
     */
    @Override
    public void hideTextEditor() {
        commitEdit();
        super.hideTextEditor();
    }

    @Override
    public void stopTextEditing() {
        hideTextEditor();
    }

    /* ----------------------------------------------------------- clipboard */

    /**
     * Copies text to the real Linux clipboard so it pastes into other apps.
     * Non-string objects keep the in-memory lightweight clipboard (via super) for
     * round-tripping within the app. {@code getPasteDataFromClipboard} prefers the
     * system clipboard and falls back to the lightweight one.
     */
    @Override
    public void copyToClipboard(Object obj) {
        String text = getPlainTextForClipboard(obj);
        // GTK clipboard set-calls replace each other, so the last non-null wins.
        // Set text first, then image, then files, so a richer representation
        // survives while a text-only copy still works.
        if (text != null) {
            LinuxNative.clipboardSetText(text);
        }
        if (obj instanceof com.codename1.ui.ClipboardContent) {
            com.codename1.ui.ClipboardContent content = (com.codename1.ui.ClipboardContent) obj;
            byte[] image = content.getBytes(com.codename1.ui.ClipboardContent.MIME_PNG);
            if (image == null) {
                image = content.getBytes(com.codename1.ui.ClipboardContent.MIME_JPEG);
            }
            if (image == null) {
                image = content.getBytes(com.codename1.ui.ClipboardContent.MIME_GIF);
            }
            if (image != null) {
                LinuxNative.clipboardSetImage(image);
            }
            Object files = content.getData(com.codename1.ui.ClipboardContent.MIME_FILE);
            if (files instanceof String[]) {
                String[] paths = (String[]) files;
                if (paths.length > 0) {
                    LinuxNative.clipboardSetFiles(paths);
                }
            } else if (files instanceof String) {
                LinuxNative.clipboardSetFiles(new String[] { (String) files });
            }
        }
        super.copyToClipboard(obj);
    }

    @Override
    public Object getPasteDataFromClipboard() {
        String text = LinuxNative.clipboardGetText();
        byte[] image = LinuxNative.clipboardGetImage();
        String[] files = LinuxNative.clipboardGetFiles();
        // When the system clipboard carries an image or files (or more than one
        // representation), reconstruct a full ClipboardContent for the caller.
        if (image != null || (files != null && files.length > 0)) {
            com.codename1.ui.ClipboardContent content = new com.codename1.ui.ClipboardContent();
            if (text != null) {
                content.setData(com.codename1.ui.ClipboardContent.MIME_TEXT, text);
            }
            if (image != null) {
                content.setData(com.codename1.ui.ClipboardContent.MIME_PNG, image);
            }
            if (files != null && files.length > 0) {
                content.setData(com.codename1.ui.ClipboardContent.MIME_FILE,
                        files.length == 1 ? files[0] : files);
            }
            return content;
        }
        if (text != null) {
            Object lightweight = super.getPasteDataFromClipboard();
            if (lightweight instanceof com.codename1.ui.ClipboardContent
                    && text.equals(((com.codename1.ui.ClipboardContent) lightweight)
                            .getText(com.codename1.ui.ClipboardContent.MIME_TEXT))) {
                return lightweight;
            }
            return text;
        }
        return super.getPasteDataFromClipboard();
    }

    /* ----------------------------------------------------- shell launches
     * dial / sendSMS / sendMessage / execute all defer to the Linux shell
     * (ShellExecuteW) so they hand off to whatever handler the user actually has
     * registered (browser, dialer, Messaging, mail client). Nothing is
     * fabricated: an absent handler reports false / does nothing rather than
     * pretending to have launched, in keeping with the port's "real or
     * unsupported" rule. */

    /**
     * Opens the given URL with the default registered handler (browser for
     * http(s), the associated app for other schemes / file paths).
     */
    @Override
    public void execute(String url) {
        if (url != null) {
            LinuxNative.shellOpen(url);
        }
    }

    /**
     * Opens the Linux dialer for the number via a {@code tel:} URI. Desktops
     * without a registered dialer simply do nothing (shellOpen reports false).
     */
    @Override
    public void dial(String phoneNumber) {
        if (phoneNumber != null) {
            LinuxNative.shellOpen("tel:" + phoneNumber.trim());
        }
    }

    /**
     * Composes an SMS through the platform Messaging app via an {@code sms:} URI
     * (with the body pre-filled). This is interactive on Linux -- the user
     * confirms in the Messaging app -- so {@link #getSMSSupport()} reports
     * {@code SMS_INTERACTIVE}.
     */
    @Override
    public void sendSMS(String phoneNumber, String message, boolean interactive) throws IOException {
        String uri = "sms:" + (phoneNumber == null ? "" : phoneNumber.trim());
        if (message != null && message.length() > 0) {
            uri += "?body=" + com.codename1.io.Util.encodeUrl(message);
        }
        if (!LinuxNative.shellOpen(uri)) {
            throw new IOException("No SMS handler is registered on this device");
        }
    }

    @Override
    public int getSMSSupport() {
        return Display.SMS_INTERACTIVE;
    }

    /**
     * Opens the platform mail client through a {@code mailto:} URI carrying the
     * recipients, subject and body. Attachments are not expressible in a mailto:
     * URI, so only the textual content is passed.
     */
    /* ----------------------------------------------------------- share
     * WinRT DataTransferManager share UI (cn1_linux_winrt.cpp). Supported only
     * on a WinRT build with a window; shares the text (the common case). */

    @Override
    public boolean isNativeShareSupported() {
        // The WinRT DataTransferManager share UI is always compiled in; it just
        // needs a host window (absent only in headless screenshot mode).
        return getDisplayWidth() > 0;
    }

    @Override
    public void share(String text, String image, String mimeType, com.codename1.ui.geom.Rectangle sourceRect) {
        String title = Display.getInstance().getProperty("AppName", "Share");
        LinuxNative.shareText(text != null ? text : "", title);
    }

    @Override
    public void sendMessage(String[] recipients, String subject, com.codename1.messaging.Message msg) {
        StringBuilder uri = new StringBuilder("mailto:");
        if (recipients != null) {
            for (int i = 0; i < recipients.length; i++) {
                if (i > 0) {
                    uri.append(',');
                }
                uri.append(recipients[i]);
            }
        }
        char sep = '?';
        if (subject != null && subject.length() > 0) {
            uri.append(sep).append("subject=").append(com.codename1.io.Util.encodeUrl(subject));
            sep = '&';
        }
        String body = msg != null ? msg.getContent() : null;
        if (body != null && body.length() > 0) {
            uri.append(sep).append("body=").append(com.codename1.io.Util.encodeUrl(body));
        }
        LinuxNative.shellOpen(uri.toString());
    }

    /* --------------------------------------------------------------- printing
     * Win32 printing (cn1_linux_print.cpp): the modal system print dialog
     * (PrintDlgW) picks the printer, then the native layer rasterizes the
     * document onto the printer DC -- WIC for images, the WinRT
     * Linux.Data.Pdf renderer for PDFs. The blocking native call runs on a
     * dedicated worker thread; Display marshals the listener onto the EDT. */

    @Override
    public boolean isPrintingSupported() {
        // The Win32 print pipeline is always compiled in. A missing printer or
        // host window (headless screenshot mode) surfaces honestly as a FAILED
        // result with a message, exactly like a spooler error would.
        return true;
    }

    /**
     * Prints a document file through the Linux printing system behind the
     * native print dialog. {@code COMPLETED} means the job was handed to the
     * print spooler, {@code CANCELLED} that the user dismissed the dialog. The
     * listener fires exactly once, from the worker thread; {@code Display}
     * wraps it onto the EDT.
     */
    @Override
    public void print(final String filePath, final String mimeType, final PrintResultListener listener) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                PrintResult result;
                try {
                    result = printImpl(filePath, mimeType);
                } catch (Throwable err) {
                    err.printStackTrace();
                    result = PrintResult.failed("Print failed: " + err);
                }
                if (listener != null) {
                    listener.onResult(result);
                }
            }
        }, "cn1-linux-print");
        t.setDaemon(true);
        t.start();
    }

    private PrintResult printImpl(String filePath, String mimeType) {
        if (filePath == null) {
            return PrintResult.failed("Print file path is null");
        }
        String path = stripFileUrl(filePath);
        if (!LinuxNative.fileExists(path) || LinuxNative.fileIsDirectory(path)) {
            return PrintResult.failed("Print file not found: " + filePath);
        }
        boolean pdf = "application/pdf".equals(mimeType);
        if (!pdf && (mimeType == null || !mimeType.startsWith("image/"))) {
            // Reject before any UI: showing the print dialog for a document we
            // can't render would waste the user's choice.
            return PrintResult.failed("Unsupported print mime type: " + mimeType);
        }
        String jobName = Display.getInstance().getProperty("AppName", "Codename One");
        int status = LinuxNative.printDocument(path, mimeType, jobName);
        if (status == 0) {
            return PrintResult.completed();
        }
        if (status == 1) {
            return PrintResult.cancelled();
        }
        String error = LinuxNative.printLastError();
        return PrintResult.failed(error != null ? error : "Print failed");
    }

    /* -------------------------------------------------- native file picker
     * The desktop file picker is the honest gallery on Linux: a real
     * IFileOpenDialog-class common dialog filtered to the requested media type,
     * rather than the framework's in-app FileTree fallback. The native dialog is
     * modal and returns the chosen path synchronously, which is delivered through
     * the listener exactly like every other port. Multi-select gallery types are
     * not offered here (isGalleryTypeSupported reports them unsupported), so they
     * keep the cross-platform fallback. */

    @Override
    public void openGallery(com.codename1.ui.events.ActionListener response, int type) {
        if (!isGalleryTypeSupported(type)) {
            throw new IllegalArgumentException("Gallery type " + type + " not supported on this platform.");
        }
        String title = type == Display.GALLERY_VIDEO ? "Select a video" : "Select a picture";
        String path = LinuxNative.fileDialog(false, type, title);
        com.codename1.ui.events.ActionEvent result = null;
        if (path != null) {
            // Hand back a file:// URL the port's FileSystemStorage understands
            // (openInputStream strips the scheme); forward slashes keep it a valid
            // path for the native CreateFileW calls.
            result = new com.codename1.ui.events.ActionEvent("file://" + path.replace('\\', '/'));
        }
        response.actionPerformed(result);
    }

    @Override
    public void openImageGallery(com.codename1.ui.events.ActionListener response) {
        openGallery(response, Display.GALLERY_IMAGE);
    }

    @Override
    public boolean isTouchDevice() {
        return LinuxNative.isTouchDevice();
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
        long c = LinuxNative.httpOpen(url, read, write);
        if (c == 0) {
            throw new IOException("Unable to open connection to " + url);
        }
        return new LinuxHttpConnection(c);
    }

    @Override
    public void setHeader(Object connection, String key, String val) {
        LinuxNative.httpSetHeader(((LinuxHttpConnection) connection).peer, key, val);
    }

    @Override
    public void setPostRequest(Object connection, boolean p) {
        LinuxNative.httpSetMethod(((LinuxHttpConnection) connection).peer, p);
    }

    @Override
    public int getContentLength(Object connection) {
        return LinuxNative.httpContentLength(((LinuxHttpConnection) connection).peer);
    }

    @Override
    public OutputStream openOutputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            String path = stripFileUrl((String) connection);
            return new LinuxOutputStream(openForWrite(path, false), false);
        }
        return new LinuxOutputStream(((LinuxHttpConnection) connection).peer, true);
    }

    @Override
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        // offset-based writing maps to opening the file for append/seek; the
        // first cut appends, which covers the common resume-write case.
        String path = stripFileUrl((String) connection);
        return new LinuxOutputStream(openForWrite(path, true), false);
    }

    @Override
    public InputStream openInputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            String path = stripFileUrl((String) connection);
            long h = LinuxNative.fileOpenRead(path);
            if (h == 0) {
                // fopen() returns NULL for a missing (or unreadable) path.
                // Wrapping that handle produced a stream that read as a
                // legitimately empty file, so callers could not tell a missing
                // file from an empty one -- the exact defect issue #1502
                // reported against iOS.
                throw new FileNotFoundException("No such file: " + path
                        + " (" + LinuxNative.lastIoError() + ")");
            }
            return new LinuxInputStream(h, false);
        }
        return new LinuxInputStream(((LinuxHttpConnection) connection).peer, true);
    }

    /// Opens `path` for writing, failing loudly when the platform cannot. A
    /// null handle otherwise yields a stream that discards every write and
    /// closes cleanly, which turns an unwritable path into a file that simply
    /// never appears.
    private long openForWrite(String path, boolean append) throws IOException {
        long h = LinuxNative.fileOpenWrite(path, append);
        if (h == 0) {
            throw new IOException("Unable to open " + path + " for writing ("
                    + LinuxNative.lastIoError() + ")");
        }
        return h;
    }

    /**
     * Resolves a classpath-style resource (e.g. {@code /theme.res}). The ParparVM
     * linux target embeds the app's classpath resources into the executable's
     * data section, so they are served straight from the ELF -- a single
     * self-contained binary, the Linux analog of the iOS .app bundle. Falls back
     * to a file shipped next to the executable (a dev/debug convenience for
     * resources that were staged rather than embedded). Returns null when absent.
     */
    @Override
    public InputStream getResourceAsStream(Class cls, String resource) {
        if (resource == null) {
            return null;
        }
        byte[] embedded = LinuxNative.resourceBytes(resource);
        if (embedded != null) {
            return new ByteArrayInputStream(embedded);
        }
        String dir = LinuxNative.executableDir();
        if (dir == null) {
            return null;
        }
        // Classpath resources are already '/'-separated, which is what the
        // filesystem wants here; this port was carrying the Windows port's
        // backslash join, so the staged-resource fallback never resolved.
        String name = resource.startsWith("/") ? resource.substring(1) : resource;
        String path = dir + "/" + name;
        long h = LinuxNative.fileOpenRead(path);
        if (h == 0) {
            return null;
        }
        return new LinuxInputStream(h, false);
    }

    @Override
    public int getResponseCode(Object connection) throws IOException {
        return LinuxNative.httpResponseCode(((LinuxHttpConnection) connection).peer);
    }

    @Override
    public String getResponseMessage(Object connection) throws IOException {
        return LinuxNative.httpResponseMessage(((LinuxHttpConnection) connection).peer);
    }

    @Override
    public String getHeaderField(String name, Object connection) throws IOException {
        return LinuxNative.httpHeaderField(((LinuxHttpConnection) connection).peer, name);
    }

    @Override
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        return LinuxNative.httpHeaderFieldNames(((LinuxHttpConnection) connection).peer);
    }

    @Override
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        String value = LinuxNative.httpHeaderField(((LinuxHttpConnection) connection).peer, name);
        if (value == null) {
            return null;
        }
        return new String[] { value };
    }

    @Override
    public void cleanup(Object o) {
        if (o instanceof LinuxHttpConnection) {
            LinuxNative.httpClose(((LinuxHttpConnection) o).peer);
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
        LinuxSocket socket = new LinuxSocket(host, port, connectTimeout);
        return socket.isConnected() ? socket : null;
    }

    @Override
    public String getHostOrIP() {
        return LinuxNative.getHostOrIP();
    }

    @Override
    public void disconnectSocket(Object socket) {
        ((LinuxSocket) socket).close();
    }

    @Override
    public boolean isSocketConnected(Object socket) {
        return socket instanceof LinuxSocket && ((LinuxSocket) socket).isConnected();
    }

    @Override
    public int getSocketAvailableInput(Object socket) {
        return ((LinuxSocket) socket).available();
    }

    @Override
    public byte[] readFromSocketStream(Object socket) {
        return ((LinuxSocket) socket).readChunk();
    }

    @Override
    public void writeToSocketStream(Object socket, byte[] data) {
        ((LinuxSocket) socket).write(data, 0, data.length);
    }

    @Override
    public String getSocketErrorMessage(Object socket) {
        return ((LinuxSocket) socket).getErrorMessage();
    }

    @Override
    public int getSocketErrorCode(Object socket) {
        return ((LinuxSocket) socket).getErrorCode();
    }

    /* ---------------------------------------------------------- websocket */

    @Override
    public boolean isWebSocketSupported() {
        return true;
    }

    @Override
    public WebSocketImpl createWebSocketImpl(String url) {
        return new LinuxWebSocketImpl(url);
    }

    /* --------------------------------------------------------- video I/O */

    private com.codename1.media.VideoIO videoIO;
    private boolean videoIOResolved;

    @Override
    public com.codename1.media.VideoIO getVideoIO() {
        if (!videoIOResolved) {
            videoIOResolved = true;
            try {
                if (LinuxNative.videoBackendAvailable()) {
                    videoIO = new LinuxVideoIO();
                }
            } catch (Throwable t) {
                videoIO = null;
            }
        }
        return videoIO;
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
                    byte[] png = LinuxNative.encodeArgbToPng(rgb, img.getWidth(), img.getHeight());
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
        LinuxNative.fileDelete(storagePath(name));
    }

    @Override
    public OutputStream createStorageOutputStream(String name) throws IOException {
        // Same reason as openOutputStream: a discarded write that reports
        // success loses the entry instead of reporting that it cannot be saved.
        return new LinuxOutputStream(openForWrite(storagePath(name), false), false);
    }

    @Override
    public InputStream createStorageInputStream(String name) throws IOException {
        String path = storagePath(name);
        long h = LinuxNative.fileOpenRead(path);
        if (h == 0) {
            throw new FileNotFoundException("No such storage entry: " + name
                    + " (" + LinuxNative.lastIoError() + ")");
        }
        return new LinuxInputStream(h, false);
    }

    @Override
    public boolean storageFileExists(String name) {
        return LinuxNative.fileExists(storagePath(name));
    }

    @Override
    public String[] listStorageEntries() {
        return LinuxNative.fileList(LinuxNative.storageDir());
    }

    @Override
    public String[] listFilesystemRoots() {
        return LinuxNative.fileRoots();
    }

    /**
     * Anchors the app home at the per-user storage directory.
     *
     * The base getAppHomePath() builds {@code listFilesystemRoots()[0] + AppName},
     * which here is the real filesystem root "/" + the app name -- unwritable, and
     * literally "/null/" when neither an AppName property nor a packageName is set.
     * That made {@code new File("x").getAbsolutePath()} resolve to "/null/x" (which
     * is exactly why a recorded "tmpaudio.wav" came back as file:///null/tmpaudio.wav
     * and would not play). Use the same writable per-user directory that Storage and
     * capturePhoto already rely on.
     *
     * The result carries the {@code file://} scheme, as it does on Android and
     * iOS. {@link com.codename1.io.File} treats any path without that scheme as
     * relative to the app home and prepends the home to it, so returning a bare
     * path made {@code new File(fs.getAppHomePath() + "x")} resolve to the home
     * directory joined to itself -- every write through that class landed on a
     * path that could not exist.
     */
    /// A stable, filesystem-safe directory name for THIS application.
    ///
    /// storageDir() names the Codename One directory shared by every CN1 app
    /// under this user account, so returning it as the app home gave two
    /// applications the same getAppHomePath(): each could read and overwrite
    /// the other's files. The base implementation appends
    /// getProperty("AppName", packageName) for exactly this reason; the reason
    /// this override exists at all is that the base builds it on an unwritable
    /// filesystem root, not that the per-app component was wrong.
    ///
    /// Never returns the literal "null": an unset AppName and packageName is
    /// what made the base implementation produce "/null/" in the first place.
    /// The application's package, as the build stamped it.
    ///
    /// Read through Display rather than the implementation's own getProperty: the generated
    /// stub publishes it with setProperty, and only Display holds what was set that way. The
    /// implementation's own accessor answers from the value derived during initImpl, which a
    /// native desktop build never has -- its stub calls Display.init(null), so there is no
    /// object to take a package from.
    ///
    /// #### Returns
    ///
    /// the package name, or null when the build did not stamp one
    private String packageIdentity() {
        try {
            return com.codename1.ui.Display.getInstance().getProperty("package_name", null);
        } catch (RuntimeException tooEarly) {
            // Asked before Display is ready. The caller falls back, and the next call answers.
            return null;
        }
    }

    private String appHomeDirName() {
        // The package first, because on this platform the directory name IS the isolation
        // boundary. Android and iOS put the app home inside an OS sandbox, so two applications
        // cannot reach each other whatever the directory is called, and the simulator
        // deliberately shares one home so several projects can be run from one workspace. A
        // native desktop build has neither: it is a plain directory under the user account, and
        // anything that can be named twice is a directory two applications share.
        //
        // AppName is a display name -- two vendors can both ship "Notes", and the sanitizer below
        // maps several reserved characters onto "_", so names that differ can still collide. The
        // package is what the store, the installer and the build all treat as the application's
        // identity, so it is what this keys on. AppName remains the fallback for a build that
        // does not carry one.
        //
        // Nothing is migrated out of a directory an earlier build used. Every native desktop
        // build until now landed on the same name, so what is in there cannot be attributed to
        // one application, and moving it would hand one application another's files.
        String name = packageIdentity();
        if (name == null || name.length() == 0) {
            name = getProperty("AppName", null);
        }
        if (name == null || name.length() == 0) {
            name = getPackageName();
        }
        if (name == null || name.length() == 0 || "null".equals(name)) {
            return "CN1App";
        }
        StringBuilder safe = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            // Reserved on Windows and awkward everywhere else; NTFS and ext4
            // both accept the rest of the printable range.
            if (c < ' ' || c == '\\' || c == '/' || c == ':' || c == '*' || c == '?'
                    || c == '"' || c == '<' || c == '>' || c == '|') {
                safe.append('_');
            } else {
                safe.append(c);
            }
        }
        return safe.toString();
    }

    @Override
    public String getAppHomePath() {
        String dir = LinuxNative.storageDir();
        if (dir == null || dir.length() == 0) {
            dir = "/tmp";
        }
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        dir += appHomeDirName() + "/";
        String home = "file://" + dir;
        if (!exists(home)) {
            mkdir(home);
        }
        return home;
    }

    @Override
    public String toNativePath(String path) {
        return stripFileUrl(path);
    }

    @Override
    public String[] listFiles(String directory) throws IOException {
        return LinuxNative.fileList(stripFileUrl(directory));
    }

    @Override
    public long getRootSizeBytes(String root) {
        return LinuxNative.fileRootSize(root);
    }

    @Override
    public long getRootAvailableSpace(String root) {
        return LinuxNative.fileRootFree(root);
    }

    @Override
    public void mkdir(String directory) {
        LinuxNative.fileMkdir(stripFileUrl(directory));
    }

    @Override
    public void deleteFile(String file) {
        LinuxNative.fileDelete(stripFileUrl(file));
    }

    @Override
    public boolean isHidden(String file) {
        return LinuxNative.fileIsHidden(stripFileUrl(file));
    }

    @Override
    public void setHidden(String file, boolean h) {
        LinuxNative.fileSetHidden(stripFileUrl(file), h);
    }

    @Override
    public long getFileLength(String file) {
        return LinuxNative.fileLength(stripFileUrl(file));
    }

    @Override
    public boolean isDirectory(String file) {
        return LinuxNative.fileIsDirectory(stripFileUrl(file));
    }

    @Override
    public boolean exists(String file) {
        return LinuxNative.fileExists(stripFileUrl(file));
    }

    @Override
    public void rename(String file, String newName) {
        LinuxNative.fileRename(stripFileUrl(file), newName);
    }

    @Override
    public char getFileSystemSeparator() {
        return '/';
    }

    /* ------------------------------------------------------------ crypto */

    /**
     * The crypto bridge, backed by OpenSSL. Every failure is reported as a
     * RuntimeException carrying the library's own reason, which
     * {@code com.codename1.security.Cipher} turns into a CryptoException --
     * an authentication failure has to be an exception rather than an empty
     * result, or a tampered message would read as an empty plaintext.
     */
    private static byte[] cryptoResult(byte[] value, String operation) {
        if (value == null) {
            throw new RuntimeException(operation + " failed: " + LinuxNative.lastCryptoError());
        }
        return value;
    }

    @Override
    public void secureRandomBytes(byte[] out) {
        // Fail loudly: KeyGenerator hands this buffer straight back as key
        // material, so a quiet return after the platform RNG failed would mint
        // a predictable key.
        if (out != null && out.length > 0 && !LinuxNative.secureRandomBytes(out)) {
            throw new RuntimeException("secure random failed: " + LinuxNative.lastCryptoError());
        }
    }

    /// Rejects an initialization vector the mode cannot use. A GCM nonce that
    /// is absent repeats across messages under one key, and a short CBC IV is
    /// read as a whole block by the platform library.
    /// AAD only binds to the ciphertext under an AEAD mode. CBC and ECB ignore it
    /// entirely, so passing it here quietly dropped data the caller believed was
    /// authenticated -- and produced ciphertext the other ports reject, because
    /// JavaSE and Android route the same call through Cipher.updateAAD, which
    /// refuses a non-AEAD mode. Refusing it keeps the ports answering alike and
    /// keeps a caller from believing in a binding that was never made.
    private static void checkAad(String transformation, byte[] aad) {
        if (aad == null || aad.length == 0) {
            return;
        }
        String mode = transformation == null ? "" : transformation;
        if (mode.indexOf("/GCM/") < 0) {
            throw new RuntimeException(
                    "Additional authenticated data requires an AEAD mode; " + mode
                            + " cannot bind it");
        }
    }

    private static void checkIv(String transformation, byte[] iv) {
        String mode = transformation == null ? "" : transformation;
        if (mode.indexOf("/GCM/") >= 0) {
            if (iv == null || iv.length == 0) {
                throw new RuntimeException("AES-GCM requires a nonce");
            }
        } else if (mode.indexOf("/ECB/") >= 0) {
            // ECB has no IV. The native ignores one, so passing it produced
            // ciphertext while quietly discarding a parameter the caller thought
            // mattered -- and JavaSE and Android reject the same call, because
            // they hand a non-null iv to JCE as an IvParameterSpec and ECB
            // refuses it. Refusing here keeps the ports answering alike.
            // Any non-null array, including a zero-length one: JavaSE branches
            // on iv != null rather than on its length, so new byte[0] builds an
            // IvParameterSpec there and ECB throws. Accepting it here would have
            // made the same call succeed on the desktop ports alone.
            if (iv != null) {
                throw new RuntimeException("AES-ECB cannot use an initialization vector");
            }
        } else if (iv == null || iv.length != 16) {
            throw new RuntimeException("AES-CBC requires a 16 byte initialization vector");
        }
    }

    @Override
    public byte[] aesEncrypt(String transformation, byte[] key, byte[] iv, byte[] aad, byte[] plaintext) {
        checkIv(transformation, iv);
        checkAad(transformation, aad);
        return cryptoResult(LinuxNative.aesCrypt(transformation, true, key, iv, aad, plaintext),
                "AES encrypt");
    }

    @Override
    public byte[] aesDecrypt(String transformation, byte[] key, byte[] iv, byte[] aad, byte[] ciphertext) {
        checkIv(transformation, iv);
        checkAad(transformation, aad);
        return cryptoResult(LinuxNative.aesCrypt(transformation, false, key, iv, aad, ciphertext),
                "AES decrypt");
    }

    @Override
    public byte[] rsaEncrypt(String transformation, byte[] publicKeyX509, byte[] plaintext) {
        return cryptoResult(LinuxNative.rsaCrypt(transformation, true, publicKeyX509, plaintext),
                "RSA encrypt");
    }

    @Override
    public byte[] rsaDecrypt(String transformation, byte[] privateKeyPkcs8, byte[] ciphertext) {
        return cryptoResult(LinuxNative.rsaCrypt(transformation, false, privateKeyPkcs8, ciphertext),
                "RSA decrypt");
    }

    @Override
    public byte[] cryptoSign(String algorithm, String keyAlgorithm, byte[] privateKeyPkcs8, byte[] data) {
        checkKeyFamily(algorithm, keyAlgorithm);
        return cryptoResult(LinuxNative.signData(algorithm, privateKeyPkcs8, data), "sign");
    }

    @Override
    public boolean cryptoVerify(String algorithm, String keyAlgorithm, byte[] publicKeyX509,
            byte[] data, byte[] signature) {
        checkKeyFamily(algorithm, keyAlgorithm);
        // An invalid signature and an unusable algorithm or key both come back
        // as false from the native. Only the second is a configuration error,
        // and JavaSE and Android raise it -- Signature.verify turns the throw
        // into a CryptoException -- so answering plain false here would let a
        // mistyped algorithm or malformed key read as "someone tampered with
        // this". Clearing the slot first is what makes the two distinguishable.
        LinuxNative.clearCryptoError();
        boolean verified = LinuxNative.verifyData(algorithm, publicKeyX509, data, signature);
        if (!verified) {
            String failure = LinuxNative.lastCryptoError();
            if (failure != null && failure.length() > 0
                    && !"unknown crypto error".equals(failure)) {
                throw new RuntimeException("verify failed: " + failure);
            }
        }
        return verified;
    }

    /// The portable contract pairs an algorithm with a key of its own family,
    /// and JavaSE rejects a mismatch when the Signature is initialised. The
    /// native here reads the family off the DER key and takes only the digest
    /// from the algorithm name, so `SHA256withRSA` handed an EC key would
    /// quietly produce an ECDSA signature -- and the matching verify would
    /// accept it, so nothing looks wrong until another port reads it. Refuse
    /// the pairing rather than silently substituting the algorithm.
    private static void checkKeyFamily(String algorithm, String keyAlgorithm) {
        if (algorithm == null || keyAlgorithm == null) {
            return;
        }
        // The label has to be one this runtime actually knows, not merely one
        // whose prefix looks familiar. startsWith("EC") called "ECfoo" an EC key
        // and every other string an RSA one, so PrivateKey.fromPkcs8("garbage",
        // der) signed happily here while JavaSE and Android hand the same label
        // to KeyFactory.getInstance and throw NoSuchAlgorithmException. Identical
        // public API calls must not depend on the port.
        String key = keyAlgorithm.toUpperCase();
        if (!"RSA".equals(key) && !"EC".equals(key)) {
            throw new RuntimeException("Unknown key algorithm: " + keyAlgorithm);
        }
        boolean wantsEc = algorithm.toUpperCase().indexOf("ECDSA") >= 0;
        boolean keyIsEc = "EC".equals(key);
        if (wantsEc != keyIsEc) {
            throw new RuntimeException(algorithm + " cannot be used with a "
                    + keyAlgorithm + " key");
        }
    }

    @Override
    public byte[][] generateRsaKeyPair(int bits) {
        byte[] blob = cryptoResult(LinuxNative.generateRsaKeyPair(bits), "RSA key generation");
        if (blob.length < 4) {
            throw new RuntimeException("RSA key generation returned a truncated pair");
        }
        int publicLength = ((blob[0] & 0xff) << 24) | ((blob[1] & 0xff) << 16)
                | ((blob[2] & 0xff) << 8) | (blob[3] & 0xff);
        if (publicLength < 0 || publicLength > blob.length - 4) {
            throw new RuntimeException("RSA key generation returned a malformed pair");
        }
        byte[] publicKey = new byte[publicLength];
        byte[] privateKey = new byte[blob.length - 4 - publicLength];
        System.arraycopy(blob, 4, publicKey, 0, publicKey.length);
        System.arraycopy(blob, 4 + publicLength, privateKey, 0, privateKey.length);
        return new byte[][] { publicKey, privateKey };
    }

    /* ------------------------------------------------------------ platform */

    @Override
    public String getPlatformName() {
        return "linux";
    }

    @Override
    public String getNativeLogSnapshot() {
        try {
            return LinuxNative.crashProtectionLogSnapshot();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void installNativeCrashHandler() {
        try {
            LinuxNative.crashProtectionInstall();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public String consumePendingNativeCrash() {
        try {
            return LinuxNative.crashProtectionConsumePending();
        } catch (Throwable ignored) {
            return null;
        }
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
        long peer = LinuxNative.mediaCreate(data, data.length, mimeType);
        if (peer == 0) {
            return null;
        }
        return new LinuxMedia(peer, onCompletion);
    }

    /**
     * URL-based playback. Hands the URI straight to GStreamer's playbin, which
     * resolves remote (http/https) sources via souphttpsrc -- so streaming works
     * without buffering the whole asset into memory first -- and plays local files
     * in place. A bare filesystem path is normalised to a file:// URI. Returns null
     * only when the engine can't be created (e.g. GStreamer not installed).
     */
    @Override
    public Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        if (uri == null) {
            return null;
        }
        String u = uri.indexOf("://") >= 0 ? uri : ("file://" + uri.replace('\\', '/'));
        long peer = LinuxNative.mediaCreateUri(u);
        if (peer == 0) {
            return null;
        }
        return new LinuxMedia(peer, onCompletion);
    }

    /**
     * Self-contained audio capture UI. The shared {@code AudioRecorderComponent}
     * (LayeredLayout + icon buttons) renders blank in this port's Sheet, so the
     * Linux port provides its own minimal Record/Done/Cancel sheet driven directly
     * by the GStreamer-backed {@link LinuxAudioRecorder}. The result path (or null
     * when cancelled/closed) is delivered exactly once via {@code response} so the
     * blocking {@code Capture.captureAudio()} wrapper always returns.
     */
    @Override
    public void captureAudio(final com.codename1.media.MediaRecorderBuilder recordingOptions,
            final com.codename1.ui.events.ActionListener response) {
        final com.codename1.media.MediaRecorderBuilder builder =
                recordingOptions == null ? new com.codename1.media.MediaRecorderBuilder() : recordingOptions;
        if (builder.getPath() == null) {
            builder.path(LinuxNative.storageDir() + getFileSystemSeparator()
                    + "cn1rec" + System.currentTimeMillis() + ".wav");
        }
        if (builder.getMimeType() == null) {
            builder.mimeType("audio/wav");
        }
        final Media[] rec = new Media[1];
        final String[] result = new String[1];        // null => cancelled
        final boolean[] delivered = new boolean[1];
        final com.codename1.ui.Sheet sheet = new com.codename1.ui.Sheet(null, "Record Audio");
        sheet.getContentPane().setLayout(com.codename1.ui.layouts.BoxLayout.y());
        final com.codename1.ui.Label status = new com.codename1.ui.Label("Press Record to start");
        final com.codename1.ui.Button recordBtn = new com.codename1.ui.Button("Record");
        final com.codename1.ui.Button cancelBtn = new com.codename1.ui.Button("Cancel");
        sheet.getContentPane().add(status).add(recordBtn).add(cancelBtn);

        recordBtn.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                try {
                    if (rec[0] == null) {
                        rec[0] = createMediaRecorder(builder);
                        if (rec[0] == null) {
                            status.setText("Recorder unavailable (GStreamer not installed?)");
                            sheet.getContentPane().revalidate();
                            return;
                        }
                        rec[0].play();
                        status.setText("Recording... press Done to finish");
                        recordBtn.setText("Done");
                        sheet.getContentPane().revalidate();
                    } else {
                        // Finish: the close listener finalizes the file and delivers.
                        result[0] = "file://" + builder.getPath();
                        sheet.back();
                    }
                } catch (Throwable t) {
                    status.setText("Error: " + t);
                    sheet.getContentPane().revalidate();
                }
            }
        });
        cancelBtn.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                result[0] = null;
                sheet.back();
            }
        });
        // Single delivery point: covers Done, Cancel, and the sheet's own close (X).
        sheet.addCloseListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                if (delivered[0]) {
                    return;
                }
                delivered[0] = true;
                if (rec[0] != null) {
                    try {
                        rec[0].pause();
                        rec[0].cleanup();   // sends EOS so wavenc finalizes the header
                    } catch (Throwable t) {
                        // ignore -- still deliver whatever we have
                    }
                }
                response.actionPerformed(result[0] != null
                        ? new com.codename1.ui.events.ActionEvent(result[0]) : null);
            }
        });
        sheet.show();
    }

    /* ---------------------------------------------------- audio recording
     * waveIn-backed PCM WAV recorder (cn1_linux_audiorec.c). createMediaRecorder
     * returns a Media whose play() starts capturing from the default microphone
     * and pause()/cleanup() finalizes the file. */

    @Override
    public Media createMediaRecorder(String path, String mimeType) throws IOException {
        if (path == null) {
            return null;
        }
        return new LinuxAudioRecorder(path, 0, 0);
    }

    @Override
    public Media createMediaRecorder(com.codename1.media.MediaRecorderBuilder builder) throws IOException {
        if (builder == null || builder.getPath() == null) {
            return null;
        }
        return new LinuxAudioRecorder(builder.getPath(), builder.getSamplingRate(), builder.getAudioChannels());
    }

    @Override
    public String[] getAvailableRecordingMimeTypes() {
        // waveIn produces 16-bit PCM WAV, which the port's MF playback also decodes.
        return new String[]{"audio/wav"};
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

    // ---------------------------------------------------------------- database

    /**
     * Resolves a database name to an absolute path. Bare names live in a "database" directory
     * under the application's storage directory; a file:// URL is resolved through
     * FileSystemStorage, matching the other ports that support custom paths.
     */
    private String resolveDatabasePath(String databaseName) {
        if (databaseName.startsWith("file://")) {
            return com.codename1.io.FileSystemStorage.getInstance().toNativePath(databaseName);
        }
        // Under this application's own directory, not storageDir() alone: that one names the
        // Codename One directory shared by every CN1 application under this user account, so two
        // applications opening "app.db" opened one file -- each able to read the other's rows and
        // overwrite them. getAppHomePath() adds the per-application component for the same reason.
        return LinuxNative.storageDir() + "/" + appHomeDirName() + "/database/" + databaseName;
    }

    @Override
    public com.codename1.db.Database openOrCreateDB(String databaseName) throws java.io.IOException {
        return openOrCreateDB(databaseName, null);
    }

    @Override
    public com.codename1.db.Database openOrCreateDB(String databaseName,
            com.codename1.db.DatabaseConfig config) throws java.io.IOException {
        String path = resolveDatabasePath(databaseName);
        int sep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (sep > 0) {
            makeDirectories(path.substring(0, sep));
        }
        String key = null;
        if (config != null && config.isEncrypted()) {
            // The resolved file, not the name it was asked for: a managed key with no explicit
            // alias is stored under what is passed here, so two accepted spellings of one database
            // would derive two different keys and the second open would report a wrong key against
            // intact data.
            key = config.resolveKeyMaterial(LinuxDatabase.registryKeyFor(path));
        }
        return new LinuxDatabase(databaseName, path, key);
    }

    /// Creates a directory and every parent of it that is missing.
    ///
    /// mkdir() is one level: the native call behind it is a bare mkdir/CreateDirectoryW, which
    /// fails when the parent is not there and reports nothing. The database lives two levels
    /// under the storage directory -- the per-application directory, then "database" -- and on a
    /// first run neither exists, so a single mkdir silently did nothing and the open that
    /// followed failed with SQLite unable to create the file.
    ///
    /// #### Parameters
    ///
    /// - `nativePath`: the directory to create, as a native path
    private void makeDirectories(String nativePath) {
        int from = 0;
        // Past the root, so the first component asked for is a real directory name rather than
        // "/", which is a call that can only fail.
        //
        // No drive-letter handling here, deliberately. This started as one helper written for
        // both native desktop ports, and skipping everything before a colon is right on Windows
        // and wrong here: a colon is an ordinary character in a Linux directory name, so
        // /tmp/missing/tag:one/app.db had every component before "tag:one" skipped and its parent
        // was never created. The two ports have their own copies; each keeps the rule its
        // filesystem actually has.
        while (from < nativePath.length() && nativePath.charAt(from) == '/') {
            from++;
        }
        for (int iter = from; iter <= nativePath.length(); iter++) {
            boolean end = iter == nativePath.length();
            if (!end && nativePath.charAt(iter) != '/' && nativePath.charAt(iter) != '\\') {
                continue;
            }
            String upTo = nativePath.substring(0, iter);
            if (upTo.length() > 0 && !exists("file://" + upTo)) {
                mkdir("file://" + upTo);
            }
        }
    }

    @Override
    public boolean isDatabaseCustomPathSupported() {
        return true;
    }

    /// The file an implicit managed key is stored under; see the open path, which resolves the
    /// same way so two spellings of one database derive one key.
    @Override
    public String databaseManagedKeyIdentity(String databaseName) {
        return LinuxDatabase.registryKeyFor(resolveDatabasePath(databaseName));
    }

    @Override
    public boolean isDatabaseEncryptionSupported() {
        return LinuxNative.sqlDbIsCipherAvailable();
    }

    @Override
    public boolean isBlobQueryParameterSupported() {
        return true;
    }

    @Override
    public void deleteDB(String databaseName) throws java.io.IOException {
        String path = resolveDatabasePath(databaseName);
        // The companions first, so removing the database itself is the last destructive step: a
        // failure before it leaves a database the caller can really delete again, rather than an
        // error reported over a database that is already gone. See databaseSidecarPaths.
        // A name that is not there is the documented no-op in the native binding.
        String[] sidecars = databaseSidecarPaths(path);
        for (int iter = 0; iter < sidecars.length; iter++) {
            LinuxNative.sqlDbDelete(sidecars[iter]);
        }
        LinuxNative.sqlDbDelete(path);
    }

    @Override
    public boolean existsDB(String databaseName) {
        return LinuxNative.sqlDbExists(resolveDatabasePath(databaseName));
    }

    @Override
    public String getDatabasePath(String databaseName) {
        if (databaseName.startsWith("file://")) {
            return databaseName;
        }
        return resolveDatabasePath(databaseName);
    }
}
