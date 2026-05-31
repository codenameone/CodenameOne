package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.io.WebSocket;
import com.codename1.io.WebSocketState;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// Device-side helper that ships screenshots to the host. Three transports,
/// tried in priority order:
///
///   1. WebSocket (`cn1ss.websocket.url` set) — ACK-paced, no os_log /
///      logcat involvement. Works on every port that supports WebSocket
///      (iOS device, iOS simulator, Android, JavaSE, JS, Mac native).
///   2. CN1SS:FILE filesystem hand-off (Mac native: `isDesktop() &&
///      !isSimulator()`) — writes PNG/JPEG to FileSystemStorage and emits
///      a path marker the runner reads directly. Sidesteps os_log's
///      900-byte rate limiter on Mac Catalyst.
///   3. CN1SS:CHUNK base64-over-stdout — universal fallback. Slow on
///      Android (the 50ms/500-byte logcat throttle is here).
///
/// Once every runner script launches Cn1ssScreenshotServer and injects
/// `cn1ss.websocket.url`, paths (2) and (3) are dead code and can go.
interface Cn1ssDeviceRunnerHelper {
    int CHUNK_SIZE_ANDROID = 500;
    int CHUNK_SIZE_DEFAULT = 900;
    // Throttle introduced in 763bd6676 (#4253). The 20ms value was tuned
    // against the original ~10-test screenshot suite; with 17 animation grid
    // tests added each emitting ~150KB PNGs (~400 chunks each), the JDK 21
    // Android job started flaking with one random "PNG chunk truncated before
    // CRC" per run on different tests across runs (SlideHorizontalTransitionTest
    // on one CI run, MultiButtonTheme_dark on the next). Bumping to 30ms gave
    // logcat extra drain time. With three more screenshot tests added by
    // the sticky-headers PR (#4829) the JDK 21 entry started flaking again
    // (one random theme stream truncated per run). Bumping to 50ms; the
    // Cn1ssDeviceRunner per-test deadline is now 30s on native platforms so
    // the slower emission still completes inside the budget for a dual
    // appearance test (~14s for two captures).
    int DELAY_ANDROID = 50;
    int MAX_PREVIEW_BYTES = 20 * 1024;
    String PREVIEW_CHANNEL = "PREVIEW";
    int[] PREVIEW_QUALITIES = new int[] {60, 50, 40, 35, 30, 25, 20, 18, 16, 14, 12, 10, 8, 6, 5, 4, 3, 2, 1};

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
        try {
            emitImageScreenshot(safeName, image, width, height);
        } finally {
            complete(onComplete);
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
        // Mac native: Display.screenshot() reads through the Metal display
        // layer, whose drawable lags the EDT's view of the current form by
        // one or more frames on headless macos-15 (no display link drives
        // present). The result is chart-bar.png ending up with chart-cubic-
        // line content, ButtonTheme_light captured mid-slide, etc. Bypass
        // the display layer entirely by painting the form into an off-
        // screen mutable image -- the pixels are written synchronously, no
        // Metal drawable race -- and feed that to the encoder. Other ports
        // (iOS device, Android, JavaScript, JavaSE simulator) keep the
        // native screenshot path because their Display.screenshot() is
        // reliable and the off-screen path can't capture native peers
        // (BrowserComponent, video, etc.) that live outside the CN1
        // paint pipeline.
        if (Display.getInstance().isDesktop() && !Display.getInstance().isSimulator()) {
            try {
                Image off = Image.createImage(width, height);
                current.paintComponent(off.getGraphics(), true);
                emitImageScreenshot(safeName, off, width, height);
            } finally {
                complete(onComplete);
            }
            return;
        }

        Display.getInstance().screenshot(screen -> {
            if (screen == null) {
                println("CN1SS:ERR:test=" + safeName + " message=Screenshot callback returned null");
                emitPlaceholderScreenshot(safeName);
                complete(onComplete);
                return;
            }
            try {
                emitImageScreenshot(safeName, screen, width, height);
            } finally {
                complete(onComplete);
            }
        });
    }

