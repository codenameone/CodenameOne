package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codenameone.examples.hellocodenameone.Base64Native;
import com.codename1.util.Base64;


public class Base64NativePerformanceTest extends BaseTest {
    private static final int PAYLOAD_BYTES = 8192;
    private static final int ITERATIONS = 6000;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        Base64Native nativeBase64 = NativeLookup.create(Base64Native.class);
        if (nativeBase64 == null || !nativeBase64.isSupported()) {
            System.out.println("CN1SS:STAT:Base64 benchmark status: skipped (native base64 bridge unavailable)");
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

        if (!isIos()) {
            warmup(nativeBase64, payload, payloadBytes, nativeEncoded, cn1EncodedBytes, cn1DecodedBuffer);
        }

        long nativeEncodeMs = measureNativeEncode(nativeBase64, payload);
        long cn1EncodeMs = measureCn1Encode(payloadBytes, cn1EncodedBytes);
        long nativeDecodeMs = measureNativeDecode(nativeBase64, nativeEncoded);
        long cn1DecodeMs = measureCn1Decode(cn1EncodedBytes, cn1DecodedBuffer);

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

        done();
        return true;
    }

    private static void warmup(Base64Native nativeBase64, String payload, byte[] payloadBytes, String nativeEncoded, byte[] cn1EncodedBytes, byte[] cn1DecodedBuffer) {
        for (int i = 0; i < 40; i++) {
            nativeBase64.encodeUtf8(payload);
            Base64.encodeNoNewline(payloadBytes, cn1EncodedBytes);
            nativeBase64.decodeToUtf8(nativeEncoded);
            Base64.decode(cn1EncodedBytes, cn1DecodedBuffer);
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

    private static String formatMs(double millis) {
        return formatDecimal(millis, 3) + " ms";
    }

    private static String formatRatio(double ratio) {
        double slowerPct = (ratio - 1.0) * 100.0;
        return formatDecimal(ratio, 3) + "x (" + formatDecimal(Math.abs(slowerPct), 1) + "% " + (slowerPct >= 0 ? "slower" : "faster") + ")";
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

    private static void emitStat(String metric, String value) {
        System.out.println("CN1SS:STAT:" + metric + ": " + value);
    }
}
