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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// The build hints of the JavaScript port, and where each one has to go.
///
/// A JavaScript build has three audiences for a hint, and a hint that reaches the wrong one
/// silently does nothing:
///
/// - the **running application**, which reads `Display.getProperty` -- the launcher sets these
///   (see [#launcherProperties(BuildRequest)]);
/// - the **page** the translator writes, `index.html`, which exists before any Java runs --
///   these become translator system properties (see [#translatorOptions(BuildRequest)]);
/// - the **bundle**, which should carry only the native themes the application can reach at
///   run time (see [#themesToShip(BuildRequest, Set)]).
///
/// The cloud builder (BuildDaemon's `ParparVMJavascriptBuilder`) carries a copy of this class;
/// keep the two identical apart from the package and the `BuildRequest` import.
public final class JavaScriptBuildHints {

    /// Hints the running application reads through `Display.getProperty`. Before this list
    /// existed the launcher set none of them, so `nativeTheme` and friends only worked when the
    /// application called `Display.setProperty` itself.
    static final String[] RUNTIME_HINTS = {
        "nativeTheme",
        "cn1.nativeTheme",
        "ios.themeMode",
        "and.themeMode",
        "cn1.androidTheme",
        "javascript.native.theme",
        "javascript.desktopTheme",
        "javascript.titleBar",
        "javascript.textSelection"
    };

    /// Every native theme the port bundle can carry in `assets/`, by resource name.
    static final List<String> ALL_THEMES = Collections.unmodifiableList(Arrays.asList(
            "iOS7Theme", "iPhoneTheme", "iOSModernTheme", "iOSModern27Theme",
            "androidTheme", "android_holo_light", "AndroidMaterialTheme",
            "WindowsFluentTheme", "MacOSAquaTheme", "GnomeAdwaitaTheme"));

    /// The runtime property the port publishes with the OS-appropriate modern theme. Code that
    /// reads it can open either modern theme, so an application naming it keeps both.
    static final String MODERN_THEME_PROPERTY = "cn1.modernThemeResource";

    /// Marker [#scanThemeReferences(File)] records when the application's own code names one of
    /// the theme hints as a string: it can set them with `Display.setProperty` before the native
    /// theme is installed, choosing at run time what the build hints did not say.
    static final String RUNTIME_THEME_CHOICE = "runtime theme choice";

    /// The theme hints an application can also set itself at run time; the runtime resolver
    /// reads each of them through `Display.getProperty`.
    static final String[] THEME_PROPERTIES = {
        "nativeTheme", "cn1.nativeTheme", "ios.themeMode", "and.themeMode", "cn1.androidTheme",
        "javascript.native.theme", "javascript.desktopTheme"
    };

    private JavaScriptBuildHints() {
    }

    /// Java source lines, for the generated launcher, that publish the runtime hints the
    /// request sets. Hints the request does not set are left out, so the port's own defaults
    /// apply.
    static String launcherProperties(BuildRequest request) {
        StringBuilder out = new StringBuilder();
        for (String hint : RUNTIME_HINTS) {
            String value = request.getArg(hint, null);
            if (value == null) {
                continue;
            }
            out.append("        Display.getInstance().setProperty(\"")
                    .append(hint).append("\", \"").append(javaString(value)).append("\");\n");
        }
        return out.toString();
    }

    /// `-D` options for the translator JVM, which writes `index.html` (see
    /// `JavascriptBundleWriter.applyPageSettings` in the ByteCodeTranslator). Only what the
    /// request actually decides is passed; the translator's defaults are the documented ones,
    /// so an older builder that passes nothing still gets the viewport and the opt-outs.
    static List<String> translatorOptions(BuildRequest request) {
        List<String> out = new ArrayList<String>();
        String title = request.getDisplayName();
        if (title != null && title.trim().length() > 0) {
            out.add("-Dcodename1.javascript.title=" + title.trim());
        }
        String translation = request.getArg("javascript.allowBrowserTranslation", null);
        if (translation != null) {
            out.add("-Dcodename1.javascript.allowBrowserTranslation=" + "true".equalsIgnoreCase(translation.trim()));
        }
        String darkReader = request.getArg("javascript.darkreaderLock", null);
        if (darkReader != null) {
            out.add("-Dcodename1.javascript.darkreaderLock=" + !"false".equalsIgnoreCase(darkReader.trim()));
        }
        return out;
    }

