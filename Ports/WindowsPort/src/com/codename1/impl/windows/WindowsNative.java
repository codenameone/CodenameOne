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

/**
 * Java side of the Win32 native bridge for the Windows port. Every {@code
 * native} method here has a matching ParparVM-mangled C function in the port's
 * {@code nativeSources} (Win32, Direct2D/DirectWrite, WIC, WinHTTP), translated
 * and linked by ParparVM's "windows" clean-target build. All methods are static
 * so the C signature is simply {@code (CODENAME_ONE_THREAD_STATE, args...)} with
 * no implicit {@code this}.
 *
 * <p>Peers (render targets, fonts, images, files, connections) are opaque
 * native pointers passed across as {@code long}; 0 means none/failure.</p>
 *
 * @author Codename One
 */
public final class WindowsNative {
    private WindowsNative() {
    }

    /* ---------------------------------------------------------- lifecycle */

    /** Writes a line to the native debug log (OutputDebugString + stderr). */
    public static native void nativeLog(String message);

    /**
     * True when the CN1_FAULT_SELFTEST environment variable is set. Read via the
     * Win32 environment (the clean target does not translate System.getenv), it
     * gates the launcher's deterministic check that a null deref surfaces as a
     * catchable NullPointerException through the native fault handler.
     */
    public static native boolean faultSelfTestEnabled();

    /* ----------------------------------------- BrowserComponent (WebView2) */

    /** True when WebView2 is compiled in (the SDK was present at build time). */
    public static native boolean browserSupported();

    /** Creates a WebView2-backed browser peer; returns an opaque native handle. */
    public static native long browserCreate(int width, int height);

    public static native void browserSetHtml(long peer, String html);

    public static native void browserSetUrl(long peer, String url);

    public static native void browserExecute(long peer, String js);

    public static native void browserSetBounds(long peer, int x, int y, int w, int h);

    /** Next queued browser event ("LOAD" or "NAV|<url>"), or null when none. */
    public static native String browserPollEvent(long peer);

    /** PNG bytes of the current WebView2 content, or null if not captured yet. */
    public static native byte[] browserCapturePng(long peer);

    public static native void browserDestroy(long peer);

    /** Creates the main window plus the Direct2D/DirectWrite/WIC factories. */
    public static native void initDisplay(String title, int width, int height);

    public static native int getDisplayWidth();

    public static native int getDisplayHeight();

    /** Real horizontal screen DPI (96 == 100% scale). */
    public static native int screenDpi();

    /** The window's graphics peer (a Direct2D render target wrapper). */
    public static native long getWindowGraphics();

    /** Presents the given dirty rectangle of the window's back buffer. */
    public static native void flushGraphics(long graphics, int x, int y, int width, int height);

    /**
     * Sets the 2D affine transform on the graphics' Direct2D render target. The
     * six values are the Codename One affine (m00,m10,m01,m11,m02,m12).
     */
    public static native void setTransform(long graphics, float m00, float m10, float m01,
            float m11, float m02, float m12);

    /**
     * Drains one queued input event into {@code out} ([type, x, y, keyCode]);
     * returns true if an event was dequeued. See the {@code CN1_EVENT_*}
     * constants in cn1_windows.h for the type codes.
     */
    public static native boolean pollEvent(int[] out);

    /**
     * Parks the calling (main) thread to keep the process alive in headless
     * screenshot mode, where there is no window message loop. The EDT exits the
     * process via {@link #headlessTick()} once the screenshot has been written.
     */
    public static native void runHeadlessLoop();

    /** Terminates the process with the given exit code. */
    public static native void exitProcess(int code);

    /** Sleeps the calling thread for the given milliseconds (native Sleep). */
    public static native void sleepMillis(int millis);

    /**
     * Parks the calling (main) thread for up to {@code timeoutMillis} to keep the
     * process alive while worker threads run, then force-exits. Callers exit
     * early via {@link #exitProcess(int)} when their async work completes.
     */
    public static native void parkMainThread(int timeoutMillis);

    /**
     * Pumps one batch of Win32 messages on the calling thread. Blocks for the
     * next window message, dispatches it (and any already-queued burst), then
     * returns so the caller can drain the translated input into Codename One on
     * this thread -- which wakes the EDT. Must be called on the thread that
     * created the window (the app's main thread, after Display.init). Returns
     * {@code false} once the window has closed and the loop should stop.
     *
     * @see WindowsImplementation#runMainEventLoop()
     */
    public static native boolean pumpMessages();

