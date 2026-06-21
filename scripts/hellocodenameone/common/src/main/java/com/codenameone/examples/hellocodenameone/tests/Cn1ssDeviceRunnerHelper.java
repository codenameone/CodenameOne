package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.io.WebSocket;
import com.codename1.io.WebSocketState;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// Device-side helper that ships screenshots to the host over a single
/// transport: a WebSocket to the host-side Cn1ssScreenshotServer. The device
/// connects to ws://HOST:8765, sends a JSON META text frame followed by the
/// binary PNG, and the host writes the file and echoes an ACK. Native ports
/// use the blocking, ACK-paced sink (Cn1ssWebSocketSink.trySend); the JS port
/// (which can't block the browser event loop) uses the async sink that
/// advances the sequential suite from the ACK callback. There is no
/// base64-over-stdout or filesystem fallback -- when the socket is
/// unavailable the screenshot is simply absent and the host-side
/// missing-screenshot guard flags it.
interface Cn1ssDeviceRunnerHelper {
    // Standard, fixed port the host-side Cn1ssScreenshotServer listens on
    // (scripts/lib/cn1ss.sh starts it with --port 8765). The runner does not
    // inject the URL per-run; the device defaults to ws://HOST:8765 below so
    // no platform-specific env/property plumbing is needed. Keep this value in
    // sync with CN1SS_WS_PORT in scripts/lib/cn1ss.sh.
    int CN1SS_WS_DEFAULT_PORT = 8765;

    static void runOnEdtSync(Runnable runnable) {
        Display display = Display.getInstance();
        if (display.isEdt()) {
            runnable.run();
        } else if (isHtml5()) {
            display.callSerially(runnable);
        } else {
            display.callSeriallyAndWait(runnable);
        }
    }

    static void emitCurrentFormScreenshot(String testName) {
        emitCurrentFormScreenshot(testName, null);
    }

    static void emitImage(Image image, String testName, Runnable onComplete) {
        String safeName = sanitizeTestName(testName);
        if (image == null) {
            println("CN1SS:ERR:test=" + safeName + " message=Image is null");
            emitPlaceholderScreenshot(safeName);
            complete(onComplete);
            return;
        }
        int width = Math.max(1, image.getWidth());
        int height = Math.max(1, image.getHeight());
        boolean async = false;
        try {
            async = emitImageScreenshot(safeName, image, width, height, onComplete);
        } finally {
            if (!async) {
                complete(onComplete);
            }
        }
    }

    static void emitCurrentFormScreenshot(String testName, Runnable onComplete) {
        String safeName = sanitizeTestName(testName);
        Form current = Display.getInstance().getCurrent();
        if (current == null) {
            println("CN1SS:ERR:test=" + safeName + " message=Current form is null");
            emitPlaceholderScreenshot(safeName);
            complete(onComplete);
            return;
        }
        int width = Math.max(1, current.getWidth());
        int height = Math.max(1, current.getHeight());
        final int seqAtRequest = Cn1ssDeviceRunner.sCurrentTestSeq;
        Display.getInstance().screenshot(screen -> {
            // Drop a late capture: if the runner already advanced past the test
            // that requested this screenshot (e.g. a heavy 4K graphics test that
            // timed out at capture-requested), the frame now on screen belongs
            // to a later test. Saving it under safeName would mislabel it (wrong
            // title bar / content). Discarding leaves the test without a capture
            // -- recorded as missing (tolerated by CN1SS_ALLOWED_MISSING) rather
            // than a false mismatch.
            if (Cn1ssDeviceRunner.sCurrentTestSeq != seqAtRequest) {
                println("CN1SS:WARN:test=" + safeName + " discarding late screenshot (runner advanced)");
                complete(onComplete);
                return;
            }
            if (screen == null) {
                println("CN1SS:ERR:test=" + safeName + " message=Screenshot callback returned null");
                emitPlaceholderScreenshot(safeName);
                complete(onComplete);
                return;
            }
            boolean async = false;
            try {
                async = emitImageScreenshot(safeName, screen, width, height, onComplete);
            } finally {
                if (!async) {
                    complete(onComplete);
                }
            }
        });
    }

