package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class Cn1ssDeviceRunnerHelper {
    private static final int CHUNK_SIZE = 2000;
    private static final int MAX_PREVIEW_BYTES = 20 * 1024;
    private static final String PREVIEW_CHANNEL = "PREVIEW";
    private static final int[] PREVIEW_QUALITIES = new int[] {60, 50, 40, 35, 30, 25, 20, 18, 16, 14, 12, 10, 8, 6, 5, 4, 3, 2, 1};

    private Cn1ssDeviceRunnerHelper() {
    }

    static void runOnEdtSync(Runnable runnable) {
        Display display = Display.getInstance();
        if (display.isEdt()) {
            runnable.run();
        } else {
            display.callSeriallyAndWait(runnable);
        }
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
            println("CN1SS:END:" + safeName);
            return false;
        }
        int width = Math.max(1, current.getWidth());
        int height = Math.max(1, current.getHeight());
        Image screenshot = Image.createImage(width, height);
        current.paintComponent(screenshot.getGraphics(), true);
        try {
            ImageIO io = ImageIO.getImageIO();
            if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                println("CN1SS:ERR:test=" + safeName + " message=PNG encoding unavailable");
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
            return true;
        } catch (IOException ex) {
            println("CN1SS:ERR:test=" + safeName + " message=" + ex);
            ex.printStackTrace();
            println("CN1SS:END:" + safeName);
            return false;
        } finally {
            screenshot.dispose();
        }
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

    private static void println(String line) {
        System.out.println(line);
    }
}
