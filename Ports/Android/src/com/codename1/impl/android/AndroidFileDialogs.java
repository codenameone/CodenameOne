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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import com.codename1.io.filechooser.FileDialogFeatures;
import com.codename1.io.filechooser.FileDialogHints;
import com.codename1.io.filechooser.FileDialogRequest;
import com.codename1.io.filechooser.FileDialogResult;
import com.codename1.io.filechooser.FileTypeFilter;
import com.codename1.io.filechooser.OpenFileRequest;
import com.codename1.io.filechooser.SaveFileRequest;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Storage Access Framework implementation of the native file open/save dialogs.
 * Opened documents are copied into the app cache and reported as FileSystemStorage
 * paths; a directory pick reports the tree URI; a save writes straight to the
 * content URI the system picker created and reports that URI.
 */
final class AndroidFileDialogs {
    private static final int REQUEST_CODE = 2101;

    private AndroidFileDialogs() {
    }

    static boolean isFeatureSupported(int feature) {
        switch (feature) {
            case FileDialogFeatures.NATIVE:
            case FileDialogFeatures.MULTI_SELECT:
            case FileDialogFeatures.SAVE_NAME_EDITING:
                return true;
            case FileDialogFeatures.DIRECTORIES:
                return Build.VERSION.SDK_INT >= 21;
            case FileDialogFeatures.INITIAL_DIRECTORY:
                // Only through the FileDialogHints.ANDROID_INITIAL_URI hint; SAF has no file paths
                return Build.VERSION.SDK_INT >= 26;
            default:
                return false;
        }
    }

