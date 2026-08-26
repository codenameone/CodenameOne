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
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import com.codename1.documents.spi.DocumentProviderBridge;
import com.codename1.impl.android.AndroidNativeUtil;

import java.io.File;
import java.io.IOException;

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

    @Override
    public void publishIndex(String indexJson) {
        Context ctx = context();
        if (ctx == null || indexJson == null) {
            return;
        }
        try {
            CN1DocumentStore.writeAtomically(CN1DocumentStore.indexFile(ctx),
                    indexJson.getBytes("UTF-8"));
        } catch (IOException err) {
            Log.e(TAG, "Could not publish the document index", err);
        }
    }

    @Override
    public void setRemoteEndpoint(String endpoint, String authToken) {
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        try {
            CN1DocumentStore.writeAtomically(CN1DocumentStore.endpointFile(ctx),
                    endpointJson(endpoint, authToken).getBytes("UTF-8"));
        } catch (IOException err) {
            Log.e(TAG, "Could not store the document endpoint", err);
        }
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
        CN1DocumentStore.Index index = CN1DocumentStore.loadIndex(ctx);
        if (index == null) {
            return;
        }
        for (CN1DocumentStore.Node node : index.nodes.values()) {
            if (node.folder) {
                notify(resolver, DocumentsContract.buildChildDocumentsUri(authority, node.id));
            }
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
        CN1DocumentStore.deleteTree(CN1DocumentStore.baseDir(ctx));
        signalChange();
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
