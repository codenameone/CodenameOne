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

package com.codename1.ui;

import java.util.ArrayList;
import java.util.List;

/// A set of alternative clipboard representations keyed by MIME type. A clipboard write should
/// normally include `text/plain` plus any richer formats it can provide. Clipboard readers negotiate
/// by inspecting `#getMimeTypes()` and requesting the representation they understand best.
///
/// Values are usually `String` or `byte[]`. Ports may support additional native value types, but must
/// retain the plain-text representation when one is supplied.
public class ClipboardContent {
    public static final String MIME_TEXT = "text/plain";
    public static final String MIME_HTML = "text/html";
    public static final String MIME_RTF = "text/rtf";
    public static final String MIME_MARKDOWN = "text/markdown";
    public static final String MIME_ASCIIDOC = "text/asciidoc";
    /// PNG image bytes (`byte[]`).
    public static final String MIME_PNG = "image/png";
    /// JPEG image bytes (`byte[]`).
    public static final String MIME_JPEG = "image/jpeg";
    /// GIF image bytes (`byte[]`).
    public static final String MIME_GIF = "image/gif";
    /// A local file reference (a file path / URI `String`, or a `String[]` for several files).
    public static final String MIME_FILE = "application/x-file-list";
    /// A newline separated list of URIs, the format desktop browsers and file managers use when
    /// a link or a file is dragged out of them.
    public static final String MIME_URI_LIST = "text/uri-list";

    private final List<String> mimeTypes = new ArrayList<String>();
    private final List<Object> values = new ArrayList<Object>();

    /// Adds or replaces a representation. Passing null removes the MIME type.
    public ClipboardContent setData(String mimeType, Object value) {
        return put(mimeType, value);
    }

    /// Declares a representation that is only built if something reads it, which is how a drag
    /// offers a file it has not written yet or an image it has not encoded yet.
    ///
    /// The provider is asked at most once and its answer is then cached, so
    /// `#getData(java.lang.String)` behaves exactly as it would for a value passed to
    /// `#setData(java.lang.String, java.lang.Object)`. It may run on a native clipboard or drag
    /// thread; see `ClipboardDataProvider`. Passing a null provider removes the MIME type.
    ///
    /// #### Parameters
    ///
    /// - `mimeType`: the MIME type this provider can produce
    ///
    /// - `provider`: the provider, or null to remove the representation
    ///
    /// #### Returns
    ///
    /// this instance, for chaining
    public ClipboardContent setDataProvider(String mimeType, ClipboardDataProvider provider) {
        return put(mimeType, provider == null ? null : new LazyValue(provider));
    }

    private ClipboardContent put(String mimeType, Object value) {
        String normalized = normalizeMimeType(mimeType);
        if (normalized.length() == 0) {
            throw new IllegalArgumentException("MIME type must not be empty");
        }
        int index = mimeTypes.indexOf(normalized);
        if (value == null) {
            if (index >= 0) {
                mimeTypes.remove(index);
                values.remove(index);
            }
            return this;
        }
        if (index >= 0) {
            values.set(index, value);
        } else {
            mimeTypes.add(normalized);
            values.add(value);
        }
        return this;
    }

    /// Returns the representation for a MIME type, or null when it isn't available.
    ///
    /// A representation registered through
    /// `#setDataProvider(java.lang.String, com.codename1.ui.ClipboardDataProvider)` is produced
    /// here, on the first call for that MIME type.
    public Object getData(String mimeType) {
        int index = mimeTypes.indexOf(normalizeMimeType(mimeType));
        if (index < 0) {
            return null;
        }
        Object value = values.get(index);
        if (value instanceof LazyValue) {
            return ((LazyValue) value).resolve(mimeTypes.get(index));
        }
        return value;
    }

    /// The value for a MIME type, produced now rather than taken from what a transfer
    /// remembered.
    ///
    /// For a transfer that can outlive the one after it. iOS keeps a drag readable for as
    /// long as a receiver holds one of its item providers, so an older session can be read
    /// while a newer one is running -- and the memo below belongs to whichever transfer
    /// armed the operation last, which would hand the older session the newer session's
    /// value, or make it produce a second one. A port in that position keeps a memo of its
    /// own, per session; see IOSImplementation.nativeDragResolveCallback.
    ///
    /// #### Parameters
    ///
    /// - `mimeType`: the representation wanted
    ///
    /// #### Returns
    ///
    /// the value, or null when this content does not offer that type
    Object produceData(String mimeType) {
        int index = mimeTypes.indexOf(normalizeMimeType(mimeType));
        if (index < 0) {
            return null;
        }
        Object value = values.get(index);
        if (value instanceof LazyValue) {
            return ((LazyValue) value).produce(mimeTypes.get(index));
        }
        return value;
    }

