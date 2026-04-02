package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeLookup;
import com.codename1.ui.CN;

public final class NativeInterfaceLanguageValidator {
    private NativeInterfaceLanguageValidator() {
    }

    public static void validate() {
        String platformName = CN.getPlatformName();
        String normalizedPlatform = platformName == null ? "" : platformName.toLowerCase();
        System.out.println("CN1SS:SWIFT_DIAG:START platform=" + platformName);
        boolean isAndroid = normalizedPlatform.contains("android");
        boolean isIos = normalizedPlatform.contains("ios") || normalizedPlatform.contains("iphone");
        if (!isAndroid && !isIos) {
            System.out.println("CN1SS:SWIFT_DIAG:SKIP platform=" + platformName);
            return;
        }

        SwiftKotlinNative nativeImpl = NativeLookup.create(SwiftKotlinNative.class);
        System.out.println("CN1SS:SWIFT_DIAG:NATIVE_LOOKUP result=" + (nativeImpl == null ? "null" : nativeImpl.getClass().getName()));
        if (nativeImpl == null) {
            throw new IllegalStateException("SwiftKotlinNative lookup returned null on " + platformName);
        }
        if (!nativeImpl.isSupported()) {
            throw new IllegalStateException("SwiftKotlinNative is not available on " + platformName);
        }

        String expected = isAndroid ? "kotlin" : "swift";
        String actual = nativeImpl.implementationLanguage();
        String diagnostics = nativeImpl.diagnostics();
        System.out.println("CN1SS:SWIFT_DIAG:RESULT expected=" + expected + " actual=" + actual + " diagnostics=" + diagnostics);
        if (!expected.equalsIgnoreCase(actual)) {
            throw new IllegalStateException("Expected " + expected + " implementation on " + platformName + " but got " + actual + ". diagnostics=" + diagnostics);
        }
    }
}
