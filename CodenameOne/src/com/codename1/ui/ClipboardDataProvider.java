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

/// Produces a clipboard or drag-and-drop representation on demand, so a payload that is
/// expensive to build is only built if something actually asks for it.
///
/// This is what makes "drag a file out of the application" workable. A drag that offers
/// `ClipboardContent#MIME_FILE` has to name the file when the drag *starts*, but the drop may
/// never happen -- the user may let go over nothing -- and the target may prefer a different
/// representation entirely. Registering a provider with
/// `ClipboardContent#setDataProvider(java.lang.String, com.codename1.ui.ClipboardDataProvider)`
/// declares that the representation is available without paying for it up front; the bytes are
/// written, or the temporary file created, at the moment the receiving application reads that
/// MIME type.
///
/// A provider is invoked at most once per `ClipboardContent` and MIME type -- the result is
/// cached -- and it may be invoked from a native drag or clipboard thread rather than the event
/// dispatch thread, so it must not touch the user interface.
public interface ClipboardDataProvider {
    /// Produces the value for one representation.
    ///
    /// #### Parameters
    ///
    /// - `mimeType`: the MIME type being requested, always one this provider was registered for
    ///
    /// #### Returns
    ///
    /// the value, normally a `String`, a `String[]` of file paths or a `byte[]`, or null when
    /// the representation turned out to be unavailable
    public Object getClipboardData(String mimeType);
}
