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

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Describes a native Save dialog for
/// `com.codename1.ui.Display#showSaveDialog(SaveFileRequest, com.codename1.util.SuccessCallback)`.
///
/// The content is supplied either as an existing file (`#sourceFile(String)`)
/// or written on demand (`#content(SaveContent)`), after the user has chosen
/// a location, format and options:
///
/// ```java
/// Display.getInstance().showSaveDialog(new SaveFileRequest()
///         .suggestedName("Report.pdf")
///         .addFilter(new FileTypeFilter("PDF").extensions("pdf").mimeTypes("application/pdf"))
///         .addFilter(new FileTypeFilter("Plain text").extensions("txt").mimeTypes("text/plain"))
///         .addOption(FileDialogOption.checkbox("charts", "Include charts", true))
///         .content((result, out) -> report.export(result.getSelectedFilter(),
///                 "true".equals(result.getOptionValue("charts")), out)),
///     result -> {
///         if (result.getError() != null) {
///             ToastBar.showErrorMessage("Export failed");
///         }
///     });
/// ```
///
/// On Android the user picks a document through the Storage Access Framework
/// and the content is streamed into it; on iOS the content is written to a
/// temporary file which the document picker then exports. Either way the
/// application never needs a storage permission.
public class SaveFileRequest extends FileDialogRequest<SaveFileRequest> {
    private String suggestedName;
    private boolean confirmOverwrite = true;
    private String sourceFile;
    private SaveContent content;

    /// Creates a Save request.
    public SaveFileRequest() {
    }

    /// Sets the file name the dialog suggests, including its extension.
    ///
    /// #### Parameters
    ///
    /// - `name`: the file name
    ///
    /// #### Returns
    ///
    /// this request
    public SaveFileRequest suggestedName(String name) {
        this.suggestedName = name;
        return this;
    }

    /// Asks before replacing an existing file. On by default; the mobile
    /// pickers always handle conflicts their own way.
    ///
    /// #### Parameters
    ///
    /// - `confirm`: false to replace silently
    ///
    /// #### Returns
    ///
    /// this request
    public SaveFileRequest confirmOverwrite(boolean confirm) {
        this.confirmOverwrite = confirm;
        return this;
    }

    /// Saves a copy of an existing `FileSystemStorage` file.
    ///
    /// #### Parameters
    ///
    /// - `path`: the file to copy
    ///
    /// #### Returns
    ///
    /// this request
    public SaveFileRequest sourceFile(String path) {
        this.sourceFile = path;
        return this;
    }

    /// Writes the content once the destination is known. Takes precedence
    /// over `#sourceFile(String)`.
    ///
    /// #### Parameters
    ///
    /// - `content`: the writer
    ///
    /// #### Returns
    ///
    /// this request
    public SaveFileRequest content(SaveContent content) {
        this.content = content;
        return this;
    }

    /// The suggested file name, or null.
    ///
    /// #### Returns
    ///
    /// the name
    public String getSuggestedName() {
        return suggestedName;
    }

    /// Whether replacing a file asks first.
    ///
    /// #### Returns
    ///
    /// true to confirm
    public boolean isConfirmOverwrite() {
        return confirmOverwrite;
    }

    /// The file being copied, or null.
    ///
    /// #### Returns
    ///
    /// the path
    public String getSourceFile() {
        return sourceFile;
    }

    /// The content writer, or null.
    ///
    /// #### Returns
    ///
    /// the writer
    public SaveContent getContent() {
        return content;
    }

    /// Writes this request's content to a stream. Ports call it once the user
    /// has chosen a destination; the stream is not closed here.
    ///
    /// #### Parameters
    ///
    /// - `result`: what the user chose
    /// - `out`: the destination
    ///
    /// #### Throws
    ///
    /// - `IOException`: if neither content nor a source file was set, or writing fails
    public void writeContent(FileDialogResult result, OutputStream out) throws IOException {
        if (content != null) {
            content.write(result, out);
            return;
        }
        if (sourceFile == null) {
            throw new IOException("SaveFileRequest has neither content nor a source file");
        }
        InputStream in = FileSystemStorage.getInstance().openInputStream(sourceFile);
        try {
            Util.copyNoClose(in, out, 8192);
        } finally {
            Util.cleanup(in);
        }
    }
}
