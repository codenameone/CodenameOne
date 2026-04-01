/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

/**
 * Minimal deterministic initialization surface extracted from
 * {@link HTML5Implementation#init(Object)}.
 */
public final class JavaScriptInitializationAdapter {
    private JavaScriptInitializationAdapter() {
    }

    public interface PropertySink {
        void setProperty(String key, String value);
    }

    public interface RuntimeHooks {
        void setDragStartPercentage(int percentage);
        void initVideoCaptureConstraints();
        void registerSaveBlobToFile();
        void initGoogle();
    }

    public static void applyEnvironment(PropertySink sink, JavaScriptRuntimeEnvironment environment) {
        sink.setProperty("Platform", environment.getPlatform());
        sink.setProperty("User-Agent", environment.getUserAgent());
        sink.setProperty("browser.language", environment.getLanguage());
        sink.setProperty("browser.name", environment.getAppName());
        sink.setProperty("browser.codeName", environment.getAppCodeName());
        sink.setProperty("browser.version", environment.getAppVersion());
        sink.setProperty("OS", "JS");
        sink.setProperty("OSVer", "1.0");
        sink.setProperty("javascript.deployment.type", environment.getDeploymentType());
    }

    public static String resolveAppArg(JavaScriptRuntimeEnvironment environment) {
        return environment.getLocationHref();
    }

    public static void runPostInit(RuntimeHooks hooks, boolean ios) {
        hooks.setDragStartPercentage(1);
        if (!ios) {
            hooks.initVideoCaptureConstraints();
        }
        hooks.registerSaveBlobToFile();
        hooks.initGoogle();
    }
}
