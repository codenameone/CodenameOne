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

    /// The identifier is passed in rather than taken from the node because the tree's root has
    /// two names: whatever id the app gave it, and `.rootContainer`, which is the only one the
    /// system accepts back when it asked for the root. Answering with the app's id there is a
    /// mismatch the browser reads as "that is not the item I asked for".
    init(node: CN1DocumentNode, parentId: NSFileProviderItemIdentifier,
         identifier: NSFileProviderItemIdentifier? = nil) {
        self.node = node
        self.parentId = parentId
        self.identifier = identifier ?? NSFileProviderItemIdentifier(node.id)
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
        if node.folder {
            return [.allowsReading, .allowsContentEnumerating]
        }
        return node.readOnly == true ? [.allowsReading] : [.allowsReading, .allowsWriting]
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
        // could have. Modification time and size are the only signals the index carries; when the
        // app omits both, republishing the same id serves the cached copy.
        let stamp = "\(node.lastModified ?? -1)-\(node.size ?? -1)"
        let data = Data(stamp.utf8)
        return NSFileProviderItemVersion(contentVersion: data, metadataVersion: data)
    }
}
