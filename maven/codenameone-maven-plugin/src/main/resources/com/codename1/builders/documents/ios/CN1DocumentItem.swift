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

    init(node: CN1DocumentNode, parentId: NSFileProviderItemIdentifier) {
        self.node = node
        self.parentId = parentId
    }

    var itemIdentifier: NSFileProviderItemIdentifier {
        NSFileProviderItemIdentifier(node.id)
    }

    var parentItemIdentifier: NSFileProviderItemIdentifier {
        parentId
    }

    var filename: String {
        node.name ?? node.id
    }

    // iOS only: AppKit's NSFileProviderItem marks the string UTI unavailable, and the macOS
    // provider is always the replicated one, which asks for contentType instead.
    #if os(iOS)
    var typeIdentifier: String {
        if node.folder {
            return "public.folder"
        }
        if #available(iOS 14.0, *) {
            return CN1DocumentItem.utType(for: node).identifier
        }
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
        let ext = (node.name as NSString?)?.pathExtension ?? ""
        if !ext.isEmpty, let t = UTType(filenameExtension: ext) {
            return t
        }
        return .data
    }

    var capabilities: NSFileProviderItemCapabilities {
        if node.folder {
            return [.allowsReading, .allowsContentEnumerating]
        }
        return node.readOnly == true ? [.allowsReading] : [.allowsReading, .allowsWriting]
    }

    var documentSize: NSNumber? {
        guard let size = node.size, size >= 0 else { return nil }
        return NSNumber(value: size)
    }

    var contentModificationDate: Date? {
        guard let ms = node.lastModified, ms >= 0 else { return nil }
        return Date(timeIntervalSince1970: TimeInterval(ms) / 1000.0)
    }

    @available(iOS 16.0, macOS 13.0, *)
    var itemVersion: NSFileProviderItemVersion {
        // The browser caches content per version, so the version has to move whenever the bytes
        // could have. Modification time and size are the only signals the index carries; when the
        // app omits both, republishing the same id serves the cached copy.
        let stamp = "\(node.lastModified ?? -1)-\(node.size ?? -1)"
        let data = Data(stamp.utf8)
        return NSFileProviderItemVersion(contentVersion: data, metadataVersion: data)
    }
}