    /**
     * Creates an offscreen (WIC-backed) Direct2D graphics target of the given
     * size. Used for headless rendering and the screenshot tests; draws go
     * through the same graphics bridge as the on-screen target.
     */
    public static native long createOffscreenGraphics(int width, int height);

    /** Encodes the offscreen target behind {@code graphics} to a PNG file. */
    public static native boolean saveGraphicsToPng(long graphics, String path);

    /**
     * Enables headless screenshot mode (call before {@code initDisplay}): the
     * display renders into an offscreen WIC bitmap of the given size and, once
     * the UI has painted and settled, the bitmap is written to {@code path} and
     * the process exits. Lets CI capture a deterministic PNG with no window.
     */
    public static native void enableHeadlessScreenshot(String path, int width, int height);

    /**
     * Encodes a {@code width*height} block of straight-alpha ARGB pixels (CN1
     * getRGB layout) to PNG and returns the bytes. Backs the port's ImageIO.
     */
    public static native byte[] encodeArgbToPng(int[] argb, int width, int height);

    /**
     * Encodes the current window render target to PNG bytes. In headless /
     * offscreen mode the target is a WIC bitmap the EDT paints into, so this is
     * a deterministic snapshot of the rendered UI (the proven render path) for
     * the cn1ss WebSocket sink. 0-length/null on failure.
     */
    public static native byte[] captureWindowToPngBytes();

    /* ----------------------------------------------------- graphics state */

    public static native int getColor(long graphics);

    public static native void setColor(long graphics, int rgb);

    public static native void setAlpha(long graphics, int alpha);

    public static native int getAlpha(long graphics);

    public static native void setNativeFont(long graphics, long font);

    public static native int getClipX(long graphics);

    public static native int getClipY(long graphics);

    public static native int getClipWidth(long graphics);

    public static native int getClipHeight(long graphics);

    public static native void setClip(long graphics, int x, int y, int width, int height);

    public static native void clipRect(long graphics, int x, int y, int width, int height);

    /** Sets the clip to an arbitrary screen-space polygon/path (flattened, same
     *  encoding as {@link #fillShape}). Used for clipRect under a transform and
     *  setClip(Shape). */
    public static native void setClipShape(long graphics, float[] coords, int[] types, int typeCount, int windingRule);

    /* ---------------------------------------------------------- drawing */

    public static native void drawLine(long graphics, int x1, int y1, int x2, int y2);

    public static native void fillRect(long graphics, int x, int y, int width, int height);

    public static native void drawRect(long graphics, int x, int y, int width, int height);

    public static native void drawRoundRect(long graphics, int x, int y, int width, int height, int arcWidth, int arcHeight);

    /** Fills an arbitrary path. {@code types} are segment ops (0=move,1=line,
     *  2=quad,3=cubic,4=close); {@code coords} is the packed coordinate stream
     *  they index. windingRule: 0=even-odd, 1=non-zero. */
    public static native void fillShape(long graphics, float[] coords, int[] types, int typeCount, int windingRule);

    /** Strokes an arbitrary path (see {@link #fillShape}) with the given line width. */
    public static native void drawShape(long graphics, float[] coords, int[] types, int typeCount, int windingRule, float lineWidth);

    public static native void fillRoundRect(long graphics, int x, int y, int width, int height, int arcWidth, int arcHeight);

    public static native void fillArc(long graphics, int x, int y, int width, int height, int startAngle, int arcAngle);

    public static native void drawArc(long graphics, int x, int y, int width, int height, int startAngle, int arcAngle);

    public static native void drawString(long graphics, String str, int x, int y);

    public static native void drawImage(long graphics, long image, int x, int y);

    public static native void drawImageScaled(long graphics, long image, int x, int y, int width, int height);

    public static native void drawRGB(long graphics, int[] rgbData, int offset, int x, int y, int width, int height, boolean processAlpha);

    /* ----------------------------------------------------------- fonts */

    public static native long createFont(int face, int style, int size);

    public static native long getDefaultFont();

    public static native int stringWidth(long font, String str);

    public static native int charWidth(long font, char c);

    public static native int charsWidth(long font, char[] chars, int offset, int length);

    public static native int fontHeight(long font);

    /**
     * Resolves a TrueType/native font name to a DirectWrite family and returns a
     * font peer. {@code native:} scheme names map to the platform UI family;
     * other names are treated as literal families. {@code fileName} is the CN1
     * fallback path, unused on Windows. 0 means failure.
     */
    public static native long loadTrueTypeFont(String fontName, String fileName);

    /** Derives a new font peer at the given pixel size and CN1 style/weight bits. */
    public static native long deriveTrueTypeFont(long font, float size, int weight);

