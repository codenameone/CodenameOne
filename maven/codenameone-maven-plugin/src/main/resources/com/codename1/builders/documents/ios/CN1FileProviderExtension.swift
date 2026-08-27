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

/// The generated file provider. Serves the browser from the tree the app published into the
/// shared App Group container, fetching anything it does not hold locally from the app's endpoint.
///
/// This process is not the app. It is started by the system when someone opens the file browser,
/// runs while the app is dead, and cannot call into Java -- which is why everything it needs
/// arrives as data through the container rather than as a callback.
final class CN1FileProviderExtension: NSObject, NSFileProviderReplicatedExtension {
    private let containerURL: URL
    private let domain: NSFileProviderDomain

    required init(domain: NSFileProviderDomain) {
        containerURL = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: CN1DocumentConfig.appGroupId)
            ?? FileManager.default.temporaryDirectory
        self.domain = domain
        super.init()
    }

    /// Where a file handed to `fetchContents` has to live. Apple: "The retrieved content at
    /// `fileContents` URL must be a regular file on the same volume as the user-visible URL. A
    /// suitable location can be retrieved using -[NSFileProviderManager
    /// temporaryDirectoryURLWithError:]." The process temporary directory is not guaranteed to be
    /// that volume, and the system clones and unlinks the file it is given.
    private func handoffDirectory() -> URL {
        if let manager = NSFileProviderManager(for: domain),
           let url = try? manager.temporaryDirectoryURL() {
            return url
        }
        return FileManager.default.temporaryDirectory
    }

    func invalidate() {
    }

    func item(for identifier: NSFileProviderItemIdentifier,
              request: NSFileProviderRequest,
              completionHandler: @escaping (NSFileProviderItem?, Error?) -> Void) -> Progress {
        let progress = Progress(totalUnitCount: 1)
        guard let index = CN1DocumentIndex.load(containerURL: containerURL) else {
            completionHandler(nil, NSFileProviderError(.noSuchItem))
            progress.completedUnitCount = 1
            return progress
        }
        guard let resolved = CN1DocumentEnumerator.resolve(identifier, in: index),
              let node = index.nodes[resolved] else {
            completionHandler(nil, NSFileProviderError(.noSuchItem))
            progress.completedUnitCount = 1
            return progress
        }
        let parent: NSFileProviderItemIdentifier
        if resolved == index.rootId {
            parent = .rootContainer
        } else if let parentId = index.parents[resolved], parentId != index.rootId {
            parent = CN1DocumentIndex.identifier(for: parentId)
        } else {
            parent = .rootContainer
        }
        // The root answers to .rootContainer, never to the app's own id for it.
        let identifier: NSFileProviderItemIdentifier? =
            resolved == index.rootId ? .rootContainer : nil
        completionHandler(CN1DocumentItem(node: node, parentId: parent, identifier: identifier,
                               containerURL: containerURL, revision: index.revision),
                          nil)
        progress.completedUnitCount = 1
        return progress
    }

    func fetchContents(for itemIdentifier: NSFileProviderItemIdentifier,
                       version requestedVersion: NSFileProviderItemVersion?,
                       request: NSFileProviderRequest,
                       completionHandler: @escaping (URL?, NSFileProviderItem?, Error?) -> Void)
            -> Progress {
        let progress = Progress(totalUnitCount: 1)
        guard let index = CN1DocumentIndex.load(containerURL: containerURL) else {
            completionHandler(nil, nil, NSFileProviderError(.noSuchItem))
            progress.completedUnitCount = 1
            return progress
        }
        guard let resolved = CN1DocumentEnumerator.resolve(itemIdentifier, in: index),
              let node = index.nodes[resolved], !node.folder else {
            completionHandler(nil, nil, NSFileProviderError(.noSuchItem))
            progress.completedUnitCount = 1
            return progress
        }
        let parentId = index.parents[resolved]
        let parent: NSFileProviderItemIdentifier = (parentId == nil || parentId == index.rootId)
            ? .rootContainer
            : CN1DocumentIndex.identifier(for: parentId!)
        let item = CN1DocumentItem(node: node, parentId: parent, containerURL: containerURL,
                                   revision: index.revision)

        // A local copy always wins, which is what makes a cached remote document open without a
        // round trip.
        if let path = node.path, !path.isEmpty {
            // Resolved through the containment check: a published path is app data and may
            // have come from a server, so "../" in it must not be able to reach the app's own
            // storage and hand it to the system picker.
            if let local = CN1DocumentIndex.resolveLocal(path: path, containerURL: containerURL),
               FileManager.default.fileExists(atPath: local.path) {
                // A clone, never the published file itself. Apple: "The system clones and unlinks
                // the received fileContents... If the extension wishes to keep a copy of the
                // content, it must provide a clone of that content as the URL passed to the
                // completion handler." Handing over the app's own file would unlink it out of the
                // shared container on the first open, and every later open would find nothing.
                let handoff = handoffDirectory().appendingPathComponent(UUID().uuidString)
                do {
                    try FileManager.default.copyItem(at: local, to: handoff)
                    completionHandler(handoff, item, nil)
                } catch {
                    completionHandler(nil, nil, CN1DocumentRemote.providerError(error))
                }
                progress.completedUnitCount = 1
                return progress
            }
        }
        guard let remoteId = node.remoteId, !remoteId.isEmpty else {
            completionHandler(nil, nil, NSFileProviderError(.noSuchItem))
            progress.completedUnitCount = 1
            return progress
        }
        // Guarded because two things can finish this fetch -- the download and a cancellation --
        // and calling the system's completion handler twice is a contract violation of its own.
        let once = CN1FetchCompletion(completionHandler)
        let container = containerURL
        let task = CN1DocumentRemote.fetch(remoteId: remoteId, containerURL: container,
                                           destination: handoffDirectory()) { url, error in
            if let url = url {
                // The publication is re-read before the bytes are handed over. A download
                // outlives the request that started it, and the app may have cleared or
                // republished meanwhile -- a logout, or an account switch that reuses the same
                // node ids for another account's objects. Handing over what arrived would
                // materialize the previous account's document after its credentials were gone.
                //
                // The item goes with it, rebuilt from the same fresh read: the one captured when
                // the request began may name a file that has since been renamed or moved.
                if let current = CN1FileProviderExtension.publishedItem(for: itemIdentifier,
                                                                       remoteId: remoteId,
                                                                       containerURL: container) {
                    once.call(url, current, nil)
                } else {
                    try? FileManager.default.removeItem(at: url)
                    once.call(nil, nil, NSFileProviderError(.noSuchItem))
                }
            } else {
                once.call(nil, nil, CN1DocumentRemote.providerError(error))
            }
            progress.completedUnitCount = 1
            progress.cancellationHandler = nil
        }
        // The Progress is the only handle File Provider has on this transfer. Apple: "If the
        // NSProgress returned by this method is cancelled, the extension should call the
        // completion handler with (nil, nil, NSUserCancelledError) in the NSProgress cancellation
        // handler" -- and the download has to actually stop, or dismissing a large file leaves it
        // transferring to completion on the user's mobile data.
        // Weakly, or the handler and the Progress that stores it hold each other and neither is
        // ever freed -- one leaked fetch per remote document opened, in a process the system
        // memory-limits harder than the app. The completion clears the handler for the same
        // reason, which also releases the task and the guard it captures.
        progress.cancellationHandler = { [weak progress] in
            task?.cancel()
            once.call(nil, nil, CocoaError(.userCancelled))
            progress?.completedUnitCount = 1
        }
        return progress
    }

    /// The item as the index has it right now, provided it still names the same remote object.
    ///
    /// Static, and given the container rather than reading it off the instance, so the closure
    /// that calls it after a download does not have to keep the extension alive to do so.
    private static func publishedItem(for identifier: NSFileProviderItemIdentifier,
                                      remoteId: String,
                                      containerURL: URL) -> NSFileProviderItem? {
        guard let index = CN1DocumentIndex.load(containerURL: containerURL),
              let resolved = CN1DocumentEnumerator.resolve(identifier, in: index),
              let node = index.nodes[resolved], node.remoteId == remoteId else {
            return nil
        }
        let parentId = index.parents[resolved]
        let parent: NSFileProviderItemIdentifier = (parentId == nil || parentId == index.rootId)
            ? .rootContainer
            : CN1DocumentIndex.identifier(for: parentId!)
        return CN1DocumentItem(node: node, parentId: parent, containerURL: containerURL,
                               revision: index.revision)
    }

    func enumerator(for containerItemIdentifier: NSFileProviderItemIdentifier,
                    request: NSFileProviderRequest) throws -> NSFileProviderEnumerator {
        CN1DocumentEnumerator(containerId: containerItemIdentifier, containerURL: containerURL)
    }

    // The published tree is a view of content the app owns; the app is the only writer. These
    // three are required by the protocol rather than optional, so they are implemented as an
    // explicit refusal -- the browser then greys out new/rename/delete instead of offering
    // them and silently losing the edit.
    func createItem(basedOn itemTemplate: NSFileProviderItem,
                    fields: NSFileProviderItemFields,
                    contents url: URL?,
                    options: NSFileProviderCreateItemOptions = [],
                    request: NSFileProviderRequest,
                    completionHandler: @escaping (NSFileProviderItem?, NSFileProviderItemFields,
                                                  Bool, Error?) -> Void) -> Progress {
        completionHandler(nil, [], false, CN1FileProviderExtension.unsupported())
        return Progress(totalUnitCount: 1)
    }

    func modifyItem(_ item: NSFileProviderItem,
                    baseVersion version: NSFileProviderItemVersion,
                    changedFields: NSFileProviderItemFields,
                    contents newContents: URL?,
                    options: NSFileProviderModifyItemOptions = [],
                    request: NSFileProviderRequest,
                    completionHandler: @escaping (NSFileProviderItem?, NSFileProviderItemFields,
                                                  Bool, Error?) -> Void) -> Progress {
        completionHandler(nil, [], false, CN1FileProviderExtension.unsupported())
        return Progress(totalUnitCount: 1)
    }

    func deleteItem(identifier: NSFileProviderItemIdentifier,
                    baseVersion version: NSFileProviderItemVersion,
                    options: NSFileProviderDeleteItemOptions = [],
                    request: NSFileProviderRequest,
                    completionHandler: @escaping (Error?) -> Void) -> Progress {
        completionHandler(CN1FileProviderExtension.unsupported())
        return Progress(totalUnitCount: 1)
    }

    private static func unsupported() -> NSError {
        NSError(domain: NSCocoaErrorDomain, code: NSFeatureUnsupportedError, userInfo: [
            NSLocalizedDescriptionKey:
                "This location is published by the app and cannot be edited from the file browser."
        ])
    }

}

/// Calls one of the system's completion handlers exactly once.
///
/// A fetch can be finished by its download or by a cancellation, and both arrive on threads of
/// the system's choosing. Whichever gets there first wins; the other is dropped.
private final class CN1FetchCompletion {
    private let lock = NSLock()
    private var done = false
    private let handler: (URL?, NSFileProviderItem?, Error?) -> Void

    init(_ handler: @escaping (URL?, NSFileProviderItem?, Error?) -> Void) {
        self.handler = handler
    }

    func call(_ url: URL?, _ item: NSFileProviderItem?, _ error: Error?) {
        lock.lock()
        let first = !done
        done = true
        lock.unlock()
        if first {
            handler(url, item, error)
        }
    }
}
