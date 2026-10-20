/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.packageinfo;

import com.codename1.ui.Display;

import dart.async.Future;

/**
 * package_info_plus's {@code PackageInfo}, from the build's own properties:
 * {@code AppName}, {@code AppVersion} and {@code package_name}. The build number is
 * the version's last dotted part when it has more than three, as Codename One builds
 * carry a single version string.
 */
public class PackageInfo {
    private static PackageInfo mock;

    private final String appName;
    private final String packageName;
    private final String version;
    private final String buildNumber;
    private final String buildSignature;
    private final String installerStore;

    public PackageInfo(String appName, String packageName, String version, String buildNumber,
            String buildSignature, String installerStore) {
        this.appName = appName;
        this.packageName = packageName;
        this.version = version;
        this.buildNumber = buildNumber;
        this.buildSignature = buildSignature == null ? "" : buildSignature;
        this.installerStore = installerStore;
    }

    public static Future<PackageInfo> fromPlatform() {
        if (mock != null) {
            return Future.value(mock);
        }
        String name = prop("AppName", "");
        String pkg = prop("package_name", "");
        String full = prop("AppVersion", "");
        String version = full;
        String build = "";
        int dots = 0;
        for (int i = 0; i < full.length(); i++) {
            if (full.charAt(i) == '.') {
                dots++;
            }
        }
        if (dots >= 3) {
            int last = full.lastIndexOf('.');
            version = full.substring(0, last);
            build = full.substring(last + 1);
        }
        String imported = importedBuildNumber();
        if (imported != null) {
            build = imported;
        }
        return Future.value(new PackageInfo(name, pkg, version, build, "", null));
    }

    /// Written by the Flutter importer from pubspec's `version: 1.2.3+42`; the 42 goes
    /// to the native build-number hints, which no device property reports back.
    static final String BUILD_NUMBER_RESOURCE = "cn1-flutter-build-number.txt";

    private static String importedBuildNumber() {
        if (!Display.isInitialized()) {
            return null;
        }
        java.io.InputStream in = Display.getInstance().getResourceAsStream(PackageInfo.class,
                "/" + BUILD_NUMBER_RESOURCE);
        if (in == null) {
            return null;
        }
        try {
            String s = com.codename1.io.Util.readToString(in, "UTF-8").trim();
            return s.length() == 0 ? null : s;
        } catch (java.io.IOException e) {
            return null;
        } finally {
            com.codename1.io.Util.cleanup(in);
        }
    }

    /** Fixes what {@link #fromPlatform} answers, for tests. */
    public static void setMockInitialValues(String appName, String packageName, String version,
            String buildNumber, String buildSignature, String installerStore, Object updateTime) {
        mock = new PackageInfo(appName, packageName, version, buildNumber, buildSignature, installerStore);
    }

    private static String prop(String key, String def) {
        return Display.isInitialized() ? Display.getInstance().getProperty(key, def) : def;
    }

    public String appName() {
        return appName;
    }

    public String packageName() {
        return packageName;
    }

    public String version() {
        return version;
    }

    public String buildNumber() {
        return buildNumber;
    }

    public String buildSignature() {
        return buildSignature;
    }

    public String installerStore() {
        return installerStore;
    }
}
