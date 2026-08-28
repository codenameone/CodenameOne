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

    /// Resolves a published relative path, refusing anything that escapes the shared directory.
    ///
    /// A path is app-supplied data and may have come from a server, so ".." in it must not be
    /// able to reach the app's own databases or preferences and hand them to the system picker.
    /// Canonicalizing first is what makes the prefix test meaningful: without it "a/../../x"
    /// still starts with the directory it escapes.
    static File resolveLocal(Context ctx, String path) {
        if (path == null || path.length() == 0) {
            return null;
        }
        try {
            File base = filesDir(ctx).getCanonicalFile();
            File candidate = new File(base, path).getCanonicalFile();
            String basePath = base.getPath();
            if (!basePath.endsWith(File.separator)) {
                basePath = basePath + File.separator;
            }
            return candidate.getPath().startsWith(basePath) ? candidate : null;
        } catch (IOException err) {
            Log.w(TAG, "Could not resolve the published path " + path, err);
            return null;
        }
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
        public final List<String> childIds = new ArrayList<String>();
    }

    /// The published tree, flattened into id-keyed lookups.
    public static final class Index {
        public final Map<String, Node> nodes = new HashMap<String, Node>();
        public String rootId;

        /// Identifies the publication this index IS, so a reader that started work against it
        /// can tell whether the app has published or cleared since.
        ///
        /// The publisher writes it, and it differs for every publication. The file's modification
        /// time was the obvious source and is not good enough: it is only as fine as the
        /// filesystem's clock, so two publishes inside one tick were one revision and a download
        /// fetched against the first was accepted after the second. That timestamp is still the
        /// fallback for an index written before the field existed.
        ///
        /// Empty when there is no file, which is itself a distinct state: nothing is published.
        public String revision = "";
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
            // Out of the same bytes as the tree, so the stamp and what it stamps are always one
            // publication. The Apple reader has to stat around its read to get that guarantee --
            // it reads and stats separately, and the app replaces this file by rename while it
            // does -- and refuses to pair a tree with a revision that is not its own. Here the
            // whole file is read in one call, so there is nothing to pair wrongly.
            //
            // Except in the fallback below, which is the one stamp that does not come from the
            // bytes: a modification time read after them can belong to a later publication. It
            // stands for an index written without the field, which nothing this framework
            // publishes produces -- the serializer has always written it.
            index.revision = doc.optString("rev", "");
            if (index.revision.length() == 0) {
                index.revision = Long.toString(file.lastModified());
            }
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
        if (node.name.length() == 0) {
            // optString keeps an empty value rather than taking the fallback, and an empty
            // display name is a row the picker shows as nothing. The Apple readers normalize the
            // same case to "item", and the publisher compares sibling names after that
            // normalization, so answering anything else here would show one publication two ways.
            node.name = "item";
        }
        node.folder = json.optBoolean("folder", false);
        node.contentType = json.optString("contentType", null);
        node.path = json.optString("path", null);
        node.remoteId = json.optString("remoteId", null);
        node.size = json.optLong("size", -1);
        node.lastModified = json.optLong("lastModified", -1);
        if (index.nodes.put(node.id, node) != null) {
            // Two nodes sharing an id resolve to whichever was read last while both parents keep
            // listing it, so the picker shows two rows that open the same content. The publisher
            // refuses this, and a hand-written index that slips through is rejected rather than
            // half-served.
            throw new JSONException("Duplicate document id " + node.id);
        }
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
    /// Serializes replacements within this process. Two background flows calling publish() at
    /// once previously shared one fixed ".tmp" path: whichever renamed first had its file pulled
    /// out from under the other, which then deleted the freshly published target and failed to
    /// find its own temporary -- leaving no index.json at all.
    /// Also held by the bridge across whole operations -- publish, endpoint write and clear --
    /// so that a clear() cannot land between a publish's temporary file and its rename and be
    /// undone by it. `synchronized` is reentrant, so `writeAtomically` taking it again inside a
    /// caller that already holds it is not a deadlock.
    static final Object WRITE_LOCK = new Object();

    static void writeAtomically(File target, byte[] data) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        synchronized (WRITE_LOCK) {
            // A name nobody else can be holding, even if the lock is ever relaxed.
            File tmp = File.createTempFile("index", ".tmp", parent);
            // The temporary file is removed if anything about writing it fails -- opening the
            // stream, the write, or the close. The cleanup below only runs for a rename that
            // failed, so a full volume or a transient storage error left one of these behind per
            // attempt, in the same directory the published tree lives in and with nothing that
            // ever reads or removes them.
            try {
                FileOutputStream out = new FileOutputStream(tmp);
                try {
                    out.write(data);
                } finally {
                    out.close();
                }
            } catch (IOException err) {
                if (!tmp.delete()) {
                    Log.w(TAG, "Could not delete the partial index " + tmp);
                }
                throw err;
            }
            if (tmp.renameTo(target)) {
                return;
            }
            // Refused. Some filesystems will not rename onto an existing name, which is what
            // this fallback is for -- but a rename also fails for reasons that will not go away,
            // a full volume or a read-only mount among them, and deleting the target first meant
            // the retry then failed with the old publication already destroyed. An app left with
            // no index at all is worse off than one whose publish did not happen.
            //
            // So the old file is moved aside rather than deleted, and put back when the retry
            // fails too.
            File aside = new File(parent, target.getName() + ".previous");
            boolean hadTarget = target.exists();
            if (hadTarget) {
                if (aside.exists() && !aside.delete()) {
                    Log.w(TAG, "Could not delete " + aside);
                }
                if (!target.renameTo(aside)) {
                    if (!tmp.delete()) {
                        Log.w(TAG, "Could not delete " + tmp);
                    }
                    throw new IOException("Could not move " + target + " aside to replace it");
                }
            }
            if (tmp.renameTo(target)) {
                if (aside.exists() && !aside.delete()) {
                    Log.w(TAG, "Could not delete " + aside);
                }
                return;
            }
            if (hadTarget && aside.exists() && !aside.renameTo(target)) {
                Log.e(TAG, "Could not restore " + target + " from " + aside);
            }
            if (!tmp.delete()) {
                Log.w(TAG, "Could not delete " + tmp);
            }
            throw new IOException("Could not rename " + tmp + " to " + target
                    + "; the previous publication was left in place");
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

    /// True when this entry is a symbolic link rather than the thing it names.
    ///
    /// Android has no `isSymbolicLink` on this API level, so the test is the canonical path
    /// against the absolute one: they differ exactly when some component of the path is a link.
    /// The comparison is done on the parent plus the name rather than on the file itself, because
    /// the app's own storage root is reached through links on Android ("/data/user/0" names
    /// "/data/data"), and comparing the whole path would then call every file a link.
    ///
    /// An IO failure answers true: refusing to descend leaves the link in place to be deleted as
    /// a file, which loses nothing, while guessing false is what the check exists to prevent.
    private static boolean isLink(File file) {
        try {
            File parent = file.getParentFile();
            String parentCanonical = parent == null
                    ? "" : parent.getCanonicalPath();
            String expected = parentCanonical + File.separator + file.getName();
            return !expected.equals(file.getCanonicalPath());
        } catch (IOException err) {
            Log.w(TAG, "Could not resolve " + file + "; not descending into it", err);
            return true;
        }
    }

    /// Depth-first delete. `File.delete` refuses a non-empty directory, so a plain delete of the
    /// root would leave every published document on disk -- which is what `clear()` exists to
    /// prevent on logout.
    static void deleteTree(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        // Descends only into a real directory. `File.isDirectory` and `listFiles` both follow
        // symbolic links, so a link inside the published tree pointing at, say, the app's
        // databases directory would have this recursion delete that directory's contents on
        // logout -- clear() is supposed to remove what the app published, not everything the link
        // can reach. Deleting the link itself removes the link, never its target, which is
        // exactly what is wanted here.
        if (file.isDirectory() && !isLink(file)) {
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
