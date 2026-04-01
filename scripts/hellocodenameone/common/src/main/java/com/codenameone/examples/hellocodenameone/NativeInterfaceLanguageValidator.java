package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeLookup;
import com.codename1.ui.CN;

public final class NativeInterfaceLanguageValidator {
    private NativeInterfaceLanguageValidator() {
    }

    public static void validate() {
        String platformName = CN.getPlatformName();
        String normalizedPlatform = platformName == null ? "" : platformName.toLowerCase();
        boolean isAndroid = normalizedPlatform.contains("android");
        boolean isIos = normalizedPlatform.contains("ios") || normalizedPlatform.contains("iphone");
        if (!isAndroid && !isIos) {
            return;
        }

        SwiftKotlinNative nativeImpl = NativeLookup.create(SwiftKotlinNative.class);
        if (nativeImpl == null || !nativeImpl.isSupported()) {
            throw new IllegalStateException("SwiftKotlinNative is not available on " + platformName);
        }

        String expected = isAndroid ? "kotlin" : "swift";
        String actual = nativeImpl.implementationLanguage();
        if (!expected.equalsIgnoreCase(actual)) {
            throw new IllegalStateException("Expected " + expected + " implementation on " + platformName + " but got " + actual);
        }
    }
}
