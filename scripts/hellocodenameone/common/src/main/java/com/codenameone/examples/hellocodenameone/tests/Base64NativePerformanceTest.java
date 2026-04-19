package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import com.codenameone.examples.hellocodenameone.Base64Native;
import com.codename1.util.Base64;
import com.codename1.util.Simd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Base64NativePerformanceTest extends BaseTest {
    private static final int PAYLOAD_BYTES = 8192;
    private static final int ITERATIONS = 6000;
    // The image benchmarks now exercise a 256×256 pixel buffer (~262 KB) which is
    // representative of common avatar/photo sizes used in real CN1 apps and large enough
    // to stress memory bandwidth (much larger than L1, comfortably within L2). Iteration
    // counts are sized so each measurement still runs in a reasonable amount of wall time.
    private static final int IMAGE_BENCHMARK_ITERATIONS = 100;
    private static final int IMAGE_BENCHMARK_SIZE = 256;
    private static final byte IMAGE_BENCHMARK_ALPHA = (byte)0x90;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            Base64Native nativeBase64 = NativeLookup.create(Base64Native.class);
            if (nativeBase64 == null || !nativeBase64.isSupported()) {
                emitStat("Base64 benchmark status", "skipped (native base64 bridge unavailable)");
                done();
                return true;
            }

            String payload = buildPayload();
            String nativeEncoded = nativeBase64.encodeUtf8(payload);
            if (nativeEncoded == null || nativeEncoded.length() == 0) {
                fail("Native Base64 encode returned empty result");
                return false;
            }

            byte[] payloadBytes;
            try {
                payloadBytes = payload.getBytes("UTF-8");
            } catch (Exception ex) {
                fail("Failed to encode payload to UTF-8: " + ex);
                return false;
            }

            String cn1Encoded = Base64.encodeNoNewline(payloadBytes);
            String nativeDecoded = nativeBase64.decodeToUtf8(nativeEncoded);
            if (!payload.equals(nativeDecoded)) {
                fail("Native Base64 decode mismatch");
                return false;
            }

            String cn1Decoded = decodeUtf8(cn1Encoded);
            if (!payload.equals(cn1Decoded)) {
                fail("CN1 Base64 decode mismatch");
                return false;
            }

            int encodedLen = ((payloadBytes.length + 2) / 3) * 4;
            byte[] cn1EncodedBytes = new byte[encodedLen];
            int encodedWritten = Base64.encodeNoNewline(payloadBytes, cn1EncodedBytes);
            if (encodedWritten != encodedLen) {
                fail("CN1 preallocated Base64 encode returned unexpected length");
                return false;
            }
            byte[] cn1DecodedBuffer = new byte[payloadBytes.length];
            boolean ios = isIos();
            Simd simd = Simd.get();
            boolean runSimdBenchmark = false;
            String simdStatus = null;
            Throwable simdFailure = null;
            byte[] simdPayloadBytes = null;
            byte[] simdEncodedBytes = null;
            byte[] simdDecodedBuffer = null;
            if (ios) {
                if (simd == null) {
                    simdStatus = "unavailable (Simd.get() returned null)";
                } else if (!simd.isSupported()) {
                    simdStatus = "unsupported on this iOS runtime";
                } else {
                    try {
                        simdPayloadBytes = simd.allocByte(payloadBytes.length);
                        System.arraycopy(payloadBytes, 0, simdPayloadBytes, 0, payloadBytes.length);
                        simdEncodedBytes = simd.allocByte(encodedLen);
                        simdDecodedBuffer = simd.allocByte(payloadBytes.length);

                        int simdEncodedWritten = Base64.encodeNoNewlineSimd(simdPayloadBytes, 0, simdPayloadBytes.length, simdEncodedBytes, 0);
                        if (simdEncodedWritten != encodedLen) {
                            simdStatus = "unavailable (unexpected SIMD encode length " + simdEncodedWritten + ")";
                        } else if (!byteArraysEqual(cn1EncodedBytes, simdEncodedBytes, encodedLen)) {
                            simdStatus = "unavailable (SIMD encode mismatch)";
                        } else {
                            int simdDecodedWritten = Base64.decodeNoWhitespaceSimd(simdEncodedBytes, 0, encodedLen, simdDecodedBuffer, 0);
                            if (simdDecodedWritten != payloadBytes.length) {
                                simdStatus = "unavailable (unexpected SIMD decode length " + simdDecodedWritten + ")";
                            } else if (!byteArraysEqual(payloadBytes, simdDecodedBuffer, payloadBytes.length)) {
                                simdStatus = "unavailable (SIMD decode mismatch)";
                            } else {
                                runSimdBenchmark = true;
                            }
                        }
                    } catch (Throwable t) {
                        simdFailure = t;
                        simdStatus = "failed (" + formatThrowable(t) + ")";
                        logThrowable("CN1SS:ERR:Base64 SIMD benchmark exception", t);
                    }
                }
            }

            if (!ios) {
                warmup(nativeBase64, payload, payloadBytes, nativeEncoded, cn1EncodedBytes, cn1DecodedBuffer,
                        false, simdPayloadBytes, simdEncodedBytes, simdDecodedBuffer, encodedLen);
            }
            if (runSimdBenchmark) {
                warmup(nativeBase64, payload, payloadBytes, nativeEncoded, cn1EncodedBytes, cn1DecodedBuffer,
                        true, simdPayloadBytes, simdEncodedBytes, simdDecodedBuffer, encodedLen);
            }

            long nativeEncodeMs = measureNativeEncode(nativeBase64, payload);
            long cn1EncodeMs = measureCn1Encode(payloadBytes, cn1EncodedBytes);
            long nativeDecodeMs = measureNativeDecode(nativeBase64, nativeEncoded);
            long cn1DecodeMs = measureCn1Decode(cn1EncodedBytes, cn1DecodedBuffer);
            long simdEncodeMs = runSimdBenchmark ? measureSimdEncode(simdPayloadBytes, simdEncodedBytes) : -1;
            long simdDecodeMs = runSimdBenchmark ? measureSimdDecode(simdEncodedBytes, simdDecodedBuffer) : -1;

            double encodeRatio = cn1EncodeMs / Math.max(1.0, (double) nativeEncodeMs);
            double decodeRatio = cn1DecodeMs / Math.max(1.0, (double) nativeDecodeMs);
            emitStat("Base64 payload size", payloadBytes.length + " bytes");
            emitStat("Base64 benchmark iterations", String.valueOf(ITERATIONS));
            emitStat("Base64 native encode", formatMs(nativeEncodeMs));
            emitStat("Base64 CN1 encode", formatMs(cn1EncodeMs));
            emitStat("Base64 encode ratio (CN1/native)", formatRatio(encodeRatio));
            emitStat("Base64 native decode", formatMs(nativeDecodeMs));
            emitStat("Base64 CN1 decode", formatMs(cn1DecodeMs));
            emitStat("Base64 decode ratio (CN1/native)", formatRatio(decodeRatio));
            if (runSimdBenchmark) {
                double simdEncodeRatioVsNative = simdEncodeMs / Math.max(1.0, (double) nativeEncodeMs);
                double simdDecodeRatioVsNative = simdDecodeMs / Math.max(1.0, (double) nativeDecodeMs);
                double simdEncodeRatioVsCn1 = simdEncodeMs / Math.max(1.0, (double) cn1EncodeMs);
                double simdDecodeRatioVsCn1 = simdDecodeMs / Math.max(1.0, (double) cn1DecodeMs);
                emitStat("Base64 SIMD encode", formatMs(simdEncodeMs));
                emitStat("Base64 encode ratio (SIMD/native)", formatRatio(simdEncodeRatioVsNative));
                emitStat("Base64 encode ratio (SIMD/CN1)", formatRatio(simdEncodeRatioVsCn1));
                emitStat("Base64 SIMD decode", formatMs(simdDecodeMs));
                emitStat("Base64 decode ratio (SIMD/native)", formatRatio(simdDecodeRatioVsNative));
                emitStat("Base64 decode ratio (SIMD/CN1)", formatRatio(simdDecodeRatioVsCn1));
            } else if (simdStatus != null) {
                emitStat("Base64 SIMD benchmark status", simdStatus);
            }

            if (simd != null && simd.isSupported()) {
                ImageIO imageIo = ImageIO.getImageIO();
                if (imageIo == null) {
                    emitStat("Image encode benchmark status", "skipped (ImageIO unavailable)");
                } else if (!imageIo.isFormatSupported(ImageIO.FORMAT_PNG)) {
                    emitStat("Image encode benchmark status", "skipped (PNG unsupported)");
                } else {
                    Image benchmarkImage = buildBenchmarkImage(IMAGE_BENCHMARK_SIZE, IMAGE_BENCHMARK_SIZE, false);
                    Image benchmarkMaskImage = buildBenchmarkImage(IMAGE_BENCHMARK_SIZE, IMAGE_BENCHMARK_SIZE, true);
                    Object benchmarkMask = benchmarkMaskImage.createMask();
                    int removeColor = benchmarkImage.getRGB()[0] & 0xffffff;
                    warmupImageBenchmarks(imageIo, benchmarkImage, benchmarkMaskImage, benchmarkMask, removeColor);
                    long createMaskScalarMs = measureCreateMask(benchmarkImage, false);
                    long createMaskSimdMs = measureCreateMask(benchmarkImage, true);
                    long applyMaskScalarMs = measureApplyMask(benchmarkImage, benchmarkMask, false);
                    long applyMaskSimdMs = measureApplyMask(benchmarkImage, benchmarkMask, true);
                    long modifyAlphaScalarMs = measureModifyAlpha(benchmarkImage, false);
                    long modifyAlphaSimdMs = measureModifyAlpha(benchmarkImage, true);
                    long modifyAlphaRemoveColorScalarMs = measureModifyAlphaRemoveColor(benchmarkImage, removeColor, false);
                    long modifyAlphaRemoveColorSimdMs = measureModifyAlphaRemoveColor(benchmarkImage, removeColor, true);
                    long pngScalarMs = measureImageEncode(imageIo, benchmarkImage, benchmarkMaskImage, ImageIO.FORMAT_PNG, 1f, false);
                    long pngSimdMs = measureImageEncode(imageIo, benchmarkImage, benchmarkMaskImage, ImageIO.FORMAT_PNG, 1f, true);
                    emitStat("Image encode benchmark iterations", String.valueOf(IMAGE_BENCHMARK_ITERATIONS));
                    emitStat("Image createMask (SIMD off)", formatMs(createMaskScalarMs));
                    emitStat("Image createMask (SIMD on)", formatMs(createMaskSimdMs));
                    emitStat("Image createMask ratio (SIMD on/off)", formatRatio(createMaskSimdMs, createMaskScalarMs));
                    emitStat("Image applyMask (SIMD off)", formatMs(applyMaskScalarMs));
                    emitStat("Image applyMask (SIMD on)", formatMs(applyMaskSimdMs));
                    emitStat("Image applyMask ratio (SIMD on/off)", formatRatio(applyMaskSimdMs, applyMaskScalarMs));
                    emitStat("Image modifyAlpha (SIMD off)", formatMs(modifyAlphaScalarMs));
                    emitStat("Image modifyAlpha (SIMD on)", formatMs(modifyAlphaSimdMs));
                    emitStat("Image modifyAlpha ratio (SIMD on/off)", formatRatio(modifyAlphaSimdMs, modifyAlphaScalarMs));
                    emitStat("Image modifyAlpha removeColor (SIMD off)", formatMs(modifyAlphaRemoveColorScalarMs));
                    emitStat("Image modifyAlpha removeColor (SIMD on)", formatMs(modifyAlphaRemoveColorSimdMs));
                    emitStat("Image modifyAlpha removeColor ratio (SIMD on/off)", formatRatio(modifyAlphaRemoveColorSimdMs, modifyAlphaRemoveColorScalarMs));
                    emitStat("Image PNG encode (SIMD off)", formatMs(pngScalarMs));
                    emitStat("Image PNG encode (SIMD on)", formatMs(pngSimdMs));
                    emitStat("Image PNG encode ratio (SIMD on/off)", formatRatio(pngSimdMs, pngScalarMs));
                    if (imageIo.isFormatSupported(ImageIO.FORMAT_JPEG)) {
                        long jpegMs = measureImageEncode(imageIo, benchmarkImage, benchmarkMaskImage, ImageIO.FORMAT_JPEG, 0.82f, false);
                        emitStat("Image JPEG encode", formatMs(jpegMs));
                    } else {
                        emitStat("Image JPEG encode benchmark status", "skipped (JPEG unsupported)");
                    }
                }
            } else {
                emitStat("Image encode benchmark status", "skipped (SIMD unsupported)");
            }

            if (simdFailure != null) {
                fail("Base64 SIMD benchmark failed: " + formatThrowable(simdFailure));
                return false;
            }

            done();
            return true;
        } catch (Throwable t) {
            emitStat("Base64 benchmark status", "failed (" + formatThrowable(t) + ")");
            logThrowable("CN1SS:ERR:Base64 benchmark exception", t);
            fail("Base64 benchmark failed: " + t);
            return false;
        }
    }

    private static void warmup(Base64Native nativeBase64, String payload, byte[] payloadBytes, String nativeEncoded, byte[] cn1EncodedBytes,
                               byte[] cn1DecodedBuffer, boolean includeSimd, byte[] simdPayloadBytes, byte[] simdEncodedBytes,
                               byte[] simdDecodedBuffer, int encodedLen) {
        for (int i = 0; i < 40; i++) {
            nativeBase64.encodeUtf8(payload);
            int cn1EncodedWritten = Base64.encodeNoNewline(payloadBytes, cn1EncodedBytes);
            if (cn1EncodedWritten != encodedLen) {
                throw new IllegalStateException("Warmup CN1 encode length mismatch");
            }
            nativeBase64.decodeToUtf8(nativeEncoded);
            int cn1DecodedWritten = Base64.decode(cn1EncodedBytes, cn1DecodedBuffer);
            if (cn1DecodedWritten != payloadBytes.length || !byteArraysEqual(payloadBytes, cn1DecodedBuffer, payloadBytes.length)) {
                throw new IllegalStateException("Warmup CN1 decode mismatch");
            }
            if (includeSimd) {
                int simdEncodedWritten = Base64.encodeNoNewlineSimd(simdPayloadBytes, 0, simdPayloadBytes.length, simdEncodedBytes, 0);
                if (simdEncodedWritten != encodedLen || !byteArraysEqual(cn1EncodedBytes, simdEncodedBytes, encodedLen)) {
                    throw new IllegalStateException("Warmup SIMD encode mismatch");
                }
                int simdDecodedWritten = Base64.decodeNoWhitespaceSimd(simdEncodedBytes, 0, encodedLen, simdDecodedBuffer, 0);
                if (simdDecodedWritten != payloadBytes.length || !byteArraysEqual(payloadBytes, simdDecodedBuffer, payloadBytes.length)) {
                    throw new IllegalStateException("Warmup SIMD decode mismatch");
                }
            }
        }
    }

    private static long measureNativeEncode(Base64Native nativeBase64, String payload) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            nativeBase64.encodeUtf8(payload);
        }
        return System.currentTimeMillis() - start;
    }

    private static long measureCn1Encode(byte[] payloadBytes, byte[] outputBuffer) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            Base64.encodeNoNewline(payloadBytes, outputBuffer);
        }
        return System.currentTimeMillis() - start;
    }

    private static long measureNativeDecode(Base64Native nativeBase64, String encoded) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            nativeBase64.decodeToUtf8(encoded);
        }
        return System.currentTimeMillis() - start;
    }

    private static long measureCn1Decode(byte[] encoded, byte[] outputBuffer) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            Base64.decode(encoded, outputBuffer);
        }
        return System.currentTimeMillis() - start;
    }

    private static long measureSimdEncode(byte[] payloadBytes, byte[] outputBuffer) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            Base64.encodeNoNewlineSimd(payloadBytes, 0, payloadBytes.length, outputBuffer, 0);
        }
        return System.currentTimeMillis() - start;
    }

    private static long measureSimdDecode(byte[] encoded, byte[] outputBuffer) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            Base64.decodeNoWhitespaceSimd(encoded, 0, encoded.length, outputBuffer, 0);
        }
        return System.currentTimeMillis() - start;
    }

    private static void warmupImageBenchmarks(ImageIO imageIo, Image benchmarkImage, Image benchmarkMaskImage,
                                              Object benchmarkMask, int removeColor) throws IOException {
        measureCreateMask(benchmarkImage, false, 20);
        measureCreateMask(benchmarkImage, true, 20);
        measureApplyMask(benchmarkImage, benchmarkMask, false, 20);
        measureApplyMask(benchmarkImage, benchmarkMask, true, 20);
        measureModifyAlpha(benchmarkImage, false, 20);
        measureModifyAlpha(benchmarkImage, true, 20);
        measureModifyAlphaRemoveColor(benchmarkImage, removeColor, false, 20);
        measureModifyAlphaRemoveColor(benchmarkImage, removeColor, true, 20);
        measureImageEncode(imageIo, benchmarkImage, benchmarkMaskImage, ImageIO.FORMAT_PNG, 1f, false, 20);
        measureImageEncode(imageIo, benchmarkImage, benchmarkMaskImage, ImageIO.FORMAT_PNG, 1f, true, 20);
    }

    private static long measureCreateMask(Image image, boolean enableSimd) {
        return measureCreateMask(image, enableSimd, IMAGE_BENCHMARK_ITERATIONS);
    }

    private static long measureCreateMask(Image image, boolean enableSimd, int iterations) {
        boolean originalSimd = Image.isSimdOptimizationsEnabled();
        try {
            Image.setSimdOptimizationsEnabled(enableSimd);
            long start = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                image.createMask();
            }
            return System.currentTimeMillis() - start;
        } finally {
            Image.setSimdOptimizationsEnabled(originalSimd);
        }
    }

    private static long measureApplyMask(Image image, Object mask, boolean enableSimd) {
        return measureApplyMask(image, mask, enableSimd, IMAGE_BENCHMARK_ITERATIONS);
    }

    private static long measureApplyMask(Image image, Object mask, boolean enableSimd, int iterations) {
        boolean originalSimd = Image.isSimdOptimizationsEnabled();
        try {
            Image.setSimdOptimizationsEnabled(enableSimd);
            long start = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                image.applyMask(mask);
            }
            return System.currentTimeMillis() - start;
        } finally {
            Image.setSimdOptimizationsEnabled(originalSimd);
        }
    }

    private static long measureModifyAlpha(Image image, boolean enableSimd) {
        return measureModifyAlpha(image, enableSimd, IMAGE_BENCHMARK_ITERATIONS);
    }

    private static long measureModifyAlpha(Image image, boolean enableSimd, int iterations) {
        boolean originalSimd = Image.isSimdOptimizationsEnabled();
        try {
            Image.setSimdOptimizationsEnabled(enableSimd);
            long start = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                image.modifyAlpha(IMAGE_BENCHMARK_ALPHA);
            }
            return System.currentTimeMillis() - start;
        } finally {
            Image.setSimdOptimizationsEnabled(originalSimd);
        }
    }

    private static long measureModifyAlphaRemoveColor(Image image, int removeColor, boolean enableSimd) {
        return measureModifyAlphaRemoveColor(image, removeColor, enableSimd, IMAGE_BENCHMARK_ITERATIONS);
    }

    private static long measureModifyAlphaRemoveColor(Image image, int removeColor, boolean enableSimd, int iterations) {
        boolean originalSimd = Image.isSimdOptimizationsEnabled();
        try {
            Image.setSimdOptimizationsEnabled(enableSimd);
            long start = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                image.modifyAlpha(IMAGE_BENCHMARK_ALPHA, removeColor);
            }
            return System.currentTimeMillis() - start;
        } finally {
            Image.setSimdOptimizationsEnabled(originalSimd);
        }
    }

    private static long measureImageEncode(ImageIO imageIo, Image benchmarkImage, Image benchmarkMaskImage,
                                           String format, float quality, boolean enableSimd) throws IOException {
        return measureImageEncode(imageIo, benchmarkImage, benchmarkMaskImage, format, quality, enableSimd, IMAGE_BENCHMARK_ITERATIONS);
    }

    private static long measureImageEncode(ImageIO imageIo, Image benchmarkImage, Image benchmarkMaskImage,
                                           String format, float quality, boolean enableSimd, int iterations) throws IOException {
        boolean originalSimd = Image.isSimdOptimizationsEnabled();
        try {
            Image.setSimdOptimizationsEnabled(enableSimd);
            long start = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                Image alphaAdjusted = benchmarkImage.modifyAlpha((byte) 0x90);
                Object mask = benchmarkMaskImage.createMask();
                Image masked = alphaAdjusted.applyMask(mask);
                ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
                imageIo.save(masked, out, format, quality);
            }
            return System.currentTimeMillis() - start;
        } finally {
            Image.setSimdOptimizationsEnabled(originalSimd);
        }
    }

    private static Image buildBenchmarkImage(int width, int height, boolean maskImage) {
        int[] rgb = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = x + y * width;
                int alpha;
                if (maskImage) {
                    alpha = 0xff;
                } else {
                    alpha = 80 + ((x * 17 + y * 29) & 0x7f);
                }
                int red = (x * 13 + y * 7) & 0xff;
                int green = (x * 5 + y * 19) & 0xff;
                int blue = (x * 23 + y * 11) & 0xff;
                rgb[offset] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        }
        return Image.createImage(rgb, width, height);
    }

    private static String decodeUtf8(String base64) {
        try {
            return new String(Base64.decode(base64.getBytes()), "UTF-8");
        } catch (Exception ex) {
            return null;
        }
    }

    private static String buildPayload() {
        StringBuilder sb = new StringBuilder(PAYLOAD_BYTES);
        for (int i = 0; i < PAYLOAD_BYTES; i++) {
            sb.append((char) ('A' + (i % 26)));
        }
        return sb.toString();
    }

    private static boolean isIos() {
        String platformName = Display.getInstance().getPlatformName();
        return platformName != null && platformName.toLowerCase().contains("ios");
    }

    private static boolean byteArraysEqual(byte[] a, byte[] b, int len) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length < len || b.length < len) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static String formatMs(double millis) {
        return formatDecimal(millis, 3) + " ms";
    }

    private static String formatRatio(double ratio) {
        double slowerPct = (ratio - 1.0) * 100.0;
        return formatDecimal(ratio, 3) + "x (" + formatDecimal(Math.abs(slowerPct), 1) + "% " + (slowerPct >= 0 ? "slower" : "faster") + ")";
    }

    private static String formatRatio(long value, long reference) {
        if (reference <= 0) {
            return "N/A (reference time was 0ms)";
        }
        return formatRatio(value / (double) reference);
    }

    private static String formatDecimal(double value, int decimals) {
        boolean negative = value < 0;
        double abs = Math.abs(value);
        long scale = 1;
        for (int i = 0; i < decimals; i++) {
            scale *= 10;
        }
        long scaled = Math.round(abs * scale);
        long whole = scaled / scale;
        long fraction = scaled % scale;
        String fractionStr = String.valueOf(fraction);
        while (fractionStr.length() < decimals) {
            fractionStr = "0" + fractionStr;
        }
        String formatted = whole + (decimals > 0 ? "." + fractionStr : "");
        return negative ? "-" + formatted : formatted;
    }

    private static String formatThrowable(Throwable t) {
        if (t == null) {
            return "unknown error";
        }
        String type = t.getClass().getSimpleName();
        String message = t.getMessage();
        if (message == null || message.length() == 0) {
            return type;
        }
        return type + ": " + message;
    }

    private static void logThrowable(String prefix, Throwable t) {
        if (t == null) {
            System.out.println(prefix + "=unknown error");
            return;
        }
        System.out.println(prefix + "=" + t);
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return;
        }
        for (StackTraceElement element : stack) {
            System.out.println("CN1SS:ERR:Stack:" + element);
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            System.out.println("CN1SS:ERR:Cause=" + cause);
            StackTraceElement[] causeStack = cause.getStackTrace();
            if (causeStack != null) {
                for (StackTraceElement element : causeStack) {
                    System.out.println("CN1SS:ERR:CauseStack:" + element);
                }
            }
        }
    }

    private static void emitStat(String metric, String value) {
        System.out.println("CN1SS:STAT:" + metric + ": " + value);
    }
}
