package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

final class Cn1ssDeviceRunnerHelper {
    private static final int CHUNK_SIZE = 900;
    private static final int MAX_PREVIEW_BYTES = 20 * 1024;
    private static final String PREVIEW_CHANNEL = "PREVIEW";
    private static final String LOG_CHANNEL = "LOG";
    private static final String START_CHANNEL = "START";
    private static final String LOG_FILE_NAME = "cn1ss-device-runner.log";
    private static final int[] PREVIEW_QUALITIES = new int[] {60, 50, 40, 35, 30, 25, 20, 18, 16, 14, 12, 10, 8, 6, 5, 4, 3, 2, 1};
    private static final String LOG_FILE_PATH = initializeLogFile();
    private static boolean globalErrorHooksInstalled;

    private Cn1ssDeviceRunnerHelper() {
    }

    private static String initializeLogFile() {
        FileSystemStorage storage = FileSystemStorage.getInstance();
        String path = storage.getAppHomePath() + LOG_FILE_NAME;
        if (storage.exists(path)) {
            storage.delete(path);
        }
        Log.getInstance().setFileURL(path);
        Log.getInstance().setFileWriteEnabled(true);
        Log.p("CN1SS logging initialized at " + path);
        return path;
    }

    static void resetLogCapture(String testName) {
        if (LOG_FILE_PATH == null || LOG_FILE_PATH.length() == 0) {
            return;
        }
        String safeName = sanitizeTestName(testName);
        FileSystemStorage storage = FileSystemStorage.getInstance();
        if (storage.exists(LOG_FILE_PATH)) {
            storage.delete(LOG_FILE_PATH);
        }
        Log.getInstance().setFileURL(LOG_FILE_PATH);
        Log.getInstance().setFileWriteEnabled(true);
        println("CN1SS:INFO:test=" + safeName + " log_reset=true path=" + LOG_FILE_PATH);
    }

    static void emitTestStartMarker(String testName) {
        String safeName = sanitizeTestName(testName);
        emitChannel(toUtf8Bytes("start:" + safeName), safeName, START_CHANNEL);
    }

    static void runOnEdtSync(Runnable runnable) {
        Display display = Display.getInstance();
        if (display.isEdt()) {
            runnable.run();
        } else {
            display.callSeriallyAndWait(runnable);
        }
    }

    static void ensureGlobalErrorTaps() {
        if (globalErrorHooksInstalled) {
            return;
        }
        globalErrorHooksInstalled = true;
        Log.bindCrashProtection(true);
        Display.getInstance().addEdtErrorHandler(evt -> {
            Object source = evt.getSource();
            if (source instanceof Throwable) {
                Throwable t = (Throwable) source;
                Log.e(t);
                println("CN1SS:ERR:edt thread throwable=" + t);
            } else {
                println("CN1SS:ERR:edt event=" + source);
            }
        });
        println("CN1SS:INFO:global error taps installed");
    }

    static void waitForMillis(long millis) {
        int duration = (int) Math.max(1, Math.min(Integer.MAX_VALUE, millis));
        Util.sleep(duration);
    }

    static boolean emitCurrentFormScreenshot(String testName) {
        String safeName = sanitizeTestName(testName);
        Form current = Display.getInstance().getCurrent();
        if (current == null) {
            println("CN1SS:ERR:test=" + safeName + " message=Current form is null");
            emitLogOutput(safeName);
            println("CN1SS:END:" + safeName);
            return false;
        }
        int width = Math.max(1, current.getWidth());
        int height = Math.max(1, current.getHeight());
        Image[] img = new Image[1];
        Display.getInstance().screenshot(screen -> img[0] = screen);
        long time = System.currentTimeMillis();
        Display.getInstance().invokeAndBlock(() -> {
            while(img[0] == null) {
                Util.sleep(50);
                // timeout
                if (System.currentTimeMillis() - time > 2000) {
                    emitLogOutput(safeName);
                    return;
                }
            }
        });
        if (img[0] == null) {
            println("CN1SS:ERR:test=" + safeName + " message=Screenshot process timed out");
            emitLogOutput(safeName);
            println("CN1SS:END:" + safeName);
            return false;
        }
        Image screenshot = img[0];
        try {
            ImageIO io = ImageIO.getImageIO();
            if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                println("CN1SS:ERR:test=" + safeName + " message=PNG encoding unavailable");
                emitLogOutput(safeName);
                println("CN1SS:END:" + safeName);
                return false;
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
            emitLogOutput(safeName);
            return true;
        } catch (IOException ex) {
            println("CN1SS:ERR:test=" + safeName + " message=" + ex);
            Log.e(ex);
            emitLogOutput(safeName);
            println("CN1SS:END:" + safeName);
            return false;
        } finally {
            screenshot.dispose();
        }
    }

    static void emitLogChannel(String testName) {
        String safeName = sanitizeTestName(testName);
        emitLogOutput(safeName);
        println("CN1SS:END:" + safeName);
    }

    private static byte[] encodePreview(ImageIO io, Image screenshot, String safeName) throws IOException {
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

    private static void emitChannel(byte[] bytes, String safeName, String channel) {
        String prefix = channel != null && channel.length() > 0 ? "CN1SS" + channel : "CN1SS";
        if (bytes == null || bytes.length == 0) {
            println(prefix + ":END:" + safeName);
            System.out.flush();
            return;
        }
        String base64 = Base64.encodeNoNewline(bytes);
        int count = 0;
        for (int pos = 0; pos < base64.length(); pos += CHUNK_SIZE) {
            int end = Math.min(pos + CHUNK_SIZE, base64.length());
            String chunk = base64.substring(pos, end);
            println(prefix + ":" + safeName + ":" + zeroPad(pos, 6) + ":" + chunk);
            count++;
        }
        println("CN1SS:INFO:test=" + safeName + " chunks=" + count + " total_b64_len=" + base64.length());
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

    private static byte[] toUtf8Bytes(String value) {
        if (value == null) {
            return new byte[0];
        }
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // UTF-8 should always be present, but fall back to ASCII-safe encoding for CN1
            byte[] bytes = new byte[value.length()];
            for (int i = 0; i < value.length(); i++) {
                bytes[i] = (byte) (value.charAt(i) & 0x7F);
            }
            return bytes;
        }
    }

    private static boolean isSafeChar(char ch) {
        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
            return true;
        }
        if (ch >= '0' && ch <= '9') {
            return true;
        }
        return ch == '_' || ch == '.' || ch == '-';
    }

    private static String zeroPad(int value, int width) {
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

    private static void emitLogOutput(String safeName) {
        byte[] logBytes = readLogBytes();
        if (logBytes == null || logBytes.length == 0) {
            println("CN1SS:INFO:test=" + safeName + " log_bytes=0");
            emitChannel("(no log entries captured)".getBytes(), safeName, LOG_CHANNEL);
            return;
        }
        println("CN1SS:INFO:test=" + safeName + " log_bytes=" + logBytes.length);
        emitChannel(logBytes, safeName, LOG_CHANNEL);
    }

    private static byte[] readLogBytes() {
        if (LOG_FILE_PATH == null || LOG_FILE_PATH.length() == 0) {
            return null;
        }
        FileSystemStorage storage = FileSystemStorage.getInstance();
        if (!storage.exists(LOG_FILE_PATH)) {
            return null;
        }
        InputStream input = null;
        try {
            input = storage.openInputStream(LOG_FILE_PATH);
            return Util.readInputStream(input);
        } catch (IOException ex) {
            Log.e(ex);
            return null;
        } finally {
            Util.cleanup(input);
        }
    }

    private static void println(String line) {
        System.out.println(line);
    }
}
