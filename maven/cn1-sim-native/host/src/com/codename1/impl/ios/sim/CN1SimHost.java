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

import java.awt.Canvas;
import java.io.File;

/**
 * Native bridge host for the iOS simulator backend: loads libcn1sim.dylib
 * (which registers the IOSNative JNI shims and this class's natives) and
 * attaches the Metal rendering surface to an AWT heavyweight canvas via JAWT.
 */
public class CN1SimHost {
    private static boolean loaded;

    private CN1SimHost() {
    }

    /**
     * Loads the native simulator library. The path comes from the
     * cn1.sim.native.path system property when set; otherwise the library is
     * resolved relative to the repository layout (maven/cn1-sim-native/target).
     * Must be called before Display.init with the iOS backend.
     */
    public static synchronized void load() {
        if (loaded) {
            return;
        }
        String path = System.getProperty("cn1.sim.native.path");
        if (path == null) {
            throw new IllegalStateException(
                    "cn1.sim.native.path is not set; point it at libcn1sim.dylib "
                            + "(build with scripts/cn1-sim-native/build-libcn1sim-mac.sh)");
        }
        File f = new File(path);
        if (!f.exists()) {
            throw new IllegalStateException("Native simulator library not found: " + f.getAbsolutePath());
        }
        // JAWT must be resident in the process before the dylib calls
        // JAWT_GetAWT (JDK 9+ no longer loads it implicitly)
        try {
            System.loadLibrary("jawt");
        } catch (UnsatisfiedLinkError e) {
            File jawt = new File(System.getProperty("java.home"), "lib/libjawt.dylib");
            if (jawt.exists()) {
                System.load(jawt.getAbsolutePath());
            }
        }
        // the Metal pipeline loads its shaders from this library file
        if (System.getenv("CN1_SIM_METALLIB") == null) {
            File metallib = new File(f.getParentFile(), "cn1sim.metallib");
            if (metallib.exists()) {
                // setenv is not possible from Java; the launcher script exports
                // CN1_SIM_METALLIB - this is just a helpful diagnostic
                System.err.println("CN1SimHost: CN1_SIM_METALLIB not set; expected "
                        + metallib.getAbsolutePath() + " - text/shape rendering will fail");
            }
        }
        System.load(f.getAbsolutePath());
        loaded = true;
    }

    /**
     * @return true once the native library has been loaded
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Attaches the native rendering surface to the given heavyweight canvas
     * via JAWT. Transitional API for the AWT-hosted mode; the pure mode uses
     * {@link #createNativeWindow} instead.
     *
     * @param canvas the AWT canvas hosting the rendered screen
     * @param width surface width in pixels
     * @param height surface height in pixels
     */
    public static native void attachSurface(Canvas canvas, int width, int height);

    /**
     * Resizes the native rendering surface (screen texture + drawable size).
     *
     * @param width new width in pixels
     * @param height new height in pixels
     */
    public static native void resizeSurface(int width, int height);

    /* ---- pure-native mode (no AWT at all) -------------------------------- */

    /**
     * Creates the native NSWindow hosting the rendering layer. Safe to call
     * from any thread; the window materializes once {@link #runEventLoop}
     * is draining the AppKit main queue.
     *
     * @param title the window title
     * @param width content width in pixels
     * @param height content height in pixels
     */
    public static native void createNativeWindow(String title, int width, int height);

    /**
     * Runs the AppKit event loop on the calling thread, which must be the
     * process main thread: launch the JVM with -XstartOnFirstThread and call
     * this from main() after spawning the application logic on another
     * thread. Blocks for the lifetime of the simulator.
     */
    public static native void runEventLoop();

    /**
     * Resizes the native window and rendering surface (rotation / skin
     * change). Synchronous: returns once the new surface is live, so repaints
     * issued afterwards land on it. Both universes must repaint afterwards.
     */
    public static native void resizeWindow(int width, int height);

