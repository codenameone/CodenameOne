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
package com.codename1.builders;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/// Which of the port's native theme resources an application actually ships.
///
/// The iOS and macOS ports each carry every native theme -- iPhone, iOS 7, the
/// two modern generations and, on macOS, Aqua -- about a megabyte between them,
/// and the builders used to copy all of them into every application. The
/// runtime loads exactly one: the mode the builder writes into the generated
/// stub decides it (`IOSImplementation.setIosMode`), nothing reads a theme mode
/// at run time, so the one it will load is known when the application is built.
///
/// The choice here mirrors `IOSImplementation.installNativeTheme()` and
/// `MacImplementation.nativeThemeResourceName()`. A theme the application's own
/// classes name -- `Resources.open("/iOS7Theme.res")` -- is kept as well, since
/// that is a use no build hint can see.
///
/// The `custom` mode ships none of them. It says the application's own theme is
/// the only one it uses -- the theme pairs it with `includeNativeBool: false` --
/// and every port's runtime answers it by installing nothing, so a platform theme
/// in the bundle would be dead weight.
final class NativeThemes {

    /// Every native theme a port jar carries, without the `.res`.
    static final String[] ALL = {
        "iPhoneTheme", "iOS7Theme", "iOSModernTheme", "iOSModern27Theme", "MacOSAquaTheme"
    };

    private NativeThemes() {
    }

    /// The native theme the runtime will install for `mode`, without the `.res`,
    /// or null for `custom`, which installs none.
    ///
    /// @param mode the theme mode written into the stub; null means the runtime default, "auto"
    /// @param generation the iOS modern generation, "26" or "27"; null means 26
    /// @param mac true for the native macOS port, whose Aqua theme answers auto/aqua/native
    static String themeFor(String mode, String generation, boolean mac) {
        String m = mode == null ? "auto" : asciiLower(mode.trim());
        if (isCustom(m)) {
            return null;
        }
        if (mac && ("auto".equals(m) || "aqua".equals(m) || "native".equals(m))) {
            return "MacOSAquaTheme";
        }
        if ("modern".equals(m) || "liquid".equals(m)) {
            return "27".equals(generation) ? "iOSModern27Theme" : "iOSModernTheme";
        }
        if ("ios7".equals(m) || "flat".equals(m) || "auto".equals(m)) {
            return "iOS7Theme";
        }
        // "legacy", "iphone", "material" and anything else fall to the pre-flat
        // theme in installNativeTheme(), so that is the one they need.
        return "iPhoneTheme";
    }

    /// Every native theme the Android port carries, without the `.res`.
    static final String[] ANDROID_ALL = {"androidTheme", "AndroidMaterialTheme", "android_holo_light"};

    /// The Display properties AndroidImplementation.installNativeTheme() reads the
    /// mode from. The builder writes them into the stub from the build hints, but an
    /// application can set them itself before the theme is installed, so one that
    /// names any of them keeps every Android theme.
    static final String[] ANDROID_MODE_PROPERTIES = {
        "and.themeMode", "cn1.androidTheme", "nativeTheme", "cn1.nativeTheme", "and.hololight"
    };

    /// The Android themes a build needs: the one installNativeTheme() resolves the
    /// hints to, and always android_holo_light, which hasNativeTheme() probes for on
    /// every device from API 14 up and without which no native theme is installed.
    /// None at all for `custom`, which installs no native theme and does not probe.
    static Set<String> androidThemesFor(String andThemeMode, String cn1AndroidTheme,
            String nativeTheme, String cn1NativeTheme, String hololight) {
        String mode = andThemeMode != null ? andThemeMode : cn1AndroidTheme;
        if (mode == null) {
            String shared = nativeTheme != null ? nativeTheme : cn1NativeTheme;
            if ("modern".equalsIgnoreCase(shared) || "native".equalsIgnoreCase(shared)) {
                mode = "material";
            } else if (isCustom(shared)) {
                mode = "custom";
            } else if ("legacy".equalsIgnoreCase(shared)) {
                mode = "hololight";
            } else if ("true".equalsIgnoreCase(hololight)) {
                mode = "legacy";
            } else {
                mode = "hololight";
            }
        } else {
            mode = asciiLower(mode.trim());
        }
        Set<String> keep = new LinkedHashSet<String>();
        if (isCustom(mode)) {
            return keep;
        }
        if ("material".equals(mode) || "modern".equals(mode) || "auto".equals(mode)) {
            keep.add("AndroidMaterialTheme");
        } else if (!"hololight".equals(mode) && !"holo".equals(mode)) {
            keep.add("androidTheme");
        }
        keep.add("android_holo_light");
        return keep;
    }

    /// Deletes the Android themes `keep` does not name from `dir`, unless the
    /// application sets the theme mode itself, in which case the choice is made at
    /// run time and every theme stays.
    static List<String> removeUnusedAndroid(File dir, Set<String> keep, File appClasses)
            throws IOException {
        if (namesAny(appClasses, ANDROID_MODE_PROPERTIES)) {
            return new ArrayList<String>();
        }
        return removeUnused(dir, keep, ANDROID_ALL, appClasses);
    }