    /// Encodes the PNG once, logs the size/hash/dupe diagnostics, then routes
    /// to whichever transport is available. The transport priority is:
    /// WebSocket (when `cn1ss.websocket.url` is set) -> CN1SS:FILE filesystem
    /// hand-off (Mac native) -> CN1SS:CHUNK base64-over-stdout (everywhere
    /// else). Once all runners launch Cn1ssScreenshotServer, paths 2 and 3
    /// disappear.
    private static void emitImageScreenshot(String safeName, Image screenshot, int width, int height) {
        try {
            ImageIO io = ImageIO.getImageIO();
            if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                println("CN1SS:ERR:test=" + safeName + " message=PNG encoding unavailable");
                emitPlaceholderScreenshot(safeName);
                return;
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

            if (Cn1ssWebSocketSink.trySend(safeName, pngBytes, hash)) {
                // WS path handled it. Host generates previews from the PNG;
                // skip the on-device preview encode to save cycles.
                return;
            }

            if (Display.getInstance().isDesktop() && !Display.getInstance().isSimulator()) {
                emitFileScreenshot(safeName, pngBytes, screenshot, io);
                return;
            }

            emitChannel(pngBytes, safeName, "");
            byte[] preview = encodePreview(io, screenshot, safeName);
            if (preview != null && preview.length > 0) {
                emitChannel(preview, safeName, PREVIEW_CHANNEL);
            } else {
                println("CN1SS:INFO:test=" + safeName + " preview_jpeg_bytes=0 preview_quality=0");
            }
        } catch (IOException ex) {
            println("CN1SS:ERR:test=" + safeName + " message=" + ex);
            Log.e(ex);
            emitPlaceholderScreenshot(safeName);
        } finally {
            screenshot.dispose();
        }
    }

    /// Save the PNG (and a JPEG preview) into the app's filesystem and
    /// announce their paths in the log. The runner copies these files
    /// directly, sidestepping the CN1SS:CHUNK base64 channel. Used on Mac
    /// native, where the app process and the test driver share a local
    /// filesystem and `os_log` rate-limits the chunked output.
    private static void emitFileScreenshot(String safeName, byte[] pngBytes,
                                           Image screenshot, ImageIO io) throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = fs.getAppHomePath();
        if (dir == null || dir.length() == 0) {
            throw new IOException("FileSystemStorage.getAppHomePath() returned empty path");
        }
        // CN1's getAppHomePath returns a path with trailing separator on
        // some platforms and without on others; normalise.
        if (!dir.endsWith("/")) {
            dir = dir + "/";
        }
        String outDir = dir + "cn1ss-screenshots/";
        fs.mkdir(outDir);
        String pngPath = outDir + safeName + ".png";
        OutputStream pngFile = fs.openOutputStream(pngPath);
        try {
            pngFile.write(pngBytes);
        } finally {
            pngFile.close();
        }
        println("CN1SS:FILE:test=" + safeName + " channel= path=" + pngPath
                + " bytes=" + pngBytes.length);

