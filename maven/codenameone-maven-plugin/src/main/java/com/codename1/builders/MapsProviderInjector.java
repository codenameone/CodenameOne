/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.builders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Injects a native map provider into an app build when the {@code maps.provider}
 * build hint selects one. The public maps API ({@code com.codename1.maps.MapView}
 * / {@code NativeMap}) never names a provider; this is the single point where a
 * concrete provider (its native-method-bearing implementation) is pushed into
 * the {@code com.codename1.maps} package of the app and wired in, keeping the
 * core framework free of any heavyweight provider SDK.
 *
 * <p>The hint is resolved per platform: {@code android.maps.provider} /
 * {@code ios.maps.provider} override the generic {@code maps.provider}. When no
 * hint is set the methods are inert, so default builds are completely
 * unaffected.</p>
 *
 * <p>Android has an app-source compile step, so the provider's Java
 * implementation is injected as source ({@code MapProviderImpl.java}) and
 * compiled against the build-injected Google Play Services dependency. iOS
 * translates compiled bytecode (no app javac), so only the Objective-C
 * MapKit implementation is injected here; the Java side of the iOS provider is
 * supplied precompiled (port/library) and wired by the native-interface
 * bridge.</p>
 */
public final class MapsProviderInjector {

    private static final String REGISTER_CALL =
            "        com.codename1.maps.MapProviderImpl.register();\n";

    private MapsProviderInjector() {
    }

    /**
     * Resolves the selected provider id for {@code platform} ("android"/"ios"),
     * or {@code null} when no provider hint is set.
     */
    public static String resolveProvider(BuildRequest request, String platform) {
        String p = request.getArg(platform + ".maps.provider", request.getArg("maps.provider", ""));
        if (p == null) {
            return null;
        }
        p = p.trim().toLowerCase();
        return p.length() == 0 ? null : p;
    }

    /**
     * Injects the Android provider implementation and dependencies. Returns the
     * startup snippet to splice into the activity's {@code onCreate}, or an
     * empty string when no provider is selected.
     *
     * @param exec    the running builder (for resource access / file copy)
     * @param request the build request carrying the hints
     * @param srcDir  the generated project's {@code src/main/java} root
     */
    public static String injectAndroid(Executor exec, BuildRequest request, File srcDir) {
        String provider = resolveProvider(request, "android");
        String template = androidTemplate(provider);
        if (template == null) {
            return "";
        }
        try {
            File pkgDir = new File(srcDir, "com" + File.separator + "codename1" + File.separator + "maps");
            pkgDir.mkdirs();
            File out = new File(pkgDir, "MapProviderImpl.java");
            copyResource(exec, template, out);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to inject map provider '" + provider + "'", ex);
        }
        if ("google".equals(provider)) {
            addGradleDependency(request, "com.google.android.gms:play-services-maps:18.2.0");
        } else if ("huawei".equals(provider)) {
            addGradleDependency(request, "com.huawei.hms:maps:6.11.0.300");
        }
        return REGISTER_CALL;
    }

    /**
     * Injects the iOS provider when one is selected. The provider's Java
     * implementation is written into the stub source dir as
     * {@code MapProviderImpl.java} (the build's javac compiles it and ParparVM
     * translates it to C, where its native methods bind to the injected
     * Objective-C), and the Objective-C is written into the native sources
     * dir. Returns the {@code onCreate}/startup snippet that registers the
     * provider, or an empty string when no provider is selected.
     *
     * @param exec      the running builder
     * @param request   the build request
     * @param stubSrc   the generated stub source dir (compiled by javac)
     * @param nativeDir the generated Xcode project's native-sources directory
     */
    public static String injectIos(Executor exec, BuildRequest request, File stubSrc, File nativeDir) {
        String provider = resolveProvider(request, "ios");
        if (!"apple".equals(provider)) {
            return "";
        }
        try {
            File pkgDir = new File(stubSrc, "com" + File.separator + "codename1" + File.separator + "maps");
            pkgDir.mkdirs();
            copyResource(exec, "maps/AppleMapProvider.javas", new File(pkgDir, "MapProviderImpl.java"));
            // Use a non-colliding file name: ParparVM generates its own
            // com_codename1_maps_MapProviderImpl.m (the Java translation), so
            // our native implementation must live in a different file. The
            // C symbol names inside resolve the externs regardless of filename.
            copyResource(exec, "maps/AppleMapProvider.m",
                    new File(nativeDir, "CN1AppleMapKit.m"));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to inject Apple map provider", ex);
        }
        return REGISTER_CALL;
    }

    /**
     * The system frameworks the selected iOS provider requires, or an empty
     * array when no provider is selected.
     */
    public static String[] iosFrameworks(BuildRequest request) {
        if ("apple".equals(resolveProvider(request, "ios"))) {
            return new String[]{"MapKit.framework", "CoreLocation.framework"};
        }
        return new String[0];
    }

    private static String androidTemplate(String provider) {
        if (provider == null || provider.length() == 0) {
            return null;
        }
        if ("apple".equals(provider)) {
            // Apple MapKit is iOS-only; on Android this hint means "no native
            // provider", so NativeMap falls back to the vector MapView.
            return null;
        }
        // Convention: maps/<Capitalized>MapProvider.javas. Adding a new
        // provider (Bing, Huawei, ...) is just dropping a template with the
        // matching name; only Google is bundled by default.
        return "maps/" + Character.toUpperCase(provider.charAt(0))
                + provider.substring(1) + "MapProvider.javas";
    }

    private static void addGradleDependency(BuildRequest request, String gav) {
        String existing = request.getArg("gradleDependencies", "");
        if (existing.contains(gav)) {
            return;
        }
        request.putArgument("gradleDependencies",
                existing + "\n    implementation '" + gav + "'\n");
    }

    private static void copyResource(Executor exec, String resource, File out) throws Exception {
        InputStream is = exec.getResourceAsStream(resource);
        if (is == null) {
            throw new IllegalStateException("Missing map provider template resource: " + resource);
        }
        FileOutputStream os = new FileOutputStream(out);
        try {
            Executor.copy(is, os);
        } finally {
            os.close();
            is.close();
        }
    }
}
