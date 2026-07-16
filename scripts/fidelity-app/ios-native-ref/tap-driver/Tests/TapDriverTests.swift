// Drives REAL taps on the NativeRef app's tab bar while the recording host
// captures the simulator screen. Genuine touches are what make UIKit play the
// full Liquid Glass selection morph -- see the project.yml commentary.
import XCTest

final class TapDriverTests: XCTestCase {
    func testDriveTabs() {
        let env = ProcessInfo.processInfo.environment
        let app = XCUIApplication(bundleIdentifier: "com.codenameone.fidelity.nativeref")
        app.launchEnvironment["NATIVEREF_MODE"] = "animate"
        app.launchEnvironment["NATIVEREF_ANIM"] = "tabs"
        app.launchEnvironment["NATIVEREF_APPEARANCE"] = env["TAPDRIVER_APPEARANCE"] ?? "light"
        app.launch()
        sleep(2)
        let bar = app.tabBars.firstMatch
        XCTAssertTrue(bar.waitForExistence(timeout: 10), "NativeRef tab bar not found")
        let buttons = bar.buttons
        XCTAssertTrue(buttons.count >= 2, "NativeRef tab bar has too few items")
        // First tab -> last tab and back, matching the deterministic CN1
        // TabsMorph frames (travel 0 -> last), with a settle pause between.
        for _ in 0..<3 {
            buttons.element(boundBy: buttons.count - 1).tap()
            Thread.sleep(forTimeInterval: 1.4)
            buttons.element(boundBy: 0).tap()
            Thread.sleep(forTimeInterval: 1.4)
        }
    }
}
