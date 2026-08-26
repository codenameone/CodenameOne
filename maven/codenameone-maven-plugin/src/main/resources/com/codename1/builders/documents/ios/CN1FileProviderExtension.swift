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

    required init(domain: NSFileProviderDomain) {
        containerURL = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: CN1DocumentConfig.appGroupId)
            ?? FileManager.default.temporaryDirectory
        super.init()
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
        let resolved = CN1DocumentEnumerator.resolve(identifier, in: index)
        guard let node = index.nodes[resolved] else {
            completionHandler(nil, NSFileProviderError(.noSuchItem))
            progress.completedUnitCount = 1
            return progress
        }
        let parent: NSFileProviderItemIdentifier
        if resolved == index.rootId {
            parent = .rootContainer
        } else if let parentId = index.parents[resolved], parentId != index.rootId {
            parent = NSFileProviderItemIdentifier(parentId)
        } else {
            parent = .rootContainer
        }
        completionHandler(CN1DocumentItem(node: node, parentId: parent), nil)
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
        let resolved = CN1DocumentEnumerator.resolve(itemIdentifier, in: index)
        guard let node = index.nodes[resolved], !node.folder else {
            completionHandler(nil, nil, NSFileProviderError(.noSuchItem))
            progress.completedUnitCount = 1
            return progress
        }
        let parentId = index.parents[resolved]
        let parent: NSFileProviderItemIdentifier = (parentId == nil || parentId == index.rootId)
            ? .rootContainer
            : NSFileProviderItemIdentifier(parentId!)
        let item = CN1DocumentItem(node: node, parentId: parent)

        // A local copy always wins, which is what makes a cached remote document open without a
        // round trip.
        if let path = node.path, !path.isEmpty {
            let local = containerURL
                .appendingPathComponent("cn1documents/files")
                .appendingPathComponent(path)
            if FileManager.default.fileExists(atPath: local.path) {
                completionHandler(local, item, nil)
                progress.completedUnitCount = 1
                return progress
            }
        }
        guard let remoteId = node.remoteId, !remoteId.isEmpty else {
            completionHandler(nil, nil, NSFileProviderError(.noSuchItem))
            progress.completedUnitCount = 1
            return progress
        }
        CN1DocumentRemote.fetch(remoteId: remoteId, containerURL: containerURL) { url, error in
            if let url = url {
                completionHandler(url, item, nil)
            } else {
                completionHandler(nil, nil, error)
            }
            progress.completedUnitCount = 1
        }
        return progress
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
