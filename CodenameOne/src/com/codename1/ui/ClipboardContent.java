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

    private final List<String> mimeTypes = new ArrayList<String>();
    private final List<Object> values = new ArrayList<Object>();

    /// Adds or replaces a representation. Passing null removes the MIME type.
    public ClipboardContent setData(String mimeType, Object value) {
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
    public Object getData(String mimeType) {
        int index = mimeTypes.indexOf(normalizeMimeType(mimeType));
        return index < 0 ? null : values.get(index);
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

    /// Returns the first available MIME type from the caller's preference list, or null.
    public String findPreferredMimeType(String[] preferredMimeTypes) {
        if (preferredMimeTypes == null) {
            return null;
        }
        for (int i = 0; i < preferredMimeTypes.length; i++) {
            String mimeType = normalizeMimeType(preferredMimeTypes[i]);
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
        return value.trim().toLowerCase();
    }
}
