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
 */
public final class WindowsNative {
    private WindowsNative() {
    }

    /* ---------------------------------------------------------- lifecycle */

    /** Writes a line to the native debug log (OutputDebugString + stderr). */
    public static native void nativeLog(String message);

    /* ------------------------------------------------------------- VideoIO */
    /** True when the Media Foundation backend for VideoIO is available (MFStartup ok). */
    public static native boolean videoBackendAvailable();
    /** True when the Media Foundation H.265/HEVC encoder MFT is present. */
    public static native boolean videoSupportsHEVC();
    // Reader (IMFSourceReader). The peer owns the source reader; metadata getters are cheap.
    public static native long videoReaderOpen(String path);
    public static native int videoReaderWidth(long peer);
    public static native int videoReaderHeight(long peer);
    public static native long videoReaderDuration(long peer);
    public static native float videoReaderFrameRate(long peer);
    public static native boolean videoReaderHasVideo(long peer);
    public static native boolean videoReaderHasAudio(long peer);
    public static native int videoReaderAudioSampleRate(long peer);
    public static native int videoReaderAudioChannels(long peer);
    /** Frame accurate decode (SetCurrentPosition + read) to width*height*4 RGBA bytes, or null. */
    public static native byte[] videoReaderFrameAt(long peer, long ms);
    /** Decodes the whole audio track to interleaved signed 16-bit little-endian PCM, or null. */
    public static native byte[] videoReaderReadAudio(long peer);
    public static native void videoReaderClose(long peer);
    // Writer (IMFSinkWriter). Streaming: configure once, write samples, finalize.
    public static native long videoWriterOpen(String outPath, boolean hevc, int width, int height, float fps,
            int videoBitRate, int gop, boolean hasAudio, int audioBitRate, int sampleRate, int channels);
    public static native void videoWriterFrame(long peer, byte[] rgba, int width, int height, long ptsMs);
    public static native void videoWriterAudio(long peer, byte[] pcm, int sampleRate, int channels, long ptsMs);
    public static native boolean videoWriterClose(long peer);

    /**
     * True when the CN1_FAULT_SELFTEST environment variable is set. Read via the
     * Win32 environment (the clean target does not translate System.getenv), it
     * gates the launcher's deterministic check that a null deref surfaces as a
     * catchable NullPointerException through the native fault handler.
     */
    public static native boolean faultSelfTestEnabled();

