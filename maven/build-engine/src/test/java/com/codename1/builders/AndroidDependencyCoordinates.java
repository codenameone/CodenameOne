/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.builders;

import com.codename1.build.shared.PlatformFeatureCatalog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Lists every Android dependency this builder can write into a generated
/// `build.gradle`, for `scripts/check-android-dependency-alignment.py`.
///
/// One line per dependency, `<group><TAB><coordinate>`. A group is what an app
/// gets together: one catalog entry, one play-services call site, or one other
/// injected module. The script resolves each group by itself, because
/// resolving them together lets Gradle's highest-version-wins hide an old pin
/// behind a newer one.
///
/// Four sources, and `AndroidSixteenKbPageFloorTest` holds the last two to
/// the builder's source so a new coordinate cannot slip past the list:
///
/// - `PlatformFeatureCatalog`, read through its API.
/// - Each `play-services-<artifact>:" + getDefaultPlayServiceVersion("<key>")`
///   call site, read from the builder source, at the decoupled default and at
///   the coupled `android.playServicesVersion` default (every play service
///   then ships at that one version).
/// - [#INJECTED]: every other module the builders write, at the version the
///   builder uses when the app sets no hint.
/// - [#NOT_SHIPPED]: coordinates in the source that are not app dependencies.
///
/// What it cannot see: versions an app picks with a hint, `android.gradleDep`
/// and cn1lib dependencies. Those are the developer's own choice.
///
/// Usage: `java ... AndroidDependencyCoordinates [output-file]` (stdout when
/// no file is given), run from `maven/build-engine` or with
/// `-Dcn1.builders.src=<dir of the builder sources>`.
public final class AndroidDependencyCoordinates {

    /// The coupled-mode default, `AndroidGradleBuilder.playServicesVersion`.
    static final String COUPLED_PLAY_SERVICES_VERSION = "12.0.1";

    /// group:artifact -> the version written when no hint overrides it.
    static final Map<String, String> INJECTED = new LinkedHashMap<String, String>();

    /// group:artifact prefixes the builder source names that are not app
    /// dependencies, with why.
    static final Map<String, String> NOT_SHIPPED = new LinkedHashMap<String, String>();

    static {
        // GRADLE_PROVIDED_LIBRARIES: a Compose application's copies, declared instead.
        INJECTED.put("androidx.annotation:annotation", "1.9.1");
        INJECTED.put("androidx.appcompat:appcompat", "1.6.1");
        INJECTED.put("androidx.browser:browser", "1.8.0");
        INJECTED.put("androidx.car.app:app", "1.4.0");
        INJECTED.put("androidx.car.app:app-projected", "1.4.0");
        INJECTED.put("androidx.collection:collection", "1.5.0");
        INJECTED.put("androidx.concurrent:concurrent-futures", "1.1.0");
        INJECTED.put("androidx.credentials:credentials", "1.3.0");
        INJECTED.put("androidx.credentials:credentials-play-services-auth", "1.3.0");
        INJECTED.put("androidx.health.connect:connect-client", "1.1.0-alpha07");
        INJECTED.put("androidx.legacy:legacy-support-v4", "1.0.0");
        INJECTED.put("androidx.multidex:multidex", "2.0.1");
        INJECTED.put("androidx.wear.protolayout:protolayout", "1.2.1");
        INJECTED.put("androidx.wear.protolayout:protolayout-material", "1.2.1");
        INJECTED.put("androidx.wear.tiles:tiles", "1.4.1");
        INJECTED.put("androidx.wear.watchface:watchface-complications-data-source", "1.2.1");
        INJECTED.put("androidx.window:window", "1.3.0");
        INJECTED.put("com.android.billingclient:billing", PlayBillingVersions.DEFAULT_VERSION);
        INJECTED.put("com.android.support:appcompat-v7", "28.0.0");
        INJECTED.put("com.android.support:multidex", "1.0.3");
        INJECTED.put("com.android.support:support-v4", "28.0.0");
        INJECTED.put("com.android.tools:desugar_jdk_libs", "2.1.5");
        INJECTED.put("com.facebook.android:facebook-android-sdk", "16.2.0");
        INJECTED.put("com.google.android.gms:play-services", "6.5.87");
        INJECTED.put("com.google.android.gms:play-services-auth", "20.6.0");
        INJECTED.put("com.google.android.gms:play-services-home", "16.0.0-beta1");
        INJECTED.put("com.google.android.gms:play-services-maps", "18.2.0");
        INJECTED.put("com.google.android.play:integrity", "1.4.0");
        INJECTED.put("com.google.android.play:review", "2.0.1");
        INJECTED.put("com.google.firebase:firebase-analytics", "21.5.0");
        INJECTED.put("com.google.firebase:firebase-messaging", "23.2.1");
        INJECTED.put("com.google.guava:guava", "31.1-android");
        INJECTED.put("com.huawei.hms:maps", "6.11.0.300");
        INJECTED.put("com.huawei.hms:push", "6.3.0.302");
        INJECTED.put("com.scottyab:rootbeer-lib", "0.1.2");
        INJECTED.put("org.jetbrains.kotlin:kotlin-stdlib", "1.9.22");
        INJECTED.put("org.jetbrains.kotlinx:kotlinx-coroutines-core", "1.10.2");
        INJECTED.put("org.unifiedpush.android:connector", "3.3.5");

        NOT_SHIPPED.put("com.android.tools.build:gradle", "the Android Gradle plugin");
        NOT_SHIPPED.put("com.google.gms:google-services", "a Gradle plugin");
        NOT_SHIPPED.put("com.huawei.agconnect:agcp", "a Gradle plugin");
        NOT_SHIPPED.put("org.jetbrains.kotlin:kotlin-gradle-plugin", "a Gradle plugin");
    }

