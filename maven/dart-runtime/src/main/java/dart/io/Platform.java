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
package dart.io;

import com.codename1.ui.Display;
import dart.core.DartMap;

/**
 * Dart's {@code Platform}, answered from {@link Display#getPlatformName()} -- so the
 * simulator reports the platform of the skin it is showing, as a Flutter app expects.
 */
public final class Platform {

    /** One of Dart's operatingSystem names: android, ios, macos, windows, linux. */
    public static final String operatingSystem = detect();
    public static final boolean isAndroid = operatingSystem.equals("android");
    public static final boolean isIOS = operatingSystem.equals("ios");
    public static final boolean isMacOS = operatingSystem.equals("macos");
    public static final boolean isWindows = operatingSystem.equals("windows");
    public static final boolean isLinux = operatingSystem.equals("linux");
    public static final boolean isFuchsia = false;
    public static final String operatingSystemVersion =
            Display.isInitialized() ? Display.getInstance().getProperty("OSVer", "") : "";
    public static final String localeName = locale();
    public static final long numberOfProcessors = 1;
    public static final String pathSeparator = "/";
    public static final String localHostname = "localhost";
    public static final String version = "3.0.0";
    /** No process environment on a device; always empty. */
    public static final DartMap<String, String> environment = new DartMap<String, String>();
    public static final String resolvedExecutable = "";
    public static final String script = "";

    // Dart's Platform members are static getters, which the transpiler reads as static
    // fields; they are computed when the class is first used, after Codename One started.

    private Platform() {
    }

    private static String detect() {
        String p = null;
        if (Display.isInitialized()) {
            p = Display.getInstance().getPlatformName();
        }
        if (p == null) {
            String os = System.getProperty("os.name");
            p = os == null ? "" : os;
        }
        if (p.equals("and") || p.regionMatches(true, 0, "android", 0, 7)) {
            return "android";
        }
        if (p.equals("ios") || p.regionMatches(true, 0, "iphone", 0, 6)) {
            return "ios";
        }
        if (p.equals("mac") || p.regionMatches(true, 0, "mac", 0, 3)) {
            return "macos";
        }
        if (p.equals("win") || p.regionMatches(true, 0, "windows", 0, 7)) {
            return "windows";
        }
        return "linux";
    }

    private static String locale() {
        if (Display.isInitialized()) {
            com.codename1.l10n.L10NManager l = Display.getInstance().getLocalizationManager();
            if (l != null) {
                return l.getLanguage() + "_" + l.getLocale();
            }
        }
        return "en_US";
    }
}
