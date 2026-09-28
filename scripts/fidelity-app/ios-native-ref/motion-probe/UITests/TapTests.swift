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

// Drives GENUINE taps on the probe's tab bar: UIKit plays the full Liquid Glass
// selection (lift, lens, bar pulse, glow) only for touch-driven selection, never
// for a programmatic selectedIndex change. PROBE_SEQ lists the gestures (taps,
// holds, drags), two seconds apart so every motion settles before the next.
import XCTest

final class TapTests: XCTestCase {
    func testTaps() {
        // xcodebuild strips the TEST_RUNNER_ prefix the capture script sets.
        let env = ProcessInfo.processInfo.environment
        // PROBE_TARGET=<bundle id> drives ANOTHER app already on screen (the Codename
        // One side of a side-by-side recording) at PROBE_TAB_POINTS "x:y,x:y,..."
        // (points, one per tab) instead of this probe's own tab bar.
        let target = env["PROBE_TARGET"] ?? ""
        let app = target.isEmpty ? XCUIApplication() : XCUIApplication(bundleIdentifier: target)
        var points: [CGPoint] = []
        if target.isEmpty {
            app.launchEnvironment["PROBE_APPEARANCE"] = env["PROBE_APPEARANCE"] ?? "light"
            app.launchEnvironment["PROBE_BACKDROP"] = env["PROBE_BACKDROP"] ?? "grey"
            app.launchEnvironment["PROBE_TABS"] = env["PROBE_TABS"] ?? "3"
            app.launchEnvironment["PROBE_SNAP"] = env["PROBE_SNAP"] ?? "0"
            app.launchEnvironment["PROBE_LOG"] = env["PROBE_LOG"] ?? "probe.log"
            app.launchEnvironment["PROBE_ICONS"] = env["PROBE_ICONS"] ?? ""
            app.launchEnvironment["PROBE_FILTERS"] = env["PROBE_FILTERS"] ?? "0"
            app.launchEnvironment["PROBE_TINT"] = env["PROBE_TINT"] ?? ""
            app.launchEnvironment["PROBE_SELECT"] = env["PROBE_SELECT"] ?? ""
            app.launch()
            sleep(3)
            let bar = app.tabBars.firstMatch
            XCTAssertTrue(bar.waitForExistence(timeout: 10))
            for i in 0..<bar.buttons.count {
                let f = bar.buttons.element(boundBy: i).frame
                points.append(CGPoint(x: f.midX, y: f.midY))
            }
        } else {
            app.activate()
            sleep(2)
            // The first touch after activating an app launched from outside does
            // not reach it; spend it on the empty backdrop above the bar.
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.4)).tap()
            sleep(2)
            for pair in (env["PROBE_TAB_POINTS"] ?? "").split(separator: ",") {
                let xy = pair.split(separator: ":")
                points.append(CGPoint(x: Double(xy[0])!, y: Double(xy[1])!))
            }
        }
        XCTAssertFalse(points.isEmpty, "no tab positions")
        let origin = app.coordinate(withNormalizedOffset: .zero)
        func at(_ p: CGPoint) -> XCUICoordinate { origin.withOffset(CGVector(dx: p.x, dy: p.y)) }
        // PROBE_SEQ, comma separated, two seconds between steps:
        //   N            tap tab N
        //   hN:MS        press and hold tab N for MS milliseconds
        //   dA-B:MS[:H]  press tab A, drag to tab B over MS milliseconds, hold H ms, release
        //   dA-B.F:MS:H  as above, but stop at fraction F of the way from A to B
        // PROBE_PERIOD_MS: start step k at k * period after the first, whatever the
        // app's idle waits cost, so two recordings share one gesture schedule.
        let period = Double(env["PROBE_PERIOD_MS"] ?? "") ?? 0
        let start = Date()
        for (k, step) in (env["PROBE_SEQ"] ?? "2,0,1,2,1,0").split(separator: ",").map(String.init).enumerated() {
            if period > 0 {
                let wait = start.addingTimeInterval(Double(k) * period / 1000).timeIntervalSinceNow
                if wait > 0 {
                    Thread.sleep(forTimeInterval: wait)
                }
            }
            if step.hasPrefix("h") {
                let p = step.dropFirst().split(separator: ":")
                at(points[Int(p[0])!]).press(forDuration: Double(p[1])! / 1000)
            } else if step.hasPrefix("d") {
                let p = step.dropFirst().split(separator: ":")
                let ends = p[0].split(separator: "-")
                let a = Int(ends[0])!
                let target = ends[1].split(separator: ".")
                let bi = Int(target[0])!
                let frac = target.count > 1 ? Double("0." + target[1])! : 1.0
                let ms = Double(p[1])!
                let hold = p.count > 2 ? Double(p[2])! / 1000 : 0.0
                let ea = points[a], eb = points[bi]
                let tx = ea.x + (eb.x - ea.x) * frac
                let velocity = XCUIGestureVelocity(abs(tx - ea.x) / CGFloat(ms / 1000))
                at(ea).press(forDuration: 0.3, thenDragTo: at(CGPoint(x: tx, y: ea.y)), withVelocity: velocity,
                             thenHoldForDuration: hold)
            } else {
                at(points[Int(step)!]).tap()
            }
            if period <= 0 {
                Thread.sleep(forTimeInterval: 2.0)
            }
        }
    }
}
