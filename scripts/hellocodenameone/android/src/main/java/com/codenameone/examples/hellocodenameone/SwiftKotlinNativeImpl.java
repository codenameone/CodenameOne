package com.codenameone.examples.hellocodenameone;

public class SwiftKotlinNativeImpl {
    public String implementationLanguage() {
        // Android native stubs are compiled with javac in the current pipeline.
        // Keep this implementation in Java even though it validates "kotlin" mode.
        return "kotlin";
    }

    public boolean isSupported() {
        return true;
    }
}
