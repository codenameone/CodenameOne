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
package com.codename1.certificatewizard.project;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public final class SigningAssetInstaller {
    private SigningAssetInstaller() {
    }

    public static void applyDebugCertificate(String settingsPath, String p12Path, String password,
                                             String profilePath) throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("codename1.ios.debug.certificate", p12Path == null ? "" : p12Path);
        updates.put("codename1.ios.debug.certificatePassword", password == null ? "" : password);
        updates.put("codename1.ios.debug.provision", profilePath == null ? "" : profilePath);
        putTeamQualifiedAppId(settingsPath, profilePath, updates);
        update(settingsPath, updates);
    }

    public static void applyReleaseCertificate(String settingsPath, String p12Path, String password,
                                               String profilePath) throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("codename1.ios.release.certificate", p12Path == null ? "" : p12Path);
        updates.put("codename1.ios.release.certificatePassword", password == null ? "" : password);
        updates.put("codename1.ios.release.provision", profilePath == null ? "" : profilePath);
        updates.put("codename1.ios.certificate", p12Path == null ? "" : p12Path);
        updates.put("codename1.ios.certificatePassword", password == null ? "" : password);
        updates.put("codename1.ios.provision", profilePath == null ? "" : profilePath);
        putTeamQualifiedAppId(settingsPath, profilePath, updates);
        update(settingsPath, updates);
    }

    public static void applyAndroidKeystore(String settingsPath, String keystorePath, String alias,
                                            String password) throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("codename1.android.keystore", keystorePath == null ? "" : keystorePath);
        updates.put("codename1.android.keystoreAlias", alias == null ? "" : alias);
        updates.put("codename1.android.keystorePassword", password == null ? "" : password);
        update(settingsPath, updates);
    }

    public static void applyMacCertificate(String settingsPath, String p12Path, String password,
                                           String profilePath, String distribution) throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        updates.put("codename1.mac.certificate", p12Path == null ? "" : p12Path);
        updates.put("codename1.mac.certificatePassword", password == null ? "" : password);
        updates.put("codename1.mac.provision", profilePath == null ? "" : profilePath);
        updates.put("codename1.arg.macNative.enabled", "true");
        if (distribution != null && distribution.length() > 0) {
            updates.put("codename1.arg.macNative.distribution", distribution);
        }
        update(settingsPath, updates);
    }

    public static void applyWidgetExtensionSigning(String settingsPath, String appGroupIdentifier,
                                                   String releaseProfilePath, String debugProfilePath)
            throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        if (appGroupIdentifier != null) {
            updates.put("codename1.arg.ios.surfaces.appGroup", appGroupIdentifier);
        }
        if (releaseProfilePath != null) {
            // The unqualified key stays populated with the distribution profile so builds
            // through tooling that predates the debug/release split keep working.
            updates.put("codename1.ios.appext.CN1Widgets.provision", releaseProfilePath);
            updates.put("codename1.ios.release.appext.CN1Widgets.provision", releaseProfilePath);
            // Blank rather than skip the debug key when no development profile was
            // produced: a stale path left by an earlier wizard run would otherwise keep
            // overriding the unqualified fallback for debug device builds. A blank
            // qualified key is dropped during build-type resolution.
            updates.put("codename1.ios.debug.appext.CN1Widgets.provision",
                    debugProfilePath == null ? "" : debugProfilePath);
        } else if (debugProfilePath != null) {
            updates.put("codename1.ios.debug.appext.CN1Widgets.provision", debugProfilePath);
        }
        if (updates.isEmpty()) {
            return;
        }
        update(settingsPath, updates);
    }

    public static void applyDocumentProviderSigning(String settingsPath, String appGroupIdentifier,
                                                    String releaseProfilePath, String debugProfilePath)
            throws IOException {
        Map<String, String> updates = new HashMap<String, String>();
        // Turning the feature on is part of installing its signing: a project whose extension has
        // an App ID, an App Group and two profiles but no hint would build without the extension
        // and leave the developer with signing assets nothing consumes.
        updates.put("codename1.arg.ios.documentProvider.enabled", "true");
        if (appGroupIdentifier != null) {
            updates.put("codename1.arg.ios.documentProvider.appGroup", appGroupIdentifier);
        }
        if (releaseProfilePath != null) {
            // The unqualified key stays populated with the distribution profile so builds
            // through tooling that predates the debug/release split keep working.
            updates.put("codename1.ios.appext.CN1Documents.provision", releaseProfilePath);
            updates.put("codename1.ios.release.appext.CN1Documents.provision", releaseProfilePath);
            // Blank rather than skip the debug key when no development profile was produced: a
            // stale path left by an earlier wizard run would otherwise keep overriding the
            // unqualified fallback for debug device builds. A blank qualified key is dropped
            // during build-type resolution.
            updates.put("codename1.ios.debug.appext.CN1Documents.provision",
                    debugProfilePath == null ? "" : debugProfilePath);
        } else if (debugProfilePath != null) {
            updates.put("codename1.ios.debug.appext.CN1Documents.provision", debugProfilePath);
        }
        update(settingsPath, updates);
    }

    /// Points `codename1.ios.appid` at the App ID prefix of the profile being installed.
    ///
    /// The build server writes that value verbatim into the app's `application-identifier` and
    /// `keychain-access-groups` entitlements, and Xcode refuses to sign when the profile says
    /// otherwise: "Provisioning profile ... doesn't match the entitlements file's values for the
    /// application-identifier and keychain-access-groups entitlements" (issue #5901). New
    /// projects are generated with a placeholder team -- `Q5GHSKAL2F.<package>` -- that is
    /// nobody's real team, so a project signed only by this wizard failed every device build
    /// with a profile that was otherwise exactly right. The old wizard rewrote the key; this one
    /// never did.
    ///
    /// The prefix is the one in the profile's own `application-identifier` entitlement, NOT its
    /// `TeamIdentifier`. The two are usually the same ten characters, but an account that still
    /// carries a legacy Bundle Seed ID issues App IDs under that seed, and the entitlement Xcode
    /// matches is the App ID's. Taking the team there wrote a value the profile cannot sign.
    ///
    /// The bundle half is the project's `codename1.packageName`, which is what the build signs
    /// as. Left untouched when the profile states no prefix or the project has no package name:
    /// guessing either would write a value that is wrong in a new way.
    static void putTeamQualifiedAppId(String settingsPath, String profilePath, Map<String, String> updates)
            throws IOException {
        if (profilePath == null || profilePath.length() == 0) {
            return;
        }
        String prefix = profileAppIdPrefix(readBytes(profilePath));
        if (prefix == null) {
            return;
        }
        String bundleId = setting(read(settingsPath), "codename1.packageName");
        if (bundleId == null || bundleId.length() == 0 || bundleId.indexOf("${") >= 0) {
            return;
        }
        updates.put("codename1.ios.appid", prefix + "." + bundleId);
    }

    /// The prefix of a provisioning profile's `application-identifier` entitlement -- the part
    /// before the first dot of `ABCDE12345.com.example.app` -- or null when it states none.
    ///
    /// A `.mobileprovision` is a CMS envelope around an XML property list, and the list is
    /// stored uncompressed, so the entitlement can be read out of the raw bytes without
    /// verifying the signature: the wizard only needs to know what Apple put there, not to trust
    /// the file. The value has to follow its key directly -- `parent-application-identifiers` is
    /// a different key -- and only a well formed ten character prefix is returned.
    public static String profileAppIdPrefix(byte[] profile) {
        if (profile == null) {
            return null;
        }
        String text;
        try {
            text = new String(profile, "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException ex) {
            return null;
        }
        String key = "<key>application-identifier</key>";
        int at = text.indexOf(key);
        if (at < 0) {
            return null;
        }
        int open = at + key.length();
        while (open < text.length() && Character.isWhitespace(text.charAt(open))) {
            open++;
        }
        if (!text.startsWith("<string>", open)) {
            return null;
        }
        int close = text.indexOf("</string>", open);
        if (close < 0) {
            return null;
        }
        String appId = text.substring(open + "<string>".length(), close).trim();
        int dot = appId.indexOf('.');
        if (dot < 0) {
            return null;
        }
        String prefix = appId.substring(0, dot);
        return isAppIdPrefix(prefix) ? prefix : null;
    }

    private static boolean isAppIdPrefix(String s) {
        if (s.length() != 10) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    /// The value of one `key=value` line, or null. Enough for the plain keys the project
    /// template writes; the file is re-read here rather than parsed as Properties because the
    /// rest of this class edits it line by line to keep the developer's layout.
    static String setting(String text, String key) {
        String[] lines = text.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && !trimmed.startsWith("#") && key.equals(trimmed.substring(0, eq).trim())) {
                return trimmed.substring(eq + 1).trim();
            }
        }
        return null;
    }

    static byte[] readBytes(String path) throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String url = ProjectIO.fsUrl(path);
        if (!fs.exists(url)) {
            return null;
        }
        InputStream in = null;
        try {
            in = fs.openInputStream(url);
            return Util.readInputStream(in);
        } finally {
            Util.cleanup(in);
        }
    }

    static void update(String settingsPath, Map<String, String> updates) throws IOException {
        String text = read(settingsPath);
        String[] lines = text.replace("\r\n", "\n").split("\n");
        StringBuilder out = new StringBuilder(text.length() + 160);
        Map<String, String> remaining = new HashMap<String, String>(updates);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && !trimmed.startsWith("#")) {
                String key = trimmed.substring(0, eq).trim();
                if (remaining.containsKey(key)) {
                    out.append(key).append('=').append(escape(remaining.get(key))).append('\n');
                    remaining.remove(key);
                    continue;
                }
            }
            out.append(line).append('\n');
        }
        for (String key : remaining.keySet()) {
            out.append(key).append('=').append(escape(remaining.get(key))).append('\n');
        }
        write(settingsPath, out.toString());
    }

    static String read(String settingsPath) throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String url = ProjectIO.fsUrl(settingsPath);
        if (fs.exists(url)) {
            InputStream in = null;
            try {
                in = fs.openInputStream(url);
                return Util.readToString(in, "UTF-8");
            } finally {
                Util.cleanup(in);
            }
        }
        return "";
    }

    static void write(String settingsPath, String text) throws IOException {
        OutputStream out = null;
        try {
            out = FileSystemStorage.getInstance().openOutputStream(ProjectIO.fsUrl(settingsPath));
            out.write(text.getBytes("UTF-8"));
            out.flush();
        } finally {
            Util.cleanup(out);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "");
    }
}
