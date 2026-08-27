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
import FileProvider
import Foundation
import UniformTypeIdentifiers

/// Presents one published node to the file browser.
///
/// The type is described twice on purpose. `typeIdentifier` is the original string UTI and is the
/// only one the pre-iOS-16 provider can use; `contentType` is the `UTType` the replicated provider
/// wants. Both are answered from the same MIME type, so the two generated providers describe an
/// item identically rather than each having its own idea of what it is.
final class CN1DocumentItem: NSObject, NSFileProviderItem {
    private let node: CN1DocumentNode
    private let parentId: NSFileProviderItemIdentifier
    private let identifier: NSFileProviderItemIdentifier
    private let revision: String
    private let containerURL: URL?

    /// The identifier is passed in rather than taken from the node because the tree's root has
    /// two names: whatever id the app gave it, and `.rootContainer`, which is the only one the
    /// system accepts back when it asked for the root. Answering with the app's id there is a
    /// mismatch the browser reads as "that is not the item I asked for".
    init(node: CN1DocumentNode, parentId: NSFileProviderItemIdentifier,
         identifier: NSFileProviderItemIdentifier? = nil, containerURL: URL? = nil,
         revision: String = "") {
        self.node = node
        self.parentId = parentId
        self.identifier = identifier ?? CN1DocumentIndex.identifier(for: node.id)
        self.containerURL = containerURL
        self.revision = revision
    }

    var itemIdentifier: NSFileProviderItemIdentifier {
        identifier
    }

    var parentItemIdentifier: NSFileProviderItemIdentifier {
        parentId
    }

    var filename: String {
        node.name ?? node.id
    }

    // iOS only: AppKit's NSFileProviderItem marks the string UTI unavailable, and the macOS
    // provider is always the replicated one, which asks for contentType instead.
    #if os(iOS)
    var typeIdentifier: String {
        if node.folder {
            return "public.folder"
        }
        if #available(iOS 14.0, *) {
            return CN1DocumentItem.utType(for: node).identifier
        }
        return "public.data"
    }
    #endif

    @available(iOS 14.0, macOS 11.0, *)
    var contentType: UTType {
        CN1DocumentItem.utType(for: node)
    }

    @available(iOS 14.0, macOS 11.0, *)
    private static func utType(for node: CN1DocumentNode) -> UTType {
        if node.folder {
            return .folder
        }
        if let declared = node.contentType, let t = UTType(mimeType: declared) {
            return t
        }
        // Falling back on the extension rather than straight to .data: the browser picks its
        // preview and its "open with" list from this, and .data offers neither.
        let ext = (node.name as NSString?)?.pathExtension ?? ""
        if !ext.isEmpty, let t = UTType(filenameExtension: ext) {
            return t
        }
        return .data
    }

    var capabilities: NSFileProviderItemCapabilities {
        // Reading only, for every item. The published tree is a view of content the app owns and
        // the app is its only writer -- createItem/modifyItem/deleteItem all refuse, and the
        // Android provider rejects a write-mode open. Advertising .allowsWriting here would make
        // the browser offer an edit-and-save flow that then fails on every save.
        if node.folder {
            return [.allowsReading, .allowsContentEnumerating]
        }
        return [.allowsReading]
    }

    var documentSize: NSNumber? {
        guard let size = node.size, size >= 0 else { return nil }
        return NSNumber(value: size)
    }

    var contentModificationDate: Date? {
        guard let ms = node.lastModified, ms >= 0 else { return nil }
        return Date(timeIntervalSince1970: TimeInterval(ms) / 1000.0)
    }

    @available(iOS 16.0, macOS 13.0, *)
    var itemVersion: NSFileProviderItemVersion {
        // The browser caches content per version, so the version has to move whenever the bytes
        // could have -- and it cannot be left to the app to remember to say so.
        //
        // For a file backed by the shared directory the bytes are right there, so the file's own
        // size and modification time are used and are authoritative: they move when the content
        // moves, whatever the app did or did not declare about it.
        //
        // A remote node has no local bytes to measure, so its declared size and date are the
        // only per-item signal. A node declaring neither -- or declaring values it never updates
        // -- would otherwise hold one constant stamp across every republish and the browser would
        // go on serving cached bytes. Those fall back to the index revision, which moves on every
        // publish: coarser, since it re-fetches that item whenever anything is published, but
        // never stale.
        //
        // Metadata is versioned separately. A rename, a move to another folder or a changed
        // content type leaves the bytes -- and so the content stamp -- exactly as they were, and
        // reusing one stamp for both would let the browser keep showing the old name in the old
        // place. Metadata comes from the index, which is rewritten on every publish, so the
        // revision belongs in this half: re-reading a name is cheap in a way re-fetching a file
        // is not.
        return NSFileProviderItemVersion(contentVersion: Data(contentStamp.utf8),
                                         metadataVersion: Data(metadataStamp.utf8))
    }

    private var metadataStamp: String {
        let parent = parentId.rawValue
        let type = node.contentType ?? ""
        return "\(node.name ?? "")|\(parent)|\(type)|\(node.size ?? -1)|\(node.lastModified ?? -1)|\(revision)"
    }

    private var contentStamp: String {
        if let path = node.path, !path.isEmpty, let container = containerURL,
           let local = CN1DocumentIndex.resolveLocal(path: path, containerURL: container),
           let attrs = try? FileManager.default.attributesOfItem(atPath: local.path) {
            let size = (attrs[.size] as? NSNumber)?.int64Value ?? -1
            let modified = (attrs[.modificationDate] as? Date)?.timeIntervalSince1970 ?? -1
            return "disk-\(size)-\(modified)"
        }
        // Remote content has no local bytes to measure, so the app's declared size and date are
        // the only per-item signal there is. They are trusted rather than overridden with the
        // revision on purpose: folding the revision in here would re-fetch every opened remote
        // document after any publish, which for a drive of any size is the expensive wrong
        // default. DocumentNode documents the other half of that bargain -- declare them, and
        // keep them accurate.
        if node.remoteId != nil, node.lastModified != nil || node.size != nil {
            return "meta-\(node.lastModified ?? -1)-\(node.size ?? -1)"
        }
        return "rev-\(revision)"
    }
}