    /// The native themes this application can open at run time, which is what the bundle has
    /// to carry. Everything else in [#ALL_THEMES] is dead weight in the public web root -- up to
    /// 2.5MB of it.
    ///
    /// This mirrors `HTML5Implementation.resolveNativeThemeResource()` in the JavaScript port,
    /// which picks the theme from these hints AND the browser's user agent. The user agent is
    /// unknown here, so the answer is the union over every browser the hints leave possible.
    /// Change the two together.
    ///
    /// #### Parameters
    ///
    /// - `request`: the build hints
    /// - `referencedByApp`: theme names (and [#MODERN_THEME_PROPERTY]) that appear as strings
    ///   in the application's own classes; an application that opens a theme itself keeps it
    static Set<String> themesToShip(BuildRequest request, Set<String> referencedByApp) {
        Set<String> out = new LinkedHashSet<String>();
        if (referencedByApp.contains(RUNTIME_THEME_CHOICE)) {
            // The application picks the theme itself, so the hints do not say which one it
            // will open. Keep them all rather than delete the one it asks for.
            out.addAll(ALL_THEMES);
            return out;
        }
        for (String name : ALL_THEMES) {
            if (referencedByApp.contains(name)) {
                out.add(name);
            }
        }
        if (referencedByApp.contains(MODERN_THEME_PROPERTY)) {
            out.add("iOSModernTheme");
            out.add("AndroidMaterialTheme");
        }
        String explicit = request.getArg("javascript.native.theme", null);
        if (explicit != null && explicit.trim().length() > 0) {
            String name = baseName(explicit.trim());
            if (ALL_THEMES.contains(name)) {
                out.add(name);
            } else {
                // A theme the application ships itself. If it fails to open the port falls
                // back to the legacy pair, so keep that pair.
                out.add("android_holo_light");
                out.add("iOS7Theme");
            }
            return out;
        }
        String shared = lower(request.getArg("nativeTheme", request.getArg("cn1.nativeTheme", null)));

        // Desktop browsers: a desktop theme replaces the mobile branch there, but phones and
        // tablets still take the mobile branch, so the two sets add up.
        String desktop = desktopThemeFor(lower(request.getArg("javascript.desktopTheme", null)), shared, null);
        if (desktop != null) {
            out.add(desktop);
        } else if (isAutoDesktop(lower(request.getArg("javascript.desktopTheme", null)), shared)) {
            out.add("WindowsFluentTheme");
            out.add("MacOSAquaTheme");
            out.add("GnomeAdwaitaTheme");
        }

        // iOS-like browsers (iOS, iPadOS and a Mac that is not given a desktop theme).
        String iosMode = lower(request.getArg("ios.themeMode", null));
        if (iosMode == null && shared != null) {
            if ("modern".equals(shared) || "auto".equals(shared) || "native".equals(shared)) {
                iosMode = "modern";
            } else if ("legacy".equals(shared)) {
                iosMode = "ios7";
            }
        }
        if (iosMode == null || "ios7".equals(iosMode) || "flat".equals(iosMode)) {
            out.add("iOS7Theme");
        } else if ("legacy".equals(iosMode) || "iphone".equals(iosMode)) {
            out.add("iPhoneTheme");
        } else {
            out.add("iOSModernTheme");
        }

        // Every other browser.
        String androidMode = lower(request.getArg("and.themeMode", request.getArg("cn1.androidTheme", null)));
        if (androidMode == null && shared != null) {
            if ("modern".equals(shared) || "auto".equals(shared) || "native".equals(shared)) {
                androidMode = "material";
            } else if ("legacy".equals(shared)) {
                androidMode = "hololight";
            }
        }
        if (androidMode == null) {
            // The port's historical default: Holo Light on an Android user agent, iOS 7 on the
            // rest.
            out.add("android_holo_light");
            out.add("iOS7Theme");
        } else if ("legacy".equals(androidMode)) {
            out.add("androidTheme");
        } else if ("hololight".equals(androidMode) || "holo".equals(androidMode)) {
            out.add("android_holo_light");
        } else {
            out.add("AndroidMaterialTheme");
        }
        return out;
    }

    /// The desktop theme a desktop browser gets, or null when it keeps the mobile branch (or,
    /// for `auto`, when the answer depends on the browser's OS -- see [#isAutoDesktop]).
    ///
    /// #### Parameters
    ///
    /// - `desktopHint`: `javascript.desktopTheme`, lower case, or null
    /// - `shared`: `nativeTheme`, lower case, or null
    /// - `os`: `win`, `mac` or `linux` to resolve `auto`; null when unknown
    static String desktopThemeFor(String desktopHint, String shared, String os) {
        if ("fluent".equals(desktopHint) || "windows".equals(desktopHint)) {
            return "WindowsFluentTheme";
        }
        if ("aqua".equals(desktopHint) || "mac".equals(desktopHint) || "macos".equals(desktopHint)) {
            return "MacOSAquaTheme";
        }
        if ("adwaita".equals(desktopHint) || "gnome".equals(desktopHint) || "linux".equals(desktopHint)) {
            return "GnomeAdwaitaTheme";
        }
        if (os != null && isAutoDesktop(desktopHint, shared)) {
            if ("win".equals(os)) {
                return "WindowsFluentTheme";
            }
            if ("mac".equals(os)) {
                return "MacOSAquaTheme";
            }
            return "GnomeAdwaitaTheme";
        }
        return null;
    }

