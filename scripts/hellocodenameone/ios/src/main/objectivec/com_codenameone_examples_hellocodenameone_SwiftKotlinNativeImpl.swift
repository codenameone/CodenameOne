import Foundation

@objc(CN1SwiftKotlinNativeBridge)
@objcMembers
public class CN1SwiftKotlinNativeBridge: NSObject {
    @objc func implementationLanguage() -> String {
        return "swift"
    }

    @objc func diagnostics() -> String {
        return "ios-swift-native-impl"
    }

    @objc func isSupported() -> Bool {
        return true
    }
}