    /// Encodes the PNG once, logs the size/hash/dupe diagnostics, then sends it
    /// to the host over the WebSocket sink (the only transport). Returns true
    /// when the async WebSocket path (JS port) has taken ownership of
    /// `onComplete` -- it will be invoked from the ACK callback, so the caller
    /// must NOT call it. Returns false on every synchronous path (native WS, or
    /// WS unavailable), where the caller advances the suite itself.
    /// `onComplete` may be null.
    private static boolean emitImageScreenshot(String safeName, Image screenshot, int width, int height,
                                               Runnable onComplete) {
        try {
            ImageIO io = ImageIO.getImageIO();
            if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                println("CN1SS:ERR:test=" + safeName + " message=PNG encoding unavailable");
                emitPlaceholderScreenshot(safeName);
                return false;
            }
            if (Display.getInstance().isSimulator()) {
                io.save(screenshot, Storage.getInstance().createOutputStream(safeName + ".png"), ImageIO.FORMAT_PNG, 1);
            }
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream(Math.max(1024, width * height / 2));
            io.save(screenshot, pngOut, ImageIO.FORMAT_PNG, 1f);
            byte[] pngBytes = pngOut.toByteArray();
            String hash = fnv1a64Hex(pngBytes);
            println("CN1SS:INFO:test=" + safeName + " png_bytes=" + pngBytes.length
                    + " png_fnv1a64=" + hash);
            String previous = Cn1ssHashTracker.recordAndCheck(hash, safeName);
            if (previous != null) {
                println("CN1SS:WARN:test=" + safeName
                        + " duplicate_image_with=" + previous + " png_fnv1a64=" + hash);
            }

            if (isHtml5()) {
                // JS cannot block on a monitor; the async WS sink advances the
                // suite from the ACK callback via onComplete.
                if (Cn1ssWebSocketSink.trySendAsync(safeName, pngBytes, hash, onComplete)) {
                    return true;
                }
                println("CN1SS:ERR:test=" + safeName + " message=websocket-unavailable");
            } else if (!Cn1ssWebSocketSink.trySend(safeName, pngBytes, hash)) {
                println("CN1SS:ERR:test=" + safeName + " message=websocket-unavailable");
            }
            // WebSocket is the only transport. When it is unavailable the
            // screenshot is simply absent and the host-side missing-screenshot
            // guard flags it -- there is no base64 / file fallback any more.
            return false;
        } catch (IOException ex) {
            println("CN1SS:ERR:test=" + safeName + " message=" + ex);
            Log.e(ex);
            emitPlaceholderScreenshot(safeName);
            return false;
        } finally {
            screenshot.dispose();
        }
    }

    static String sanitizeTestName(String testName) {
        if (testName == null || testName.length() == 0) {
            return "default";
        }
        StringBuffer sanitized = new StringBuffer(testName.length());
        for (int i = 0; i < testName.length(); i++) {
            char ch = testName.charAt(i);
            if (isSafeChar(ch)) {
                sanitized.append(ch);
            } else {
                sanitized.append('_');
            }
        }
        return sanitized.toString();
    }

    static boolean isSafeChar(char ch) {
        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
            return true;
        }
        if (ch >= '0' && ch <= '9') {
            return true;
        }
        return ch == '_' || ch == '.' || ch == '-';
    }

    static void println(String line) {
        System.out.println(line);
    }

    static void emitPlaceholderScreenshot(String safeName) {
        try {
            ImageIO io = ImageIO.getImageIO();
            if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                println("CN1SS:END:" + safeName);
                return;
            }
            Image placeholder = Image.createImage(1, 1, 0xffffffff);
            try {
                ByteArrayOutputStream pngOut = new ByteArrayOutputStream(128);
                io.save(placeholder, pngOut, ImageIO.FORMAT_PNG, 1f);
                byte[] pngBytes = pngOut.toByteArray();
                println("CN1SS:INFO:test=" + safeName + " png_bytes=" + pngBytes.length + " placeholder=1");
                if (isHtml5()) {
                    // Fire-and-forget on JS (no onComplete: the caller advances
                    // the suite for placeholders).
                    Cn1ssWebSocketSink.trySendAsync(safeName, pngBytes, fnv1a64Hex(pngBytes), null);
                } else {
                    Cn1ssWebSocketSink.trySend(safeName, pngBytes, fnv1a64Hex(pngBytes));
                }
                // WebSocket-only: if the socket is unavailable the placeholder
                // is dropped along with the real screenshot; no base64 channel.
                println("CN1SS:END:" + safeName);
            } finally {
                placeholder.dispose();
            }
        } catch (Throwable t) {
            println("CN1SS:ERR:test=" + safeName + " message=placeholder_emit_failed " + t);
            println("CN1SS:END:" + safeName);
        }
    }

    static void complete(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }

    static boolean isHtml5() {
        return "HTML5".equals(Display.getInstance().getPlatformName());
    }

    /// Returns the JS port's cumulative bridge-call counters as
    /// "jso=N:host=M", or null on platforms without a JS bridge. On HTML5
    /// the translated body below is replaced at runtime by a port.js
    /// bindCiFallback override reading jvm.__cn1JsoDispatchCount /
    /// jvm.__cn1HostCallCount. Consumed by BridgeBulkTransferGuardTest to
    /// assert that large-volume transfers (resource streams, pixel
    /// buffers, storage) cost bridge calls proportional to OPERATIONS,
    /// not BYTES -- the per-element regression class that has now bitten
    /// three separate times (single-byte ArrayBufferInputStream.read,
    /// pre-bulk readBulkImpl, surface-encode/getRGB).
    static String jsBridgeCallCounts() {
        return null;
    }

    /// Computes a 64-bit FNV-1a hash of the given bytes. FNV-1a is fast and
    /// has no platform dependencies (no java.security, no java.util.zip
    /// CRC32 wrapping subtleties). 64 bits is enough to make accidental
    /// collisions on real-world PNG payloads vanishingly unlikely while
    /// keeping the hash short enough to log on a single line. The mixup
    /// detector in `Cn1ssHashTracker` calls this on every emitted image so
    /// that two tests producing bit-identical bytes (the symptom of an iOS
    /// Metal stale-frame capture: MultiButtonTheme_light returning Tabs
    /// Theme_light's pixels because the CAMetalLayer hadn't been re-
    /// presented in time) get flagged with a CN1SS:WARN line.
    static String fnv1a64Hex(byte[] bytes) {
        long h = 0xcbf29ce484222325L;
        long prime = 0x100000001b3L;
        for (int i = 0; i < bytes.length; i++) {
            h ^= bytes[i] & 0xff;
            h *= prime;
        }
        StringBuilder sb = new StringBuilder(16);
        for (int i = 60; i >= 0; i -= 4) {
            int nib = (int) ((h >>> i) & 0xf);
            sb.append((char) (nib < 10 ? '0' + nib : 'a' + (nib - 10)));
        }
        return sb.toString();
    }
}

