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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/// The one place that knows how a skin is written into, and read back out of, the
/// `skins` preference. Every entry in that preference is one of:
///
/// - a classpath resource, `/iPhoneX.skin`
/// - a `file:` URI produced by [#toEntry], which is how everything the user adds
///   and everything the OTA gallery downloads is stored
/// - a bare filesystem path, which older versions wrote and whose entries are
///   still in people's preference nodes
///
/// The reason this is a class rather than four inlined expressions: a `file:` URI is
/// **percent-encoded**, and the menu used to turn one back into a file with
/// `new File(new URL(entry).getFile())`, which does not decode. A skin under a path
/// holding a space, a `#`, a `%` or a browser's `name (1).skin` de-duplication suffix
/// therefore resolved to a file that does not exist, and the entry was dropped from
/// the Skins menu with no message -- the skin loaded and applied, it just could never
/// be selected again. See SkinPathTest for the measured matrix.
final class SkinPath {
    /// `;` separates entries in the preference and is a legal filename character on
    /// every platform we run on, while `File.toURI()` leaves it alone. Encoding it on
    /// the way in is what keeps a skin called `a;b.skin` from being stored as two
    /// entries that each resolve to nothing.
    private static final String ENTRY_SEPARATOR = ";";

    private SkinPath() {
    }

    /// Renders a local skin file as a preference entry. Absolute, percent-encoded, and
    /// safe to concatenate into the separated list.
    static String toEntry(File skin) {
        if (skin == null) {
            return null;
        }
        return escapeSeparator(skin.getAbsoluteFile().toURI().toString());
    }

    /// Resolves a preference entry to the file it names, or `null` when the entry does
    /// not name a local file at all (a classpath resource, an `http:` URL, junk).
    ///
    /// The returned file is **not** checked for existence -- a classpath entry like
    /// `/iPhoneX.skin` is indistinguishable from an absolute path here, and telling
    /// the two apart is the caller's job.
    static File toFile(String entry) {
        if (entry == null || entry.length() == 0) {
            return null;
        }
        if (isFileUri(entry)) {
            try {
                return new File(new URI(entry));
            } catch (Exception uriFailed) {
                // Not a well-formed URI: either an entry an older version wrote
                // without encoding it, or one carrying an authority component
                // (file://localhost/...). Both still name a file, and the legacy
                // reading is the only one that can find it.
                try {
                    return new File(new URL(entry).getFile());
                } catch (Exception urlFailed) {
                    return null;
                }
            }
        }
        if (hasRemoteScheme(entry)) {
            // It parses as a scheme, but a colon is a legal character in a relative
            // filename on every platform except Windows, so `theme:blue.skin` is a
            // scheme to the eye and a file on disk. Ask the disk before rejecting it;
            // a real remote entry never answers yes (`new File("http://host/x")` does
            // not exist), so this cannot reclassify one.
            File onDisk = new File(entry);
            if (onDisk.exists()) {
                return onDisk;
            }
            return null;
        }
        return new File(entry);
    }

    /// Whether the entry is a `file:` URI. Matched **case-insensitively**: a URI scheme
    /// is case-insensitive by RFC 3986, `new URL("FILE://...")` resolves as a local file,
    /// and entries reaching us from `-Dskin`, `CN1_SIMULATOR_SKIN` or a hand-edited
    /// preference are not necessarily the lowercase form `File.toURI()` emits.
    ///
    /// `regionMatches` rather than `toLowerCase().startsWith(..)`: case folding is locale
    /// sensitive, and a device set to Turkish folds `I` to a dotless i, so the comparison
    /// against an ASCII constant would fail for those users.
    private static boolean isFileUri(String entry) {
        return entry.regionMatches(true, 0, "file:", 0, 5);
    }

    /// Whether the entry carries a URI scheme other than `file:` -- `http:`, `https:`,
    /// `jar:file:...` -- and so names something that is not a local file.
    ///
    /// A scheme of a single character is deliberately NOT one: that is a Windows drive
    /// letter, and `C:\skins\Pixel9.skin` is a path this has to leave alone. Real
    /// schemes are longer, so the two never collide in practice.
    private static boolean hasRemoteScheme(String entry) {
        int colon = entry.indexOf(':');
        if (colon < 2) {
            return false;
        }
        for (int i = 0; i < colon; i++) {
            char c = entry.charAt(i);
            boolean schemeChar = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.';
            if (!schemeChar) {
                return false;
            }
        }
        char first = entry.charAt(0);
        return (first >= 'a' && first <= 'z') || (first >= 'A' && first <= 'Z');
    }

    /// The label the Skins menu shows for an entry: the file name for anything that
    /// names a file, the resource name for a classpath skin, and the raw entry when it
    /// is neither.
    static String displayName(String entry) {
        if (entry == null || entry.length() == 0) {
            return entry;
        }
        File f = toFile(entry);
        if (f != null && (isFileUri(entry) || f.exists())) {
            return f.getName();
        }
        if (entry.startsWith("/")) {
            return entry.substring(1);
        }
        return entry;
    }

    /// Splits the stored preference into entries, dropping the empty ones that older
    /// writers left behind (the default value ends in a separator, and appending to it
    /// produced `;;`).
    static List<String> split(String preference) {
        List<String> entries = new ArrayList<String>();
        if (preference == null) {
            return entries;
        }
        for (String entry : preference.split(ENTRY_SEPARATOR)) {
            if (entry.length() > 0) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /// Whether the preference already lists this exact entry. Compared whole rather
    /// than with `String.contains`, which answers yes for any entry that happens to be
    /// a substring of another one -- `/AppleWatch45mm.skin` is a substring of a user's
    /// `MyAppleWatch45mm.skin`, and the bundled skin would then never be registered.
    static boolean contains(String preference, String entry) {
        if (entry == null) {
            return false;
        }
        return split(preference).contains(entry);
    }

    /// Appends an entry to the preference unless it is already listed, and returns the
    /// new preference value.
    static String append(String preference, String entry) {
        if (entry == null || entry.length() == 0) {
            return preference;
        }
        List<String> entries = split(preference);
        if (entries.contains(entry)) {
            return join(entries);
        }
        entries.add(entry);
        return join(entries);
    }

    /// Renders entries back into the stored form.
    static String join(List<String> entries) {
        StringBuilder sb = new StringBuilder();
        for (String entry : entries) {
            if (entry == null || entry.length() == 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(ENTRY_SEPARATOR);
            }
            sb.append(entry);
        }
        return sb.toString();
    }

    private static String escapeSeparator(String uri) {
        if (uri.indexOf(';') < 0) {
            return uri;
        }
        return uri.replace(";", "%3B");
    }
}