    /// The runtime property an application reads to learn which modern theme the
    /// port carries, and then installs it itself -- the screenshot suite does, and
    /// its dark captures all came out light when that theme had been filtered out.
    /// IOSImplementation answers it with the configured generation's modern theme.
    static final String NATIVE_THEME_RESOURCE_PROPERTY = "cn1.nativeThemeResource";

    /// Removes the Apple native themes this build does not need from `dir`: every
    /// one except the theme the stub's mode will install, a theme the application
    /// names itself, and -- when the application asks the port for its modern
    /// theme through {@link #NATIVE_THEME_RESOURCE_PROPERTY} -- that modern theme.
    ///
    /// @return the names removed, for the build log
    static List<String> removeUnusedApple(File dir, String mode, String generation, boolean mac,
            File appClasses) throws IOException {
        Set<String> keep = new LinkedHashSet<String>();
        String theme = themeFor(mode, generation, mac);
        if (theme != null) {
            keep.add(theme);
        }
        if (namesAny(appClasses, new String[] {NATIVE_THEME_RESOURCE_PROPERTY})) {
            keep.add(themeFor("modern", generation, false));
        }
        return removeUnused(dir, keep, ALL, appClasses);
    }

    /// True for the `custom` theme mode: the application ships the only theme it
    /// uses, so no platform theme is packaged and none is installed.
    static boolean isCustom(String mode) {
        return mode != null && "custom".equalsIgnoreCase(mode.trim());
    }

    /// The single native theme a native desktop port (Linux, Windows) bundles in its
    /// port jar, and installs from `init()` whenever the file is present.
    ///
    /// Those ports have one theme and no mode to choose between, so the only question
    /// is whether to ship it: not when `desktop.themeMode` is `custom`, or when it is
    /// unset and the cross-platform `nativeTheme` (`cn1.nativeTheme`) is. The same
    /// resolution the JavaSE desktop port runs.
    ///
    /// @return true when the port's theme file should be removed before translation
    static boolean desktopShipsNoTheme(String desktopThemeMode, String nativeTheme,
            String cn1NativeTheme) {
        if (desktopThemeMode != null && desktopThemeMode.trim().length() > 0) {
            return isCustom(desktopThemeMode);
        }
        return isCustom(nativeTheme != null ? nativeTheme : cn1NativeTheme);
    }

    /// Removes `fileName` from `dir` when present, for a desktop port whose theme
    /// the build does not ship. See [#desktopShipsNoTheme(String, String, String)].
    ///
    /// @return true when the file was there and is now gone
    static boolean removeDesktopTheme(File dir, String fileName) throws IOException {
        File f = new File(dir, fileName);
        if (!f.isFile()) {
            return false;
        }
        if (!f.delete()) {
            throw new IOException("Could not remove the unused theme " + f);
        }
        return true;
    }

    /// Deletes from `dir` every native theme other than `keep` and the ones the
    /// application names in `appClasses`.
    ///
    /// @return the names removed, for the build log
    static List<String> removeUnused(File dir, String keep, File appClasses) throws IOException {
        Set<String> k = new LinkedHashSet<String>();
        k.add(keep);
        return removeUnused(dir, k, ALL, appClasses);
    }

    private static List<String> removeUnused(File dir, Set<String> keep, String[] candidates,
            File appClasses) throws IOException {
        List<String> removed = new ArrayList<String>();
        if (dir == null || !dir.isDirectory()) {
            return removed;
        }
        Set<String> referenced = new LinkedHashSet<String>();
        for (String name : candidates) {
            if (namesAny(appClasses, new String[] {name + ".res"})) {
                referenced.add(name);
            }
        }
        for (String name : candidates) {
            if (keep.contains(name) || referenced.contains(name)) {
                continue;
            }
            File f = new File(dir, name + ".res");
            if (f.isFile()) {
                if (!f.delete()) {
                    throw new IOException("Could not remove the unused theme " + f);
                }
                removed.add(name);
            }
        }
        return removed;
    }

    /// True when a class under `classesDir` contains any of `needles` as a string.
    ///
    /// The ports' own `com.codename1.impl` packages are skipped, since they name
    /// every theme and every mode property, and so is `com.codename1.annotations`,
    /// whose build-hint declarations name the properties without setting them.
    static boolean namesAny(File classesDir, String[] needles) throws IOException {
        return classesDir != null && classesDir.isDirectory()
                && scan(classesDir, classesDir, needles);
    }

    private static boolean scan(File root, File f, String[] needles) throws IOException {
        if (f.isDirectory()) {
            String rel = root.toPath().relativize(f.toPath()).toString().replace('\\', '/');
            if ("com/codename1/impl".equals(rel) || "com/codename1/annotations".equals(rel)) {
                return false;
            }
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    if (scan(root, c, needles)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (!f.getName().endsWith(".class")) {
            return false;
        }
        // The class file's constant pool holds a string literal as modified UTF-8,
        // which for these ASCII names is the same bytes as the name itself.
        String body = new String(Files.readAllBytes(f.toPath()), StandardCharsets.ISO_8859_1);
        for (String needle : needles) {
            if (body.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String asciiLower(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return b.toString();
    }
}
