/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
