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

    private var syncDirectory: URL? {
        if let raw = ProcessInfo.processInfo.environment["CN1IV_SYNC_DIR"], !raw.isEmpty {
            return URL(fileURLWithPath: raw)
        }
        let fallback = "/tmp/cn1-input-validation-sync"
        if FileManager.default.fileExists(atPath: fallback) {
            return URL(fileURLWithPath: fallback)
        }
        return nil
    }

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testGestureSuite() throws {
        let app = XCUIApplication(bundleIdentifier: bundleIdentifier)
        let syncDir = syncDirectory
        app.launch()
        if syncDir == nil {
            // Local fallback when the shell harness is not coordinating from
            // CN1IV:READY log markers.
            Thread.sleep(forTimeInterval: 2.5)
        }

        try driveTap(app: app, syncDir: syncDir)
        if syncDir == nil {
            Thread.sleep(forTimeInterval: stepDelaySeconds)
        }

        try driveDrag(app: app, syncDir: syncDir)
        if syncDir == nil {
            Thread.sleep(forTimeInterval: stepDelaySeconds)
        }

        try driveLongPress(app: app, syncDir: syncDir)
        Thread.sleep(forTimeInterval: max(stepDelaySeconds, 10.0))
    }

    private func driveTap(app: XCUIApplication, syncDir: URL?) throws {
        try waitForGate("tap", syncDir: syncDir)
        let center = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        center.tap()
    }

    private func driveDrag(app: XCUIApplication, syncDir: URL?) throws {
        try waitForGate("drag", syncDir: syncDir)
        // Sweep horizontally across the middle band so the CN1 drag detector
        // collects enough pointerDragged samples to exceed its 3-sample floor.
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.55))
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.55))
        start.press(forDuration: 0.05, thenDragTo: end)
    }

    private func driveLongPress(app: XCUIApplication, syncDir: URL?) throws {
        try waitForGate("longpress", syncDir: syncDir)
        let target = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.56))
        target.press(forDuration: 2.25)
    }

    private func waitForGate(_ name: String, syncDir: URL?) throws {
        guard let syncDir = syncDir else {
            return
        }
        let gate = syncDir.appendingPathComponent("\(name).go")
        let deadline = Date().addingTimeInterval(45.0)
        while Date() < deadline {
            if FileManager.default.fileExists(atPath: gate.path) {
                return
            }
            Thread.sleep(forTimeInterval: 0.1)
        }
        throw NSError(
            domain: "CN1InputValidationUITests",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: "Timed out waiting for \(gate.path)"]
        )
    }
}
