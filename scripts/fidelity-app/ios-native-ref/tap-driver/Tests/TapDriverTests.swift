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
