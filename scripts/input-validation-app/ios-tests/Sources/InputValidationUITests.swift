// Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// XCUITest target that drives the CN1 input-validation app through tap,
// drag, and long-press gestures on the iOS simulator. We rely on coordinate
// taps rather than accessibility queries because the CN1 iOS port does not
// surface child Components as XCUIElements -- the whole CN1 form renders into
// one GL/Metal-backed view from XCUITest's perspective. The driver shell
// script asserts the CN1IV:EVENT:* lines appear in the os_log stream; this
// file only sequences the physical inputs.

import XCTest

final class InputValidationUITests: XCTestCase {
    // Bundle identifier of the CN1-built iOS app under test. The CN1 maven
    // plugin derives the iOS CFBundleIdentifier from
    // `codename1.packageName` in common/codenameone_settings.properties, so
    // keeping that property and this default in sync is enough. The
    // CN1IV_BUNDLE_ID env var override is for local runs against an app
    // built with a different packageName.
    private var bundleIdentifier: String {
        return ProcessInfo.processInfo.environment["CN1IV_BUNDLE_ID"]
            ?? "com.codenameone.inputvalidation"
    }

    private var stepDelaySeconds: TimeInterval {
        if let raw = ProcessInfo.processInfo.environment["CN1IV_STEP_DELAY_SEC"],
           let v = Double(raw) {
            return v
        }
        return 3.0
    }

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testGestureSuite() throws {
        let app = XCUIApplication(bundleIdentifier: bundleIdentifier)
        app.launch()
        // Wait for the form to render before driving inputs. The CN1 EDT needs
        // a moment after process launch to mount the GLViewController, run the
        // first paint, and start dispatching pointer events. Without this delay
        // taps fire into the splash screen.
        Thread.sleep(forTimeInterval: 2.5)

        try driveTap(app: app)
        Thread.sleep(forTimeInterval: stepDelaySeconds)

        try driveDrag(app: app)
        Thread.sleep(forTimeInterval: stepDelaySeconds)

        try driveLongPress(app: app)
        Thread.sleep(forTimeInterval: stepDelaySeconds)
    }

    private func driveTap(app: XCUIApplication) throws {
        let center = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        center.tap()
    }

    private func driveDrag(app: XCUIApplication) throws {
        // Sweep horizontally across the middle band so the CN1 drag detector
        // collects enough pointerDragged samples to exceed its 3-sample floor.
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.55))
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.55))
        start.press(forDuration: 0.05, thenDragTo: end)
    }

    private func driveLongPress(app: XCUIApplication) throws {
        let center = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        center.press(forDuration: 1.5)
    }
}
