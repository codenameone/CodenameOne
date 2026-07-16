// Minimal host application for the UI-test bundle. XCUITest requires a host
// app target; the actual work happens in the test, which launches and taps
// the NativeRef app.
import UIKit

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        let w = UIWindow(frame: UIScreen.main.bounds)
        w.rootViewController = UIViewController()
        w.makeKeyAndVisible()
        window = w
        return true
    }
}
