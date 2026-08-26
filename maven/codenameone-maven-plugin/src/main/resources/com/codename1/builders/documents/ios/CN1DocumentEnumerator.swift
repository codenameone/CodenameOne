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

/// Lists the children of one published folder.
final class CN1DocumentEnumerator: NSObject, NSFileProviderEnumerator {
    private let containerId: NSFileProviderItemIdentifier
    private let containerURL: URL

    init(containerId: NSFileProviderItemIdentifier, containerURL: URL) {
        self.containerId = containerId
        self.containerURL = containerURL
    }

    func invalidate() {
    }

    func enumerateItems(for observer: NSFileProviderEnumerationObserver,
                        startingAt page: NSFileProviderPage) {
        // Re-read on every enumeration rather than caching: the app republishes while this
        // process is alive, and a cached tree would keep serving the previous publish until the
        // extension happened to be torn down.
        guard let index = CN1DocumentIndex.load(containerURL: containerURL) else {
            observer.finishEnumerating(upTo: nil)
            return
        }
        // The working set is the container `documentsSignalChange` invalidates, and the system
        // asks it what changed. A provider that answers "nothing is in my working set" is
        // answering honestly but uselessly: the signal then teaches the browser nothing and a
        // closed or cached location goes on showing the previous publish. With no change
        // tracking to offer (this index carries no per-item history), the whole published tree
        // is the working set.
        if containerId == .workingSet {
            let items = index.nodes.values.compactMap { node -> CN1DocumentItem? in
                guard node.id != index.rootId else { return nil }
                let parentId = index.parents[node.id]
                let parent: NSFileProviderItemIdentifier =
                    (parentId == nil || parentId == index.rootId)
                        ? .rootContainer
                        : NSFileProviderItemIdentifier(parentId!)
                return CN1DocumentItem(node: node, parentId: parent, containerURL: containerURL,
                                       revision: index.revision)
            }
            observer.didEnumerate(items)
            observer.finishEnumerating(upTo: nil)
            return
        }
        let resolved = CN1DocumentEnumerator.resolve(containerId, in: index)
        guard let children = index.childIds[resolved] else {
            observer.finishEnumerating(upTo: nil)
            return
        }
        let parent: NSFileProviderItemIdentifier = resolved == index.rootId
            ? .rootContainer
            : NSFileProviderItemIdentifier(resolved)
        let items = children.compactMap { id -> CN1DocumentItem? in
            guard let node = index.nodes[id] else { return nil }
            return CN1DocumentItem(node: node, parentId: parent, containerURL: containerURL,
                                   revision: index.revision)
        }
        observer.didEnumerate(items)
        observer.finishEnumerating(upTo: nil)
    }

    func enumerateChanges(for observer: NSFileProviderChangeObserver,
                          from anchor: NSFileProviderSyncAnchor) {
        // The published tree carries no change log, so every signal is answered with "start over".
        // That is honest rather than lazy: claiming no changes would leave the browser showing a
        // stale tree after a republish, which is the whole point of signalChange().
        observer.finishEnumeratingWithError(NSFileProviderError(.syncAnchorExpired))
    }

    func currentSyncAnchor(completionHandler: @escaping (NSFileProviderSyncAnchor?) -> Void) {
        completionHandler(nil)
    }

    /// Maps the browser's root token onto whatever id the app gave its root node.
    static func resolve(_ identifier: NSFileProviderItemIdentifier,
                        in index: CN1DocumentIndex) -> String {
        identifier == .rootContainer ? index.rootId : identifier.rawValue
    }
}