    private static final Pattern PLAY_SERVICE_CALL = Pattern.compile(
            "com\\.google\\.android\\.gms:play-services-([a-z0-9\\-]+):\"\\s*\\+\\s*"
            + "getDefaultPlayServiceVersion\\(\"([a-z0-9\\-]+)\"\\)");

    /// A Maven coordinate prefix in a Java string literal: `'group:artifact:`
    /// or `"group:artifact:`.
    static final Pattern COORDINATE_PREFIX = Pattern.compile(
            "[\"']([a-z][a-z0-9_\\-]*(?:\\.[a-z0-9_\\-]+)+:[A-Za-z0-9_.\\-]+):");

    private AndroidDependencyCoordinates() {
    }

    static File buildersSource() {
        return new File(System.getProperty("cn1.builders.src",
                "src/main/java/com/codename1/builders"));
    }

    static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.ISO_8859_1);
    }

    /// play-services artifact -> the key its version is looked up under.
    static Map<String, String> playServiceCallSites() throws IOException {
        Map<String, String> out = new TreeMap<String, String>();
        Matcher m = PLAY_SERVICE_CALL.matcher(read(new File(buildersSource(), "AndroidGradleBuilder.java")));
        while (m.find()) {
            out.put(m.group(1), m.group(2));
        }
        return out;
    }

    /// Every group:artifact a builder source names in a string literal.
    static Set<String> coordinatePrefixesInSource() throws IOException {
        Set<String> out = new LinkedHashSet<String>();
        File[] files = buildersSource().listFiles();
        if (files == null) {
            throw new IOException("no builder sources at " + buildersSource());
        }
        for (File file : files) {
            if (!file.getName().endsWith(".java")) {
                continue;
            }
            Matcher m = COORDINATE_PREFIX.matcher(read(file));
            while (m.find()) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    /// @return the lines, in a stable order
    static List<String> lines() throws IOException {
        List<String> out = new ArrayList<String>();
        for (PlatformFeatureCatalog.Entry entry : PlatformFeatureCatalog.entries()) {
            for (String dependency : entry.androidGradleDeps()) {
                out.add("catalog:" + entry.classPrefix() + "\t" + dependency);
            }
        }
        Map<String, String> defaults = AndroidGradleBuilder.defaultPlayServiceVersionsView();
        for (Map.Entry<String, String> site : playServiceCallSites().entrySet()) {
            // getDefaultPlayServiceVersion falls back to the coupled version
            // for a key with no default of its own, as it does in coupled mode.
            String version = defaults.get(site.getValue());
            String artifact = "com.google.android.gms:play-services-" + site.getKey();
            if (version != null) {
                out.add("play-services:" + site.getKey() + "\t" + artifact + ":" + version);
            }
            out.add("play-services-coupled:" + site.getKey() + "\t" + artifact + ":"
                    + COUPLED_PLAY_SERVICES_VERSION);
        }
        for (Map.Entry<String, String> module : INJECTED.entrySet()) {
            // The Wear modules go in together (watchSurfaceDependencyBlock), and
            // complications resolves its protolayout-proto only beside them.
            String group = module.getKey().startsWith("androidx.wear.")
                    ? "injected:wear" : "injected:" + module.getKey();
            out.add(group + "\t" + module.getKey() + ":" + module.getValue());
        }
        return Collections.unmodifiableList(out);
    }

    public static void main(String[] args) throws IOException {
        Writer target = args.length > 0
                ? new OutputStreamWriter(new FileOutputStream(args[0]), "UTF-8")
                : new OutputStreamWriter(System.out, "UTF-8");
        PrintWriter writer = new PrintWriter(target);
        try {
            for (String line : lines()) {
                writer.println(line);
            }
        } finally {
            writer.flush();
            if (args.length > 0) {
                writer.close();
            }
        }
        if (writer.checkError()) {
            throw new IOException("could not write the coordinates");
        }
    }
}
