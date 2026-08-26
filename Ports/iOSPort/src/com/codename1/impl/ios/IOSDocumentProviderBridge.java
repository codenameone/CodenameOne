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
package com.codename1.impl.ios;

import com.codename1.documents.spi.DocumentProviderBridge;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;

import java.io.IOException;
import java.io.OutputStream;

/// Apple `DocumentProviderBridge` backing the `com.codename1.documents` API with FileProvider.
///
/// Everything published is persisted into the shared App Group container (the group id comes from
/// the `CN1DocumentsAppGroup` Info.plist key injected by the build) where the generated
/// CN1Documents extension reads it while the app process is dead:
///
/// - the tree: `<container>/cn1documents/index.json`
/// - the endpoint and token: `<container>/cn1documents/endpoint.json`
/// - the bytes of any node carrying a path: `<container>/cn1documents/files/<path>`
///
/// Both JSON files are written write-then-rename so the extension never observes a partial file.
/// That is not a nicety here: the extension is a separate process the system may start at any
/// instant, including midway through a publish, and a half-written index is a file browser showing
/// half a tree.
///
/// All file IO goes through `FileSystemStorage` (which tolerates the container's plain absolute
/// paths): `java.io.File`'s mutating methods are unimplemented natives on the ParparVM runtime --
/// referencing them fails the native link.
///
/// Shared unchanged with the AppKit macOS port, which reaches the same natives.
///
/// This whole class is dead code unless the build linked the document provider natives (the
/// `CN1_USE_DOCUMENTS` define the builder flips when the app references `com.codename1.documents`);
/// without it every native answers unsupported and the public API no-ops.
final class IOSDocumentProviderBridge implements DocumentProviderBridge {
    /// The directory, relative to the container, that everything published lives under. Namespaced
    /// so the container stays shareable with the other features that use the same App Group.
    private static final String ROOT = "cn1documents";

    private final IOSNative nativeInstance;
    private final FileSystemStorage fs = FileSystemStorage.getInstance();
    private boolean warnedNoContainer;

    IOSDocumentProviderBridge(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    public boolean isDocumentProviderSupported() {
        return nativeInstance.documentProviderSupported();
    }

    public String getSharedDirectory() {
        String container = containerPath();
        if (container == null) {
            return null;
        }
        String files = container + "/" + ROOT + "/files";
        mkdirs(container, ROOT + "/files");
        return files;
    }

    public void publishIndex(String indexJson) {
        String container = containerPath();
        if (container == null || indexJson == null) {
            return;
        }
        try {
            mkdirs(container, ROOT);
            writeAtomically(container + "/" + ROOT, "index.json",
                    indexJson.getBytes("UTF-8"));
            // Registering on every publish rather than once at startup: the domain is what makes
            // the location exist at all, and an app that publishes before the first registration
            // completed would otherwise have written a tree nothing is listening for.
            nativeInstance.documentsRegisterDomain();
        } catch (IOException err) {
            Log.e(err);
        }
    }

    public void setRemoteEndpoint(String endpoint, String authToken) {
        String container = containerPath();
        if (container == null) {
            return;
        }
        try {
            mkdirs(container, ROOT);
            writeAtomically(container + "/" + ROOT, "endpoint.json",
                    endpointJson(endpoint, authToken).getBytes("UTF-8"));
        } catch (IOException err) {
            Log.e(err);
        }
    }

    public void signalChange() {
        if (containerPath() == null) {
            return;
        }
        nativeInstance.documentsSignalChange();
    }

    public void clear() {
        String container = containerPath();
        if (container == null) {
            return;
        }
        nativeInstance.documentsRemoveDomain();
        deleteTree(container + "/" + ROOT);
    }

    /// Hand-built rather than routed through a serializer: two optional strings do not justify
    /// pulling the JSON writer into the port, and the extension's decoder expects exactly these
    /// two keys.
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

    private String containerPath() {
        String container = nativeInstance.getDocumentsContainerPath();
        if (container == null || container.length() == 0) {
            if (!warnedNoContainer) {
                warnedNoContainer = true;
                Log.p("DocumentProvider: no App Group container is available; check that the "
                        + "build set ios.documentProvider.appGroup and that the app group in "
                        + "the CN1DocumentsAppGroup Info.plist key exists on the App ID");
            }
            return null;
        }
        if (container.endsWith("/")) {
            container = container.substring(0, container.length() - 1);
        }
        return container;
    }

    private void writeAtomically(String dir, String name, byte[] data) throws IOException {
        String target = dir + "/" + name;
        String tmp = target + ".tmp";
        write(tmp, data);
        if (fs.exists(target)) {
            fs.delete(target);
        }
        // a relative new name renames within the same directory
        fs.rename(tmp, name);
        if (!fs.exists(target)) {
            throw new IOException("Failed to rename " + tmp + " to " + target);
        }
    }

    private void write(String path, byte[] data) throws IOException {
        OutputStream os = fs.openOutputStream(path);
        try {
            os.write(data);
        } finally {
            os.close();
        }
    }

    private void mkdirs(String base, String relative) {
        StringBuilder current = new StringBuilder(base);
        int start = 0;
        while (start < relative.length()) {
            int slash = relative.indexOf('/', start);
            String segment = slash < 0 ? relative.substring(start)
                    : relative.substring(start, slash);
            if (segment.length() > 0) {
                current.append('/').append(segment);
                String path = current.toString();
                if (!fs.exists(path)) {
                    fs.mkdir(path);
                }
            }
            if (slash < 0) {
                break;
            }
            start = slash + 1;
        }
    }

    /// Depth-first delete. `FileSystemStorage.delete` refuses a non-empty directory, so a plain
    /// delete of the root would silently leave every published document in the container -- which
    /// is exactly what `clear()` exists to prevent on logout.
    private void deleteTree(String path) {
        if (!fs.exists(path)) {
            return;
        }
        if (fs.isDirectory(path)) {
            String[] children = null;
            try {
                children = fs.listFiles(path);
            } catch (IOException err) {
                Log.e(err);
            }
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    String child = children[i];
                    if (child.endsWith("/")) {
                        child = child.substring(0, child.length() - 1);
                    }
                    deleteTree(path + "/" + child);
                }
            }
        }
        fs.delete(path);
    }
}
