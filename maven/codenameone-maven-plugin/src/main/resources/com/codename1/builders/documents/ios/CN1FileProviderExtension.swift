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
        // No item is built here on purpose. Both branches below hand the system one read after
        // the bytes are in hand, from a fresh index, because the publication can change while a
        // copy or a download is running -- and an item captured now would describe the file as it
        // was when the request arrived, not as it is when it is handed over.

        // Guarded because two things can finish this fetch -- the work and a cancellation -- and
        // calling the system's completion handler twice is a contract violation of its own.
        let once = CN1FetchCompletion(completionHandler)

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
                let container = containerURL
                let generation = index.revision
                // Off the request thread. fetchContents is expected to return its Progress
                // promptly, and copying a large document takes long enough that doing it here
                // holds the reply until it finishes -- which the system reads as a provider that
                // has stopped responding.
                DispatchQueue.global(qos: .userInitiated).async {
                    do {
                        try FileManager.default.copyItem(at: local, to: handoff)
                        // Re-read afterwards, exactly as the remote branch does. A large file
                        // takes long enough to copy that a clear() or an account-switch republish
                        // can land during it, and the copy finishes from the source handle either
                        // way -- so without this the previous publication's bytes are handed over
                        // after the logout that was supposed to remove them.
                        if let current = CN1FileProviderExtension.publishedItem(
                                for: itemIdentifier, path: path, generation: generation,
                                containerURL: container) {
                            if !once.call(handoff, current, nil) {
                                // A cancellation got there first, so nothing will ever collect
                                // this copy.
                                try? FileManager.default.removeItem(at: handoff)
                            }
                        } else {
                            try? FileManager.default.removeItem(at: handoff)
                            once.call(nil, nil, NSFileProviderError(.noSuchItem))
                        }
                    } catch {
                        once.call(nil, nil, CN1DocumentRemote.providerError(error))
                    }
                    progress.completedUnitCount = 1
                    progress.cancellationHandler = nil
                }
                // Cancelling answers the system at once and drops the copy when it lands. The
                // copy itself is not interrupted: FileManager cannot be stopped mid-file, and
                // chunking a local disk copy to gain that would cost more than it saves -- there
                // is no network here, and the bytes are deleted rather than kept.
                progress.cancellationHandler = { [weak progress] in
                    once.call(nil, nil, CocoaError(.userCancelled))
                    progress?.completedUnitCount = 1
                }
                return progress
            }
        }
        guard let remoteId = node.remoteId, !remoteId.isEmpty else {
            completionHandler(nil, nil, NSFileProviderError(.noSuchItem))
            progress.completedUnitCount = 1
            return progress
        }
        let container = containerURL
        // Captured before the download starts, so what comes back is checked against what was
        // asked for rather than against whatever happens to be current when it arrives.
        let requested = CN1RemoteVersion(node, revision: index.revision)
        let credentials = CN1DocumentRemote.credentialStamp(containerURL: container)
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
                                                                       version: requested,
                                                                       credentials: credentials,
                                                                       containerURL: container) {
                    if !once.call(url, current, nil) {
                        // A cancellation got there first, so File Provider will never collect
                        // this file -- and nothing else would ever delete it. Repeatedly
                        // cancelling near the end of a download would otherwise fill the
                        // extension's temporary directory with whole documents.
                        try? FileManager.default.removeItem(at: url)
                    }
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
    /// The same, for a node served from the shared directory.
    ///
    /// No credential is involved here, so the guard is the publication itself: the revision the
    /// copy started from has to be the revision still on disk. The path cannot stand in for it --
    /// an account switch that clears and republishes a conventional name like
    /// "documents/invoice.pdf" satisfies a path check while the bytes in hand came from the
    /// publication before it, read from a source that is by then unlinked.
    ///
    /// Being this strict costs a re-copy whenever a publish lands mid-copy, which is the right
    /// trade for a local file and the wrong one for a download -- which is why the remote branch
    /// keys on the credential instead, and says so there.
    private static func publishedItem(for identifier: NSFileProviderItemIdentifier,
                                      path: String, generation: String,
                                      containerURL: URL) -> NSFileProviderItem? {
        guard let index = CN1DocumentIndex.load(containerURL: containerURL),
              index.revision == generation,
              let resolved = CN1DocumentEnumerator.resolve(identifier, in: index),
              let node = index.nodes[resolved], node.path == path else {
            return nil
        }
        return item(for: node, resolved: resolved, in: index, containerURL: containerURL)
    }

    private static func publishedItem(for identifier: NSFileProviderItemIdentifier,
                                      remoteId: String, version: CN1RemoteVersion,
                                      credentials: String,
                                      containerURL: URL) -> NSFileProviderItem? {
        guard let index = CN1DocumentIndex.load(containerURL: containerURL),
              let resolved = CN1DocumentEnumerator.resolve(identifier, in: index),
              let node = index.nodes[resolved], node.remoteId == remoteId,
              CN1RemoteVersion(node, revision: index.revision) == version,
              CN1DocumentRemote.credentialStamp(containerURL: containerURL) == credentials else {
            // The credential is compared as well as the node and the remote object: an account
            // switch reuses node ids, and can reuse the server's keys for them, so those two
            // alone would let the previous account's bytes through.
            //
            // And the declared version, because a server-side revision usually keeps its key: an
            // app that republishes the node with a new size or date while the old bytes are still
            // arriving would otherwise have them handed over AND rebuilt into an item carrying
            // the new version, so the browser would cache stale content under a stamp that says
            // it is current -- and never ask again.
            return nil
        }
        return item(for: node, resolved: resolved, in: index, containerURL: containerURL)
    }

    /// Builds the item for a node the caller has already resolved in a freshly loaded index.
    private static func item(for node: CN1DocumentNode, resolved: String,
                             in index: CN1DocumentIndex, containerURL: URL) -> NSFileProviderItem {
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

    /// - Returns: true when this call is the one that answered the system. A loser has to clean
    ///   up whatever it was about to hand over, since nothing downstream will ever see it.
    @discardableResult
    func call(_ url: URL?, _ item: NSFileProviderItem?, _ error: Error?) -> Bool {
        lock.lock()
        let first = !done
        done = true
        lock.unlock()
        if first {
            handler(url, item, error)
        }
        return first
    }
}
