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

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Where the published document tree lives on Android, and how it is read back.
///
/// The layout deliberately matches the Apple one byte for byte -- `cn1documents/index.json`,
/// `cn1documents/endpoint.json`, `cn1documents/files/...` -- so the format the core serializes is
/// genuinely one format rather than two that happen to agree today.
///
/// Android has no app-group split: the provider runs in this app's own process, so this reads the
/// app's ordinary files directory rather than a shared container. It still re-reads on every
/// query, for the same reason the Apple side does: the app republishes while the provider is
/// alive, and a cached tree would keep serving the previous publish.
public final class CN1DocumentStore {
    private static final String TAG = "CN1Documents";

    private CN1DocumentStore() {
    }

    /// The root of everything this feature persists.
    public static File baseDir(Context ctx) {
        return new File(ctx.getFilesDir(), "cn1documents");
    }

    /// The directory the app writes payload bytes into; node paths are relative to it.
    public static File filesDir(Context ctx) {
        return new File(baseDir(ctx), "files");
    }

    static File indexFile(Context ctx) {
        return new File(baseDir(ctx), "index.json");
    }

    static File endpointFile(Context ctx) {
        return new File(baseDir(ctx), "endpoint.json");
    }

    /// One entry of the published tree.
    public static final class Node {
        public String id;
        public String parentId;
        public String name;
        public boolean folder;
        public String contentType;
        public String path;
        public String remoteId;
        public long size = -1;
        public long lastModified = -1;
        public boolean readOnly;
        public final List<String> childIds = new ArrayList<String>();
    }

    /// The published tree, flattened into id-keyed lookups.
    public static final class Index {
        public final Map<String, Node> nodes = new HashMap<String, Node>();
        public String rootId;
    }

    /// Reads the index the app last published, or null when it has published nothing yet -- which
    /// is an empty browser rather than an error.
    public static Index loadIndex(Context ctx) {
        File file = indexFile(ctx);
        if (!file.exists()) {
            return null;
        }
        try {
            JSONObject doc = new JSONObject(readText(file));
            JSONObject root = doc.optJSONObject("root");
            if (root == null) {
                return null;
            }
            Index index = new Index();
            Node rootNode = read(root, null, index);
            index.rootId = rootNode.id;
            return index;
        } catch (JSONException err) {
            return null;
        } catch (IOException err) {
            return null;
        }
    }

    private static Node read(JSONObject json, String parentId, Index index) throws JSONException {
        Node node = new Node();
        node.id = json.getString("id");
        node.parentId = parentId;
        node.name = json.optString("name", node.id);
        node.folder = json.optBoolean("folder", false);
        node.contentType = json.optString("contentType", null);
        node.path = json.optString("path", null);
        node.remoteId = json.optString("remoteId", null);
        node.size = json.optLong("size", -1);
        node.lastModified = json.optLong("lastModified", -1);
        node.readOnly = json.optBoolean("readOnly", false);
        index.nodes.put(node.id, node);
        JSONArray children = json.optJSONArray("children");
        if (children != null) {
            for (int i = 0; i < children.length(); i++) {
                JSONObject child = children.optJSONObject(i);
                if (child != null) {
                    node.childIds.add(read(child, node.id, index).id);
                }
            }
        }
        return node;
    }

    /// The endpoint and token the app configured, as a two-element array of
    /// {endpoint, authToken}; either element may be null.
    public static String[] loadEndpoint(Context ctx) {
        File file = endpointFile(ctx);
        if (!file.exists()) {
            return new String[] {null, null};
        }
        try {
            JSONObject json = new JSONObject(readText(file));
            return new String[] {
                json.has("endpoint") ? json.optString("endpoint", null) : null,
                json.has("authToken") ? json.optString("authToken", null) : null
            };
        } catch (JSONException err) {
            return new String[] {null, null};
        } catch (IOException err) {
            return new String[] {null, null};
        }
    }

    /// Replaces a file's contents write-then-rename, so a reader never observes a partial file.
    static void writeAtomically(File target, byte[] data) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        File tmp = new File(target.getPath() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write(data);
        } finally {
            out.close();
        }
        if (target.exists() && !target.delete()) {
            throw new IOException("Could not replace " + target);
        }
        if (!tmp.renameTo(target)) {
            throw new IOException("Could not rename " + tmp + " to " + target);
        }
    }

    static String readText(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /// Depth-first delete. `File.delete` refuses a non-empty directory, so a plain delete of the
    /// root would leave every published document on disk -- which is what `clear()` exists to
    /// prevent on logout.
    static void deleteTree(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    deleteTree(children[i]);
                }
            }
        }
        if (!file.delete()) {
            // Said out loud rather than swallowed: this runs from clear(), whose whole purpose is
            // that nothing published survives a logout. A silent failure here leaves the previous
            // user's documents on disk and readable by the next one.
            Log.w(TAG, "Could not delete " + file);
        }
    }
}