/// Tracks recently-emitted screenshot hashes per test name so a stale-frame
/// capture (the same PNG bytes attributed to two different tests in a row)
/// gets surfaced via CN1SS:WARN markers instead of silently shipping the
/// wrong image to the comparator. Keeps the most recent 64 entries.
///
/// Lives in a separate package-private class because Cn1ssDeviceRunnerHelper
/// is an interface and can't hold mutable static state.
///
/// Storage uses two parallel arrays (hash[i] paired with testName[i]) rather
/// than a HashMap-typed static field. The Cn1ssDeviceRunner header-comment
/// at lines 215-222 documents that "static collections initialised via a
/// static method call ... broke iOS class loading -- Cn1ssDeviceRunner
/// failed to load before runSuite() could even log a single starting
/// test=... entry, leaving the suite to time out at the 300s end-marker
/// deadline." The first attempt at this tracker used `private static final
/// Map<String, String> hashToTest = new LinkedHashMap<>()` and reproduced
/// exactly that symptom on the iOS Metal CI run -- the simulator booted,
/// installed the app, then never emitted a single CN1SS line and timed
/// out at 30 minutes. Plain primitive arrays of String avoid touching the
/// HashMap class init path during the host class's `<clinit>`.
final class Cn1ssHashTracker {
    private static final int MAX_TRACKED = 64;
    private static final String[] hashes = new String[MAX_TRACKED];
    private static final String[] tests = new String[MAX_TRACKED];
    private static int count;

    private Cn1ssHashTracker() {
    }

    /// Records the hash for `safeName` and returns the test name that
    /// previously emitted the same hash, or null if this is the first time.
    /// Caller logs a CN1SS:WARN line when a duplicate is found so the
    /// downstream comparator can flag the affected test as a likely
    /// stale-frame capture.
    ///
    /// O(MAX_TRACKED) per call -- 64-entry linear scan is trivial vs the
    /// PNG hash itself (which scans every byte of the image).
    static synchronized String recordAndCheck(String hashHex, String safeName) {
        String previous = null;
        for (int i = 0; i < count; i++) {
            if (hashHex.equals(hashes[i])) {
                previous = tests[i];
                if (safeName.equals(previous)) {
                    return null;
                }
                break;
            }
        }
        if (count < MAX_TRACKED) {
            hashes[count] = hashHex;
            tests[count] = safeName;
            count++;
        } else {
            System.arraycopy(hashes, 1, hashes, 0, MAX_TRACKED - 1);
            System.arraycopy(tests, 1, tests, 0, MAX_TRACKED - 1);
            hashes[MAX_TRACKED - 1] = hashHex;
            tests[MAX_TRACKED - 1] = safeName;
        }
        return previous;
    }
}

