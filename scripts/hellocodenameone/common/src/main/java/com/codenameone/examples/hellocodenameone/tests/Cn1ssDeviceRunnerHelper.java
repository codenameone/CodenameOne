package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

    /// Emits an off-screen Image PNG directly through System.out without
    /// going through [emitChannel]. The JS port has a fallback bound to
    /// emitChannel's method ID that hijacks the primary screenshot channel
    /// and replaces the payload with a host capture of the visible browser
    /// canvas (workaround for OffscreenCanvas staleness in
    /// Display.screenshot()). For tests that already constructed the
    /// ground-truth Image themselves (animation/transition grids,
    /// AbstractComponentReplaceScreenshotTest), the hijack throws away the
    /// correct bytes and substitutes a stale visible canvas. Use this entry
    /// point so the Java-rendered PNG reaches the chunk stream verbatim.
    static void emitImageDirect(Image image, String testName, Runnable onComplete) {
        String safeName = sanitizeTestName(testName);
        if (image == null) {
            println("CN1SS:ERR:test=" + safeName + " message=Image is null");
            emitPlaceholderScreenshot(safeName);
            complete(onComplete);
            return;
        }
        try {
            ImageIO io = ImageIO.getImageIO();
            if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                println("CN1SS:ERR:test=" + safeName + " message=PNG encoding unavailable");
                emitPlaceholderScreenshot(safeName);
                return;
            }
            int width = Math.max(1, image.getWidth());
            int height = Math.max(1, image.getHeight());
            if (Display.getInstance().isSimulator()) {
                io.save(image, Storage.getInstance().createOutputStream(safeName + ".png"), ImageIO.FORMAT_PNG, 1);
            }
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream(Math.max(1024, width * height / 2));
            io.save(image, pngOut, ImageIO.FORMAT_PNG, 1f);
            byte[] pngBytes = pngOut.toByteArray();
            println("CN1SS:INFO:test=" + safeName + " png_bytes=" + pngBytes.length);
            emitChannelDirect(pngBytes, safeName, "");

            byte[] preview = encodePreview(io, image, safeName);
            if (preview != null && preview.length > 0) {
                emitChannelDirect(preview, safeName, PREVIEW_CHANNEL);
            } else {
                println("CN1SS:INFO:test=" + safeName + " preview_jpeg_bytes=0 preview_quality=0");
            }
        } catch (IOException ex) {
            println("CN1SS:ERR:test=" + safeName + " message=" + ex);
            Log.e(ex);
            emitPlaceholderScreenshot(safeName);
        } finally {
            image.dispose();
            complete(onComplete);
        }
    }

    static void emitImage(Image image, String testName, Runnable onComplete) {
        String safeName = sanitizeTestName(testName);
        if (image == null) {
            println("CN1SS:ERR:test=" + safeName + " message=Image is null");
            emitPlaceholderScreenshot(safeName);
            complete(onComplete);
            return;
        }
        try {
            ImageIO io = ImageIO.getImageIO();
            if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                println("CN1SS:ERR:test=" + safeName + " message=PNG encoding unavailable");
                emitPlaceholderScreenshot(safeName);
                return;
            }
            int width = Math.max(1, image.getWidth());
            int height = Math.max(1, image.getHeight());
            if (Display.getInstance().isSimulator()) {
                io.save(image, Storage.getInstance().createOutputStream(safeName + ".png"), ImageIO.FORMAT_PNG, 1);
            }
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream(Math.max(1024, width * height / 2));
            io.save(image, pngOut, ImageIO.FORMAT_PNG, 1f);
            byte[] pngBytes = pngOut.toByteArray();
            println("CN1SS:INFO:test=" + safeName + " png_bytes=" + pngBytes.length);
            emitChannel(pngBytes, safeName, "");

            byte[] preview = encodePreview(io, image, safeName);
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
            image.dispose();
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
        Display.getInstance().screenshot(screen -> {
            if (screen == null) {
                println("CN1SS:ERR:test=" + safeName + " message=Screenshot callback returned null");
                emitPlaceholderScreenshot(safeName);
                complete(onComplete);
                return;
            }
            Image screenshot = screen;
            try {
                ImageIO io = ImageIO.getImageIO();
                if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                    println("CN1SS:ERR:test=" + safeName + " message=PNG encoding unavailable");
                    emitPlaceholderScreenshot(safeName);
                    return;
                }
                if(Display.getInstance().isSimulator()) {
                    io.save(screenshot, Storage.getInstance().createOutputStream(safeName + ".png"), ImageIO.FORMAT_PNG, 1);
                }
                ByteArrayOutputStream pngOut = new ByteArrayOutputStream(Math.max(1024, width * height / 2));
                io.save(screenshot, pngOut, ImageIO.FORMAT_PNG, 1f);
                byte[] pngBytes = pngOut.toByteArray();
                println("CN1SS:INFO:test=" + safeName + " png_bytes=" + pngBytes.length);
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
                complete(onComplete);
            }
        });
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

    /// Same body as [emitChannel] but with a different method ID so the JS
    /// port's `emitChannelFastJs` fallback (in port.js) does not bind to
    /// it. Used by [emitImageDirect] so off-screen Image PNG bytes reach
    /// the chunk stream verbatim instead of being replaced with a host
    /// capture of the (potentially stale) visible browser canvas.
    static void emitChannelDirect(byte[] bytes, String safeName, String channel) {
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
}
