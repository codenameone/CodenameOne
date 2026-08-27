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
import android.net.Uri;
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

    /// Long enough for a slow mobile connection, short enough that a dead endpoint cannot pin a
    /// binder thread for the life of the process.
    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 30000;

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
        // Registered before the index is even read. A picker that queried this provider before
        // the app's first publish would otherwise hold an unobserved empty cursor, and the
        // notifyChange that first publish fires would have nothing to invalidate -- the source
        // would stay missing from a picker that is already open.
        result.setNotificationUri(getContext().getContentResolver(),
                DocumentsContract.buildRootsUri(authority()));
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
        //
        // Checked against the publication it was resolved from, after the descriptor exists. A
        // binder call can be scheduled out between reading the index and opening the file, and a
        // logout plus a new login can land in that gap: the path is a relative one the next
        // account may reuse, so the open would return the NEW account's bytes to a request the
        // old one made -- and a revoked grant does not reach into a call already running. The
        // descriptor is closed rather than returned when the publication moved under it.
        if (node.path != null && node.path.length() > 0) {
            File local = CN1DocumentStore.resolveLocal(getContext(), node.path);
            if (local != null && local.exists()) {
                ParcelFileDescriptor descriptor =
                        ParcelFileDescriptor.open(local, ParcelFileDescriptor.MODE_READ_ONLY);
                CN1DocumentStore.Index current = CN1DocumentStore.loadIndex(getContext());
                CN1DocumentStore.Node reread =
                        current == null ? null : current.nodes.get(documentId);
                if (current != null && current.revision.equals(index.revision)
                        && reread != null && node.path.equals(reread.path)) {
                    return descriptor;
                }
                try {
                    descriptor.close();
                } catch (IOException err) {
                    Log.w(TAG, "Could not close a withdrawn descriptor for " + documentId, err);
                }
                throw new FileNotFoundException("Published document " + documentId
                        + " was withdrawn while it was being opened");
            }
        }
        if (node.remoteId == null || node.remoteId.length() == 0) {
            throw new FileNotFoundException("Published document " + documentId
                    + " names neither a local path nor a remote id");
        }
        String[] credentials = CN1DocumentStore.loadEndpoint(getContext());
        // The download is given the credentials that were captured, not left to load them again.
        // Two reads can straddle an account switch: the check would then compare the pair it
        // captured against the pair that is current -- equal if the app switched away and back --
        // while the request had actually been sent with the other account's token. Handing the
        // same pair to both makes what was fetched and what is checked the same thing.
        File cached = fetch(node, credentials, signal);
        // The download runs outside the publication lock and takes as long as the network takes,
        // so the app may have cleared -- a logout -- or switched accounts meanwhile. Handing the
        // descriptor over regardless would give the picker the departed user's document, and
        // leave its bytes in a cache directory clear() does not walk.
        //
        // Node ids are the app's own record keys and a switch reuses them, so the check is the
        // remote id AND the credential the download was authorized with, which is the thing a
        // switch always changes.
        if (!stillPublished(documentId, node, index.revision, credentials)) {
            if (!cached.delete()) {
                Log.w(TAG, "Could not delete the withdrawn download " + cached);
            }
            throw new FileNotFoundException("Published document " + documentId
                    + " was withdrawn while it was being fetched");
        }
        // Unlinked whether or not the descriptor is created. The open itself can fail -- a
        // process out of file descriptors is the ordinary way -- and leaving the download behind
        // there put a whole document in the cache per attempt, which nothing reads and nothing
        // clears. On success the descriptor keeps the inode alive for as long as the caller
        // reads it, so the bytes survive while the name does not: that is what stops a download
        // outliving the request that fetched it, and what keeps a departed user's document out
        // of a directory clear() has no reason to walk.
        try {
            return ParcelFileDescriptor.open(cached, ParcelFileDescriptor.MODE_READ_ONLY);
        } finally {
            if (!cached.delete()) {
                Log.w(TAG, "Could not delete the served download " + cached);
            }
        }
    }

    /// Whether the document still names the same remote object, at the same version, under the
    /// same credential.
    ///
    /// Re-read from disk rather than taken from the copy the request started with: the point is
    /// to see what the app has done since.
    ///
    /// The declared size and date are compared as well as the id. A server-side revision usually
    /// keeps its key, so an app that republishes the node with a new size or date while the old
    /// bytes are still streaming would otherwise have those bytes handed to the picker as the new
    /// version. That is the same signal the Apple providers version remote content by, and the
    /// same bargain: an app that declares neither gets no per-item change detection, which
    /// DocumentNode documents.
    private boolean stillPublished(String documentId, CN1DocumentStore.Node requested,
            String revision, String[] credentials) {
        CN1DocumentStore.Index index = CN1DocumentStore.loadIndex(getContext());
        CN1DocumentStore.Node node = index == null ? null : index.nodes.get(documentId);
        if (node != null && requested.lastModified < 0 && !index.revision.equals(revision)) {
            // A node with no DATE has nothing per-item to compare, so it is bound to the
            // publication it was requested from -- the same fallback its content version uses on
            // the Apple side. A size is not a change signal: content can change to different
            // bytes of the same length, so an object revised under the same remote id and
            // republished while the old response was still streaming passed every check.
            //
            // A node that declares a date is deliberately not bound to the revision: that would
            // discard every download racing any publish, which for a drive of any size is the
            // expensive wrong default. DocumentNode documents the other half of the bargain.
            return false;
        }
        // openDocument only reaches this for a node whose remote id it already checked, but the
        // field is nullable and read here through a second object, so it is guarded rather than
        // assumed.
        String remoteId = requested.remoteId;
        if (node == null || remoteId == null || !remoteId.equals(node.remoteId)
                || node.size != requested.size
                || node.lastModified != requested.lastModified) {
            return false;
        }
        String[] current = CN1DocumentStore.loadEndpoint(getContext());
        return equalOrBothNull(credentials[0], current[0])
                && equalOrBothNull(credentials[1], current[1]);
    }

    private static boolean equalOrBothNull(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /// Downloads a remote item into the cache directory.
    ///
    /// Synchronous on purpose: `openDocument` runs on a binder thread and must hand back a
    /// descriptor to already-present bytes, so there is nothing useful to do but wait. The
    /// cancellation signal is honoured between chunks, which is what keeps a large download from
    /// outliving the picker the user just dismissed.
    private File fetch(CN1DocumentStore.Node node, String[] endpoint, CancellationSignal signal)
            throws FileNotFoundException {
        if (endpoint[0] == null || endpoint[0].length() == 0) {
            throw new FileNotFoundException("Document " + node.id + " is stored remotely but no "
                    + "endpoint was configured; call DocumentProvider.setRemoteEndpoint");
        }
        HttpURLConnection connection = null;
        try {
            // Built through Uri rather than by pasting strings: "fetch" belongs on the PATH and
            // the id has to join whatever query the endpoint already carries. Concatenation put
            // the suffix inside the query of an endpoint like
            // "https://api.example.com/documents?tenant=42" and then added a second "?", so the
            // request went somewhere the server does not serve and the caller's own parameter was
            // lost. appendPath and appendQueryParameter also do the escaping.
            Uri url = Uri.parse(endpoint[0]).buildUpon()
                    .appendPath("fetch")
                    .appendQueryParameter("id", node.remoteId)
                    .build();
            connection = (HttpURLConnection) new URL(url.toString()).openConnection();
            // Finite by necessity. This runs on a provider binder thread, and the default is no
            // timeout at all: an endpoint that accepts the connection and then stops answering
            // would block here forever, where the cancellation signal cannot reach it because
            // the thread is stuck in a socket read. Enough stalled opens and the whole document
            // location stops responding.
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
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
            // Downloaded into a file nobody else can be holding, then moved into place. Two
            // clients opening the same document arrive here on separate binder threads; writing
            // straight to the shared path let one truncate the bytes the other had already
            // handed back a descriptor for, which the reader saw as a corrupt document.
            //
            // The rename is what makes it safe rather than merely rarer: a descriptor already
            // opened on the old file keeps that inode, and any open after the rename sees a file
            // that was complete before it was ever linked to this name.
            // The stream first, then the file. getInputStream can fail after a 200 -- a peer
            // that disconnects once the headers are out -- and creating the file first left an
            // empty one behind on every such attempt, since the cleanup that removes it lives
            // inside the block below. Cache files nothing will ever read, accumulating per retry.
            InputStream in = connection.getInputStream();
            File partial = File.createTempFile("fetch", ".part", cacheDir);
            try {
                FileOutputStream out = new FileOutputStream(partial);
                try {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        if (signal != null) {
                            // Throws out of the read loop; the finally below closes the stream
                            // and the outer finally disconnects, so a cancelled open does not
                            // leave a socket draining in the background.
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
            return partial;
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
                    // Locale.ROOT, because this folds an ASCII extension for a lookup table of
                    // ASCII keys. A Turkish device lowercases "GIF" to a dotless i and
                    // MimeTypeMap then misses it, so every .GIF and .TIFF in the tree is served as
                    // octet-stream -- which the picker filters out.
                    .getMimeTypeFromExtension(
                            name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT));
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