    /// Forgets every value a provider has produced, so the next transfer asks for it again.
    ///
    /// Resolving once is right *within* a transfer -- a drop queries and then reads, and the
    /// promised file must not be written twice -- but not beyond one. A component may install
    /// a single `NativeDragOperation` and be dragged again and again, and a provider that
    /// generates a temporary file would then have published, on the second drag, a path from
    /// the first: stale, or by then cleaned up and gone.
    ///
    /// Called where an operation is armed for a new session; see
    /// `NativeDragOperation#resetProvidedValues()`.
    void resetProvidedValues() {
        for (int iter = 0; iter < values.size(); iter++) {
            Object value = values.get(iter);
            if (value instanceof LazyValue) {
                ((LazyValue) value).forget();
            }
        }
    }

    /// A provider plus the value it produced. The value is resolved once per transfer and
    /// remembered, so a target that asks twice -- as a drop does when it queries and then
    /// reads -- does not run the provider twice and does not, for instance, write the
    /// promised file twice. `#resetProvidedValues()` is what ends a transfer's memory.
    private static final class LazyValue {
        private final ClipboardDataProvider provider;
        private Object resolved;
        private boolean done;

        LazyValue(ClipboardDataProvider provider) {
            this.provider = provider;
        }

        synchronized Object resolve(String mimeType) {
            if (!done) {
                done = true;
                resolved = provider.getClipboardData(mimeType);
            }
            return resolved;
        }

        synchronized void forget() {
            done = false;
            resolved = null;
        }

        /// Runs the provider without remembering the answer, for a caller that remembers
        /// it somewhere of its own. See `ClipboardContent#produceData(java.lang.String)`.
        Object produce(String mimeType) {
            return provider.getClipboardData(mimeType);
        }
    }

    /// Returns the binary (`byte[]`) representation for a MIME type -- e.g. the raw bytes of an image
    /// or file -- or null when it isn't available or isn't binary.
    public byte[] getBytes(String mimeType) {
        Object value = getData(mimeType);
        return value instanceof byte[] ? (byte[]) value : null;
    }

    /// Returns a string representation, or null when the value isn't a string.
    public String getText(String mimeType) {
        Object value = getData(mimeType);
        return value instanceof String ? (String) value : null;
    }

    /// Returns true when this content includes the requested MIME type.
    public boolean hasMimeType(String mimeType) {
        return mimeTypes.contains(normalizeMimeType(mimeType));
    }

    /// Returns the available MIME types in preference order.
    public String[] getMimeTypes() {
        return mimeTypes.toArray(new String[mimeTypes.size()]);
    }

    /// Sets the `#MIME_FILE` representation from a list of file paths or `file:` URIs.
    ///
    /// Ports differ on whether a single file is carried as a `String` or a one element
    /// `String[]`; this writes the form the rest of the framework expects, and
    /// `#getFiles()` reads either.
    ///
    /// #### Parameters
    ///
    /// - `paths`: the file paths, or null to remove the representation
    ///
    /// #### Returns
    ///
    /// this instance, for chaining
    public ClipboardContent setFiles(String[] paths) {
        if (paths == null || paths.length == 0) {
            return setData(MIME_FILE, null);
        }
        if (paths.length == 1) {
            return setData(MIME_FILE, paths[0]);
        }
        return setData(MIME_FILE, paths.clone());
    }

    /// Returns the `#MIME_FILE` representation as a path array regardless of whether the
    /// producer stored one `String` or a `String[]`, or null when no files are available.
    public String[] getFiles() {
        Object value = getData(MIME_FILE);
        if (value instanceof String[]) {
            String[] paths = (String[]) value;
            return paths.length == 0 ? null : paths.clone();
        }
        if (value instanceof String && ((String) value).length() > 0) {
            return new String[] { (String) value };
        }
        return null;
    }

    /// Returns the first available MIME type from the caller's preference list, or null.
    public String findPreferredMimeType(String[] preferredMimeTypes) {
        if (preferredMimeTypes == null) {
            return null;
        }
        for (String preferredMimeType : preferredMimeTypes) {
            String mimeType = normalizeMimeType(preferredMimeType);
            if (mimeTypes.contains(mimeType)) {
                return mimeType;
            }
        }
        return null;
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        int semicolon = mimeType.indexOf(';');
        String value = semicolon < 0 ? mimeType : mimeType.substring(0, semicolon);
        return asciiLower(value.trim());
    }

    /// Lowercases ASCII letters only, so the result never depends on the device locale.
    ///
    /// String.toLowerCase() is locale sensitive, and a Turkish or Azerbaijani default turns
    /// I into a dotless i: IMAGE/PNG normalized under one of those locales stopped being
    /// equal to image/png, so every check against the framework's own constants failed and
    /// a port no longer recognized the representation at all. MIME types, schemes and file
    /// extensions are ASCII by definition, which is what makes folding only ASCII correct
    /// rather than merely safe. Codename One has no java.util.Locale to ask for the root
    /// locale instead.
    private static String asciiLower(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }
}
