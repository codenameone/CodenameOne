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

/// One entry of the index the app published. Mirrors com.codename1.documents.DocumentNode.
struct CN1DocumentNode: Decodable {
    let id: String
    let name: String?
    let folder: Bool
    let contentType: String?
    let path: String?
    let remoteId: String?
    let children: [CN1DocumentNode]?

    /// The declared size, or nil when the app declared none.
    ///
    /// Normalized HERE rather than at each reader. The publisher omits the field for a node with
    /// no size, but a negative value spells the same thing everywhere else in the format --
    /// DocumentNode carries -1 for "unknown" and the Android provider tests for it -- and a
    /// reader that took it literally would report a document of negative length, version it by
    /// that number, and refuse every download whose length did not match it. One boundary, so
    /// there is no next reader to forget.
    var size: Int64? { declaredSize.flatMap { $0 >= 0 ? $0 : nil } }

    /// The declared modification date in milliseconds, or nil when the app declared none.
    ///
    /// Same sentinel, and it decides more than a displayed date: a node with no date has no
    /// per-item change signal, so its version falls back to the publication's revision. Read
    /// literally, a negative date would look like a real one and freeze the content stamp of a
    /// document whose bytes change without changing length.
    var lastModified: Int64? { declaredLastModified.flatMap { $0 >= 0 ? $0 : nil } }

    private let declaredSize: Int64?
    private let declaredLastModified: Int64?

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case folder
        case contentType
        case path
        case remoteId
        case children
        case declaredSize = "size"
        case declaredLastModified = "lastModified"
    }
}

struct CN1DocumentIndexDocument: Decodable {
    let v: Int
    /// The publication's own revision, written by the publisher. Optional so an index written
    /// before the field existed still parses; the file's modification time stands in there.
    let rev: String?
    let root: CN1DocumentNode
}

/// The published tree, flattened once into identifier-keyed lookups.
final class CN1DocumentIndex {
    static let rootIdentifier = "root"

    private(set) var nodes: [String: CN1DocumentNode] = [:]
    private(set) var parents: [String: String] = [:]
    private(set) var childIds: [String: [String]] = [:]
    let rootId: String

    /// Changes whenever the app republishes. The index file is rewritten on every publish, so
    /// its modification time is a revision counter the format did not have to grow a field for.
    /// `CN1DocumentItem` folds it into the item version of any node that declares neither a size
    /// nor a modification date, which is the only case where the node itself carries no signal
    /// that its bytes changed.
    let revision: String

    init(root: CN1DocumentNode, revision: String) {
        rootId = root.id
        self.revision = revision
        index(root, parent: nil)
    }

    /// The first id that was already taken when it was inserted, if any.
    ///
    /// The publisher refuses a tree whose ids collide, and refuses the spellings that
    /// normalization rewrites so that two ids cannot become one key here. This is the backstop
    /// for whatever that cannot see: these keys are Swift Strings, compared canonically, and the
    /// publisher has no normalizer to call -- so a pair it could not rule out arrives as two
    /// nodes and lands on one key, and the second would silently replace the first while both
    /// rows stayed in the tree. Two names, one document, no error anywhere. Refusing the whole
    /// index instead is the same trade the publisher makes: the location reads as empty, which
    /// the app can see and fix, rather than quietly wrong.
    private(set) var duplicateId: String?

    private func index(_ node: CN1DocumentNode, parent: String?) {
        if nodes[node.id] != nil {
            duplicateId = node.id
        }
        nodes[node.id] = node
        if let parent = parent {
            parents[node.id] = parent
            childIds[parent, default: []].append(node.id)
        }
        for child in node.children ?? [] {
            index(child, parent: node.id)
        }
    }

    /// Reads the index the app last published into the shared container. Returns nil when the app
    /// has published nothing yet, which is an empty file browser rather than an error.
    /// Prefix carried by every identifier this provider hands the system.
    ///
    /// `DocumentNode` places no restriction on ids, so without a namespace an app id equal to a
    /// File Provider token -- `NSFileProviderItemIdentifier.rootContainer.rawValue`, or the
    /// working-set one -- is indistinguishable from the token itself: the node is listed, and
    /// every later request for it is answered by the root or swallowed by the working-set
    /// enumeration. Prefixing makes the two spaces disjoint by construction rather than by a
    /// rule the publisher has to remember.
    private static let identifierPrefix = "cn1:"

    /// The identifier the system should use for a published node.
    static func identifier(for nodeId: String) -> NSFileProviderItemIdentifier {
        NSFileProviderItemIdentifier(identifierPrefix + nodeId)
    }

    /// The published node an identifier refers to, or nil when it is a system token.
    static func nodeId(for identifier: NSFileProviderItemIdentifier) -> String? {
        guard identifier.rawValue.hasPrefix(identifierPrefix) else {
            return nil
        }
        return String(identifier.rawValue.dropFirst(identifierPrefix.count))
    }

    /// The directory published `path` values are relative to.
    static func filesDirectory(containerURL: URL) -> URL {
        containerURL.appendingPathComponent("cn1documents/files", isDirectory: true)
    }

