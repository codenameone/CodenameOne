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

/// Describes a native Open dialog for
/// `com.codename1.ui.Display#showOpenDialog(OpenFileRequest, com.codename1.util.SuccessCallback)`.
///
/// ```java
/// Display.getInstance().showOpenDialog(new OpenFileRequest()
///         .title("Import photos")
///         .multiSelect(true)
///         .addFilter(new FileTypeFilter("Images").extensions("png", "jpg").mimeTypes("image/*"))
///         .preview(path -> new Label(path)),
///     result -> {
///         if (!result.isCancelled()) {
///             for (String p : result.getPaths()) {
///                 Log.p("Picked " + p);
///             }
///         }
///     });
/// ```
///
/// The paths reported are readable with `com.codename1.io.FileSystemStorage`.
/// On Android and iOS the picked document is copied into the app's cache first,
/// since those platforms hand out content that outlives no process.
public class OpenFileRequest extends FileDialogRequest<OpenFileRequest> {
    private boolean multiSelect;
    private boolean directories;
    private FilePreviewProvider preview;

    /// Lets the user pick several files. Platforms without
    /// `FileDialogFeatures#MULTI_SELECT` return one.
    ///
    /// #### Parameters
    ///
    /// - `multiSelect`: true to allow several files
    ///
    /// #### Returns
    ///
    /// this request
    public OpenFileRequest multiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
        return this;
    }

    /// Picks folders instead of files, where `FileDialogFeatures#DIRECTORIES`
    /// is supported.
    ///
    /// #### Parameters
    ///
    /// - `directories`: true to pick folders
    ///
    /// #### Returns
    ///
    /// this request
    public OpenFileRequest directories(boolean directories) {
        this.directories = directories;
        return this;
    }

    /// Supplies a preview shown beside the file list as the selection changes,
    /// where `FileDialogFeatures#PREVIEW` is supported.
    ///
    /// #### Parameters
    ///
    /// - `preview`: the preview provider
    ///
    /// #### Returns
    ///
    /// this request
    public OpenFileRequest preview(FilePreviewProvider preview) {
        this.preview = preview;
        return this;
    }

    /// Whether several files may be picked.
    ///
    /// #### Returns
    ///
    /// true for multi selection
    public boolean isMultiSelect() {
        return multiSelect;
    }

    /// Whether folders are picked.
    ///
    /// #### Returns
    ///
    /// true for folders
    public boolean isDirectories() {
        return directories;
    }

    /// The preview provider, or null.
    ///
    /// #### Returns
    ///
    /// the provider
    public FilePreviewProvider getPreview() {
        return preview;
    }
}
