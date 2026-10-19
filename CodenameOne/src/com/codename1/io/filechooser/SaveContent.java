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

import java.io.IOException;
import java.io.OutputStream;

/// Writes the content of a file once the user has chosen where to save it.
///
/// Called off the EDT, so a large document can be serialized without freezing
/// the UI. The stream is closed by the caller. `SaveFileRequest#onFilterChanged`
/// and `FileDialogResult#getOptionValue(String)` tell it which format and
/// options the user picked.
public interface SaveContent {
    /// Writes the file.
    ///
    /// #### Parameters
    ///
    /// - `result`: what the user chose (location, filter, options)
    /// - `out`: the destination
    ///
    /// #### Throws
    ///
    /// - `IOException`: if writing fails; the save is reported as failed
    void write(FileDialogResult result, OutputStream out) throws IOException;
}
