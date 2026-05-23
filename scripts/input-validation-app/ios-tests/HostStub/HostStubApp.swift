// Minimal UIKit host app for the XCUITest target. Application-only UI
// testing (no host app, USES_XCTRUNNER) errored with an opaque
// `** TEST FAILED **` under Xcode 16.4. Giving the test bundle a regular
// host -- even one that does nothing -- is the standard XCUITest setup and
// removes that failure mode. The actual UI tests attach to the
// already-installed CN1 input-validation app by bundle id via
// XCUIApplication(bundleIdentifier:), so what this stub does is irrelevant
// -- it just needs to be a launchable app the test runner can host into.

import UIKit

@UIApplicationMain
final class HostStubAppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        let w = UIWindow(frame: UIScreen.main.bounds)
        let vc = UIViewController()
        vc.view.backgroundColor = .black
        w.rootViewController = vc
        w.makeKeyAndVisible()
        self.window = w
        return true
    }
}
