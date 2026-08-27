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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import com.codename1.documents.spi.DocumentProviderBridge;
import com.codename1.impl.android.AndroidNativeUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The Android implementation of the document provider SPI.
///
/// Published trees are persisted through `CN1DocumentStore`, where the manifest-declared
/// `CN1DocumentsProvider` reads them. There is no second process here and no app group -- the
/// provider runs in this app -- but the publish/read split is kept identical to the Apple side so
/// one publishing discipline works everywhere.
public class AndroidDocumentProviderBridge implements DocumentProviderBridge {
    private static final String TAG = "CN1Documents";

    @Override
    public boolean isDocumentProviderSupported() {
        return context() != null && authority() != null;
    }

    @Override
    public String getSharedDirectory() {
        Context ctx = context();
        if (ctx == null) {
            return null;
        }
        File dir = CN1DocumentStore.filesDir(ctx);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Could not create " + dir);
            return null;
        }
        return dir.getAbsolutePath();
    }

    /// Folder ids that existed before the last publish or clear.
    ///
    /// A folder cursor is registered against its own child-documents URI, and notifying the roots
    /// URI does not reach it. Notifying only the folders in the NEW index therefore leaves an open
    /// picker sitting on a folder the publish removed -- it keeps its rows until it happens to
    /// requery. These are remembered across the write so the notification can cover what left as
    /// well as what arrived.
    private final List<String> staleFolderIds = new ArrayList<String>();

    @Override
    public void publishIndex(String indexJson) {
        Context ctx = context();
        if (ctx == null || indexJson == null) {
            return;
        }
        // The whole operation holds the store's lock, not just the write. publish() and clear()
        // are called from application code on whatever thread it likes -- a logout clear() racing
        // a background publish() could otherwise delete the tree between this write's temporary
        // file and its rename, and the rename would then put the departing user's index back
        // after the clear had returned.
        synchronized (CN1DocumentStore.WRITE_LOCK) {
            CN1DocumentStore.Index previous = CN1DocumentStore.loadIndex(ctx);
            rememberFolders(previous);
            try {
                CN1DocumentStore.writeAtomically(CN1DocumentStore.indexFile(ctx),
                        indexJson.getBytes("UTF-8"));
            } catch (IOException err) {
                Log.e(TAG, "Could not publish the document index", err);
                return;
            }
            // A document dropped by this publish loses its grants HERE, not at logout. An app
            // that opened it through the picker can have persisted access to it, and clear()
            // only knows about what the last index held -- so a node removed by an ordinary
            // publish kept its grant, and a later account republishing that id handed the old
            // holder the new content with no picker in sight.
            revokeWithdrawn(ctx, previous, CN1DocumentStore.loadIndex(ctx));
        }
    }

    /// Records the folders of the index that is about to be replaced or deleted.
    private void rememberFolders(CN1DocumentStore.Index index) {
        if (index == null) {
            return;
        }
        synchronized (staleFolderIds) {
            for (CN1DocumentStore.Node node : index.nodes.values()) {
                if (node.folder && !staleFolderIds.contains(node.id)) {
                    staleFolderIds.add(node.id);
                }
            }
        }
    }

    @Override
    public void setRemoteEndpoint(String endpoint, String authToken) {
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        // Under the same lock as publish and clear: this file holds the bearer token, so a
        // clear() that interleaved with it would leave the token on disk after logout.
        synchronized (CN1DocumentStore.WRITE_LOCK) {
            try {
                CN1DocumentStore.writeAtomically(CN1DocumentStore.endpointFile(ctx),
                        endpointJson(endpoint, authToken).getBytes("UTF-8"));
            } catch (IOException err) {
                Log.e(TAG, "Could not store the document endpoint", err);
            }
        }
        // Told, not left to be noticed: a picker holding this location open re-queries on the
        // notification, and anything it fetches afterwards goes out under the new credential.
        // Outside the lock, as clear()'s notification is.
        signalChange();
    }

    @Override
    public void signalChange() {
        Context ctx = context();
        String authority = authority();
        if (ctx == null || authority == null) {
            return;
        }
        ContentResolver resolver = ctx.getContentResolver();
        // The roots URI is what the picker watches while the location is closed, so notifying
        // only a document would leave a closed-and-reopened picker showing the previous publish.
        notify(resolver, DocumentsContract.buildRootsUri(authority));
        // Every folder cursor registers against its own child-documents URI, so an already open
        // folder needs its own notification: the roots URI does not reach it.
        //
        // Deliberately not inside a catch(Throwable): the generic iteration below compiles to a
        // checkcast, and ParparVM does not throw for a failed cast -- a handler wrapping this
        // would be one the device can never run. Each notify guards itself instead.
        List<String> folders = new ArrayList<String>();
        synchronized (staleFolderIds) {
            folders.addAll(staleFolderIds);
            staleFolderIds.clear();
        }
        CN1DocumentStore.Index index = CN1DocumentStore.loadIndex(ctx);
        if (index != null) {
            for (CN1DocumentStore.Node node : index.nodes.values()) {
                if (node.folder && !folders.contains(node.id)) {
                    folders.add(node.id);
                }
            }
        }
        for (int i = 0; i < folders.size(); i++) {
            notify(resolver, DocumentsContract.buildChildDocumentsUri(authority, folders.get(i)));
        }
    }

    /// A notification failure is never worth taking down the publish that already succeeded.
    private static void notify(ContentResolver resolver, Uri uri) {
        try {
            resolver.notifyChange(uri, null);
        } catch (Throwable t) {
            Log.e(TAG, "Could not signal the document provider for " + uri, t);
        }
    }

    @Override
    public void clear() {
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        // Under the store's lock so a publish in flight cannot rename its freshly written index
        // back in behind the deletion. The notification is sent afterwards, outside the lock:
        // it mutates nothing and the ContentResolver round trip should not block a publish.
        synchronized (CN1DocumentStore.WRITE_LOCK) {
            // Remembered before the tree goes, or the folders that just disappeared could not be
            // named afterwards and an open picker would keep showing them.
            rememberFolders(CN1DocumentStore.loadIndex(ctx));
            revokeGrants(ctx);
            CN1DocumentStore.deleteTree(CN1DocumentStore.baseDir(ctx));
        }
        signalChange();
    }

    /// Withdraws the grants for documents this publish no longer carries.
    ///
    /// Reads the index back after the write rather than trusting the JSON it was handed: the
    /// comparison has to be between what a reader will see now and what one saw before, and a
    /// write that did not take should withdraw nothing.
    private void revokeWithdrawn(Context ctx, CN1DocumentStore.Index previous,
            CN1DocumentStore.Index current) {
        String authority = authority();
        if (authority == null || previous == null || current == null) {
            return;
        }
        for (CN1DocumentStore.Node node : previous.nodes.values()) {
            if (current.nodes.containsKey(node.id)) {
                continue;
            }
            revoke(ctx, DocumentsContract.buildDocumentUri(authority, node.id), node.id);
            if (node.folder && android.os.Build.VERSION.SDK_INT >= 21) {
                revoke(ctx, DocumentsContract.buildTreeDocumentUri(authority, node.id), node.id);
            }
        }
    }

    /// Withdraws the URI permissions this provider handed out for the documents being cleared.
    ///
    /// An app that opened a document through the picker can PERSIST its grant, and a grant
    /// outlives the tree: deleting the index takes the content away but leaves the permission
    /// behind. Node ids are the app's own record keys and an account switch reuses them -- the
    /// download path is built around that -- so the next login republishing "invoice-1" would
    /// hand the previous holder the new account's document through a URI it already has, with no
    /// picker in sight.
    ///
    /// Best effort by nature: a grant this process did not issue is not ours to revoke, and the
    /// call is documented to do nothing in that case rather than to fail. It runs before the
    /// deletion so the index can still say which documents there are.
    private void revokeGrants(Context ctx) {
        String authority = authority();
        CN1DocumentStore.Index index = CN1DocumentStore.loadIndex(ctx);
        if (authority == null || index == null) {
            return;
        }
        // The READ mode alone. revokeUriPermission takes the access modes and nothing else --
        // FLAG_GRANT_PERSISTABLE_URI_PERMISSION is not one of them, and passing it makes the call
        // reject the whole request, which the catch below would then have swallowed once per
        // node while every grant stayed live. Revoking the read mode takes its persisted grant
        // with it, which is the grant this is here for.
        for (CN1DocumentStore.Node node : index.nodes.values()) {
            revoke(ctx, DocumentsContract.buildDocumentUri(authority, node.id), node.id);
            // A folder can also have been picked whole, through ACTION_OPEN_DOCUMENT_TREE, and
            // that grant is on the /tree/ URI rather than the /document/ one -- so revoking
            // documents alone left the holder tree-scoped access to everything republished
            // under the same folder id. buildTreeDocumentUri arrived in API 21 and the port
            // still builds for 19, where no tree grant can exist to revoke.
            if (node.folder && android.os.Build.VERSION.SDK_INT >= 21) {
                revoke(ctx, DocumentsContract.buildTreeDocumentUri(authority, node.id), node.id);
            }
        }
    }

    /// One revocation, with its own failure. One document that cannot be revoked must not stop
    /// the rest, and must not stop the deletion that follows -- the content going away is the
    /// part that matters.
    private void revoke(Context ctx, android.net.Uri uri, String nodeId) {
        try {
            ctx.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable t) {
            Log.w(TAG, "Could not revoke access to " + nodeId, t);
        }
    }

    /// Hand-built rather than routed through a serializer: two optional strings do not justify
    /// pulling one in, and the provider's reader expects exactly these two keys.
    private static String endpointJson(String endpoint, String authToken) {
        StringBuilder sb = new StringBuilder("{");
        if (endpoint != null && endpoint.length() > 0) {
            sb.append("\"endpoint\":\"").append(escapeJson(endpoint)).append("\"");
        }
        if (authToken != null && authToken.length() > 0) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append("\"authToken\":\"").append(escapeJson(authToken)).append("\"");
        }
        return sb.append('}').toString();
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int pad = hex.length(); pad < 4; pad++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /// The authority the builder gave the provider in the manifest. Derived from the package
    /// rather than looked up, matching what AndroidGradleBuilder writes.
    private String authority() {
        Context ctx = context();
        return ctx == null ? null : ctx.getPackageName() + ".documents";
    }

    private static Context context() {
        return AndroidNativeUtil.getContext();
    }
}
