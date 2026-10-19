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

package com.codename1.io.filechooser;

import java.util.ArrayList;
import java.util.List;

/// A named group of file types a file dialog offers, such as "PNG Image" or
/// "Text Documents".
///
/// A filter can name the same types three ways, and each platform reads the
/// one it understands: desktop dialogs and the fallback chooser use the
/// extensions, Android and the browser use the MIME types, and iOS and macOS
/// use the Uniform Type Identifiers when given, deriving them from the
/// extensions otherwise. Supplying extensions alone works everywhere.
///
/// ```java
/// FileTypeFilter png = new FileTypeFilter("PNG Image")
///         .extensions("png").mimeTypes("image/png").utTypes("public.png");
/// ```
public class FileTypeFilter {
    private final String label;
    private final List<String> extensions = new ArrayList<String>();
    private final List<String> mimeTypes = new ArrayList<String>();
    private final List<String> utTypes = new ArrayList<String>();

    /// Creates a filter with the label the dialog shows for it.
    ///
    /// #### Parameters
    ///
    /// - `label`: the human readable name of the filter
    public FileTypeFilter(String label) {
        this.label = label;
    }

    /// Adds file extensions, with or without the leading dot.
    ///
    /// #### Parameters
    ///
    /// - `ext`: extensions such as `"png"` or `".jpg"`
    ///
    /// #### Returns
    ///
    /// this filter
    public FileTypeFilter extensions(String... ext) {
        for (String e : ext) {
            if (e != null) {
                e = e.trim();
                if (e.startsWith(".")) {
                    e = e.substring(1);
                }
                if (e.length() > 0) {
                    extensions.add(e);
                }
            }
        }
        return this;
    }

    /// Adds MIME types such as `"image/png"` or `"image/*"`.
    ///
    /// #### Parameters
    ///
    /// - `types`: the MIME types
    ///
    /// #### Returns
    ///
    /// this filter
    public FileTypeFilter mimeTypes(String... types) {
        for (String t : types) {
            if (t != null && t.trim().length() > 0) {
                mimeTypes.add(t.trim());
            }
        }
        return this;
    }

    /// Adds Apple Uniform Type Identifiers such as `"public.png"`. Ignored
    /// outside iOS and macOS.
    ///
    /// #### Parameters
    ///
    /// - `types`: the type identifiers
    ///
    /// #### Returns
    ///
    /// this filter
    public FileTypeFilter utTypes(String... types) {
        for (String t : types) {
            if (t != null && t.trim().length() > 0) {
                utTypes.add(t.trim());
            }
        }
        return this;
    }

    /// The label the dialog shows.
    ///
    /// #### Returns
    ///
    /// the label
    public String getLabel() {
        return label;
    }

    /// The extensions, without leading dots.
    ///
    /// #### Returns
    ///
    /// a copy of the extension list
    public String[] getExtensions() {
        return extensions.toArray(new String[extensions.size()]);
    }

    /// The MIME types.
    ///
    /// #### Returns
    ///
    /// a copy of the MIME type list
    public String[] getMimeTypes() {
        return mimeTypes.toArray(new String[mimeTypes.size()]);
    }

    /// The Uniform Type Identifiers.
    ///
    /// #### Returns
    ///
    /// a copy of the identifier list
    public String[] getUtTypes() {
        return utTypes.toArray(new String[utTypes.size()]);
    }

    /// Checks whether a file name ends with one of this filter's extensions.
    /// A filter without extensions accepts every name.
    ///
    /// #### Parameters
    ///
    /// - `name`: the file name or path
    ///
    /// #### Returns
    ///
    /// true if the name matches
    public boolean acceptsName(String name) {
        if (extensions.isEmpty()) {
            return true;
        }
        if (name == null) {
            return false;
        }
        for (String e : extensions) {
            int start = name.length() - e.length() - 1;
            if (start >= 0 && name.charAt(start) == '.'
                    && name.regionMatches(true, start + 1, e, 0, e.length())) {
                return true;
            }
        }
        return false;
    }
}
