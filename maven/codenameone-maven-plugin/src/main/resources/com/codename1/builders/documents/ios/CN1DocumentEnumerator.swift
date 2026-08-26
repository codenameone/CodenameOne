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
            return CN1DocumentItem(node: node, parentId: parent)
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
