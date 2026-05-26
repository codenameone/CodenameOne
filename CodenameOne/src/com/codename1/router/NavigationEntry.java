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
package com.codename1.router;

import com.codename1.ui.Form;

/// A single frame on the `Navigation` stack: the URL that produced the form
/// and the `Form` instance the route built. Returned from
/// `Navigation#getStack`, `Navigation#getCurrent`, and accepted by
/// `Navigation#popTo` so a breadcrumb UI can pop back to any prior entry.
///
/// Entries are immutable value objects; equality is by identity.
public final class NavigationEntry {

    private final String path;
    private final Form form;

    NavigationEntry(String path, Form form) {
        this.path = path;
        this.form = form;
    }

    /// The path (URL minus scheme + host) that produced this entry, e.g.
    /// `/users/42`.
    public String getPath() {
        return path;
    }

    /// The `Form` instance the route builder produced.
    public Form getForm() {
        return form;
    }

    /// Convenience: the form's title, useful as a breadcrumb label. Returns
    /// the empty string when the form has no title set.
    public String getTitle() {
        String t = form == null ? null : form.getTitle();
        return t == null ? "" : t;
    }

    @Override
    public String toString() {
        return "NavigationEntry{" + path + "}";
    }
}