    /**
     * Keeps the simulator window above other applications.
     */
    public static native void setAlwaysOnTop(boolean onTop);

    /**
     * Starts a native text-editing session: floats an NSTextField over the
     * given window rectangle. Commits (Enter / focus loss) arrive through
     * {@link #nativeEditingDone}.
     *
     * @param text initial text
     * @param x window-coordinate rectangle of the edited component
     * @param fontPx the CN1 font height, used to size the native font
     */
    public static native void editString(String text, int x, int y, int w, int h,
            long fontPeer, int fgColor, boolean multiline);

    /**
     * Called from the native text field when an editing session commits.
     */
    static void nativeEditingDone(String text) {
        com.codename1.impl.ios.sim.bridge.EditingCallback cb =
                com.codename1.impl.ios.sim.bridge.BridgeRegistry.takeEditingCallback();
        if (cb != null) {
            cb.editingDone(text);
        }
    }

    /* ---- native peers (browser, media) + file picker ---------------------- */

    public static native long peerCreateWebView();

    public static native void peerWebLoadURL(long peer, String url);

    public static native void peerWebLoadHTML(long peer, String html, String baseUrl);

    /**
     * Positions a peer's native view at the given window rectangle, adding
     * it to the window on first use.
     */
    public static native void peerSetFrame(long peer, int x, int y, int w, int h);

    public static native void peerRemove(long peer);

    public static native void peerRelease(long peer);

    /**
     * Creates an AVPlayer-backed media peer; video peers carry an
     * AVPlayerView positionable via {@link #peerSetFrame}.
     */
    public static native long mediaCreate(String url, boolean video);

    /** op: 0=play 1=pause 2=seek(arg ms) */
    public static native void mediaControl(long peer, int op, int arg);

    /** what: 0=time ms, 1=duration ms, 2=playing(0/1) */
    public static native int mediaQuery(long peer, int what);

    /**
     * Opens the native file panel (the simulator's camera/gallery); the
     * result arrives through {@link #nativeFilePicked}.
     */
    public static native void pickFile();

    /**
     * Called with the picked path, or null when the user cancelled.
     */
    static void nativeFilePicked(String path) {
        com.codename1.impl.ios.sim.bridge.PickCallback cb =
                com.codename1.impl.ios.sim.bridge.BridgeRegistry.takePickCallback();
        if (cb != null) {
            cb.picked(path);
        }
    }

    /**
     * Called from the native text field on every text change mid-session.
     */
    static void nativeEditingUpdate(String text) {
        com.codename1.impl.ios.sim.bridge.EditingCallback cb =
                com.codename1.impl.ios.sim.bridge.BridgeRegistry.getEditingCallback();
        if (cb != null) {
            cb.editingUpdate(text);
        }
    }

    /**
     * Installs the screen overlay: an image (the skin's screen-rect crop,
     * transparent except the rounded corner bezels) composited on top of the
     * app's pixels in the presented frame. Pass peer 0 to clear.
     *
     * @param peer the native image peer
     * @param x window-coordinate rectangle the overlay scales into
     * @param clipX window-coordinate clip (the visible viewport intersection
     *        - in zoom mode the overlay rectangle overflows the scroller)
     */
    public static native void setScreenOverlay(long peer, int x, int y, int w, int h,
            int clipX, int clipY, int clipW, int clipH);

    private static final int POINTER_PRESSED = 1;
    private static final int POINTER_RELEASED = 2;
    private static final int POINTER_DRAGGED = 3;

    /**
     * Installed in isolated mode: window input routes into the child app
     * universe (translated to its screen rectangle) instead of the parent's
     * Display.
     */
    static volatile IsolatedAppRunner isolatedRunner;

    /**
     * True when the parent universe runs its own Codename One Display (shell
     * mode) - input outside the app screen falls through to it.
     */
    static volatile boolean parentDisplayActive;