    /* ---------------------------------------------------------- images */

    public static native long createImageFromARGB(int[] argb, int width, int height);

    public static native long createImageFromFile(String path);

    public static native long createImageFromBytes(byte[] data, int offset, int length);

    public static native long createMutableImage(int width, int height, int fillColor);

    public static native int imageWidth(long image);

    public static native int imageHeight(long image);

    public static native long scaleImage(long image, int width, int height);

    public static native void imageGetRGB(long image, int[] arr, int offset, int x, int y, int width, int height);

    public static native long getImageGraphics(long image);

    /* ------------------------------------------------------- filesystem */

    /**
     * The bytes of a classpath resource embedded in the executable's PE resource
     * section (e.g. {@code "/theme.res"}), or {@code null} when no such resource
     * was embedded. Resources are baked in by the ParparVM windows target so the
     * single .exe is self-contained -- the Windows analog of the iOS .app bundle.
     */
    public static native byte[] resourceBytes(String name);

    public static native long fileOpenRead(String path);

    public static native long fileOpenWrite(String path, boolean append);

    public static native int fileRead(long handle, byte[] buffer, int offset, int length);

    public static native int fileWrite(long handle, byte[] buffer, int offset, int length);

    public static native void fileClose(long handle);

    public static native boolean fileExists(String path);

    public static native boolean fileIsDirectory(String path);

    public static native long fileLength(String path);

    public static native void fileDelete(String path);

    public static native void fileMkdir(String path);

    public static native void fileRename(String path, String newName);

    public static native String[] fileList(String dir);

    public static native String storageDir();

    /** Directory of the running executable; resources are resolved relative to it. */
    public static native String executableDir();

    public static native String[] fileRoots();

    public static native long fileRootSize(String root);

    public static native long fileRootFree(String root);

    public static native boolean fileIsHidden(String path);

    public static native void fileSetHidden(String path, boolean hidden);

    /* --------------------------------------------------------- network */

    public static native long httpOpen(String url, boolean read, boolean write);

    public static native void httpSetMethod(long connection, boolean post);

    public static native void httpSetHeader(long connection, String key, String value);

    public static native int httpResponseCode(long connection);

    public static native String httpResponseMessage(long connection);

    public static native int httpContentLength(long connection);

    public static native String httpHeaderField(long connection, String name);

    public static native String[] httpHeaderFieldNames(long connection);

    public static native int httpReadBody(long connection, byte[] buffer, int offset, int length);

    public static native int httpWriteBody(long connection, byte[] buffer, int offset, int length);

    public static native void httpClose(long connection);

    /* ---------------------------------------------------------- sockets */

    /** Connects a TCP socket to host:port (timeoutMillis 0 = blocking). 0 = failure. */
    public static native long socketConnect(String host, int port, int timeoutMillis);

    /** Blocking read into buffer[offset..offset+length); returns bytes read, -1 on EOF/error. */
    public static native int socketRead(long socket, byte[] buffer, int offset, int length);

    /** Writes the full range; returns bytes written, -1 on error. */
    public static native int socketWrite(long socket, byte[] buffer, int offset, int length);

    /** Bytes available for a non-blocking read without blocking. */
    public static native int socketAvailable(long socket);

    /** Closes and frees the socket peer. */
    public static native void socketClose(long socket);

    /** Last WinSock error code for the socket, or -1. */
    public static native int socketErrorCode(long socket);

    /** True while the socket peer is open. */
    public static native boolean socketConnected(long socket);

    /** The local host name (used by Socket.getHostOrIP). */
    public static native String getHostOrIP();

    /* ------------------------------------------------------- clipboard */

    public static native void clipboardSetText(String text);

    public static native String clipboardGetText();

    /* ----------------------------------------------------------- media */

    /** Creates a Media Foundation player from the given bytes (spooled to a temp
     *  file). {@code length} is the valid byte count; {@code mimeType} hints the
     *  source resolver. Returns a peer handle, or 0 on failure. */
    public static native long mediaCreate(byte[] data, int length, String mimeType);

    public static native void mediaPlay(long peer);

    public static native void mediaPause(long peer);

    public static native void mediaSetTime(long peer, int millis);

    public static native int mediaGetTime(long peer);

    public static native int mediaGetDuration(long peer);

    public static native void mediaSetVolume(long peer, int volume);

    public static native boolean mediaIsPlaying(long peer);

    public static native boolean mediaIsEnded(long peer);

    public static native boolean mediaIsVideo(long peer);

    /** Stops playback, frees the engine and deletes the temp file. */
    public static native void mediaDestroy(long peer);
}
