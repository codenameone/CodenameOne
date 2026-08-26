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
        let resolved = CN1DocumentEnumerator.resolve(identifier, in: index)
        guard let node = index.nodes[resolved] else {
            throw NSFileProviderError(.noSuchItem)
        }
        let parentId = index.parents[resolved]
        let parent: NSFileProviderItemIdentifier = (parentId == nil || parentId == index.rootId)
            ? .rootContainer
            : NSFileProviderItemIdentifier(parentId!)
        // The root answers to .rootContainer, never to the app's own id for it.
        let identifier: NSFileProviderItemIdentifier? =
            resolved == index.rootId ? .rootContainer : nil
        return CN1DocumentItem(node: node, parentId: parent, identifier: identifier)
    }

    override func urlForItem(withPersistentIdentifier identifier: NSFileProviderItemIdentifier)
            -> URL? {
        guard let item = try? item(for: identifier) else {
            return nil
        }
        // One directory per identifier, because two published items may legitimately share a
        // filename in different folders and this API keys purely on the URL.
        return storageURL.appendingPathComponent(identifier.rawValue, isDirectory: true)
            .appendingPathComponent(item.filename)
    }

    override func persistentIdentifierForItem(at url: URL) -> NSFileProviderItemIdentifier? {
        let dir = url.deletingLastPathComponent().lastPathComponent
        return dir.isEmpty ? nil : NSFileProviderItemIdentifier(dir)
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
        let resolved = CN1DocumentEnumerator.resolve(identifier, in: index)
        guard let node = index.nodes[resolved], !node.folder else {
            completionHandler(NSFileProviderError(.noSuchItem))
            return
        }
        if let path = node.path, !path.isEmpty {
            let local = containerURL
                .appendingPathComponent("cn1documents/files")
                .appendingPathComponent(path)
            if FileManager.default.fileExists(atPath: local.path) {
                completionHandler(CN1FileProviderClassic.place(local, at: url, copy: true))
                return
            }
        }
        guard let remoteId = node.remoteId, !remoteId.isEmpty else {
            completionHandler(NSFileProviderError(.noSuchItem))
            return
        }
        CN1DocumentRemote.fetch(remoteId: remoteId, containerURL: containerURL) { fetched, error in
            guard let fetched = fetched else {
                completionHandler(error ?? NSFileProviderError(.noSuchItem))
                return
            }
            completionHandler(CN1FileProviderClassic.place(fetched, at: url, copy: false))
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
