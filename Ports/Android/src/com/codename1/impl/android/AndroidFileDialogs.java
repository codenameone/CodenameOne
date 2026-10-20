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
                // The portable initialDirectory(path) is ignored: SAF starts from a
                // document URI and a file path has no general mapping to one. The
                // FileDialogHints.ANDROID_INITIAL_URI hint (API 26+) is the Android
                // equivalent, and a hint is not what this feature reports.
                return false;
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
                final int grantFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | (data.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Copying can be slow for cloud documents, keep it off the UI thread
                new Thread(new Runnable() {
                    public void run() {
                        FileDialogResult result;
                        // Every cache directory this import creates; a failed
                        // import returns none of them, so they are removed.
                        List<File> staged = new ArrayList<File>();
                        try {
                            // Checked before anything is copied: a selection with
                            // any file the filters reject fails whole, rather than
                            // handing back a quietly incomplete subset.
                            boolean rejected = false;
                            if (!request.isDirectories()) {
                                for (Uri u : uris) {
                                    if (!acceptedByFilters(request.getFilters(), resolver, u)) {
                                        rejected = true;
                                        break;
                                    }
                                }
                            }
                            List<String> paths = new ArrayList<String>();
                            if (!rejected) {
                                for (Uri u : uris) {
                                    // A tree URI is not a path FileSystemStorage can
                                    // list; a folder is copied, as iOS does.
                                    paths.add(request.isDirectories() ? copyTreeToCache(resolver, u, staged)
                                            : copyToCache(resolver, u, staged));
                                }
                            }
                            if (rejected) {
                                result = FileDialogResult.cancelled(request);
                                result.setError(new IOException("A selected file does not match the dialog's filters"));
                            } else {
                                result = new FileDialogResult(request, paths.toArray(new String[paths.size()]), -1, null);
                                // Persisted only once the import succeeded: a grant
                                // for a selection the app never receives would
                                // still count against the persisted-grant quota.
                                if (persist) {
                                    for (Uri u : uris) {
                                        try {
                                            resolver.takePersistableUriPermission(u, grantFlags);
                                        } catch (SecurityException ignored) {
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            result = new FileDialogResult(request, null, -1, null);
                            result.setError(e);
                        } catch (SecurityException e) {
                            result = new FileDialogResult(request, null, -1, null);
                            result.setError(e);
                        }
                        if (result.getError() != null) {
                            for (File f : staged) {
                                deleteRecursive(f);
                            }
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
        String type = mimes.length == 1 ? mimes[0] : commonMime(mimes);
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

    private static void start(final FileDialogRequest<?> request, final SuccessCallback<FileDialogResult> callback,
                              final Intent intent, final IntentResultListener listener) {
        if (!Display.getInstance().isEdt()) {
            // The busy check below and the listener registration inside
            // startActivityForResult must not interleave with another launch;
            // running every launch on the EDT serializes them.
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    start(request, callback, intent, listener);
                }
            });
            return;
        }
        if (AndroidNativeUtil.getActivity() == null) {
            FileDialogResult r = FileDialogResult.cancelled(request);
            r.setError(new IOException("No activity to show the file dialog"));
            deliver(callback, r);
            return;
        }
        android.app.Activity activity = AndroidNativeUtil.getActivity();
        if (activity instanceof CodenameOneActivity && ((CodenameOneActivity) activity).isWaitingForResult()) {
            // The activity has ONE result channel and setIntentResultListener
            // silently refuses a new listener while another flow (camera,
            // gallery, VPN consent) is outstanding; launching anyway would send
            // the picker's result to that flow and never answer this dialog.
            FileDialogResult r = FileDialogResult.cancelled(request);
            r.setError(new IOException("Another activity result is outstanding"));
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

    /*
     * One MIME type standing for several, for ACTION_CREATE_DOCUMENT which takes
     * a single type: "image/png" and "image/jpeg" become "image/*", so the picker
     * still offers image providers. Types of different families have no common
     * form and answer null, which the caller resolves from the name or falls
     * back to application/octet-stream.
     */
    private static String commonMime(String[] mimes) {
        String family = null;
        for (String m : mimes) {
            int slash = m.indexOf('/');
            if (slash <= 0) {
                return null;
            }
            String f = m.substring(0, slash);
            if (family == null) {
                family = f;
            } else if (!family.equals(f)) {
                return null;
            }
        }
        return family == null || "*".equals(family) ? null : family + "/*";
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

    /*
     * The picker filters by MIME type only. An extension the platform cannot map
     * widens the request to every type (see mimeTypes), so the selection is
     * checked here: a file passes when some filter accepts its name or its type,
     * or a filter accepts everything.
     */
    private static boolean acceptedByFilters(FileTypeFilter[] filters, ContentResolver resolver, Uri uri) {
        if (filters.length == 0) {
            return true;
        }
        String name = displayName(resolver, uri);
        String type = null;
        try {
            type = resolver.getType(uri);
        } catch (RuntimeException ignored) {
        }
        for (FileTypeFilter f : filters) {
            String[] ext = f.getExtensions();
            String[] mimes = f.getMimeTypes();
            if (ext.length == 0 && mimes.length == 0) {
                return true;
            }
            for (String e : ext) {
                if ("*".equals(e)) {
                    return true;
                }
            }
            if (ext.length > 0 && f.acceptsName(name)) {
                return true;
            }
            if (type != null && mimeMatches(mimes, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mimeMatches(String[] mimes, String type) {
        for (String m : mimes) {
            if (m.equalsIgnoreCase(type) || "*/*".equals(m)) {
                return true;
            }
            if (m.endsWith("/*") && type.regionMatches(true, 0, m, 0, m.length() - 1)) {
                return true;
            }
        }
        return false;
    }

    private static String safeName(String name, String fallback) {
        if (name == null || name.length() == 0) {
            return fallback;
        }
        // "." and ".." would resolve to the staging folder or its parent.
        if (".".equals(name) || "..".equals(name)) {
            return fallback;
        }
        return name.replace('/', '_').replace('\\', '_');
    }

    /** A directory per pick keeps the display name intact without clobbering an earlier copy. */
    private static File pickDirectory(Uri uri) throws IOException {
        File dir = new File(AndroidNativeUtil.getContext().getCacheDir(),
                "filedialog" + File.separator + System.currentTimeMillis() + "_" + Integer.toHexString(uri.hashCode()));
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        return dir;
    }

    private static String copyTreeToCache(ContentResolver resolver, Uri tree, List<File> staged) throws IOException {
        String rootId = android.provider.DocumentsContract.getTreeDocumentId(tree);
        Uri rootDoc = android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, rootId);
        File dir = pickDirectory(tree);
        staged.add(dir);
        File dest = new File(dir, safeName(displayName(resolver, rootDoc), "folder"));
        copyTreeChildren(resolver, tree, rootId, dest);
        return "file://" + dest.getAbsolutePath();
    }

    private static void copyTreeChildren(ContentResolver resolver, Uri tree, String parentId, File dest)
            throws IOException {
        if (!dest.isDirectory() && !dest.mkdirs()) {
            throw new IOException("Cannot create " + dest);
        }
        Uri children = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        List<String[]> entries = new ArrayList<String[]>();
        Cursor cursor = resolver.query(children, new String[]{
                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
        if (cursor == null) {
            throw new IOException("Cannot list " + tree);
        }
        try {
            while (cursor.moveToNext()) {
                entries.add(new String[]{cursor.getString(0), cursor.getString(1), cursor.getString(2)});
            }
        } finally {
            cursor.close();
        }
        for (String[] e : entries) {
            File child = uniqueChild(dest, safeName(e[1], "document"));
            if (android.provider.DocumentsContract.Document.MIME_TYPE_DIR.equals(e[2])) {
                copyTreeChildren(resolver, tree, e[0], child);
            } else {
                copyStream(resolver, android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, e[0]), child);
            }
        }
    }

    /*
     * A provider (a cloud drive, typically) may list two siblings under one
     * display name. Each gets its own local name, "name (2).ext" and so on,
     * rather than the second overwriting the first or merging into it.
     */
    private static File uniqueChild(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) {
            return f;
        }
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int n = 2; ; n++) {
            f = new File(dir, base + " (" + n + ")" + ext);
            if (!f.exists()) {
                return f;
            }
        }
    }

    private static String copyToCache(ContentResolver resolver, Uri uri, List<File> staged) throws IOException {
        File dir = pickDirectory(uri);
        staged.add(dir);
        File dest = new File(dir, safeName(displayName(resolver, uri), "document"));
        copyStream(resolver, uri, dest);
        return "file://" + dest.getAbsolutePath();
    }

    private static void deleteRecursive(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteRecursive(c);
            }
        }
        if (!f.delete()) {
            f.deleteOnExit();
        }
    }

    private static void copyStream(ContentResolver resolver, Uri uri, File dest) throws IOException {
        InputStream in = resolver.openInputStream(uri);
        if (in == null) {
            throw new IOException("Cannot open " + uri);
        }
        try {
            OutputStream out = new FileOutputStream(dest);
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > -1) {
                    out.write(buffer, 0, read);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
