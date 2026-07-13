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
package com.codename1.surfaces;

import java.util.ArrayList;
import java.util.List;

/// Declares one kind of home-screen widget the app offers (an app may offer several). The kind id
/// must match an entry in the `surfaces.json` file kept in the project's resources: widget kinds
/// are compiled into the native app (the widget gallery entries, the iOS widget bundle and the
/// Android receivers are all static), so the build needs them at build time while this runtime
/// declaration drives publishing and validation. A mismatch logs a prominent warning.
///
/// Ids are restricted to `[a-z][a-z0-9_]*` because they are used to derive native type and
/// resource names.
public class WidgetKind {
    private final String id;
    private String displayName;
    private String description;
    private final List<WidgetSize> supportedSizes = new ArrayList<WidgetSize>();

    /// Creates a widget kind declaration.
    ///
    /// #### Parameters
    ///
    /// - `id`: the stable kind identifier, `[a-z][a-z0-9_]*`
    public WidgetKind(String id) {
        if (id == null || !isValidId(id)) {
            throw new IllegalArgumentException("Widget kind ids must match [a-z][a-z0-9_]* but got: " + id);
        }
        this.id = id;
    }

    private static boolean isValidId(String id) {
        int n = id.length();
        if (n == 0) {
            return false;
        }
        char first = id.charAt(0);
        if (first < 'a' || first > 'z') {
            return false;
        }
        for (int i = 1; i < n; i++) {
            char c = id.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /// Sets the title shown in the platform widget gallery.
    ///
    /// #### Parameters
    ///
    /// - `displayName`: the gallery title
    ///
    /// #### Returns
    ///
    /// this kind, for chaining
    public WidgetKind setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /// Sets the description shown in the platform widget gallery.
    ///
    /// #### Parameters
    ///
    /// - `description`: the gallery description
    ///
    /// #### Returns
    ///
    /// this kind, for chaining
    public WidgetKind setDescription(String description) {
        this.description = description;
        return this;
    }

    /// Adds a supported size family. When no size is added the kind defaults to `SMALL` and
    /// `MEDIUM`.
    ///
    /// #### Parameters
    ///
    /// - `size`: the size family to support
    ///
    /// #### Returns
    ///
    /// this kind, for chaining
    public WidgetKind addSupportedSize(WidgetSize size) {
        if (size != null && !supportedSizes.contains(size)) {
            supportedSizes.add(size);
        }
        return this;
    }

    /// Returns the stable kind identifier.
    public String getId() {
        return id;
    }

    /// Returns the gallery title, or null.
    public String getDisplayName() {
        return displayName;
    }

    /// Returns the gallery description, or null.
    public String getDescription() {
        return description;
    }

    /// Returns the supported size families; `SMALL` and `MEDIUM` when none were added explicitly.
    public List<WidgetSize> getSupportedSizes() {
        if (supportedSizes.isEmpty()) {
            List<WidgetSize> def = new ArrayList<WidgetSize>(2);
            def.add(WidgetSize.SMALL);
            def.add(WidgetSize.MEDIUM);
            return def;
        }
        return supportedSizes;
    }
}