    static void showOpen(final OpenFileRequest request, final SuccessCallback<FileDialogResult> callback) {
        final Intent intent;
        if (request.isDirectories()) {
            if (Build.VERSION.SDK_INT < 21) {
                deliver(callback, FileDialogResult.cancelled(request));
                return;
            }
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            String[] mimes = mimeTypes(request.getFilters(), -1);
            if (mimes.length == 1) {
                intent.setType(mimes[0]);
            } else {
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
            }
            if (request.isMultiSelect()) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
        }
        applyInitialUri(request, intent);
        final boolean persist = request.getBooleanHint(FileDialogHints.ANDROID_PERSIST_PERMISSION, false);
        start(request, callback, intent, new IntentResultListener() {
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                if (resultCode != Activity.RESULT_OK || data == null) {
                    deliver(callback, FileDialogResult.cancelled(request));
                    return;
                }
                final List<Uri> uris = new ArrayList<Uri>();
                ClipData clip = data.getClipData();
                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri u = clip.getItemAt(i).getUri();
                        if (u != null) {
                            uris.add(u);
                        }
                    }
                }
                if (uris.isEmpty() && data.getData() != null) {
                    uris.add(data.getData());
                }
                if (uris.isEmpty()) {
                    deliver(callback, FileDialogResult.cancelled(request));
                    return;
                }
                final ContentResolver resolver = AndroidNativeUtil.getContext().getContentResolver();
                if (persist) {
                    int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | (data.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    for (Uri u : uris) {
                        try {
                            resolver.takePersistableUriPermission(u, flags);
                        } catch (SecurityException ignored) {
                        }
                    }
                }
                if (request.isDirectories()) {
                    String[] paths = new String[uris.size()];
                    for (int i = 0; i < paths.length; i++) {
                        paths[i] = uris.get(i).toString();
                    }
                    deliver(callback, new FileDialogResult(request, paths, -1, null));
                    return;
                }
                // Copying can be slow for cloud documents, keep it off the UI thread
                new Thread(new Runnable() {
                    public void run() {
                        FileDialogResult result;
                        try {
                            String[] paths = new String[uris.size()];
                            for (int i = 0; i < paths.length; i++) {
                                paths[i] = copyToCache(resolver, uris.get(i));
                            }
                            result = new FileDialogResult(request, paths, -1, null);
                        } catch (IOException e) {
                            result = new FileDialogResult(request, null, -1, null);
                            result.setError(e);
                        } catch (SecurityException e) {
                            result = new FileDialogResult(request, null, -1, null);
                            result.setError(e);
                        }
                        deliver(callback, result);
                    }
                }, "FileDialogCopy").start();
            }
        });
    }

    static void showSave(final SaveFileRequest request, final SuccessCallback<FileDialogResult> callback) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String name = request.getSuggestedName();
        String[] mimes = mimeTypes(request.getFilters(), request.getSelectedFilter());
        String type = mimes.length == 1 ? mimes[0] : null;
        if (type == null || type.indexOf('*') >= 0) {
            String fromName = name == null ? null : mimeForExtension(extensionOf(name));
            if (fromName != null) {
                type = fromName;
            } else if (type == null) {
                type = "application/octet-stream";
            }
        }
        intent.setType(type);
        if (name != null) {
            intent.putExtra(Intent.EXTRA_TITLE, name);
        }
        applyInitialUri(request, intent);
        final boolean persist = request.getBooleanHint(FileDialogHints.ANDROID_PERSIST_PERMISSION, false);
        start(request, callback, intent, new IntentResultListener() {
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                final Uri uri = data == null ? null : data.getData();
                if (resultCode != Activity.RESULT_OK || uri == null) {
                    deliver(callback, FileDialogResult.cancelled(request));
                    return;
                }
                final ContentResolver resolver = AndroidNativeUtil.getContext().getContentResolver();
                if (persist) {
                    try {
                        resolver.takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                    }
                }
                final FileDialogResult result = new FileDialogResult(request, new String[]{uri.toString()}, -1, null);
                new Thread(new Runnable() {
                    public void run() {
                        OutputStream out = null;
                        try {
                            out = resolver.openOutputStream(uri);
                            if (out == null) {
                                throw new IOException("Cannot open " + uri);
                            }
                            request.writeContent(result, out);
                        } catch (IOException e) {
                            result.setError(e);
                        } catch (SecurityException e) {
                            result.setError(e);
                        } finally {
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (IOException e) {
                                    if (result.getError() == null) {
                                        result.setError(e);
                                    }
                                }
                            }
                        }
                        deliver(callback, result);
                    }
                }, "FileDialogSave").start();
            }
        });
    }

    private static void start(FileDialogRequest<?> request, SuccessCallback<FileDialogResult> callback,
                              Intent intent, IntentResultListener listener) {
        if (AndroidNativeUtil.getActivity() == null) {
            FileDialogResult r = FileDialogResult.cancelled(request);
            r.setError(new IOException("No activity to show the file dialog"));
            deliver(callback, r);
            return;
        }
        AndroidNativeUtil.startActivityForResult(intent, REQUEST_CODE, listener);
    }

    private static void applyInitialUri(FileDialogRequest<?> request, Intent intent) {
        String initial = request.getStringHint(FileDialogHints.ANDROID_INITIAL_URI);
        if (initial != null && initial.length() > 0 && Build.VERSION.SDK_INT >= 26) {
            intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(initial));
        }
    }

    private static void deliver(final SuccessCallback<FileDialogResult> callback, final FileDialogResult result) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                callback.onSucess(result);
            }
        });
    }

    /**
     * MIME types for the filters, or just the filter at {@code only} when that is a valid index.
     * Answers a single wildcard when any filter cannot be expressed as a MIME type, since
     * narrowing further would hide files the filter accepts.
     */
    private static String[] mimeTypes(FileTypeFilter[] filters, int only) {
        Set<String> out = new LinkedHashSet<String>();
        boolean unknown = false;
        for (int i = 0; i < filters.length; i++) {
            if (only >= 0 && only < filters.length && i != only) {
                continue;
            }
            FileTypeFilter f = filters[i];
            String[] m = f.getMimeTypes();
            if (m != null && m.length > 0) {
                for (String s : m) {
                    out.add(asciiLower(s));
                }
                continue;
            }
            String[] ext = f.getExtensions();
            if (ext == null || ext.length == 0) {
                unknown = true;
                continue;
            }
            for (String e : ext) {
                String mime = mimeForExtension(e);
                if (mime == null) {
                    unknown = true;
                } else {
                    out.add(mime);
                }
            }
        }
        if (unknown || out.isEmpty()) {
            return new String[]{"*/*"};
        }
        return out.toArray(new String[out.size()]);
    }

    private static String mimeForExtension(String ext) {
        if (ext == null) {
            return null;
        }
        if (ext.startsWith("*.")) {
            ext = ext.substring(2);
        } else if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        if (ext.length() == 0 || "*".equals(ext)) {
            return null;
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(asciiLower(ext));
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? null : name.substring(dot + 1);
    }

    // Locale independent fold: String.toLowerCase() breaks extensions on Turkish devices
    private static String asciiLower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + ('a' - 'A'));
            }
        }
        return new String(c);
    }

    private static String displayName(ContentResolver resolver, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    return cursor.getString(idx);
                }
            }
        } catch (RuntimeException ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static String copyToCache(ContentResolver resolver, Uri uri) throws IOException {
        String name = displayName(resolver, uri);
        if (name == null || name.length() == 0) {
            name = "document";
        }
        name = name.replace('/', '_').replace('\\', '_');
        // A directory per pick keeps the display name intact without clobbering an earlier copy
        File dir = new File(AndroidNativeUtil.getContext().getCacheDir(),
                "filedialog" + File.separator + System.currentTimeMillis() + "_" + Integer.toHexString(uri.hashCode()));
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        File dest = new File(dir, name);
        InputStream in = resolver.openInputStream(uri);
        if (in == null) {
            throw new IOException("Cannot open " + uri);
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(dest);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
            if (out != null) {
                out.close();
            }
        }
        return "file://" + dest.getAbsolutePath();
    }
}
