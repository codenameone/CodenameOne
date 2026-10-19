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

/// Platform specific hints for `FileDialogRequest#hint(String, Object)`.
///
/// A hint tunes a native dialog in a way only some platforms can. A platform
/// that does not know a hint ignores it, so an application can set every hint
/// it cares about unconditionally. The prefix names the platform that reads it.
public final class FileDialogHints {
    /// `Boolean`: show the Finder tag field in a macOS save panel.
    public static final String MAC_SHOWS_TAG_FIELD = "mac.showsTagField";
    /// `Boolean`: allow creating folders from a macOS panel. Defaults to true.
    public static final String MAC_CAN_CREATE_DIRECTORIES = "mac.canCreateDirectories";
    /// `Boolean`: show hidden files in a macOS panel.
    public static final String MAC_SHOWS_HIDDEN_FILES = "mac.showsHiddenFiles";
    /// `Boolean`: let the user hide the extension in a macOS save panel.
    public static final String MAC_CAN_SELECT_HIDDEN_EXTENSION = "mac.canSelectHiddenExtension";
    /// `Boolean`: treat packages (`.app`, `.bundle`) as folders in a macOS open panel.
    public static final String MAC_TREATS_PACKAGES_AS_DIRECTORIES = "mac.treatsFilePackagesAsDirectories";
    /// `Boolean`: show file extensions in the iOS document picker.
    public static final String IOS_SHOWS_FILE_EXTENSIONS = "ios.showsFileExtensions";
    /// `Boolean`: move rather than copy the exported file on iOS. Defaults to copy.
    public static final String IOS_EXPORT_AS_MOVE = "ios.exportAsMove";
    /// `String`: a `content://` URI the Android picker starts in.
    public static final String ANDROID_INITIAL_URI = "android.initialUri";
    /// `Boolean`: keep persistable read/write permission to the picked
    /// Android document across restarts.
    public static final String ANDROID_PERSIST_PERMISSION = "android.persistPermission";
    /// `String`: a GUID string; Windows remembers the last folder and size
    /// separately per GUID, so different dialogs of one app do not share state.
    public static final String WIN_CLIENT_GUID = "win.clientGuid";
    /// `String`: the folder Windows shows when the user has no recent folder,
    /// as opposed to `FileDialogRequest#initialDirectory`, which always wins.
    public static final String WIN_DEFAULT_FOLDER = "win.defaultFolder";
    /// `String`: an id under which the browser remembers the last directory
    /// (`showSaveFilePicker`'s `id`).
    public static final String WEB_ID = "web.id";
    /// `String`: a well-known browser directory to start in: `desktop`,
    /// `documents`, `downloads`, `music`, `pictures` or `videos`.
    public static final String WEB_START_IN = "web.startIn";
    /// `Boolean`: show hidden files in the GTK chooser and the simulator.
    public static final String DESKTOP_SHOW_HIDDEN = "desktop.showHidden";

    private FileDialogHints() {
    }
}