/// Singleton WebSocket sink. Lazily connects on first send. ACK pacing:
/// after every binary upload, the sender thread blocks on a per-test latch
/// that the WS onTextMessage handler releases when the host echoes back an
/// `ACK <safeName>` text frame. ACK_TIMEOUT_MS is generous (10s) -- the
/// host writes the PNG to disk and ACKs immediately on LAN; if we hit the
/// timeout something is genuinely broken and the test should fail loudly.
///
/// `trySend` returns true if the WS path successfully uploaded the PNG
/// (or transiently failed after the connection was established), and false
/// if WS is unavailable (no URL configured, unsupported platform, connect
/// timed out). WebSocket is the only transport now, so a false return just
/// means the screenshot is absent and the host-side guard flags it.
final class Cn1ssWebSocketSink {
    private static final int ACK_TIMEOUT_MS = 10_000;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final Map<String, AckLatch> pending = new HashMap<String, AckLatch>();
    private static WebSocket socket;
    private static volatile boolean attemptedConnect;
    private static volatile boolean unavailable;

    // ---- Async path (JavaScript port) ----
    // The JS port runs on the browser event loop and forbids blocking on a
    // monitor (Object.wait throws BlockingDisallowedException, even off the
    // EDT), so the blocking trySend/connect above cannot be used there. The
    // async path never blocks: it connects, sends on open, and advances the
    // sequential test suite from the ACK callback by invoking the per-test
    // onComplete. ASYNC_IDLE -> ASYNC_CONNECTING -> ASYNC_OPEN / ASYNC_FAILED.
    private static final int ASYNC_IDLE = 0;
    private static final int ASYNC_CONNECTING = 1;
    private static final int ASYNC_OPEN = 2;
    private static final int ASYNC_FAILED = 3;
    private static int asyncState = ASYNC_IDLE;
    private static WebSocket asyncSocket;
    private static final Map<String, Runnable> asyncPending = new HashMap<String, Runnable>();
    // The suite is sequential (each test waits for onComplete before the next),
    // so at most one screenshot is in flight; this holds the single send that
    // arrived while the socket was still connecting. {name, png, hash, onComplete}
    private static Object[] asyncQueuedWhileConnecting;

    private Cn1ssWebSocketSink() {
    }

    /// The server URL: an explicit -Dcn1ss.websocket.url wins (JavaSE), else the
    /// fixed standard port on the host loopback (10.0.2.2 from the Android
    /// emulator, 127.0.0.1 elsewhere -- iOS sim, Mac Catalyst, the browser).
    private static String resolveUrl() {
        String url = Display.getInstance().getProperty("cn1ss.websocket.url", "");
        if (url == null || url.length() == 0) {
            String host = "and".equals(Display.getInstance().getPlatformName())
                    ? "10.0.2.2" : "127.0.0.1";
            url = "ws://" + host + ":" + Cn1ssDeviceRunnerHelper.CN1SS_WS_DEFAULT_PORT;
        }
        return url;
    }

    /// Non-blocking send for the JS port. Returns true when the WebSocket path
    /// has taken ownership of completion (it will run onComplete from the ACK
    /// callback, or immediately if the send fails); false when WS is
    /// unavailable, in which case the screenshot is simply absent (no fallback).
    static synchronized boolean trySendAsync(String safeName, byte[] pngBytes, String hashHex, Runnable onComplete) {
        if (asyncState == ASYNC_FAILED) {
            return false;
        }
        if (asyncState == ASYNC_IDLE) {
            if (!WebSocket.isSupported()) {
                asyncState = ASYNC_FAILED;
                System.out.println("CN1SS:INFO:ws-sink-unavailable reason=not-supported");
                return false;
            }
            connectAsync();
        }
        if (asyncState == ASYNC_OPEN) {
            sendAsyncNow(safeName, pngBytes, hashHex, onComplete);
            return true;
        }
        if (asyncState == ASYNC_CONNECTING) {
            // Hold the single in-flight send until onConnect flushes it.
            asyncQueuedWhileConnecting = new Object[] { safeName, pngBytes, hashHex, onComplete };
            return true;
        }
        return false;
    }

