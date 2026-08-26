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
package com.codename1.documents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// One entry in the tree an app publishes through `DocumentProvider`: a folder or a file, as the
/// system file browser will show it.
///
/// A node is identified by an `id` that is stable for the lifetime of the item. The platform
/// remembers ids -- a favourite in the Files app, a recent document, a running download -- so
/// reusing an id for different content, or renumbering ids on every publish, makes the browser
/// point at the wrong thing. Derive the id from your own record key rather than from list order.
///
/// A file node names its content in one of two ways, matching the two modes described on
/// `DocumentProvider`:
///
/// - `setPath` -- a path relative to `DocumentProvider.getSharedDirectory()`, for bytes the app
///   has already written into the shared container.
/// - `setRemoteId` -- an opaque key the app's HTTPS endpoint understands, for content fetched on
///   demand.
///
/// A node with neither is a placeholder: it is listed, and opening it fails. A node with both
/// prefers the local path, which is what makes a cached copy of a remote document open instantly.
///
/// ```java
/// DocumentNode root = DocumentNode.folder("root", "Invoices");
/// root.add(DocumentNode.file("inv-2031", "January.pdf")
///         .setContentType("application/pdf")
///         .setPath("invoices/january.pdf")
///         .setSize(len));
/// ```
public class DocumentNode {
    private final String id;
    private final boolean folder;
    private String name;
    private String contentType;
    private String path;
    private String remoteId;
    private long size = -1;
    private long lastModified = -1;
    private final List<DocumentNode> children = new ArrayList<DocumentNode>();

    /// Creates a node.
    ///
    /// #### Parameters
    ///
    /// - `id`: the stable identity of this item; must not be null or empty
    /// - `name`: the display name shown in the file browser
    /// - `folder`: true for a folder, false for a file
    public DocumentNode(String id, String name, boolean folder) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("A document node needs a non-empty id");
        }
        this.id = id;
        this.name = name;
        this.folder = folder;
    }

    /// Creates a folder node.
    ///
    /// #### Parameters
    ///
    /// - `id`: the stable identity of this folder
    /// - `name`: the display name
    ///
    /// #### Returns
    ///
    /// the new folder
    public static DocumentNode folder(String id, String name) {
        return new DocumentNode(id, name, true);
    }

    /// Creates a file node.
    ///
    /// #### Parameters
    ///
    /// - `id`: the stable identity of this file
    /// - `name`: the display name, normally including an extension
    ///
    /// #### Returns
    ///
    /// the new file
    public static DocumentNode file(String id, String name) {
        return new DocumentNode(id, name, false);
    }

    /// Returns the stable identity of this item.
    public String getId() {
        return id;
    }

    /// Returns true when this node is a folder.
    public boolean isFolder() {
        return folder;
    }

    /// Returns the display name shown in the file browser.
    public String getName() {
        return name;
    }

    /// Sets the display name shown in the file browser.
    ///
    /// #### Parameters
    ///
    /// - `name`: the display name
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public DocumentNode setName(String name) {
        this.name = name;
        return this;
    }

    /// Returns the MIME type of this item's content, or null when unknown.
    public String getContentType() {
        return contentType;
    }

    /// Sets the MIME type of this item's content. Worth setting: it is how the browser decides
    /// which apps can open the item and which preview to draw. Ignored for folders.
    ///
    /// #### Parameters
    ///
    /// - `contentType`: a MIME type such as `application/pdf`
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public DocumentNode setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /// Returns the path of this item's content relative to the shared directory, or null.
    public String getPath() {
        return path;
    }

    /// Points this item at bytes the app has written under
    /// `DocumentProvider.getSharedDirectory()`.
    ///
    /// #### Parameters
    ///
    /// - `path`: a relative path such as `invoices/january.pdf`; a leading separator is ignored
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public DocumentNode setPath(String path) {
        this.path = path;
        return this;
    }

    /// Returns the key this item's content is fetched by from the remote endpoint, or null.
    public String getRemoteId() {
        return remoteId;
    }

    /// Points this item at content fetched on demand from the endpoint given to
    /// `DocumentProvider.setRemoteEndpoint`.
    ///
    /// #### Parameters
    ///
    /// - `remoteId`: an opaque key the endpoint understands
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public DocumentNode setRemoteId(String remoteId) {
        this.remoteId = remoteId;
        return this;
    }

    /// Returns the size in bytes, or -1 when unknown.
    public long getSize() {
        return size;
    }

    /// Sets the size in bytes. The browser shows this before any content is fetched, so it is
    /// worth setting for remote items even though it costs a round trip to learn.
    ///
    /// #### Parameters
    ///
    /// - `size`: the size in bytes, or -1 when unknown
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public DocumentNode setSize(long size) {
        this.size = size;
        return this;
    }

    /// Returns the last-modified time in milliseconds since the epoch, or -1 when unknown.
    public long getLastModified() {
        return lastModified;
    }

    /// Sets the last-modified time.
    ///
    /// #### Parameters
    ///
    /// - `lastModified`: milliseconds since the epoch, or -1 when unknown
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public DocumentNode setLastModified(long lastModified) {
        this.lastModified = lastModified;
        return this;
    }


    /// Adds a child to this folder.
    ///
    /// #### Parameters
    ///
    /// - `child`: the child node
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public DocumentNode add(DocumentNode child) {
        if (child == null) {
            return this;
        }
        if (!folder) {
            throw new IllegalStateException("Cannot add a child to the file node " + id);
        }
        children.add(child);
        return this;
    }

    /// Returns the children of this folder, empty for a file.
    public List<DocumentNode> getChildren() {
        return Collections.unmodifiableList(children);
    }
}
