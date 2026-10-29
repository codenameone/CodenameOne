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
package com.codename1.androidcompat.runtime;

import android.content.Intent;
import android.net.Uri;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.filechooser.FileDialogResult;
import com.codename1.io.filechooser.FileTypeFilter;
import com.codename1.io.filechooser.OpenFileRequest;
import com.codename1.io.filechooser.SaveFileRequest;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// The Storage Access Framework's document intents, `ACTION_OPEN_DOCUMENT`,
/// `ACTION_GET_CONTENT` for a non-media type and `ACTION_CREATE_DOCUMENT`,
/// answered by the platform's own Open and Save dialogs
/// (`Display#showOpenDialog`, `Display#showSaveDialog`).
///
/// Opening answers the picked file as a `file:` URI the `ContentResolver`
/// reads; several when `EXTRA_ALLOW_MULTIPLE` is set, in the result's
/// `ClipData` as Android puts them.
///
/// Creating cannot work the way Android does. Android hands the application
/// a writable document the user has already placed; on iOS the document
/// picker can only *export* a file that exists. So `ACTION_CREATE_DOCUMENT`
/// answers at once with a staging file of the requested name, and when the
/// application closes the stream it opened on that URI the Save dialog is
/// shown to put the written file where the user chooses. The visible
/// difference is the order (the file is written, then placed) and that a
/// cancelled Save is not reported back: the application has already seen
/// `RESULT_OK`, and Android has no result to revoke it with. The staged
/// file is removed once the dialog closes either way; one never closed is
/// removed when the next run first creates a document.
public final class DocumentIntents {

    /// Staging path to the export it is waiting for: name and MIME type.
    private static final Map<String, String[]> PENDING_EXPORTS = new HashMap<String, String[]>();
    private static int stagingCounter;

    private DocumentIntents() {
    }