    private static void connectAsync() {
        asyncState = ASYNC_CONNECTING;
        WebSocket ws = WebSocket.build(resolveUrl())
                .onConnect(new WebSocket.ConnectHandler() {
                    public void onConnect(WebSocket w) {
                        asyncState = ASYNC_OPEN;
                        flushQueuedAsync();
                    }
                })
                .onTextMessage(new WebSocket.TextHandler() {
                    public void onText(WebSocket w, String message) {
                        handleAckAsync(message);
                    }
                })
                .onClose(new WebSocket.CloseHandler() {
                    public void onClose(WebSocket w, int code, String reason) {
                        failAsync("closed:" + code);
                    }
                })
                .onError(new WebSocket.ErrorHandler() {
                    public void onError(WebSocket w, Exception ex) {
                        failAsync("error:" + ex.getMessage());
                    }
                });
        asyncSocket = ws;
        ws.connect(0);
    }

    private static void sendAsyncNow(String name, byte[] png, String hash, Runnable onComplete) {
        try {
            String meta = "META {\"test\":\"" + name + "\",\"png_bytes\":"
                    + png.length + ",\"png_fnv1a64\":\"" + hash + "\"}";
            asyncSocket.send(meta);
            asyncSocket.send(png);
            if (onComplete != null) {
                synchronized (asyncPending) {
                    asyncPending.put(name, onComplete);
                }
            }
        } catch (Throwable t) {
            System.out.println("CN1SS:ERR:test=" + name + " message=ws-async-send-failed:" + t);
            Log.e(t);
            if (onComplete != null) {
                onComplete.run(); // never stall the sequential suite
            }
        }
    }

    private static void flushQueuedAsync() {
        Object[] q = asyncQueuedWhileConnecting;
        asyncQueuedWhileConnecting = null;
        if (q != null) {
            sendAsyncNow((String) q[0], (byte[]) q[1], (String) q[2], (Runnable) q[3]);
        }
    }

    private static void handleAckAsync(String text) {
        if (text == null || !text.startsWith("ACK ")) {
            return;
        }
        String body = text.substring(4).trim();
        int sp = body.indexOf(' ');
        String name = sp > 0 ? body.substring(0, sp) : body;
        Runnable r;
        synchronized (asyncPending) {
            r = asyncPending.remove(name);
        }
        if (r != null) {
            r.run(); // advance the suite to the next test
        }
    }

    /// Connection failed or dropped: stop using WS and release every waiter so
    /// the sequential suite proceeds. Missing screenshots then surface through
    /// the host-side count guard rather than hanging the run.
    private static void failAsync(String reason) {
        boolean firstFailure = asyncState != ASYNC_FAILED;
        asyncState = ASYNC_FAILED;
        if (firstFailure) {
            System.out.println("CN1SS:INFO:ws-sink-unavailable reason=" + reason);
        }
        Object[] q = asyncQueuedWhileConnecting;
        asyncQueuedWhileConnecting = null;
        if (q != null && q[3] != null) {
            ((Runnable) q[3]).run();
        }
        java.util.List<Runnable> waiters = new java.util.ArrayList<Runnable>();
        synchronized (asyncPending) {
            waiters.addAll(asyncPending.values());
            asyncPending.clear();
        }
        for (Runnable r : waiters) {
            if (r != null) {
                r.run();
            }
        }
    }

    static synchronized boolean trySend(String safeName, byte[] pngBytes, String hashHex) {
        if (!ensureConnected()) {
            return false;
        }
        final AckLatch latch = new AckLatch();
        synchronized (pending) {
            pending.put(safeName, latch);
        }
        try {
            String meta = "META {\"test\":\"" + safeName + "\",\"png_bytes\":"
                    + pngBytes.length + ",\"png_fnv1a64\":\"" + hashHex + "\"}";
            socket.send(meta);
            socket.send(pngBytes);
        } catch (Throwable t) {
            synchronized (pending) {
                pending.remove(safeName);
            }
            System.out.println("CN1SS:ERR:test=" + safeName + " message=ws-send-failed:" + t);
            Log.e(t);
            return true; // WS path was attempted; don't fall through to chunks.
        }
        boolean acked = latch.await(ACK_TIMEOUT_MS);
        synchronized (pending) {
            pending.remove(safeName);
        }
        if (!acked) {
            System.out.println("CN1SS:ERR:test=" + safeName
                    + " message=ws-ack-timeout-after-" + ACK_TIMEOUT_MS + "ms");
        }
        return true;
    }