    /// Whether a desktop browser picks its theme by OS. Only `nativeTheme=native` asks for it:
    /// `modern` means the mobile modern themes, exactly as it does for the JavaSE desktop build.
    static boolean isAutoDesktop(String desktopHint, String shared) {
        return (desktopHint == null || "auto".equals(desktopHint)) && "native".equals(shared);
    }

    /// Scans the application's classes -- loose, or inside jars -- for the theme names in
    /// [#ALL_THEMES] (as `Name.res`), for [#MODERN_THEME_PROPERTY], and for the
    /// [#THEME_PROPERTIES] (recorded as [#RUNTIME_THEME_CHOICE]). A class that names a resource
    /// or a property stores the string verbatim in its constant pool, so a byte search finds it.
    ///
    /// The framework's annotation and implementation packages are skipped (see
    /// [#isFrameworkPath(String)]): the tree is the application merged with the framework, and
    /// those spell the hint names too.
    /// Call this BEFORE the port is merged in, for the same reason: the port names every theme.
    static Set<String> scanThemeReferences(File classesDir) throws IOException {
        Set<String> found = new LinkedHashSet<String>();
        if (classesDir != null && classesDir.isDirectory()) {
            scanDirectory(classesDir, "", found);
        }
        return found;
    }

    /// The framework packages that spell the theme hint names without choosing a theme: the
    /// build-hint annotations declare them, and the ports read them. Only these are skipped --
    /// not all of `com/codename1/`, which applications and cn1libs use for their own packages
    /// too, and whose calls to `Display.setProperty` must still be seen.
    static boolean isFrameworkPath(String relativePath) {
        return relativePath.startsWith("com/codename1/annotations/")
                || relativePath.startsWith("com/codename1/impl/");
    }

    private static void scanDirectory(File dir, String relativePath, Set<String> found) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String childPath = relativePath.length() == 0 ? child.getName() : relativePath + "/" + child.getName();
            if (child.isDirectory()) {
                if (!isFrameworkPath(childPath + "/")) {
                    scanDirectory(child, childPath, found);
                }
            } else if (child.getName().endsWith(".class")) {
                InputStream in = new FileInputStream(child);
                try {
                    scanBytes(readAll(in), found);
                } finally {
                    in.close();
                }
            } else if (child.getName().endsWith(".jar")) {
                ZipInputStream zin = new ZipInputStream(new FileInputStream(child));
                try {
                    ZipEntry e;
                    while ((e = zin.getNextEntry()) != null) {
                        if (!e.isDirectory() && e.getName().endsWith(".class") && !isFrameworkPath(e.getName())) {
                            scanBytes(readAll(zin), found);
                        }
                    }
                } finally {
                    zin.close();
                }
            }
        }
    }

    static void scanBytes(byte[] data, Set<String> found) {
        // ISO-8859-1 maps each byte to one char, so an ASCII needle is found exactly where the
        // bytes are, whatever else the class file contains.
        String text = new String(data, StandardCharsets.ISO_8859_1);
        for (String name : ALL_THEMES) {
            if (text.indexOf(name + ".res") >= 0) {
                found.add(name);
            }
        }
        if (text.indexOf(MODERN_THEME_PROPERTY) >= 0) {
            found.add(MODERN_THEME_PROPERTY);
        }
        for (String property : THEME_PROPERTIES) {
            if (containsConstant(text, property)) {
                found.add(RUNTIME_THEME_CHOICE);
                break;
            }
        }
    }

    /// Whether `text` holds `value` as a whole constant-pool string: a CONSTANT_Utf8 entry is a
    /// tag byte 1 and a two-byte length before its bytes. "nativeTheme" must not match inside
    /// "nativeThemeResource" or another longer name.
    private static boolean containsConstant(String text, String value) {
        int len = value.length();
        int from = 0;
        while (true) {
            int at = text.indexOf(value, from);
            if (at < 0) {
                return false;
            }
            if (at >= 3 && text.charAt(at - 3) == 1 && text.charAt(at - 2) == (char) ((len >> 8) & 0xff)
                    && text.charAt(at - 1) == (char) (len & 0xff)) {
                return true;
            }
            from = at + 1;
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static String baseName(String path) {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.endsWith(".res") ? name.substring(0, name.length() - 4) : name;
    }

    private static String lower(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.length() == 0) {
            return null;
        }
        // An ASCII fold: these are hint keywords, and toLowerCase() is locale sensitive.
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return b.toString();
    }

    private static String javaString(String value) {
        StringBuilder b = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '"': b.append("\\\""); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        String hex = Integer.toHexString(c);
                        b.append("\\u");
                        for (int p = hex.length(); p < 4; p++) {
                            b.append('0');
                        }
                        b.append(hex);
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.toString();
    }
}
