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
package com.codename1.router.tools;

import java.util.ArrayList;
import java.util.List;

/// Generates an `assetlinks.json` payload for Android App Links. The output is
/// intended to be hosted at `https://your.domain/.well-known/assetlinks.json`,
/// served over HTTPS with `Content-Type: application/json` and no redirects.
///
/// The Android system fetches this file at app install time and grants the app
/// the right to handle web intents for the domain automatically. Without it,
/// Android falls back to disambiguation chooser even if the app declares the
/// intent filter.
///
/// #### SHA-256 cert fingerprint
///
/// You can extract the fingerprint from your release keystore with:
///
/// ```sh
/// keytool -list -v -keystore your.keystore -alias your-alias | grep "SHA256:"
/// ```
///
/// The fingerprint must be supplied in colon-separated hex form (the format
/// `keytool` emits).
///
/// #### Example
///
/// ```java
/// String json = new AssetLinksBuilder()
///     .addApp("com.example.app",
///             "14:6D:E9:83:C5:73:06:50:D8:EE:B9:95:2F:34:FC:64:16:A0:83:42:E6:1D:BE:A8:8A:04:96:B2:3F:CF:44:E5")
///     .build();
/// ```
///
/// #### Reference
///
/// Google's documentation: <https://developer.android.com/training/app-links/verify-android-applinks>
public final class AssetLinksBuilder {

    private final List<Entry> entries = new ArrayList<Entry>();

    /// Adds an app entry. `packageName` is the application id from
    /// `AndroidManifest.xml`. `sha256Fingerprint` is the SHA-256 of the signing
    /// certificate, colon-separated hex.
    ///
    /// To support multiple build flavors (debug + release), call this method
    /// multiple times -- assetlinks.json supports an array of entries.
    public AssetLinksBuilder addApp(String packageName, String sha256Fingerprint) {
        if (packageName == null || packageName.length() == 0) {
            throw new IllegalArgumentException("packageName required");
        }
        if (sha256Fingerprint == null || sha256Fingerprint.length() == 0) {
            throw new IllegalArgumentException("sha256Fingerprint required");
        }
        Entry e = new Entry(packageName);
        e.fingerprints.add(sha256Fingerprint);
        entries.add(e);
        return this;
    }

    /// Adds an additional fingerprint to the most recently added app entry --
    /// useful when both Play App Signing's upload cert and your release cert
    /// should be verified.
    public AssetLinksBuilder addFingerprint(String sha256Fingerprint) {
        if (entries.isEmpty()) {
            throw new IllegalStateException("call addApp(...) before addFingerprint(...)");
        }
        entries.get(entries.size() - 1).fingerprints.add(sha256Fingerprint);
        return this;
    }

    /// Builds the JSON string. UTF-8 encoded, formatted for readability.
    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            sb.append("  {\n");
            sb.append("    \"relation\": [\"delegate_permission/common.handle_all_urls\"],\n");
            sb.append("    \"target\": {\n");
            sb.append("      \"namespace\": \"android_app\",\n");
            sb.append("      \"package_name\": \"").append(jsonEscape(e.pkg)).append("\",\n");
            sb.append("      \"sha256_cert_fingerprints\": [");
            for (int j = 0; j < e.fingerprints.size(); j++) {
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append('"').append(jsonEscape(e.fingerprints.get(j))).append('"');
            }
            sb.append("]\n");
            sb.append("    }\n");
            sb.append("  }");
            if (i < entries.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final class Entry {
        final String pkg;
        final List<String> fingerprints = new ArrayList<String>();
        Entry(String p) {
            this.pkg = p;
        }
    }
}
