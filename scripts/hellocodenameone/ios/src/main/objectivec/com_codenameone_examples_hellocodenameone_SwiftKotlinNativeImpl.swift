import Foundation

@objc(com_codenameone_examples_hellocodenameone_SwiftKotlinNativeImpl)
class com_codenameone_examples_hellocodenameone_SwiftKotlinNativeImpl: NSObject {
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
