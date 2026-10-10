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

// Waits for a process to put its first window on screen and prints the wall
// clock time at which it was seen, as seconds since the epoch.
//
//   firstwindow <pid> <timeout-seconds>
//
// The macOS half of the desktop-compat benchmarks (run-macos-bench.sh) uses it
// to time start-up without touching either application: the window list is read
// from the window server, which needs no permission for bounds and owner. It
// answers "a window of at least 100 x 100 points is on screen", not "its first
// frame is painted" -- reading pixels would need the Screen Recording
// permission -- so macOS start-up figures are the earlier of the two events the
// Linux leg reports.
//
// Exit status: 0 when a window appeared, 1 on timeout, 2 on bad arguments.

import CoreGraphics
import Foundation

let arguments = CommandLine.arguments
guard arguments.count == 3, let pid = Int(arguments[1]), let timeout = Double(arguments[2]) else {
    FileHandle.standardError.write("usage: firstwindow <pid> <timeout-seconds>\n".data(using: .utf8)!)
    exit(2)
}

let deadline = Date().addingTimeInterval(timeout)
while Date() < deadline {
    let options: CGWindowListOption = [.optionOnScreenOnly, .excludeDesktopElements]
    if let windows = CGWindowListCopyWindowInfo(options, kCGNullWindowID) as? [[String: Any]] {
        for window in windows {
            guard let owner = window[kCGWindowOwnerPID as String] as? Int, owner == pid,
                  let layer = window[kCGWindowLayer as String] as? Int, layer == 0,
                  let bounds = window[kCGWindowBounds as String] as? [String: Any],
                  let width = bounds["Width"] as? Double, let height = bounds["Height"] as? Double,
                  width >= 100, height >= 100 else {
                continue
            }
            print(String(format: "%.4f", Date().timeIntervalSince1970))
            exit(0)
        }
    }
    usleep(4000)
}
exit(1)
