/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.filepicker;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.io.filechooser.FileDialogResult;
import com.codename1.io.filechooser.FileTypeFilter;
import com.codename1.io.filechooser.OpenFileRequest;
import com.codename1.io.filechooser.SaveFileRequest;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import dart.async.Completer;
import dart.async.Future;
import dart.core.DartList;
import dart.typed_data.Uint8List;

/**
 * file_picker's {@code FilePicker}, over {@code Display.showOpenDialog} and
 * {@code showSaveDialog}. {@code type} and {@code allowedExtensions} become the dialog's
 * filter, {@code withData} reads the bytes, {@code saveFile} writes {@code bytes} to the
 * chosen path. Titles and the initial directory are passed on; compression, read streams,
 * {@code onFileLoading} and {@code lockParentWindow} are accepted and not applied. A
 * cancelled dialog, or no display to show one on, answers null, as the plugin does.
 */
public class FilePicker {

    /** {@code FilePicker.platform}. */
    public static FilePicker platform = new FilePicker();

    public FilePicker() {
    }

    static FileTypeFilter filter(FileType type, List<?> extensions) {
        if (type == FileType.image) {
            return new FileTypeFilter("Images").mimeTypes("image/*");
        }
        if (type == FileType.video) {
            return new FileTypeFilter("Videos").mimeTypes("video/*");
        }
        if (type == FileType.audio) {
            return new FileTypeFilter("Audio").mimeTypes("audio/*");
        }
        if (type == FileType.media) {
            return new FileTypeFilter("Media").mimeTypes("image/*", "video/*");
        }
        if (type == FileType.custom && extensions != null && !extensions.isEmpty()) {
            String[] ext = new String[extensions.size()];
            for (int i = 0; i < ext.length; i++) {
                String e = String.valueOf(extensions.get(i));
                ext[i] = e.startsWith(".") ? e.substring(1) : e;
            }
            return new FileTypeFilter("Files").extensions(ext);
        }
        return null;
    }

    static String nameOf(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash < 0 ? path : path.substring(slash + 1);
    }

    static PlatformFile file(String path, boolean withData) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        Uint8List bytes = null;
        long size = 0;
        try {
            size = fs.getLength(path);
            if (withData) {
                InputStream in = fs.openInputStream(path);
                try {
                    byte[] b = Util.readInputStream(in);
                    bytes = Uint8List.fromBytes(b);
                    size = b.length;
                } finally {
                    Util.cleanup(in);
                }
            }
        } catch (java.io.IOException e) {
            com.codename1.io.Log.e(e);
        }
        return new PlatformFile(path, nameOf(path), size, bytes, null, path);
    }

    public Future<FilePickerResult> pickFiles(String dialogTitle, String initialDirectory, FileType type,
            List<?> allowedExtensions, Object onFileLoading, Boolean allowCompression, Long compressionQuality,
            Boolean allowMultiple, Boolean withData, Boolean withReadStream, Boolean lockParentWindow,
            Boolean readSequential) {
        if (type == FileType.custom && (allowedExtensions == null || allowedExtensions.isEmpty())) {
            return Future.error(new dart.core.ArgumentError(
                    "If you are using a custom extension filter, please use the FileType.custom instead."));
        }
        if (!Display.isInitialized()) {
            return Future.value(null);
        }
        OpenFileRequest req = new OpenFileRequest().multiSelect(Boolean.TRUE.equals(allowMultiple));
        if (dialogTitle != null) {
            req.title(dialogTitle);
        }
        if (initialDirectory != null) {
            req.initialDirectory(initialDirectory);
        }
        FileTypeFilter f = filter(type, allowedExtensions);
        if (f != null) {
            req.addFilter(f);
        }
        final boolean data = Boolean.TRUE.equals(withData);
        final Completer<FilePickerResult> done = new Completer<FilePickerResult>();
        Display.getInstance().showOpenDialog(req, new SuccessCallback<FileDialogResult>() {
            @Override
            public void onSucess(FileDialogResult r) {
                String[] paths = r == null || r.isCancelled() ? null : r.getPaths();
                if (paths == null || paths.length == 0) {
                    done.complete(null);
                    return;
                }
                DartList<PlatformFile> files = new DartList<PlatformFile>();
                for (String p : paths) {
                    files.add(file(p, data));
                }
                done.complete(new FilePickerResult(files));
            }
        });
        return done.future();
    }

    public Future<String> getDirectoryPath(String dialogTitle, Boolean lockParentWindow, String initialDirectory) {
        if (!Display.isInitialized()) {
            return Future.value(null);
        }
        OpenFileRequest req = new OpenFileRequest().directories(true);
        if (dialogTitle != null) {
            req.title(dialogTitle);
        }
        if (initialDirectory != null) {
            req.initialDirectory(initialDirectory);
        }
        final Completer<String> done = new Completer<String>();
        Display.getInstance().showOpenDialog(req, new SuccessCallback<FileDialogResult>() {
            @Override
            public void onSucess(FileDialogResult r) {
                done.complete(r == null || r.isCancelled() ? null : r.getPath());
            }
        });
        return done.future();
    }

    public Future<String> saveFile(String dialogTitle, String fileName, String initialDirectory, FileType type,
            List<?> allowedExtensions, final Uint8List bytes, Boolean lockParentWindow) {
        if (!Display.isInitialized()) {
            return Future.value(null);
        }
        SaveFileRequest req = new SaveFileRequest();
        if (dialogTitle != null) {
            req.title(dialogTitle);
        }
        if (fileName != null) {
            req.suggestedName(fileName);
        }
        if (initialDirectory != null) {
            req.initialDirectory(initialDirectory);
        }
        FileTypeFilter f = filter(type, allowedExtensions);
        if (f != null) {
            req.addFilter(f);
        }
        final Completer<String> done = new Completer<String>();
        Display.getInstance().showSaveDialog(req, new SuccessCallback<FileDialogResult>() {
            @Override
            public void onSucess(FileDialogResult r) {
                String path = r == null || r.isCancelled() ? null : r.getPath();
                if (path != null && bytes != null) {
                    OutputStream out = null;
                    try {
                        out = FileSystemStorage.getInstance().openOutputStream(path);
                        out.write(bytes.toBytes());
                    } catch (java.io.IOException e) {
                        Util.cleanup(out);
                        out = null;
                        done.completeError(e);
                        return;
                    } finally {
                        Util.cleanup(out);
                    }
                }
                done.complete(path);
            }
        });
        return done.future();
    }

    /** The picked files are the platform's own, not copies, so there is nothing to clear. */
    public Future<Boolean> clearTemporaryFiles() {
        return Future.value(Boolean.TRUE);
    }
}
