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

/// Capabilities a platform's file dialogs may have, queried with
/// `com.codename1.ui.Display#isFileDialogFeatureSupported(int)`.
///
/// Every platform can open and save a single file, at worst through the
/// Codename One fallback chooser; the features below are the ones that vary.
public final class FileDialogFeatures {
    /// The dialog is the platform's own, not the Codename One fallback.
    public static final int NATIVE = 1;
    /// `OpenFileRequest#multiSelect(boolean)` returns several files.
    public static final int MULTI_SELECT = 2;
    /// `OpenFileRequest#directories(boolean)` picks folders.
    public static final int DIRECTORIES = 3;
    /// More than one `FileTypeFilter` is shown as a user-selectable list.
    ///
    /// Where `NATIVE` is false, this and `OPTIONS` and `ACCESSORY_COMPONENT`
    /// describe the Codename One save form; the fallback open dialog shows
    /// none of the three.
    public static final int FILTER_LIST = 4;
    /// `FileDialogOption` controls are shown.
    public static final int OPTIONS = 5;
    /// `FileDialogRequest#accessory(com.codename1.ui.Component)` is shown.
    public static final int ACCESSORY_COMPONENT = 6;
    /// `OpenFileRequest#preview(FilePreviewProvider)` is called as the
    /// selection changes.
    public static final int PREVIEW = 7;
    /// `FileDialogRequest#initialDirectory(String)` is honoured.
    public static final int INITIAL_DIRECTORY = 8;
    /// The save dialog lets the user edit the suggested name.
    public static final int SAVE_NAME_EDITING = 9;

    private FileDialogFeatures() {
    }
}