    /// Whether `intent` is a document intent this class answers.
    public static boolean handles(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_OPEN_DOCUMENT.equals(action) || Intent.ACTION_CREATE_DOCUMENT.equals(action)) {
            return true;
        }
        // GET_CONTENT for images and videos goes to the gallery (see
        // ComponentActivity.pickContent); anything else is a document.
        return Intent.ACTION_GET_CONTENT.equals(action) && !isMediaType(intent.getType());
    }

    static boolean isMediaType(String type) {
        return type != null && (type.regionMatches(true, 0, "image/", 0, 6)
                || type.regionMatches(true, 0, "video/", 0, 6));
    }

    /// The result of a document intent: Android's result code and data.
    public interface Result {
        void onResult(int resultCode, Intent data);
    }

    /// Shows the dialog for `intent` and reports what the user picked, on
    /// the EDT. `resultCode` is `Activity.RESULT_OK` (-1) or
    /// `RESULT_CANCELED` (0).
    public static void start(final Intent intent, final Result result) {
        if (Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())) {
            create(intent, result);
            return;
        }
        OpenFileRequest request = new OpenFileRequest()
                .multiSelect(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false));
        FileTypeFilter filter = filterFor(intent);
        if (filter != null) {
            request.addFilter(filter);
        }
        Display.getInstance().showOpenDialog(request, new SuccessCallback<FileDialogResult>() {
            @Override
            public void onSucess(FileDialogResult value) {
                String[] paths = value == null || value.isCancelled() || value.getError() != null
                        ? null : value.getPaths();
                if (paths == null || paths.length == 0) {
                    result.onResult(0, null);
                    return;
                }
                Intent data = new Intent();
                data.setData(toUri(paths[0]));
                if (paths.length > 1 || intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)) {
                    android.content.ClipData clip = android.content.ClipData.newRawUri(null, toUri(paths[0]));
                    for (int i = 1; i < paths.length; i++) {
                        clip.addItem(new android.content.ClipData.Item(toUri(paths[i])));
                    }
                    data.setClipData(clip);
                }
                data.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                result.onResult(-1, data);
            }
        });
    }

    private static void create(Intent intent, final Result result) {
        String name = intent.getStringExtra(Intent.EXTRA_TITLE);
        if (name == null || name.length() == 0) {
            name = "Untitled";
        }
        String stagedName = stagingName(name);
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String root = fs.getAppHomePath() + STAGING_DIR;
        if (stagingCounter == 0) {
            // Left by an earlier run that never closed its stream: nothing
            // can export it any more.
            deleteTree(root);
        }
        String dir = root + (++stagingCounter) + "/";
        fs.mkdir(root);
        fs.mkdir(dir);
        String path = dir + stagedName;
        PENDING_EXPORTS.put(path, new String[] {name, intent.getType(), dir});
        final Intent data = new Intent();
        data.setData(toUri(path));
        data.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        // Android delivers a result after the launching call returns, never
        // inside it; code that sets state after launch() relies on that.
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                result.onResult(-1, data);
            }
        });
    }

    private static final String STAGING_DIR = "cn1-documents/";

    /// The staged file's name: the title with every character a URI or a
    /// file system treats specially replaced, so the `file:` URI handed to
    /// the application names exactly this file when it comes back. The Save
    /// dialog still suggests the title as given.
    static String stagingName(String name) {
        StringBuilder b = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '-' || c == '_';
            b.append(safe ? c : '_');
        }
        String s = b.toString();
        return s.startsWith(".") ? "_" + s : s;
    }

    private static void deleteTree(String dir) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        if (!fs.exists(dir)) {
            return;
        }
        try {
            String[] children = fs.listFiles(dir);
            if (children != null) {
                for (String c : children) {
                    String child = dir + c;
                    if (fs.isDirectory(child)) {
                        deleteTree(child.endsWith("/") ? child : child + "/");
                    } else {
                        fs.delete(child);
                    }
                }
            }
        } catch (IOException e) {
            // Best effort: a leftover staging file costs space, nothing else.
        }
        fs.delete(dir);
    }

    /// Wraps the stream the `ContentResolver` opened on `path`: for a
    /// document `ACTION_CREATE_DOCUMENT` staged, closing it shows the Save
    /// dialog that places the file. Any other stream comes back unchanged.
    public static OutputStream wrapForExport(final String path, OutputStream out) {
        if (!PENDING_EXPORTS.containsKey(path)) {
            return out;
        }
        return new java.io.FilterOutputStream(out) {
            private boolean closed;

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                if (closed) {
                    return;
                }
                closed = true;
                super.close();
                export(path);
            }
        };
    }

    private static void export(final String path) {
        final String[] pending = PENDING_EXPORTS.remove(path);
        if (pending == null) {
            return;
        }
        Runnable show = new Runnable() {
            @Override
            public void run() {
                SaveFileRequest request = new SaveFileRequest().suggestedName(pending[0]).sourceFile(path);
                if (pending[1] != null && pending[1].indexOf('*') < 0) {
                    request.addFilter(new FileTypeFilter(pending[1]).mimeTypes(pending[1]));
                }
                Display.getInstance().showSaveDialog(request, new SuccessCallback<FileDialogResult>() {
                    @Override
                    public void onSucess(FileDialogResult value) {
                        FileSystemStorage.getInstance().delete(path);
                        FileSystemStorage.getInstance().delete(pending[2]);
                    }
                });
            }
        };
        // Usually closed on the EDT, where the dialog is shown at once; a
        // stream written on a worker thread shows it on the EDT.
        if (Display.getInstance().isEdt()) {
            show.run();
        } else {
            Display.getInstance().callSerially(show);
        }
    }

    /// The filter for the intent's MIME type, or its `EXTRA_MIME_TYPES`;
    /// null for any type.
    private static FileTypeFilter filterFor(Intent intent) {
        String[] types = intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);
        if (types == null || types.length == 0) {
            String type = intent.getType();
            types = type == null ? null : new String[] {type};
        }
        if (types == null) {
            return null;
        }
        for (String t : types) {
            // A list that also admits anything filters nothing.
            if (t == null || "*/*".equals(t) || "*".equals(t)) {
                return null;
            }
        }
        return new FileTypeFilter("Documents").mimeTypes(types);
    }

    private static Uri toUri(String path) {
        if (path.startsWith("file:")) {
            return Uri.parse(path);
        }
        return Uri.parse("file://" + path);
    }
}
