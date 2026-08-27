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

/// The pre-iOS-16 file provider, generated instead of `CN1FileProviderExtension` when the project
/// asks for a deployment target below the replicated API's floor.
///
/// Apple deprecated this API, and it is a materially weaker one: the browser addresses items by
/// URL under a working directory this extension maintains, so every item served has to be
/// materialized there first. It is generated only when asked for, and the two are never both in
/// a target -- they claim the same extension point.
final class CN1FileProviderClassic: NSFileProviderExtension {
    private lazy var containerURL: URL = FileManager.default
        .containerURL(forSecurityApplicationGroupIdentifier: CN1DocumentConfig.appGroupId)
        ?? FileManager.default.temporaryDirectory

    private var storageURL: URL {
        NSFileProviderManager.default.documentStorageURL
    }

    private func index() -> CN1DocumentIndex? {
        CN1DocumentIndex.load(containerURL: containerURL)
    }

    override func item(for identifier: NSFileProviderItemIdentifier) throws -> NSFileProviderItem {
        guard let index = index() else {
            throw NSFileProviderError(.noSuchItem)
        }
        guard let resolved = CN1DocumentEnumerator.resolve(identifier, in: index),
              let node = index.nodes[resolved] else {
            throw NSFileProviderError(.noSuchItem)
        }
        let parentId = index.parents[resolved]
        let parent: NSFileProviderItemIdentifier = (parentId == nil || parentId == index.rootId)
            ? .rootContainer
            : CN1DocumentIndex.identifier(for: parentId!)
        // The root answers to .rootContainer, never to the app's own id for it.
        let identifier: NSFileProviderItemIdentifier? =
            resolved == index.rootId ? .rootContainer : nil
        return CN1DocumentItem(node: node, parentId: parent, identifier: identifier,
                               containerURL: containerURL, revision: index.revision)
    }

    override func urlForItem(withPersistentIdentifier identifier: NSFileProviderItemIdentifier)
            -> URL? {
        guard let item = try? item(for: identifier) else {
            return nil
        }
        // One directory per identifier, because two published items may legitimately share a
        // filename in different folders and this API keys purely on the URL.
        //
        // The identifier is encoded first. Node ids are the app's own record keys and
        // DocumentNode puts no restriction on them, so an id like "account/42" would otherwise
        // become two path components -- and persistentIdentifierForItem, which reads back a
        // single component, would return "42" and then resolve to no such item.
        // The leaf is sanitized independently of the display name. The publisher refuses names
        // carrying a separator, but this provider also serves whatever index is on disk, and a
        // name like "reports/2031.pdf" here would add a directory level -- after which
        // persistentIdentifierForItem reads "reports" instead of the encoded id and nothing can
        // be materialized. A "../" leaf would walk out of the per-item directory entirely, and
        // the copy and remove below would follow it.
        return storageURL
            .appendingPathComponent(CN1FileProviderClassic.encode(identifier.rawValue),
                                    isDirectory: true)
            .appendingPathComponent(CN1FileProviderClassic.storageLeaf(item.filename))
    }

    override func persistentIdentifierForItem(at url: URL) -> NSFileProviderItemIdentifier? {
        let dir = url.deletingLastPathComponent().lastPathComponent
        guard !dir.isEmpty, let decoded = CN1FileProviderClassic.decode(dir) else {
            return nil
        }
        return NSFileProviderItemIdentifier(decoded)
    }

    /// Encodes an arbitrary node id into exactly one path component, and back.
    ///
    /// Percent-encoding with a deliberately narrow allowed set: the separator has to go, and so
    /// does "%" itself or decoding would be ambiguous.
    ///
    /// Uppercase letters are encoded as well, which is what makes the mapping injective on a
    /// case-INSENSITIVE volume. Node ids are the app's own record keys, so "Invoice" and "invoice"
    /// are two items; leaving both unencoded would give them one directory, and whichever
    /// materialized second would overwrite the first while persistentIdentifierForItem answered
    /// with the wrong id for both. The output is unambiguous because the only uppercase left in it
    /// is the hex of an escape, and "%" never survives unencoded to start a false one.
    ///
    /// Worked through: "cn1:Foo" encodes to "cn1%3A%46oo" and "cn1:foo" to "cn1%3Afoo", which
    /// stay distinct when the filesystem folds them to "cn1%3a%46oo" and "cn1%3afoo".
    static func encode(_ identifier: String) -> String {
        let allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyz0123456789-_")
        return identifier.addingPercentEncoding(withAllowedCharacters: allowed) ?? identifier
    }

    static func decode(_ component: String) -> String? {
        component.removingPercentEncoding
    }

    /// Reduces a display name to something safe as a single path component.
    static func storageLeaf(_ filename: String) -> String {
        var leaf = filename.replacingOccurrences(of: "/", with: "_")
        leaf = leaf.replacingOccurrences(of: "\\", with: "_")
        if leaf.isEmpty || leaf == "." || leaf == ".." {
            leaf = "item"
        }
        return leaf
    }

    override func providePlaceholder(at url: URL,
                                     completionHandler: @escaping (Error?) -> Void) {
        guard let identifier = persistentIdentifierForItem(at: url),
              let item = try? item(for: identifier) else {
            completionHandler(NSFileProviderError(.noSuchItem))
            return
        }
        do {
            let placeholder = NSFileProviderManager.placeholderURL(for: url)
            try FileManager.default.createDirectory(
                at: placeholder.deletingLastPathComponent(),
                withIntermediateDirectories: true)
            try NSFileProviderManager.writePlaceholder(at: placeholder, withMetadata: item)
            completionHandler(nil)
        } catch {
            completionHandler(error)
        }
    }