        byte[] preview = encodePreview(io, screenshot, safeName);
        if (preview != null && preview.length > 0) {
            String previewPath = outDir + safeName + ".jpg";
            OutputStream previewOut = fs.openOutputStream(previewPath);
            try {
                previewOut.write(preview);
            } finally {
                previewOut.close();
            }
            println("CN1SS:FILE:test=" + safeName + " channel=" + PREVIEW_CHANNEL
                    + " path=" + previewPath + " bytes=" + preview.length);
        } else {
            println("CN1SS:INFO:test=" + safeName + " preview_jpeg_bytes=0 preview_quality=0");
        }
    }

    static byte[] encodePreview(ImageIO io, Image screenshot, String safeName) throws IOException {
        byte[] chosenPreview = null;
        int chosenQuality = 0;
        int smallestBytes = Integer.MAX_VALUE;
        for (int quality : PREVIEW_QUALITIES) {
            ByteArrayOutputStream previewOut = new ByteArrayOutputStream(Math.max(512, screenshot.getWidth() * screenshot.getHeight() / 4));
            io.save(screenshot, previewOut, ImageIO.FORMAT_JPEG, quality / 100f);
            byte[] previewBytes = previewOut.toByteArray();
            if (previewBytes.length == 0) {
                continue;
            }
            if (previewBytes.length < smallestBytes) {
                smallestBytes = previewBytes.length;
                chosenPreview = previewBytes;
                chosenQuality = quality;
            }
            if (previewBytes.length <= MAX_PREVIEW_BYTES) {
                break;
            }
        }
        if (chosenPreview != null) {
            println("CN1SS:INFO:test=" + safeName + " preview_jpeg_bytes=" + chosenPreview.length + " preview_quality=" + chosenQuality);
            if (chosenPreview.length > MAX_PREVIEW_BYTES) {
                println("CN1SS:WARN:test=" + safeName + " preview_exceeds_limit_bytes=" + chosenPreview.length + " max_preview_bytes=" + MAX_PREVIEW_BYTES);
            }
        }
        return chosenPreview;
    }

    static void emitChannel(byte[] bytes, String safeName, String channel) {
        String prefix = channel != null && channel.length() > 0 ? "CN1SS" + channel : "CN1SS";
        if (bytes == null || bytes.length == 0) {
            println(prefix + ":END:" + safeName);
            System.out.flush();
            return;
        }
        String base64 = Base64.encodeNoNewline(bytes);
        int count = 0;

        boolean isAndroid = "and".equals(Display.getInstance().getPlatformName());
        int chunkSize = isAndroid ? CHUNK_SIZE_ANDROID : CHUNK_SIZE_DEFAULT;
        int delay = isAndroid ? DELAY_ANDROID : 0;

        for (int pos = 0; pos < base64.length(); pos += chunkSize) {
            int end = Math.min(pos + chunkSize, base64.length());
            String chunk = base64.substring(pos, end);
            println(prefix + ":" + safeName + ":" + zeroPad(pos, 6) + ":" + chunk);
            count++;
            // Slow down to prevent logcat buffer overflow/truncation
            if (delay > 0) {
                Util.sleep(delay);
            }
        }
        println("CN1SS:INFO:test=" + safeName + " chunks=" + count + " total_b64_len=" + base64.length());
        if (delay > 0) {
            Util.sleep(50);
        }
        println(prefix + ":END:" + safeName);
        System.out.flush();
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

    static String zeroPad(int value, int width) {
        String text = Integer.toString(value);
        if (text.length() >= width) {
            return text;
        }
        StringBuffer builder = new StringBuffer(width);
        for (int i = text.length(); i < width; i++) {
            builder.append('0');
        }
        builder.append(text);
        return builder.toString();
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
                if (Cn1ssWebSocketSink.trySend(safeName, pngBytes, fnv1a64Hex(pngBytes))) {
                    return;
                }
                emitChannel(pngBytes, safeName, "");
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
/// `ACK <safeName>` text frame. ACK_TIMEOUT_MS is generous (10s) — the
/// host writes the PNG to disk and ACKs immediately on LAN; if we hit the
/// timeout something is genuinely broken and the test should fail loudly.
///
/// `trySend` returns true if the WS path successfully uploaded the PNG
/// (or transiently failed after the connection was established), and false
/// if WS is unavailable (no URL configured, unsupported platform, connect
/// timed out). When trySend returns false the caller falls back to the
/// CN1SS:FILE or CN1SS:CHUNK paths.
final class Cn1ssWebSocketSink {
    private static final int ACK_TIMEOUT_MS = 10_000;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final Map<String, AckLatch> pending = new HashMap<String, AckLatch>();
    private static WebSocket socket;
    private static volatile boolean attemptedConnect;
    private static volatile boolean unavailable;

    private Cn1ssWebSocketSink() {
    }

    static synchronized boolean trySend(String safeName, byte[] pngBytes, String hashHex) {
        if (!ensureConnected()) {
            return false;
        }
        AckLatch latch = new AckLatch();
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
        // Display.getProperty() is the only property source the CN1
        // bytecode-compliance check allows on the device runtime --
        // java.lang.System#getProperty(String,String) is not in
        // ParparVM's JavaAPI. Runner scripts feed cn1ss.websocket.url to
        // Display.getProperty via the maven plugin's -Dproperty=...
        // -> Display hint pass-through, which works on every port.
        String url = Display.getInstance().getProperty("cn1ss.websocket.url", "");
        if (url == null || url.length() == 0) {
            unavailable = true;
            return false;
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