    /// Resolves a published relative path, refusing anything that escapes the shared directory.
    ///
    /// A path is app-supplied data and may have come from a server, so ".." in it must not be
    /// able to reach the app's own databases or preferences and hand them to the system picker.
    /// Standardizing first is what makes the prefix check meaningful: without it "a/../../x"
    /// still starts with the directory it escapes.
    static func resolveLocal(path: String, containerURL: URL) -> URL? {
        // Symlinks are resolved on both sides before the prefix test. standardizedFileURL only
        // folds ".." lexically, so a link inside the published tree -- "files/shared" pointing at
        // the container root -- would let "shared/endpoint.json" pass a purely textual check and
        // hand the file browser the endpoint file, bearer token included. Resolving first means
        // the comparison is between the two real locations.
        //
        // The base is resolved too, not just the candidate: the container path itself runs
        // through symlinks on both iOS and macOS ("/private/var/..."), so resolving one side
        // alone would make every legitimate path look like an escape.
        let base = filesDirectory(containerURL: containerURL)
            .standardizedFileURL.resolvingSymlinksInPath()
        let candidate = base.appendingPathComponent(path)
            .standardizedFileURL.resolvingSymlinksInPath()
        var basePath = base.path
        if !basePath.hasSuffix("/") {
            basePath += "/"
        }
        return candidate.path.hasPrefix(basePath) ? candidate : nil
    }

    static func load(containerURL: URL) -> CN1DocumentIndex? {
        let url = containerURL.appendingPathComponent("cn1documents/index.json")
        // Stat, read, stat again, and only trust the pair when the file did not move under it.
        //
        // The app replaces this file whole, by rename, while the extension is reading: a stat
        // taken after the read can belong to a publication the bytes in hand are not from, and
        // the tree would then be served -- names, parents, the lot -- stamped with a revision
        // that says it is the newer one. Everything downstream believes that stamp: it is the
        // metadata version, the guard on an in-flight copy, and the fallback content version.
        //
        // Bounded rather than a loop: the writer is not adversarial, it is an app publishing, so
        // a couple of retries covers a republish landing mid-read. A last attempt that still saw
        // the file move is returned only when the decoded document carries its OWN revision:
        // tree and stamp are then one publication, merely an older one, and a stale-but-whole
        // tree beats an empty browser because the next publish signals again. Without that field
        // the only stamp available is the second stat, which may belong to a publication these
        // bytes are not from -- an old tree presented as the current one, which the metadata
        // version, the guard on an in-flight copy and the fallback content version all believe.
        // There is nothing honest to answer there, so it answers nothing.
        for attempt in 0..<3 {
            let before = modificationStamp(of: url)
            guard let data = try? Data(contentsOf: url) else {
                return nil
            }
            guard let doc = try? JSONDecoder().decode(CN1DocumentIndexDocument.self,
                                                      from: data) else {
                return nil
            }
            let after = modificationStamp(of: url)
            if before == after {
                // The revision is the publisher's own when it wrote one; see below.
                // The publisher's own revision when it wrote one. It differs for every
                // publication, which the file's modification time does not: two publishes inside
                // one filesystem tick share a timestamp, and a download fetched against the first
                // was then accepted after the second. The timestamp remains the fallback for an
                // index written before the field existed.
                return accept(CN1DocumentIndex(root: doc.root, revision: doc.rev ?? after))
            }
            if attempt == 2, let revision = doc.rev {
                return accept(CN1DocumentIndex(root: doc.root, revision: revision))
            }
        }
        return nil
    }

    /// Refuses a tree whose ids collided when it was flattened.
    private static func accept(_ index: CN1DocumentIndex) -> CN1DocumentIndex? {
        if let duplicate = index.duplicateId {
            NSLog("[cn1documents] the published index names \(duplicate) twice, or twice in "
                + "spellings that are one name after normalization; refusing it")
            return nil
        }
        return index
    }

    private static func modificationStamp(of url: URL) -> String {
        (try? FileManager.default.attributesOfItem(atPath: url.path)[.modificationDate])
            .flatMap { $0 as? Date }
            .map { String($0.timeIntervalSince1970) } ?? ""
    }
}

/// The part of a node that says which revision of a remote object it names.
///
/// Compared before a download is accepted: the remote id is the key, and the key survives a
/// server-side revision, so an app that republishes with a new size or date while the previous
/// bytes are still arriving has to be able to reject them.
struct CN1RemoteVersion: Equatable {
    let size: Int64?
    let lastModified: Int64?
    let revision: String?

    init(_ node: CN1DocumentNode, revision: String) {
        size = node.size
        lastModified = node.lastModified
        // A node with no DATE is versioned by the publication it came from, exactly as its
        // content stamp is. A size is not a change signal -- content can change to different
        // bytes of the same length -- so with no date there is nothing per-item to compare, and
        // a republish while the old download was in flight would accept those bytes and then
        // label them with the new item's revision-based version: cached as current, indefinitely.
        //
        // A node that declares a date is left alone by the revision on purpose. Folding it in
        // would discard every download racing any publish, which for a drive of any size is the
        // expensive wrong default; DocumentNode documents the other half of that bargain.
        self.revision = lastModified == nil ? revision : nil
    }
}

/// Whether two opaque keys are the same key.
///
/// By UTF-8 bytes, not by Swift's ==, which compares canonically: "e" followed by U+0301 and the
/// precomposed letter are one string to Swift and two different query values to an endpoint that
/// reads bytes. A response fetched for one would otherwise be accepted as the other. The
/// publisher refuses decomposed remote ids, so this is for an index that arrived some other way.
func cn1SameOpaqueKey(_ a: String?, _ b: String?) -> Bool {
    guard let a = a, let b = b else {
        return a == nil && b == nil
    }
    return Array(a.utf8) == Array(b.utf8)
}

/// Whether this node is served from the shared directory rather than from the endpoint.
///
/// Both providers prefer a local path when one resolves, so a node that has gained one has
/// stopped being a remote item -- and a download still in flight for it is answering a question
/// nobody is asking any more.
func cn1HasLocalContent(_ node: CN1DocumentNode, containerURL: URL) -> Bool {
    guard let path = node.path, !path.isEmpty,
          let local = CN1DocumentIndex.resolveLocal(path: path, containerURL: containerURL) else {
        return false
    }
    return FileManager.default.fileExists(atPath: local.path)
}
