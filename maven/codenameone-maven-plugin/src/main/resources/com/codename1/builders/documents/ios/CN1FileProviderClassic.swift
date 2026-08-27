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
    /// The downloads currently running, by the URL each was asked to materialize.
    ///
    /// This API hands out no Progress and no per-request handle -- stopProvidingItem is told
    /// about a URL, not about a transfer -- so the only way to reach a running fetch is to keep
    /// the map. Doing without looked defensible while the cost was counted as wasted data. It is
    /// not: a download completing after a stop RECREATES the file the system had just evicted,
    /// and answers a request nobody is waiting for any more.
    private let inFlightLock = NSLock()
    private var inFlight: [URL: URLSessionTask] = [:]
    /// Bumped whenever a URL is stopped, so a fetch that had already finished downloading when
    /// the stop arrived can still tell that it is no longer wanted.
    private var stopGeneration: [URL: Int] = [:]

    /// One lock per materialization URL, held only while the shared path is touched.
    ///
    /// Two requests for the same URL can overlap -- a stop and an immediate reopen while the
    /// first copy is still running -- and both used to write straight to it. The stale one could
    /// overwrite what the newer one had already materialized and then delete the shared path as
    /// its own cleanup, leaving the newer request reporting success over nothing.
    ///
    /// The long part of the work never holds this. A copy or a download goes to a private file
    /// first; the lock covers the moment it is claimed and moved into place, and the moment a
    /// stop takes the URL away, so those cannot interleave.
    private var urlGates: [URL: NSLock] = [:]

    private func gate(for url: URL) -> NSLock {
        inFlightLock.lock()
        defer { inFlightLock.unlock() }
        if let existing = urlGates[url] {
            return existing
        }
        let created = NSLock()
        urlGates[url] = created
        return created
    }

    /// Moves a staged file onto the shared URL, but only while this request still owns it.
    ///
    /// - Returns: nil when the file is in place, an error when the move failed, and
    ///   NSFileProviderError(.noSuchItem) when a stop claimed the URL first -- in which case
    ///   nothing was written and the staged file has been removed.
    private func install(_ staged: URL, at url: URL, generation: Int) -> Error? {
        let lock = gate(for: url)
        lock.lock()
        defer { lock.unlock() }
        guard stillWanted(at: url, generation: generation) else {
            try? FileManager.default.removeItem(at: staged)
            return NSFileProviderError(.noSuchItem)
        }
        return CN1FileProviderClassic.place(staged, at: url, copy: false)
    }

    private func beginFetch(at url: URL) -> Int {
        inFlightLock.lock()
        defer { inFlightLock.unlock() }
        return stopGeneration[url] ?? 0
    }

    private func registerFetch(_ task: URLSessionTask?, at url: URL) {
        guard let task = task else {
            return
        }
        inFlightLock.lock()
        inFlight[url] = task
        inFlightLock.unlock()
    }

    /// Drops this fetch's entry, and only this one.
    ///
    /// The entry is matched by task identity rather than by URL alone. A cancelled download can
    /// call back after the same URL has been reopened, and removing whatever was under the key
    /// took the REPLACEMENT's task out of the map -- so a later stop found nothing to cancel and
    /// a large download went on using the network after its eviction. Generations cannot stand in
    /// for this: two opens with no stop between them share one.
    private func clearFetch(_ task: URLSessionTask?, at url: URL) {
        inFlightLock.lock()
        if let task = task, inFlight[url] === task {
            inFlight.removeValue(forKey: url)
        }
        inFlightLock.unlock()
    }

    /// Whether a stop has arrived for `url` since `generation` was taken. Read-only: the
    /// in-flight entry is claimed by finishFetch, so this is the after-the-write check.
    private func stillWanted(at url: URL, generation: Int) -> Bool {
        inFlightLock.lock()
        defer { inFlightLock.unlock() }
        return (stopGeneration[url] ?? 0) == generation
    }

    /// Whether the fetch that started at `generation` may still write to `url`.
    private func finishFetch(at url: URL, generation: Int) -> Bool {
        inFlightLock.lock()
        defer { inFlightLock.unlock() }
        return (stopGeneration[url] ?? 0) == generation
    }

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
        // The root IS documentStorageURL in this API -- that is what the storage URL means -- so
        // it cannot be given a directory of its own like every other node. Treating it as an
        // ordinary item put the root at "<storage>/<encoded-root-id>/<root-name>", which nothing
        // else in the API agrees with, and left persistentIdentifierForItem unable to answer for
        // the storage URL at all.
        if identifier == .rootContainer {
            return storageURL
        }
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
        // The other half of the root mapping above: the storage URL is the root container, and
        // reading a per-item directory out of it would answer with whatever the last path
        // component of the storage directory happens to be called.
        if url.standardizedFileURL == storageURL.standardizedFileURL {
            return .rootContainer
        }
        let dir = url.deletingLastPathComponent().lastPathComponent
        guard !dir.isEmpty else {
            return nil
        }
        if let decoded = CN1FileProviderClassic.decode(dir) {
            return NSFileProviderItemIdentifier(decoded)
        }
        // A digest directory: the identifier cannot be read back out of it, so it is matched
        // against the published ones instead.
        guard let index = index(), let resolved = decodeDigest(dir, in: index) else {
            return nil
        }
        return NSFileProviderItemIdentifier(resolved)
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
        let escaped = identifier.addingPercentEncoding(withAllowedCharacters: allowed)
            ?? identifier
        if escaped.utf8.count <= maxComponentBytes {
            return escaped
        }
        // Too long to be a path component. The publisher refuses ids that reach this -- it
        // budgets the escaped, namespaced identifier against the same limit -- so this is for an
        // index that arrived some other way, the same reason the display name and the storage
        // leaf are sanitized here rather than trusted. A digest is bounded whatever went in, and
        // decode resolves it by looking for the node it belongs to.
        return digestPrefix + CN1DocumentRemote.digest(identifier)
            .prefix(16).map { String(format: "%02x", $0) }.joined()
    }

    /// 255 is where APFS, ext4 and NTFS all stop, and this is one component.
    private static let maxComponentBytes = 255
    /// Marks a directory name as a digest rather than an escaped identifier. Not a legal escaped
    /// form -- "%" is always escaped to "%25" -- so the two cannot be confused.
    private static let digestPrefix = "cn1h-"

    static func decode(_ component: String) -> String? {
        guard !component.hasPrefix(digestPrefix) else {
            return nil
        }
        return component.removingPercentEncoding
    }

    /// The identifier a digest directory belongs to, found by digesting the published ones.
    ///
    /// Only reached for a component encode() could not spell out, which the publisher does not
    /// produce; the ordinary path never loads the index for this.
    private func decodeDigest(_ component: String, in index: CN1DocumentIndex) -> String? {
        for nodeId in index.nodes.keys {
            let identifier = CN1DocumentIndex.identifier(for: nodeId)
            if CN1FileProviderClassic.encode(identifier.rawValue) == component {
                return identifier.rawValue
            }
        }
        return nil
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
                // Copied, then checked against the publication it was copied from. The copy is
                // synchronous but a large file takes long enough for a clear() to finish during
                // it, and place() CREATES the storage directory -- so without this a logout would
                // be followed by the departed user's bytes being written back into the storage it
                // had just purged. The revision is the guard rather than the path: an account
                // switch republishing a conventional name like "documents/invoice.pdf" satisfies
                // a path check while the bytes came from the publication before it.
                // And against a stop, exactly as the remote branch is. stopProvidingItem runs
                // on another thread: it deletes this URL and moves the stop generation, and a
                // copy still running then puts the file back through place(), which recreates
                // the storage directory too -- so the evicted item is materialized again and the
                // request completes after the system stopped asking for it. The publication
                // check cannot see that, because nothing about the publication changed.
                let publication = index.revision
                let stopped = beginFetch(at: url)
                let container = containerURL
                // Off the provider thread, as the replicated provider's copy is. This one is
                // called on the thread that has to return from startProvidingItem, and copying a
                // large document inline holds every other request behind it until it finishes --
                // long enough for the system to treat the provider as hung.
                DispatchQueue.global(qos: .userInitiated).async {
                    // Staged beside the destination -- same directory, so the same volume -- and
                    // only then claimed. The shared URL is never written by a request that has
                    // been superseded, which is what stopped a stale copy from overwriting a
                    // newer one and then deleting it as its own cleanup.
                    let staged = self.storageURL.appendingPathComponent(UUID().uuidString)
                    if let error = CN1FileProviderClassic.place(local, at: staged, copy: true) {
                        completionHandler(error)
                        return
                    }
                    guard CN1FileProviderClassic.stillPublished(identifier, path: path,
                                                                generation: publication,
                                                                containerURL: container) else {
                        try? FileManager.default.removeItem(at: staged)
                        completionHandler(NSFileProviderError(.noSuchItem))
                        return
                    }
                    let placed = self.install(staged, at: url, generation: stopped)
                    if placed != nil {
                        self.providePlaceholder(at: url) { _ in }
                    }
                    completionHandler(placed)
                }
                return
            }
        }
        guard let remoteId = node.remoteId, !remoteId.isEmpty else {
            completionHandler(NSFileProviderError(.noSuchItem))
            return
        }
        let container = containerURL
        // Captured before the download starts, for the reason the replicated provider gives: a
        // server-side revision usually keeps its key, so an app republishing the node with a new
        // size or date while the old bytes are still arriving has to be able to reject them.
        let requested = CN1RemoteVersion(node, revision: index.revision)
        // Read once, so the credential the request is sent with is the one the completion is
        // checked against; see CN1DocumentRemote.stamp.
        let settings = CN1DocumentRemote.settings(containerURL: container)
        let credentials = CN1DocumentRemote.stamp(settings)
        // Registered below so stopProvidingItem can reach it; the generation is captured first
        // so a stop landing after the download finishes is still seen.
        let generation = beginFetch(at: url)
        // The completion has to name its OWN task to drop the right entry, and the task does not
        // exist until fetch returns -- so it is handed over in a box, filled in below before
        // anything can start: the task comes back suspended.
        let box = CN1FetchBox()
        let task = CN1DocumentRemote.fetch(remoteId: remoteId,
                                           settings: settings) { fetched, error in
            self.clearFetch(box.task, at: url)
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
            guard self.finishFetch(at: url, generation: generation) else {
                // Stopped while this was in flight. The bytes are dropped rather than placed: the
                // system evicted this URL and is not waiting for it any more.
                try? FileManager.default.removeItem(at: fetched)
                completionHandler(NSFileProviderError(.noSuchItem))
                return
            }
            guard CN1FileProviderClassic.stillPublished(identifier, remoteId: remoteId,
                                                        version: requested,
                                                        credentials: credentials,
                                                        containerURL: container) else {
                try? FileManager.default.removeItem(at: fetched)
                completionHandler(NSFileProviderError(.noSuchItem))
                return
            }
            // The download is already a private file, so this is the same claim the local branch
            // makes: the shared URL is written only while this request still owns it, under its
            // gate, so a stop and a reopen cannot both be writing there.
            let placed = self.install(fetched, at: url, generation: generation)
            if placed != nil {
                // Either the move failed or a stop claimed the URL first. The download goes
                // either way: install moves rather than copies, so a failed move leaves the whole
                // file where it landed, and the likely reason is a full volume that every retry
                // would make worse. The placeholder is rewritten for the failure case; after a
                // stop it is already there, and writing it again costs nothing.
                try? FileManager.default.removeItem(at: fetched)
                self.providePlaceholder(at: url) { _ in }
            }
            completionHandler(placed)
        }
        // Registered before it is started, so a stop arriving in between finds a task to cancel
        // rather than a download it can only refuse to store afterwards.
        box.task = task
        registerFetch(task, at: url)
        task?.resume()
    }

    override func stopProvidingItem(at url: URL) {
        // Any download still running for this URL is cancelled first, and the generation moves so
        // one that has already finished downloading cannot place its bytes either. Without that,
        // a fetch completing just after this call rebuilds the very file the eviction below
        // removed, and hands the system a document it stopped asking for.
        let lock = gate(for: url)
        lock.lock()
        inFlightLock.lock()
        let task = inFlight.removeValue(forKey: url)
        stopGeneration[url] = (stopGeneration[url] ?? 0) + 1
        inFlightLock.unlock()

        // The materialized copy is a cache of content the app owns, so dropping it loses nothing
        // and keeps the working directory from growing without bound. Under the URL's gate, so a
        // materialization cannot be installing at the same moment: the generation moves and the
        // file goes as one step, and any request holding an older generation is refused before it
        // writes anything.
        try? FileManager.default.removeItem(at: url)
        lock.unlock()
        task?.cancel()
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
    /// The local equivalent: same node, same path, and the same publication the copy came from.
    private static func stillPublished(_ identifier: NSFileProviderItemIdentifier,
                                       path: String, generation: String,
                                       containerURL: URL) -> Bool {
        guard let index = CN1DocumentIndex.load(containerURL: containerURL),
              index.revision == generation,
              let resolved = CN1DocumentEnumerator.resolve(identifier, in: index),
              let node = index.nodes[resolved] else {
            return false
        }
        return node.path == path
    }

    private static func stillPublished(_ identifier: NSFileProviderItemIdentifier,
                                       remoteId: String, version: CN1RemoteVersion,
                                       credentials: String, containerURL: URL) -> Bool {
        guard let index = CN1DocumentIndex.load(containerURL: containerURL),
              let resolved = CN1DocumentEnumerator.resolve(identifier, in: index),
              let node = index.nodes[resolved], node.remoteId == remoteId,
              // The declared version too: the remote id is the app's key for the object and a
              // server-side revision keeps it, so an id match alone would move the previous
              // revision's bytes into storage after the newer publication is already current.
              CN1RemoteVersion(node, revision: index.revision) == version else {
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
            // Whatever was written before the failure goes with it. The placeholder was already
            // replaced by the time a copy can fail part-way -- a full volume is the likely cause
            // -- so leaving it there would put a truncated document at the materialization path,
            // visible to every later provider call as though it were the real one. The caller
            // writes the placeholder back.
            try? FileManager.default.removeItem(at: destination)
            return error
        }
    }
}

/// Carries a task to the completion handler that belongs to it.
///
/// The task does not exist until `fetch` returns, and its completion has to name it to drop the
/// right in-flight entry. The task comes back suspended, so this is filled in before anything can
/// run and no synchronisation is needed on top of that ordering.
private final class CN1FetchBox {
    var task: URLSessionTask?
}