    private static boolean ensureConnected() {
        if (socket != null && socket.getReadyState() == WebSocketState.OPEN) {
            return true;
        }
        if (unavailable) {
            return false;
        }
        if (attemptedConnect) {
            // Previous attempt completed but the socket is no longer open
            // (closed/errored). Treat as unavailable for the rest of the
            // run; the runner script's whole point of launching the WS
            // server is that it stays up for the whole suite.
            unavailable = true;
            return false;
        }
        // A -Dcn1ss.websocket.url override still wins where the launcher can
        // set Display properties (e.g. the JavaSE simulator via the maven
        // plugin's -Dproperty=...). Everywhere else we don't inject anything:
        // the host runs Cn1ssScreenshotServer on the fixed standard port and
        // the device defaults to ws://HOST:CN1SS_WS_DEFAULT_PORT. HOST is the
        // host loopback as seen from the app -- the Android emulator reaches
        // it via 10.0.2.2, every other target (iOS simulator, Mac Catalyst,
        // the browser, JavaSE) shares 127.0.0.1.
        String url = Display.getInstance().getProperty("cn1ss.websocket.url", "");
        if (url == null || url.length() == 0) {
            String host = "and".equals(Display.getInstance().getPlatformName())
                    ? "10.0.2.2" : "127.0.0.1";
            url = "ws://" + host + ":" + Cn1ssDeviceRunnerHelper.CN1SS_WS_DEFAULT_PORT;
        }
        if (!WebSocket.isSupported()) {
            unavailable = true;
            System.out.println("CN1SS:INFO:ws-sink-unavailable reason=not-supported");
            return false;
        }
        attemptedConnect = true;
        return connect(url);
    }

    private static boolean connect(String url) {
        final Object connectGate = new Object();
        final boolean[] connected = new boolean[1];
        final String[] errReason = new String[1];
        WebSocket ws = WebSocket.build(url)
                .onConnect(new WebSocket.ConnectHandler() {
                    @Override
                    public void onConnect(WebSocket w) {
                        synchronized (connectGate) {
                            connected[0] = true;
                            connectGate.notifyAll();
                        }
                    }
                })
                .onTextMessage(new WebSocket.TextHandler() {
                    @Override
                    public void onText(WebSocket w, String message) {
                        handleAck(message);
                    }
                })
                .onClose(new WebSocket.CloseHandler() {
                    @Override
                    public void onClose(WebSocket w, int code, String reason) {
                        drainPending();
                    }
                })
                .onError(new WebSocket.ErrorHandler() {
                    @Override
                    public void onError(WebSocket w, Exception ex) {
                        synchronized (connectGate) {
                            errReason[0] = ex.getMessage();
                            connectGate.notifyAll();
                        }
                        drainPending();
                    }
                });
        socket = ws;
        ws.connect(CONNECT_TIMEOUT_MS);
        long deadline = System.currentTimeMillis() + CONNECT_TIMEOUT_MS;
        synchronized (connectGate) {
            while (!connected[0] && errReason[0] == null) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    errReason[0] = "connect-timeout";
                    break;
                }
                try {
                    connectGate.wait(remaining);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    errReason[0] = "interrupted";
                    break;
                }
            }
        }
        if (connected[0]) {
            return true;
        }
        unavailable = true;
        socket = null;
        System.out.println("CN1SS:INFO:ws-sink-unavailable reason=" + errReason[0]);
        return false;
    }

    private static void handleAck(String text) {
        if (text == null || !text.startsWith("ACK ")) {
            return;
        }
        String body = text.substring(4).trim();
        String testName;
        int spaceIdx = body.indexOf(' ');
        if (spaceIdx > 0) {
            testName = body.substring(0, spaceIdx);
        } else {
            testName = body;
        }
        AckLatch latch;
        synchronized (pending) {
            latch = pending.get(testName);
        }
        if (latch != null) {
            latch.release();
        }
    }

    private static void drainPending() {
        synchronized (pending) {
            for (Map.Entry<String, AckLatch> e : pending.entrySet()) {
                e.getValue().release();
            }
            pending.clear();
        }
    }

    private static final class AckLatch {
        private boolean released;

        synchronized boolean await(long timeoutMs) {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!released) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return false;
                }
                try {
                    wait(remaining);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }

        synchronized void release() {
            released = true;
            notifyAll();
        }
    }
}
