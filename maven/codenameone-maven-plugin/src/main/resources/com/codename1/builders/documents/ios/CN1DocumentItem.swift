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
    /// - Parameter localStamp: the source snapshot this item describes, for an item handed over
    ///   WITH a copy of that source. Without it the content version is read from the file each
    ///   time the system asks, so an app that rewrites the source between the hand-off and the
    ///   system's read would have the copied bytes labelled with the new file's version -- cached
    ///   as current, and never asked for again. An item that is only being enumerated passes
    ///   nothing and describes the file as it is.
    init(node: CN1DocumentNode, parentId: NSFileProviderItemIdentifier,
         identifier: NSFileProviderItemIdentifier? = nil, containerURL: URL? = nil,
         revision: String = "", localStamp: String? = nil) {
        self.node = node
        self.parentId = parentId
        self.identifier = identifier ?? CN1DocumentIndex.identifier(for: node.id)
        self.containerURL = containerURL
        self.revision = revision
        self.frozenLocalStamp = localStamp
    }

    private let frozenLocalStamp: String?

    var itemIdentifier: NSFileProviderItemIdentifier {
        identifier
    }

    var parentItemIdentifier: NSFileProviderItemIdentifier {
        parentId
    }

    var filename: String {
        CN1DocumentItem.displayName(node.name ?? node.id)
    }

    /// Reduces whatever the index offers to something the browser will accept as a file name.
    ///
    /// The publisher already refuses a name -- or an id standing in for one -- that carries a
    /// separator or is "." or "..", so a tree published through the API never reaches this. This
    /// reader serves whatever index is on disk, though, and a name it cannot show is an item
    /// Files may drop from the listing entirely, which is a worse failure than a renamed one.
    /// The classic provider's storage leaf is sanitized for exactly this reason.
    ///
    /// The identifier is untouched by any of this: it is namespaced and escaped separately, so a
    /// renamed display name still opens the right node.
    static func displayName(_ raw: String) -> String {
        var cleaned = raw.replacingOccurrences(of: "/", with: "_")
        cleaned = cleaned.replacingOccurrences(of: "\\", with: "_")
        if cleaned.isEmpty || cleaned == "." || cleaned == ".." {
            cleaned = "item"
        }
        return cleaned
    }

    // iOS only: AppKit's NSFileProviderItem marks the string UTI unavailable, and the macOS
    // provider is always the replicated one, which asks for contentType instead.
    #if os(iOS)
    /// The legacy content version, which is the only one the pre-iOS-16 provider has.
    ///
    /// itemVersion replaced it and the classic provider never reaches that path, so without this
    /// nothing there carries the content stamp: a metadata-less remote item republished after an
    /// account change kept the same nil date and the same size, and Files went on serving the
    /// bytes it had materialized for the previous account. The stamp already folds in the
    /// credential generation and the publication revision, so handing it over as bytes is all
    /// this needs to do.
    ///
    /// iOS only, as the SDK marks it: the macOS provider is always the replicated one.
    var versionIdentifier: Data? {
        CN1DocumentRemote.digest(contentStamp)
    }

    var typeIdentifier: String {
        if node.folder {
            return "public.folder"
        }
        if #available(iOS 14.0, *) {
            return CN1DocumentItem.utType(for: node).identifier
        }
        // iOS 13 and below get public.data rather than a type derived from the extension. The
        // lookup that would give a better answer there is MobileCoreServices'
        // UTTypeCreatePreferredIdentifierForTag, which is deprecated on every SDK this is built
        // with, so using it puts a deprecation warning in every customer build of the classic
        // provider for a preview icon on a version range that is already the fallback path. The
        // item still opens; it is the preview and the "open with" list that are generic.
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
        //
        // Taken from the name the browser is actually SHOWN -- which falls back to the node id --
        // rather than from node.name alone. A node that declared no name and no content type but
        // carries an id like "report.pdf" is listed as report.pdf and was still typed as raw
        // data, so it previewed as nothing and opened with everything.
        let ext = (displayName(node.name ?? node.id) as NSString).pathExtension
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
        if let ms = node.lastModified, ms >= 0 {
            return Date(timeIntervalSince1970: TimeInterval(ms) / 1000.0)
        }
        // Falls back to the file on disk when the app declared no date. The pre-iOS-16 provider
        // has no itemVersion to tell it the bytes moved, so this date is the only change signal it
        // gets: without the fallback a locally backed item that never declared one looks unchanged
        // forever and the browser goes on serving the copy it materialized first.
        //
        // A remote node has nothing to measure and stays nil rather than borrowing the publish
        // revision. A revision-derived date would move on every publish and re-fetch every opened
        // remote document each time, which for a drive of any size is the expensive wrong default
        // -- the same bargain contentStamp documents below.
        if let path = node.path, !path.isEmpty, let container = containerURL,
           let local = CN1DocumentIndex.resolveLocal(path: path, containerURL: container),
           let attrs = try? FileManager.default.attributesOfItem(atPath: local.path) {
            return attrs[.modificationDate] as? Date
        }
        return nil
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
        // Hashed, not handed over as text. Apple: "Components are limited to 128 bytes in
        // size", and these stamps carry a name, a parent id, a content type, a remote id and a
        // revision -- a long name alone can pass that on its own, and an over-long component is
        // a version the system rejects, which is a document that will not enumerate. A digest is
        // 32 bytes whatever went into it, and equality is all a version is compared for.
        return NSFileProviderItemVersion(contentVersion: CN1DocumentRemote.digest(contentStamp),
                                         metadataVersion: CN1DocumentRemote.digest(metadataStamp))
    }

    private var metadataStamp: String {
        // Length-prefixed, because three of these are free-form text and "|" is a character a
        // name or an id may contain. Joined plainly, a rename from "a" to "a|cn1:b" alongside a
        // move from parent "b|cn1:c" to "c" produced the same string -- the same metadata
        // version for different metadata, so the browser kept showing the old name in the old
        // place. The remote content stamp is prefixed for the same reason.
        return field(node.name ?? "") + field(parentId.rawValue) + field(node.contentType ?? "")
            + field(String(node.size ?? -1)) + field(String(node.lastModified ?? -1))
            + field(revision)
    }

    /// One field of a stamp, written so no value can be mistaken for a boundary.
    private func field(_ value: String) -> String {
        "\(value.utf8.count):\(value)"
    }

    /// What a locally backed file looks like right now, or nil when it is not there.
    ///
    /// The file number as well as the size and the date. A publish that swaps the bytes by
    /// writing a temporary file and renaming it over the old one -- which is how anything careful
    /// replaces a file, and how the Codename One side writes the index itself -- gives the path a
    /// different file, and it can carry the size and the timestamp of the one it replaced. Size
    /// and date alone would call that unchanged and the browser would go on serving what it
    /// cached.
    ///
    /// The publish revision is deliberately NOT folded in: it moves on every publish, so every
    /// materialized local document would be re-read whenever the app republishes anything. What
    /// that would additionally catch is an in-place rewrite that restores both the size and the
    /// modification time, which nothing reachable through FileSystemStorage can do -- it has no
    /// way to set a timestamp.
    ///
    /// Shared with the providers, which take it before and after copying a file: the bytes handed
    /// over and the version they are labelled with have to come from the same snapshot, and that
    /// only holds if both are measured the same way.
    static func localStamp(path: String, containerURL: URL) -> String? {
        guard let local = CN1DocumentIndex.resolveLocal(path: path, containerURL: containerURL),
              let attrs = try? FileManager.default.attributesOfItem(atPath: local.path) else {
            return nil
        }
        let size = (attrs[.size] as? NSNumber)?.int64Value ?? -1
        let modified = (attrs[.modificationDate] as? Date)?.timeIntervalSince1970 ?? -1
        let file = (attrs[.systemFileNumber] as? NSNumber)?.uint64Value ?? 0
        return "disk-\(size)-\(modified)-\(file)"
    }

    private var contentStamp: String {
        if let frozen = frozenLocalStamp {
            return frozen
        }
        if let path = node.path, !path.isEmpty, let container = containerURL,
           let stamp = CN1DocumentItem.localStamp(path: path, containerURL: container) {
            return stamp
        }
        // Remote content has no local bytes to measure, so the app's declared size and date are
        // the only per-item signal there is. They are trusted rather than overridden with the
        // revision on purpose: folding the revision in here would re-fetch every opened remote
        // document after any publish, which for a drive of any size is the expensive wrong
        // default. DocumentNode documents the other half of that bargain -- declare them, and
        // keep them accurate.
        // A DATE is the per-item signal. A size is not: content that changes to different bytes
        // of the same length -- a corrected total on an invoice, a redacted page -- leaves it
        // exactly where it was, so a node declaring only a size has nothing that moves when its
        // content does. Those fall through to the revision below, which moves on every publish:
        // coarser, and the only honest answer for an item that reports no version of its own.
        if let remoteId = node.remoteId, node.lastModified != nil {
            // The credential generation leads, so an account switch invalidates what the browser
            // cached even when the node id, the remote id, the size and the date all survive it.
            // Without that the system reuses its materialized copy and never asks for the item,
            // so the credential check on the download path is never reached.
            // The remote id is part of the stamp, not just the declared metadata. Repointing a
            // node at different content of the same length -- a new revision of an invoice under
            // a new key -- moves nothing else here, and the browser would go on serving the bytes
            // it cached for the old object.
            let generation = containerURL.map {
                CN1DocumentRemote.credentialGeneration(containerURL: $0)
            } ?? ""
            // The remote id is length-prefixed. It is free-form text and the fields are joined
            // with "-", so without that a change from ("a", nil, 2) to ("a-", 1, 2) produces the
            // same string -- and an unchanged content version is a browser that goes on serving
            // the bytes it cached for the previous remote object. The other fields are numbers
            // and the generation is fixed-width hex, so neither can swallow a separator.
            return "meta-\(generation)-\(remoteId.count):\(remoteId)"
                + "-\(node.lastModified ?? -1)-\(node.size ?? -1)"
        }
        if let container = containerURL, node.remoteId != nil {
            // A remote node that declared neither size nor date still has a credential, and
            // changing the endpoint or the token without republishing has to invalidate what the
            // browser holds. Without this the version stays "rev-<revision>" across an account
            // switch that did not republish, and the previous account's materialized bytes are
            // served as current.
            return "rev-\(revision)-\(CN1DocumentRemote.credentialGeneration(containerURL: container))"
        }
        return "rev-\(revision)"
    }
}