    /* ----------------------------------------- crash protection */
    // Pairs with cn1_windows_crash_protection.c. crashProtectionInstall is
    // idempotent; SetUnhandledExceptionFilter catches every SEH-raised
    // fault (access violation, stack overflow, divide-by-zero, illegal
    // instruction) and signal() catches the abort()/raise() paths the
    // CRT exposes. Stderr is spliced into a 32 KB ring buffer so the
    // OutputDebugString / fprintf chatter leading up to the crash makes
    // it into the GitHub issue body.
    public static native void crashProtectionInstall();
    public static native String crashProtectionLogSnapshot();
    public static native String crashProtectionConsumePending();

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
     * True when an integrated or external touch digitizer is present, used for
     * {@code Display.isTouchScreen()} (queries GetSystemMetrics(SM_DIGITIZER)).
     */
    public static native boolean isTouchDevice();

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
     * Enables offscreen-capture mode for the long-running cn1ss WebSocket
     * screenshot suite (call before {@code initDisplay}). A hidden window is
     * still created -- so the message pump, DPI and exact client size match a
     * normal run -- but the EDT paints into an offscreen WIC bitmap of that
     * client size, so {@link #captureWindowToPngBytes()} returns a real rendered
     * frame every time instead of falling back to a per-screenshot mutable-image
     * repaint (the expensive step that stalled the slow windows-11-arm runner
     * mid-suite). Unlike {@link #enableHeadlessScreenshot} there is no auto-exit.
     */
    public static native void enableOffscreenCapture();

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

    /**
     * Loads a bundled TrueType font from its bytes (read out of the exe's embedded
     * PE resources) via the DirectWrite in-memory font loader -- no file on disk,
     * keeping the executable self-contained. Falls back to a system family from
     * {@code fontName} when {@code data} is null/empty or registration fails.
     */
    public static native long loadTrueTypeFontFromMemory(String fontName, byte[] data);

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

    /* ------------------------------------------------- native text editing */

    /**
     * Overlays a native Win32 EDIT control at the component's text-area bounds
     * (padding already applied), styled to match the Codename One field: the same
     * font ({@code fontPeer}, a CN1Font*), foreground/background colour (0xRRGGBB,
     * or -1 for default) and {@code align} (0 left, 1 center, 2 right). Focuses it
     * and returns an opaque peer (0 when there is no host window, e.g. headless).
     * {@code singleLine} commits on Enter; a multi-line control inserts newlines
     * and commits on focus loss. Created on the pump thread; the EDT polls
     * {@link #editIsDone(long)}.
     */
    public static native long editStringAt(int x, int y, int w, int h, String text,
            boolean singleLine, int maxSize, long fontPeer, int fgColor, int bgColor, int align);

    /** True once the user has committed the native edit (Enter / focus loss). */
    public static native boolean editIsDone(long peer);

    /** The native EDIT control's current text. */
    public static native String editGetText(long peer);

    /** Tears down the native EDIT control. */
    public static native void editClose(long peer);

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

    /* ----------------------------------------------------- shell / launch */

    /**
     * Hands a URI or path to the Windows shell ({@code ShellExecuteW "open"}):
     * {@code http(s):} opens the browser, {@code tel:} the dialer, {@code sms:}
     * the Messaging app, {@code mailto:} the mail client, a filesystem path its
     * registered program. Returns {@code true} only when the shell accepted it
     * (no handler registered reports {@code false} rather than fabricating
     * success).
     */
    public static native boolean shellOpen(String target);

    /* ----------------------------------------------------- secure storage */

    /**
     * Encrypts {@code data} with the Windows Data Protection API
     * ({@code CryptProtectData}) so the ciphertext is decryptable only by the
     * current Windows user on this machine. Returns the encrypted bytes, or
     * {@code null} on failure. Backs {@link WindowsSecureStorage}.
     */
    public static native byte[] dpapiProtect(byte[] data);

    /** Inverse of {@link #dpapiProtect}: decrypts a DPAPI blob, or {@code null}. */
    public static native byte[] dpapiUnprotect(byte[] data);

    /* --------------------------------------------------- local notifications */

    /**
     * Displays a local-notification balloon ({@code Shell_NotifyIcon}) with the
     * given title/body, remembering {@code id} so a click can be routed back. The
     * call is marshaled to the window-owning pump thread. No-op in headless mode.
     */
    public static native void showNotification(String id, String title, String body);

    /**
     * Returns (and clears) the id of a clicked notification balloon, or
     * {@code null} when none was clicked since the last poll. Drained by the EDT.
     */
    public static native String notificationPollClicked();

    /**
     * Shows a modal native file dialog and returns the chosen path, or
     * {@code null} on cancel (or in headless mode). {@code type} mirrors the
     * Display gallery types (0 image, 1 video, 2 all) and selects the file
     * filter. The dialog runs on the window-owning pump thread; the caller
     * blocks until the user chooses.
     */
    public static native String fileDialog(boolean save, int type, String title);

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

    /* ------------------------------------------------------ audio recording */

    /**
     * Starts recording from the default microphone (waveIn) into a PCM WAV file
     * at {@code path}. {@code sampleRate}/{@code channels} use sensible defaults
     * when &lt;= 0. Returns an opaque peer, or 0 on failure (no mic / open error).
     */
    public static native long audioRecStart(String path, int sampleRate, int channels);

    /** Stops recording, finalizes the WAV header and frees the peer. */
    public static native void audioRecStop(long peer);

    /* ------------------------------------------- biometric (Windows Hello) */

    /**
     * Windows Hello availability via {@code UserConsentVerifier}: 0 Available,
     * 1 DeviceNotPresent, 2 NotConfiguredForUser, 3 DisabledByPolicy,
     * 4 DeviceBusy. Returns 1 when WinRT is unavailable (stub build).
     */
    public static native int biometricAvailability();

    /**
     * Shows the Windows Hello consent prompt with {@code message}; returns true
     * when the user is verified. Blocks (call off the EDT). Returns false in the
     * stub build.
     */
    public static native boolean biometricAuthenticate(String message);

    /* -------------------------------------------------- location (WinRT) */

    /**
     * Fills {@code out} with the current fix [latitude, longitude, accuracy(m),
     * altitude(m), direction(deg), velocity(m/s)] from the WinRT Geolocator and
     * returns true; false when location is disabled/unavailable. Blocks (call off
     * the EDT).
     */
    public static native boolean locationGetCurrent(double[] out);

    /* -------------------------------------------------- contacts (WinRT) */

    /**
     * Reads the user's contacts via the WinRT {@code ContactStore} and returns
     * them as a single blob: records separated by {@code 0x1E}, fields (id, name,
     * phone, email) by {@code 0x1F}. {@code null} when the store is inaccessible
     * (no WinRT / access denied); empty string when there are simply no contacts.
     * Blocks (call off the EDT).
     */
    public static native String contactsGetAll();

    /* ----------------------------------------------------- share (WinRT) */

    /**
     * Shows the Windows share UI (WinRT {@code DataTransferManager}) for the given
     * text + title, marshaled to the window thread. Returns false in headless /
     * WinRT-less builds.
     */
    public static native boolean shareText(String text, String title);

    /* --------------------------------------------------------------- printing */

    /**
     * Prints a document file through the Win32 printing system: shows the modal
     * system print dialog ({@code PrintDlgW}, run on the window-owning pump
     * thread like {@link #fileDialog}), then rasterizes the document and spools
     * it to the chosen printer's device context. {@code mimeType} selects the
     * renderer -- {@code image/*} decodes through the port's WIC helper,
     * {@code application/pdf} rasterizes each page via WinRT Windows.Data.Pdf
     * at the printer's DPI. Blocks for the whole flow (call off the EDT).
     * Returns 0 once the job was handed to the print spooler, 1 when the user
     * cancelled the dialog, 2 on failure ({@link #printLastError()} carries a
     * short description).
     */
    public static native int printDocument(String path, String mimeType, String jobName);

    /**
     * Short description of the most recent {@link #printDocument} failure, or
     * {@code null} when the last request did not fail.
     */
    public static native String printLastError();

    /* ----------------------------------------------------------- camera */

    /**
     * Grabs a single frame from the default webcam via Media Foundation and
     * returns it as a CN1 ARGB {@code int[]} (length width*height), filling
     * {@code outDims[0]=width, [1]=height}. Returns {@code null} when there is no
     * camera or capture fails. Blocks briefly (call off the EDT).
     */
    public static native int[] cameraCaptureFrame(int[] outDims);

    /**
     * Friendly names of the connected video capture devices, packed as
     * {@code "name|external|0|0;name|external|0|0;..."} (the format
     * {@link WindowsCameraImpl} parses into {@code CameraInfo}). Empty string when
     * none / Media Foundation is unavailable.
     */
    public static native String cameraEnumerate();

    /**
     * Starts a continuous capture session on the given device index (a worker
     * thread runs a Media Foundation source-reader loop, keeping the latest frame).
     * {@code reqW/reqH} are preferred preview dimensions (best-effort). Returns an
     * opaque session handle, or 0 if no camera / startup failed.
     */
    public static native long cameraSessionStart(int deviceIndex, int reqW, int reqH);

    /** Stops the capture session, joins its worker thread and frees it. */
    public static native void cameraSessionStop(long handle);

    /** Pauses / resumes frame grabbing without tearing down the session. */
    public static native void cameraSessionSetPaused(long handle, boolean paused);

    /**
     * Returns a copy of the most recent captured frame as a CN1 ARGB {@code int[]}
     * (length width*height), filling {@code outDims[0]=width, [1]=height}.
     * {@code null} until the first frame has arrived. The preview peer and the
     * frame listener poll this.
     */
    public static native int[] cameraSessionLatestFrame(long handle, int[] outDims);

    // ----------------------------------------------------------- native peers

    /**
     * Generic native-peer placement (implemented in cn1_windows_peer.cpp). The
     * {@code peer} is an app-provided child HWND (boxed as a long) returned from a
     * {@code @NativeInterface}; these reparent it onto the host window and
     * move/size/show it to track the lightweight {@link com.codename1.ui.PeerComponent}.
     */
    public static native void peerInitialized(long peer, int x, int y, int w, int h);

    /** Repositions / resizes the peer HWND to the component's absolute bounds. */
    public static native void peerSetBounds(long peer, int x, int y, int w, int h);

    /** Shows / hides the peer HWND (transition lightweight mode). */
    public static native void peerSetVisible(long peer, boolean visible);

    /** Hides and detaches the peer HWND (the app still owns its lifetime). */
    public static native void peerDeinitialized(long peer);

    /** Fills {@code out[0]=w, [1]=h} with the peer HWND's current size (0 if none). */
    public static native void peerCalcPreferredSize(long peer, int dispW, int dispH, int[] out);

    /**
     * Captures the peer HWND to CN1 ARGB pixels via {@code PrintWindow} (length
     * width*height), filling {@code outDims[0]=w, [1]=h}, for the offscreen
     * screenshot / transition peer image. {@code null} on failure.
     */
    public static native int[] peerCaptureArgb(long peer, int[] outDims);

    // ---------------------------------------------------------------------
    // 3D / Direct3D 11 backend (com.codename1.gpu). Implemented in
    // nativeSources/cn1_windows_d3d.cpp. Every peer is an opaque long (a D3D
    // object pointer cast to long); 0 means none / unsupported. The render model
    // is offscreen: the device renders one frame into an off-screen render target
    // and gl3dCaptureFrame reads it back as a PNG for the peer-image path.
    // ---------------------------------------------------------------------

    /** Creates the D3D11 context (device + offscreen render target manager); 0 if Direct3D is unavailable. */
    public static native long gl3dCreateContext();
    /** Destroys the context and all GPU objects it still owns. */
    public static native void gl3dDestroyContext(long contextPeer);
    /** (Re)sizes the offscreen render target to width x height and begins a frame. */
    public static native void gl3dBeginFrame(long contextPeer, int width, int height);
    /** Resolves the current frame's render target and returns it encoded as PNG bytes. */
    public static native byte[] gl3dCaptureFrame(long contextPeer);
    /** Writes the current frame to a PNG file (headless-screenshot convenience). */
    public static native boolean gl3dCaptureToFile(long contextPeer, String path);

    /** Uploads interleaved vertex floats into an immutable D3D vertex buffer. */
    public static native long gl3dCreateFloatBuffer(float[] data, int floatCount);
    public static native void gl3dUpdateFloatBuffer(long bufferPeer, float[] data, int floatCount);
    /** Uploads 16-bit indices into a D3D index buffer. */
    public static native long gl3dCreateShortBuffer(short[] data, int indexCount);
    public static native void gl3dUpdateShortBuffer(long bufferPeer, short[] data, int indexCount);
    /** Uploads packed ARGB pixels into an RGBA D3D texture + SRV. */
    public static native long gl3dCreateTexture(int[] argb, int width, int height);
    public static native void gl3dDisposeBuffer(long bufferPeer);
    public static native void gl3dDisposeTexture(long texturePeer);
    public static native void gl3dDisposePipeline(long pipelinePeer);

    /**
     * Compiles the supplied HLSL source (D3DCompile vs_4_0 / ps_4_0) once and
     * builds the input layout + blend/rasterizer/depth state for the given render
     * state. Returns the pipeline handle or 0.
     */
    public static native long gl3dGetOrCreatePipeline(long contextPeer, String key, String hlslSource,
            int blendMode, int cullMode, int depthTest, int depthWrite);

    public static native void gl3dClear(long contextPeer, int argbColor, boolean clearColor, boolean clearDepth);
    public static native void gl3dSetViewport(long contextPeer, int x, int y, int width, int height);

    public static native void gl3dDrawIndexed(long contextPeer, long pipelinePeer, long vboPeer, int strideBytes,
            long iboPeer, int indexCount, int primitive, float[] uniforms, int uniformFloats,
            long texturePeer, int texFilter, int texWrap);
    public static native void gl3dDrawArrays(long contextPeer, long pipelinePeer, long vboPeer, int strideBytes,
            int vertexCount, int primitive, float[] uniforms, int uniformFloats,
            long texturePeer, int texFilter, int texWrap);
}
