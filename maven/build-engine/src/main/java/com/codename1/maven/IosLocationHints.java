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
package com.codename1.maven;

import com.codename1.build.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Adds the iOS build hints an imported Android application's location use
/// needs, when the project does not set them itself.
///
/// Android declares location access with manifest permissions; iOS refuses
/// to ask for it at all without a usage description in `Info.plist`, and
/// stops background updates without the `location` background mode. An
/// Android project carries neither, so the same code that tracks a run on
/// Android silently gets no fixes on iOS. The hints are written into
/// `codenameone_settings.properties` rather than supplied behind the
/// developer's back, so they are visible, cloud builds see them, and editing
/// the text is how the prompt is changed.
///
/// A key already in the file, or declared through the `@IosPrivacy`
/// annotation in the project's sources, is left alone: the build refuses a
/// hint declared both ways. Lines are appended (or the one
/// `ios.background_modes` line extended); the file is never re-saved through
/// `Properties`, which would drop its comments and reorder it.
public final class IosLocationHints {

    static final String WHEN_IN_USE = "codename1.arg.ios.NSLocationWhenInUseUsageDescription";
    static final String ALWAYS = "codename1.arg.ios.NSLocationAlwaysUsageDescription";
    static final String ALWAYS_AND_WHEN_IN_USE = "codename1.arg.ios.NSLocationAlwaysAndWhenInUseUsageDescription";
    static final String BACKGROUND_MODES = "codename1.arg.ios.background_modes";

    private static final String SETTINGS = "codenameone_settings.properties";

    private IosLocationHints() {
    }

    /// The settings file of the project `androidDir` (`src/main/android`)
    /// belongs to, or null.
    public static File settingsFor(File androidDir) {
        File dir = androidDir == null ? null : androidDir.getAbsoluteFile();
        for (int i = 0; i < 5 && dir != null; i++) {
            File f = new File(dir, SETTINGS);
            if (f.isFile()) {
                return f;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    /// Updates `settings` for the manifest `permissions`; answers the keys
    /// it added or changed.
    public static List<String> apply(File settings, List<String> permissions, List<File> sourceRoots, Log log)
            throws IOException {
        List<String> changed = new ArrayList<String>();
        if (settings == null || !settings.isFile() || permissions == null) {
            return changed;
        }
        boolean foreground = permissions.contains("android.permission.ACCESS_FINE_LOCATION")
                || permissions.contains("android.permission.ACCESS_COARSE_LOCATION");
        boolean background = permissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION")
                || permissions.contains("android.permission.FOREGROUND_SERVICE_LOCATION");
        if (!foreground && !background) {
            return changed;
        }
        Map<String, String> wanted = new LinkedHashMap<String, String>();
        wanted.put(WHEN_IN_USE, "This app uses your location while you are using it.");
        if (background) {
            wanted.put(ALWAYS, "This app uses your location in the background.");
            wanted.put(ALWAYS_AND_WHEN_IN_USE, "This app uses your location, including in the background.");
        }
        String text = new String(Files.readAllBytes(settings.toPath()), "ISO-8859-1");
        String eol = text.indexOf("\r\n") >= 0 ? "\r\n" : "\n";
        StringBuilder out = new StringBuilder(text);
        for (Map.Entry<String, String> e : wanted.entrySet()) {
            if (lineOf(text, e.getKey()) >= 0 || declaredByAnnotation(e.getKey(), sourceRoots)) {
                continue;
            }
            appendLine(out, eol, e.getKey() + "=" + e.getValue());
            changed.add(e.getKey());
        }
        if (background) {
            int at = lineOf(out.toString(), BACKGROUND_MODES);
            if (at < 0) {
                appendLine(out, eol, BACKGROUND_MODES + "=location");
                changed.add(BACKGROUND_MODES);
            } else {
                int end = lineEnd(out, at);
                String line = out.substring(at, end);
                String value = line.substring(line.indexOf('=') + 1).trim();
                if (!hasMode(value, "location")) {
                    out.replace(at, end, BACKGROUND_MODES + "=" + (value.length() == 0 ? "" : value + ",")
                            + "location");
                    changed.add(BACKGROUND_MODES);
                }
            }
        }
        if (!changed.isEmpty()) {
            Files.write(settings.toPath(), out.toString().getBytes("ISO-8859-1"));
            if (log != null) {
                log.info("Added iOS location build hints for the manifest's location permissions to "
                        + settings + ": " + changed);
            }
        }
        return changed;
    }

    static boolean hasMode(String value, String mode) {
        for (String m : value.split("[,\\s]+")) {
            if (m.equals(mode)) {
                return true;
            }
        }
        return false;
    }

    /// Offset of the line that sets `key`, or -1. A commented line does not
    /// count.
    private static int lineOf(String text, String key) {
        int from = 0;
        while (from <= text.length()) {
            int end = text.indexOf('\n', from);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(from, end).trim();
            if (line.startsWith(key)) {
                String rest = line.substring(key.length()).trim();
                if (rest.startsWith("=") || rest.startsWith(":")) {
                    // Indentation is preserved by pointing at the key itself.
                    return text.indexOf(key, from);
                }
            }
            from = end + 1;
        }
        return -1;
    }

    private static int lineEnd(CharSequence text, int from) {
        int i = from;
        while (i < text.length() && text.charAt(i) != '\n' && text.charAt(i) != '\r') {
            i++;
        }
        return i;
    }

    private static void appendLine(StringBuilder out, String eol, String line) {
        if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
            out.append(eol);
        }
        out.append(line).append(eol);
    }

    /// Whether a source names the `@IosPrivacy` member behind `key`. Textual,
    /// since this runs before anything is compiled; a false positive only
    /// means a hint the developer has to add by hand.
    private static boolean declaredByAnnotation(String key, List<File> sourceRoots) throws IOException {
        String member;
        if (WHEN_IN_USE.equals(key)) {
            member = "locationWhenInUseUsageDescription";
        } else if (ALWAYS.equals(key)) {
            member = "locationAlwaysUsageDescription";
        } else if (ALWAYS_AND_WHEN_IN_USE.equals(key)) {
            member = "locationAlwaysAndWhenInUseUsageDescription";
        } else {
            return false;
        }
        if (sourceRoots == null) {
            return false;
        }
        for (File root : sourceRoots) {
            if (mentions(root, member)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentions(File f, String member) throws IOException {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) {
                    if (mentions(k, member)) {
                        return true;
                    }
                }
            }
            return false;
        }
        String n = f.getName();
        if (!n.endsWith(".java") && !n.endsWith(".kt")) {
            return false;
        }
        String src = new String(Files.readAllBytes(f.toPath()), "ISO-8859-1");
        return src.indexOf("IosPrivacy") >= 0 && src.indexOf(member) >= 0;
    }
}
