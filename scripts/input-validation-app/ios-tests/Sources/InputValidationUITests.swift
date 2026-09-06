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
//
// XCUITest target that drives the CN1 input-validation app through tap,
// drag, long-press and keyboard-typing gestures on the iOS simulator. We
// rely on coordinate taps rather than accessibility queries because the CN1
// iOS port does not surface child Components as XCUIElements -- the whole
// CN1 form renders into one GL/Metal-backed view from XCUITest's
// perspective. The driver shell script asserts the CN1IV:EVENT:* lines
// appear in the os_log stream; this file only sequences the physical
// inputs.

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
        // Say which mode this run is in. Without a directory every waitForGate
        // returns immediately, so an ungated run looks exactly like a perfectly
        // synchronised one right up until a step races -- worth one log line.
        NSLog("CN1IV: gate directory %@", syncDir?.path ?? "<none: running ungated on fixed delays>")
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
        if syncDir == nil {
            Thread.sleep(forTimeInterval: stepDelaySeconds)
        }

        try driveKeyType(app: app, syncDir: syncDir)
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

    private func driveKeyType(app: XCUIApplication, syncDir: URL?) throws {
        try waitForGate("keytype", syncDir: syncDir)
        // KeyTypeStep places its TextField in BorderLayout.CENTER with
        // generous padding/margin, matching the layout TapStep and
        // LongPressStep use so a single screen-center tap focuses it on
        // every iPhone size class on the CI runner.
        let center = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        center.tap()
        // typeKey, not typeText. typeText refuses to synthesise anything
        // unless some accessibility element reports hasKeyboardFocus, and CN1
        // publishes its own accessibility tree (updateAccessibilityTree assigns
        // container.accessibilityElements), which replaces the real subviews --
        // so the native CN1UITextField never appears in the tree no matter how
        // long we wait for it. typeKey posts the key events directly, which is
        // the hardware-keyboard path this step is about: they arrive as UIPress
        // on the responder chain through GLViewController, where issue #5709
        // was swallowing them. The on-screen keyboard raises no UIPress, which
        // is why tapping its keys kept working.
        //
        // Retried rather than typed once after a fixed sleep: CN1's
        // editStringAtImpl needs a moment to install the native editor and make
        // it first responder, and it took three and a half seconds on a
        // simulator busy serving XCUITest accessibility snapshots. Keys typed
        // before then are simply dropped, so the loop costs nothing and removes
        // the guess. KeyTypeStep asserts the field CONTAINS "cn1", so the
        // repeats a slow start can leave in front of it are harmless.
        //
        // Stopped by the driver's keytype.stop gate, written as soon as the step
        // resolves either way. The app exits a second and a half after the suite
        // finishes, and typing into a process that has left fails the XCUITest
        // run even though every event landed; app.state is re-checked before
        // each key so the loop cannot outlive the app in the gap between the
        // gate being written and this loop waking up.
        for _ in 0..<15 {
            if stopRequested("keytype", syncDir: syncDir) {
                return
            }
            for key in ["c", "n", "1"] {
                guard app.state == .runningForeground else {
                    return
                }
                app.typeKey(key, modifierFlags: [])
            }
            Thread.sleep(forTimeInterval: 1.0)
        }
    }

    private func stopRequested(_ name: String, syncDir: URL?) -> Bool {
        guard let syncDir = syncDir else {
            return false
        }
        return FileManager.default.fileExists(
            atPath: syncDir.appendingPathComponent("\(name).stop").path)
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