    override func startProvidingItem(at url: URL,
                                     completionHandler: @escaping (Error?) -> Void) {
        guard let identifier = persistentIdentifierForItem(at: url),
              let index = index() else {
            completionHandler(NSFileProviderError(.noSuchItem))
            return
        }
        guard let resolved = CN1DocumentEnumerator.resolve(identifier, in: index),
              let node = index.nodes[resolved], !node.folder else {
            completionHandler(NSFileProviderError(.noSuchItem))
            return
        }
        if let path = node.path, !path.isEmpty {
            // Resolved through the containment check: a published path is app data and may
            // have come from a server, so "../" in it must not be able to reach the app's own
            // storage and hand it to the system picker.
            if let local = CN1DocumentIndex.resolveLocal(path: path, containerURL: containerURL),
               FileManager.default.fileExists(atPath: local.path) {
                completionHandler(CN1FileProviderClassic.place(local, at: url, copy: true))
                return
            }
        }
        guard let remoteId = node.remoteId, !remoteId.isEmpty else {
            completionHandler(NSFileProviderError(.noSuchItem))
            return
        }
        // The task is deliberately dropped here. The classic API hands out no Progress and no
        // per-request cancellation handle -- `stopProvidingItem` is told about a URL, not about a
        // transfer -- so there is nothing to hang a cancel on without keeping a URL-to-task map
        // alive in the extension. The replicated path, which every supported OS uses, does cancel.
        let container = containerURL
        let credentials = CN1DocumentRemote.credentialStamp(containerURL: container)
        CN1DocumentRemote.fetch(remoteId: remoteId, containerURL: container) { fetched, error in
            guard let fetched = fetched else {
                completionHandler(error ?? NSFileProviderError(.noSuchItem))
                return
            }
            // The publication is checked again before the bytes are written, and once more after.
            //
            // A download outlives the request that started it, and the app may have called
            // clear() meanwhile -- a logout. place() creates the destination directory, so
            // without this the download would rebuild the storage the clear had just purged and
            // leave the departed user's document sitting in it, readable through a browser that
            // still has the folder open. Both checks are needed: the first covers a clear that
            // finished before the write, the second a clear that landed during it. A clear after
            // the second check purges the file itself.
            guard CN1FileProviderClassic.stillPublished(identifier, remoteId: remoteId,
                                                        credentials: credentials,
                                                        containerURL: container) else {
                try? FileManager.default.removeItem(at: fetched)
                completionHandler(NSFileProviderError(.noSuchItem))
                return
            }
            let placed = CN1FileProviderClassic.place(fetched, at: url, copy: false)
            if !CN1FileProviderClassic.stillPublished(identifier, remoteId: remoteId,
                                                     credentials: credentials,
                                                     containerURL: container) {
                try? FileManager.default.removeItem(at: url)
                completionHandler(NSFileProviderError(.noSuchItem))
                return
            }
            completionHandler(placed)
        }
    }

    override func stopProvidingItem(at url: URL) {
        // The materialized copy is a cache of content the app owns, so dropping it loses nothing
        // and keeps the working directory from growing without bound.
        try? FileManager.default.removeItem(at: url)
        providePlaceholder(at: url) { _ in }
    }

    override func enumerator(for containerItemIdentifier: NSFileProviderItemIdentifier)
            throws -> NSFileProviderEnumerator {
        CN1DocumentEnumerator(containerId: containerItemIdentifier, containerURL: containerURL)
    }

    /// Puts the bytes where the browser expects them. The local copy is copied rather than moved:
    /// it is the app's own file, and moving it out of the container would delete the original.
    /// Whether the identifier still names the same remote object in the published index.
    ///
    /// Reads the index from disk each time rather than trusting the copy the request started
    /// with: that is the whole point -- the file is gone once the app has cleared, and an item
    /// dropped by a republish is gone from a fresh read.
    ///
    /// The remote id is compared, not merely the identifier's existence. Node ids are the app's
    /// own record keys and an account switch usually reuses them -- "inbox", "invoice-1" -- while
    /// pointing them at another account's objects. Checking only that the id is still there would
    /// let a download started before the switch write the previous account's bytes into the new
    /// account's file.
    private static func stillPublished(_ identifier: NSFileProviderItemIdentifier,
                                       remoteId: String, credentials: String,
                                       containerURL: URL) -> Bool {
        guard let index = CN1DocumentIndex.load(containerURL: containerURL),
              let resolved = CN1DocumentEnumerator.resolve(identifier, in: index),
              let node = index.nodes[resolved], node.remoteId == remoteId else {
            return false
        }
        // And the same credential the download was authorized with. An account switch that reuses
        // both the node id and the server's key for it would otherwise pass everything above,
        // and the previous account's bytes would be written into the new account's file.
        return CN1DocumentRemote.credentialStamp(containerURL: containerURL) == credentials
    }

    private static func place(_ source: URL, at destination: URL, copy: Bool) -> Error? {
        do {
            try FileManager.default.createDirectory(
                at: destination.deletingLastPathComponent(),
                withIntermediateDirectories: true)
            if FileManager.default.fileExists(atPath: destination.path) {
                try FileManager.default.removeItem(at: destination)
            }
            if copy {
                try FileManager.default.copyItem(at: source, to: destination)
            } else {
                try FileManager.default.moveItem(at: source, to: destination)
            }
            return nil
        } catch {
            return error
        }
    }
}
