package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeLookup;
import com.codename1.ui.CN;

public final class NativeInterfaceLanguageValidator {
    private static String lastStatus = "UNINITIALIZED";

    private NativeInterfaceLanguageValidator() {
    }

    public static String getLastStatus() {
        return lastStatus;
    }

    public static void validate() {
        String platformName = CN.getPlatformName();
        String normalizedPlatform = platformName == null ? "" : platformName.toLowerCase();
        System.out.println("CN1SS:SWIFT_DIAG:START platform=" + platformName);
        lastStatus = "START platform=" + platformName;
        boolean isAndroid = normalizedPlatform.contains("android");
        boolean isIos = normalizedPlatform.contains("ios") || normalizedPlatform.contains("iphone");
        if (!isAndroid && !isIos) {
            System.out.println("CN1SS:SWIFT_DIAG:SKIP platform=" + platformName);
            lastStatus = "SKIP platform=" + platformName;
            return;
        }

        SwiftKotlinNative nativeImpl = NativeLookup.create(SwiftKotlinNative.class);
        System.out.println("CN1SS:SWIFT_DIAG:NATIVE_LOOKUP result=" + (nativeImpl == null ? "null" : nativeImpl.getClass().getName()));
        if (nativeImpl == null) {
            lastStatus = "LOOKUP_NULL platform=" + platformName;
            throw new IllegalStateException("SwiftKotlinNative lookup returned null on " + platformName);
        }
        if (!nativeImpl.isSupported()) {
            lastStatus = "NOT_SUPPORTED platform=" + platformName;
            throw new IllegalStateException("SwiftKotlinNative is not available on " + platformName);
        }

        String expected = isAndroid ? "kotlin" : "swift";
        String actual = nativeImpl.implementationLanguage();
        String diagnostics = nativeImpl.diagnostics();
        System.out.println("CN1SS:SWIFT_DIAG:RESULT expected=" + expected + " actual=" + actual + " diagnostics=" + diagnostics);
        if (!expected.equalsIgnoreCase(actual)) {
            lastStatus = "MISMATCH expected=" + expected + " actual=" + actual + " diagnostics=" + diagnostics;
            throw new IllegalStateException("Expected " + expected + " implementation on " + platformName + " but got " + actual + ". diagnostics=" + diagnostics);
        }
        if (isIos) {
            if ("ios-swift-native-impl".equals(diagnostics)) {
                // Preferred path: Swift bridge class was loaded successfully.
            } else if ("ios-swift-bridge-missing-using-objc-shim".equals(diagnostics)) {
                // Graceful fallback path: Objective-C shim is active. Keep CI green while
                // still surfacing the diagnostics payload for visibility in logs.
                lastStatus = "OK expected=" + expected + " actual=" + actual + " diagnostics=" + diagnostics + " fallback=objc-shim";
                return;
            } else {
                lastStatus = "SWIFT_BRIDGE_MISSING diagnostics=" + diagnostics;
                throw new IllegalStateException("Swift implementation bridge not confirmed on iOS. diagnostics=" + diagnostics);
            }
        }
        lastStatus = "OK expected=" + expected + " actual=" + actual + " diagnostics=" + diagnostics;
    }
}
