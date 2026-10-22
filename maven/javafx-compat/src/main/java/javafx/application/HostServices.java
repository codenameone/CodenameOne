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
package javafx.application;

import com.codename1.ui.Display;

/// The services of the platform an application runs on; an application gets
/// its instance from [Application#getHostServices()].
public final class HostServices {

    HostServices() {
    }

    /// The location the application was started from. A Codename One
    /// application is not started from a location: the empty string.
    public String getCodeBase() {
        return "";
    }

    /// The location of the document that holds the application; the empty
    /// string, as [#getCodeBase()].
    public String getDocumentBase() {
        return "";
    }

    /// Resolves `rel` against `base`, or against [#getDocumentBase()] when
    /// `base` is null or empty.
    public String resolveURI(String base, String rel) {
        if (rel == null) {
            return base == null ? "" : base;
        }
        if (base == null || base.length() == 0 || rel.indexOf(':') > 0) {
            return rel;
        }
        if (base.endsWith("/") || rel.startsWith("/")) {
            return base + rel;
        }
        int slash = base.lastIndexOf('/');
        return slash < 0 ? rel : base.substring(0, slash + 1) + rel;
    }

    /// Opens `uri` in the platform's browser, or in whichever application
    /// the platform has for it.
    public void showDocument(String uri) {
        if (uri != null && Display.isInitialized()) {
            Display.getInstance().execute(uri);
        }
    }
}
