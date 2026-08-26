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
package com.codename1.impl.android.documents;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/// Exposes the tree the app published through `com.codename1.documents.DocumentProvider` as a
/// source in the Android storage picker and the Files app.
///
/// Declared in the manifest by the builder when the app references `com.codename1.documents`,
/// with `android:permission="android.permission.MANAGE_DOCUMENTS"` -- which gates the *caller*,
/// not this app: only the system document UI holds that permission, so nothing else can bind.
///
/// Unlike the Apple side there is no second process and no app group: this runs inside the app,
/// reading the same `cn1documents` layout the app writes. The tree is still re-read per query
/// rather than cached, because the app republishes underneath a live provider.
public class CN1DocumentsProvider extends DocumentsProvider {
    private static final String TAG = "CN1Documents";

    private static final String[] DEFAULT_ROOT_PROJECTION = {
        DocumentsContract.Root.COLUMN_ROOT_ID,
        DocumentsContract.Root.COLUMN_DOCUMENT_ID,
        DocumentsContract.Root.COLUMN_TITLE,
        DocumentsContract.Root.COLUMN_FLAGS,
        DocumentsContract.Root.COLUMN_ICON,
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_FLAGS,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        MatrixCursor result = new MatrixCursor(
                projection == null ? DEFAULT_ROOT_PROJECTION : projection);
        CN1DocumentStore.Index index = CN1DocumentStore.loadIndex(getContext());
        if (index == null) {
            // No publish yet. An empty cursor is a source that does not appear in the picker,
            // which is better than one that appears and then cannot be opened.
            return result;
        }
        CN1DocumentStore.Node root = index.nodes.get(index.rootId);
        MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, index.rootId);
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, index.rootId);
        row.add(DocumentsContract.Root.COLUMN_TITLE,
                root == null || root.name == null ? applicationLabel() : root.name);
        row.add(DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD);
        row.add(DocumentsContract.Root.COLUMN_ICON, applicationIcon());
        // Without this the picker has no observer registered against the roots URI, so the
        // notifyChange() the bridge fires on publish or clear invalidates nothing and an open
        // DocumentsUI keeps showing the previous tree until it happens to requery.
        result.setNotificationUri(getContext().getContentResolver(),
                DocumentsContract.buildRootsUri(authority()));
        return result;
    }

    /// The authority the builder gave this provider in the manifest.
    private String authority() {
        return getContext().getPackageName() + ".documents";
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(
                projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        CN1DocumentStore.Index index = CN1DocumentStore.loadIndex(getContext());
        CN1DocumentStore.Node node = index == null ? null : index.nodes.get(documentId);
        if (node == null) {
            throw new FileNotFoundException("No published document " + documentId);
        }
        addRow(result, node);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection,
            String sortOrder) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(
                projection == null ? DEFAULT_DOCUMENT_PROJECTION : projection);
        CN1DocumentStore.Index index = CN1DocumentStore.loadIndex(getContext());
        CN1DocumentStore.Node parent = index == null ? null : index.nodes.get(parentDocumentId);
        if (parent == null) {
            throw new FileNotFoundException("No published document " + parentDocumentId);
        }
        for (int i = 0; i < parent.childIds.size(); i++) {
            CN1DocumentStore.Node child = index.nodes.get(parent.childIds.get(i));
            if (child != null) {
                addRow(result, child);
            }
        }
        // Same reason as queryRoots: an open folder has to be observing something for the
        // publish-time notify to reach it.
        result.setNotificationUri(getContext().getContentResolver(),
                DocumentsContract.buildChildDocumentsUri(authority(), parentDocumentId));
        return result;
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        CN1DocumentStore.Index index = CN1DocumentStore.loadIndex(getContext());
        if (index == null) {
            return false;
        }
        // Walks the whole ancestor chain rather than checking the immediate parent: the contract
        // is "is a descendant", and answering only for direct children breaks drag-and-drop and
        // tree-scoped permissions for anything nested.
        CN1DocumentStore.Node node = index.nodes.get(documentId);
        while (node != null && node.parentId != null) {
            if (node.parentId.equals(parentDocumentId)) {
                return true;
            }
            node = index.nodes.get(node.parentId);
        }
        return false;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode,
            CancellationSignal signal) throws FileNotFoundException {
        if (mode != null && mode.indexOf('w') >= 0) {
            // The published tree is a view of content the app owns; the app is the only writer.
            throw new FileNotFoundException(
                    "This location is published by the app and cannot be written to");
        }
        CN1DocumentStore.Index index = CN1DocumentStore.loadIndex(getContext());
        CN1DocumentStore.Node node = index == null ? null : index.nodes.get(documentId);
        if (node == null || node.folder) {
            throw new FileNotFoundException("No published document " + documentId);
        }
        // A local copy always wins, which is what makes a cached remote document open without a
        // round trip.
        if (node.path != null && node.path.length() > 0) {
            File local = CN1DocumentStore.resolveLocal(getContext(), node.path);
            if (local != null && local.exists()) {
                return ParcelFileDescriptor.open(local, ParcelFileDescriptor.MODE_READ_ONLY);
            }
        }
        if (node.remoteId == null || node.remoteId.length() == 0) {
            throw new FileNotFoundException("Published document " + documentId
                    + " names neither a local path nor a remote id");
        }
        File cached = fetch(node, signal);
        return ParcelFileDescriptor.open(cached, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    /// Downloads a remote item into the cache directory.
    ///
    /// Synchronous on purpose: `openDocument` runs on a binder thread and must hand back a
    /// descriptor to already-present bytes, so there is nothing useful to do but wait. The
    /// cancellation signal is honoured between chunks, which is what keeps a large download from
    /// outliving the picker the user just dismissed.
    private File fetch(CN1DocumentStore.Node node, CancellationSignal signal)
            throws FileNotFoundException {
        String[] endpoint = CN1DocumentStore.loadEndpoint(getContext());
        if (endpoint[0] == null || endpoint[0].length() == 0) {
            throw new FileNotFoundException("Document " + node.id + " is stored remotely but no "
                    + "endpoint was configured; call DocumentProvider.setRemoteEndpoint");
        }
        HttpURLConnection connection = null;
        try {
            String base = endpoint[0];
            String url = (base.endsWith("/") ? base + "fetch" : base + "/fetch")
                    + "?id=" + URLEncoder.encode(node.remoteId, "UTF-8");
            connection = (HttpURLConnection) new URL(url).openConnection();
            if (endpoint[1] != null && endpoint[1].length() > 0) {
                connection.setRequestProperty("Authorization", "Bearer " + endpoint[1]);
            }
            if (connection.getResponseCode() != 200) {
                throw new IOException("The document endpoint answered "
                        + connection.getResponseCode());
            }
            File cacheDir = new File(getContext().getCacheDir(), "cn1documents");
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                throw new IOException("Could not create " + cacheDir);
            }
            File target = new File(cacheDir, cacheName(node.id));
            // Downloaded into a file nobody else can be holding, then moved into place. Two
            // clients opening the same document arrive here on separate binder threads; writing
            // straight to the shared path let one truncate the bytes the other had already
            // handed back a descriptor for, which the reader saw as a corrupt document.
            //
            // The rename is what makes it safe rather than merely rarer: a descriptor already
            // opened on the old file keeps that inode, and any open after the rename sees a file
            // that was complete before it was ever linked to this name.
            File partial = File.createTempFile("fetch", ".part", cacheDir);
            InputStream in = connection.getInputStream();
            try {
                FileOutputStream out = new FileOutputStream(partial);
                try {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        if (signal != null) {
                            signal.throwIfCanceled();
                        }
                        out.write(buffer, 0, len);
                    }
                } finally {
                    out.close();
                }
            } catch (Throwable t) {
                if (!partial.delete()) {
                    Log.w(TAG, "Could not delete the partial download " + partial);
                }
                throw t instanceof IOException ? (IOException) t : new IOException(t);
            } finally {
                in.close();
            }
            if (!partial.renameTo(target)) {
                // A rename onto an existing name fails on some filesystems; the existing file is
                // a complete copy of the same content, so serve the partial's own path instead of
                // failing the open.
                return partial;
            }
            return target;
        } catch (IOException err) {
            Log.e(TAG, "Could not fetch document " + node.id, err);
            throw new FileNotFoundException("Could not fetch document " + node.id + ": "
                    + err.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /// The cache file name for a node id.
    ///
    /// Node ids are the app's own record keys and may contain anything, including separators, so
    /// this has to collapse to a single path component. Replacing the awkward characters is not
    /// enough on its own -- "a/b" and "a_b" collapse to the same name, and two different
    /// documents would then share one cache file. The id's hash is appended so distinct ids stay
    /// distinct while the readable part is kept for anyone looking at the cache directory.
    private static String cacheName(String id) {
        StringBuilder sb = new StringBuilder(id.length() + 12);
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            sb.append(Character.isLetterOrDigit(c) || c == '.' || c == '-' ? c : '_');
        }
        sb.append('-').append(Integer.toHexString(id.hashCode()));
        return sb.toString();
    }

    private void addRow(MatrixCursor result, CN1DocumentStore.Node node) {
        MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, node.id);
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, node.name);
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType(node));
        row.add(DocumentsContract.Document.COLUMN_FLAGS, 0);
        if (node.size >= 0) {
            row.add(DocumentsContract.Document.COLUMN_SIZE, Long.valueOf(node.size));
        }
        if (node.lastModified >= 0) {
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    Long.valueOf(node.lastModified));
        }
    }

    private static String mimeType(CN1DocumentStore.Node node) {
        if (node.folder) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        }
        if (node.contentType != null && node.contentType.length() > 0) {
            return node.contentType;
        }
        // Falling back on the extension rather than straight to octet-stream: the picker filters
        // by MIME type, and octet-stream matches almost no filter, so the item would be listed
        // and then greyed out.
        String name = node.name == null ? "" : node.name;
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            String guessed = android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(name.substring(dot + 1).toLowerCase());
            if (guessed != null) {
                return guessed;
            }
        }
        return "application/octet-stream";
    }

    private String applicationLabel() {
        try {
            return getContext().getApplicationInfo()
                    .loadLabel(getContext().getPackageManager()).toString();
        } catch (Throwable t) {
            return "Documents";
        }
    }

    private int applicationIcon() {
        try {
            return getContext().getApplicationInfo().icon;
        } catch (Throwable t) {
            return 0;
        }
    }
}
