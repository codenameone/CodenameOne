/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;

/// The bridge a pure editor view uses to reach the owning editor component and, through it, the platform
/// text input source. The owning `com.codename1.ui.AbstractEditorComponent` implements this so the low
/// level implementation access (which is package private to `com.codename1.ui`) stays out of the editor
/// engine package.
public interface EditorHost {
    /// True when the platform can bind a low level text input client (see
    /// `com.codename1.impl.CodenameOneImplementation#isTextInputSupported`).
    boolean isTextInputSupported();

    /// Binds the client to the platform text input source and returns an opaque handle.
    Object startTextInput(TextInputClient client, TextInputConfig config);

    /// Pushes the client's editing state down to the bound input source.
    void updateTextInputState(Object handle, TextInputState state);

    /// Unbinds a previously bound text input client.
    void stopTextInput(Object handle);

    /// Notifies the owning editor that the document content changed so it can fire its change listeners.
    void editorChanged();

    /// Delivers a semantic editor event (e.g. a code completion request) back to the owning editor.
    void fireEditorEvent(String type, String value);
}
