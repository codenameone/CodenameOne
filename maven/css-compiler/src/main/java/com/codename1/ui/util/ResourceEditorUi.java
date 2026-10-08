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
package com.codename1.ui.util;

/// The few things [EditableResources] needs from a user interface.
///
/// `EditableResources` is both the resource editor's document model and the
/// writer the CSS compiler saves a theme with. It used to open Swing dialogs
/// itself, which made a `HeadlessException` reachable from a build that has
/// no display. The dialogs now live behind this class: the default answers
/// without a user interface, and the resource editor installs a Swing
/// implementation when it starts.
///
/// One instance serves the whole JVM, like the password state it mediates.
public class ResourceEditorUi {

    private static ResourceEditorUi instance = new ResourceEditorUi();

    /// The installed implementation; never null.
    public static ResourceEditorUi get() {
        return instance;
    }

    /// Replaces the implementation. Passing null restores the default.
    public static void install(ResourceEditorUi ui) {
        instance = ui == null ? new ResourceEditorUi() : ui;
    }

    /// Asks for the password of a protected resource file.
    ///
    /// #### Parameters
    ///
    /// - `current`: the password used last, or null; an editor may offer it.
    ///
    /// #### Returns
    ///
    /// the password, or null when none is available -- the user cancelled, or
    /// there is no user to ask, which is the case here.
    public String promptPassword(String current) {
        return null;
    }

    /// Reports a problem that does not stop the operation in progress. The
    /// default writes it to standard error.
    public void reportError(String title, String message) {
        System.err.println(title + ": " + message);
    }

    /// Called whenever the "has unsaved changes" state of a resource file is
    /// set. The default does nothing.
    public void modifiedChanged(boolean modified) {
    }
}