    /**
     * Called by the native view for each mouse event; dispatches into the
     * Codename One event pipeline. Invoked on the AppKit main thread - the
     * Display pointer methods enqueue onto the EDT, so no further hand-off
     * is needed.
     */
    static void nativePointerEvent(int type, int x, int y) {
        IsolatedAppRunner runner = isolatedRunner;
        if (runner != null) {
            if (runner.routePointer(type, x, y) || !parentDisplayActive) {
                return;
            }
            // fall through: shell chrome input
        }
        com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
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

    /**
     * Called by the native view for key events (1=down, 2=up).
     */
    static void nativeKeyEvent(int type, int code) {
        IsolatedAppRunner runner = isolatedRunner;
        if (runner != null) {
            com.codename1.impl.ios.sim.bridge.InputSink sink =
                    com.codename1.impl.ios.sim.bridge.BridgeRegistry.getInputSink();
            if (sink != null) {
                sink.keyEvent(type, code);
            }
            return;
        }
        com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
        if (type == 1) {
            d.keyPressed(code);
        } else {
            d.keyReleased(code);
        }
    }

    /**
     * @return true once the native window's rendering surface is live (the
     * layer is attached and the screen texture exists) - drawing flushed
     * before this point would be dropped
     */
    public static native boolean isSurfaceReady();

    /**
     * Called when the user closes the native window.
     */
    static void nativeWindowClosed() {
        System.exit(0);
    }

    /** Receives trackpad / mouse-wheel scroll deltas from the native view. */
    public interface ScrollWheelHandler {
        void scrollWheel(int dx, int dy);
    }

    /**
     * Installed by the launcher: scrolls the shell's zoom viewport.
     */
    public static volatile ScrollWheelHandler scrollWheelHandler;

    /**
     * Called from the native view for scroll-wheel / trackpad events.
     */
    static void nativeScrollWheel(int dx, int dy) {
        ScrollWheelHandler h = scrollWheelHandler;
        if (h != null) {
            h.scrollWheel(dx, dy);
        }
    }

    /** Receives the content size after the user finished a window drag. */
    public interface WindowResizeHandler {
        void windowResized(int width, int height);
    }

    /**
     * Installed by the launcher: rescales the simulator chrome when the user
     * resizes the window.
     */
    public static volatile WindowResizeHandler windowResizeHandler;

    /**
     * Called from the window delegate when a user-driven live resize ends.
     */
    static void nativeWindowResized(int width, int height) {
        WindowResizeHandler h = windowResizeHandler;
        if (h != null) {
            h.windowResized(width, height);
        }
    }

    /**
     * Called by the native menu when the user selects an item; dispatches to
     * whichever universe pushed the menu.
     */
    static void nativeMenuCommand(int index) {
        com.codename1.impl.ios.sim.bridge.MenuDispatcher d =
                com.codename1.impl.ios.sim.bridge.BridgeRegistry.getMenuDispatcher();
        if (d != null) {
            d.fireMenuCommand(index);
        }
    }

    /**
     * Fires the native menu command with the given label - the programmatic
     * equivalent of the user selecting it, used by test drivers.
     *
     * @return true when the label resolved to a command
     */
    public static boolean fireMenuCommandByLabel(String label) {
        com.codename1.impl.ios.sim.bridge.MenuDispatcher d =
                com.codename1.impl.ios.sim.bridge.BridgeRegistry.getMenuDispatcher();
        if (d == null) {
            return false;
        }
        int idx = d.indexOfLabel(label);
        if (idx < 0) {
            return false;
        }
        d.fireMenuCommand(idx);
        return true;
    }

    /**
     * Writes the most recently presented frame to a PNG file - works in pure
     * (headless-AWT) mode where no Robot exists. A non-positive width or
     * height saves the full frame; otherwise the output is cropped to the
     * given window rectangle (the "Screenshot" command crops to the app
     * screen, "Screenshot With Skin" passes zeros).
     *
     * @param path destination PNG path
     * @return true when the file was written
     */
    public static native boolean saveScreenshot(String path, int x, int y, int w, int h);
}
