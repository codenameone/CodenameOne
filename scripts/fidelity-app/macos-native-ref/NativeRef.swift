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
// macOS (AppKit) native reference app for the Codename One fidelity suite.
//
// Direct counterpart to ios-native-ref/NativeRef.swift, and built the same way: one Swift
// file, no xcodeproj, a hand-written Info.plist (see scripts/build-macos-native-ref.sh).
//
// AppKit rather than SwiftUI. SwiftUI works in a -parse-as-library single file, but it
// needs an NSHostingView, adds a layout-timing indirection, and on macOS renders
// AppKit-derived controls anyway. AppKit gives direct control over isHighlighted /
// isEnabled / state and the natural intrinsicContentSize the tile contract depends on.
//
// Two things this app exists to find out, neither of which can be answered from the repo:
//
//   1. Can it capture at all? CGWindowListCreateImage is deprecated on macOS 14/15 and
//      gated behind the Screen Recording TCC permission, which cannot be granted on a
//      hosted runner. If it comes back nil or black, the vibrancy tiles cannot be captured
//      honestly and must be dropped from the set rather than committed wrong -- a missing
//      golden is honest, a blank one scores 0% forever and reads as a theme bug.
//   2. What does the runner's appearance actually look like? A headless Mac reports
//      backingScaleFactor 1.0, which is a configuration no real Mac has, so the capture
//      scale is forced rather than inherited.
import AppKit

let outDir = ProcessInfo.processInfo.environment["NATIVEREF_OUT"] ?? ""
let isProbe = (ProcessInfo.processInfo.environment["NATIVEREF_MODE"] ?? "probe") != "capture"
let goldenSet = ProcessInfo.processInfo.environment["CN1SS_FIDELITY_GOLDEN_SET"] ?? "macos-aqua"

// Every real Mac is 2x. The runner is very likely 1x, so the bitmap is built explicitly at
// this scale and AppKit is asked to render into it, exactly as the iOS reference forces
// CAPTURE_SCALE rather than inheriting the simulator's.
let CAPTURE_SCALE: CGFloat = 2.0

var blockers: [String] = []

func blocker(_ msg: String) { blockers.append(msg) }

func jsonEscape(_ s: String) -> String {
    s.replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "\"", with: "\\\"")
}

final class RefApp: NSObject, NSApplicationDelegate {
    var window: NSWindow!
    var button: NSButton!

    func applicationDidFinishLaunching(_ note: Notification) {
        // .regular so the app can actually become frontmost. An NSWindow that is not key
        // draws EVERY AppKit control in its inactive, greyed style -- the same class of
        // silent, uniform wrongness as a GTK window stuck in backdrop state.
        NSApp.setActivationPolicy(.regular)
        NSApp.appearance = NSAppearance(named: .aqua)

        let rect = NSRect(x: 0, y: 0, width: 480, height: 240)
        window = NSWindow(contentRect: rect,
                          styleMask: [.titled, .closable],
                          backing: .buffered,
                          defer: false)
        window.title = "cn1-native-ref"
        window.animationBehavior = .none

        button = NSButton(title: "Default", target: nil, action: nil)
        button.bezelStyle = .rounded
        button.sizeToFit()
        button.setFrameOrigin(NSPoint(x: 24, y: rect.height - button.frame.height - 24))
        window.contentView?.addSubview(button)

        window.makeKeyAndOrderFront(nil)
        window.orderFrontRegardless()
        NSAnimationContext.current.duration = 0

        // macOS 14 and later will not let an app steal focus from the frontmost one, so a
        // single activate() call is not enough on a machine where something else is in
        // front. On a CI runner nothing is competing and this succeeds on the first turn;
        // locally it will not, which is precisely the difference this app exists to
        // measure. Retried rather than called once so a slow session start is not mistaken
        // for a restriction.
        for _ in 0..<10 {
            NSApp.activate(ignoringOtherApps: true)
            window.makeKey()
            RunLoop.current.run(until: Date().addingTimeInterval(0.2))
            if window.isKeyWindow && NSApp.isActive { break }
        }

        // Let the first frame actually present before anything is read back.
        RunLoop.current.run(until: Date().addingTimeInterval(1.0))
        finish()
    }

    /// The honest capture: the composited window, which is the only way vibrancy and any
    /// behind-window material appear at all.
    func captureViaWindowList() -> NSImage? {
        let id = CGWindowID(window.windowNumber)
        guard let cg = CGWindowListCreateImage(.null,
                                               .optionIncludingWindow,
                                               id,
                                               [.boundsIgnoreFraming, .bestResolution]) else {
            return nil
        }
        return NSImage(cgImage: cg, size: .zero)
    }

    /// The fallback: renders every *drawn* AppKit control correctly and needs no
    /// permission, but cannot see an NSVisualEffectView. Good enough for buttons, fields,
    /// sliders and switches; not good enough for a vibrancy tile, which is why the manifest
    /// records which path produced the set.
    func captureViaCacheDisplay(_ view: NSView) -> NSImage? {
        let pt = view.bounds.size
        guard pt.width > 0, pt.height > 0 else { return nil }
        guard let rep = NSBitmapImageRep(bitmapDataPlanes: nil,
                                         pixelsWide: Int(pt.width * CAPTURE_SCALE),
                                         pixelsHigh: Int(pt.height * CAPTURE_SCALE),
                                         bitsPerSample: 8,
                                         samplesPerPixel: 4,
                                         hasAlpha: true,
                                         isPlanar: false,
                                         colorSpaceName: .calibratedRGB,
                                         bytesPerRow: 0,
                                         bitsPerPixel: 0) else { return nil }
        rep.size = pt
        view.cacheDisplay(in: view.bounds, to: rep)
        let img = NSImage(size: pt)
        img.addRepresentation(rep)
        return img
    }

    func isBlank(_ image: NSImage) -> Bool {
        guard let tiff = image.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff) else { return true }
        var seen = Set<UInt32>()
        let w = rep.pixelsWide, h = rep.pixelsHigh
        if w == 0 || h == 0 { return true }
        for y in stride(from: 0, to: h, by: max(1, h / 32)) {
            for x in stride(from: 0, to: w, by: max(1, w / 32)) {
                if let c = rep.colorAt(x: x, y: y) {
                    let k = (UInt32(c.redComponent * 255) << 16)
                        | (UInt32(c.greenComponent * 255) << 8)
                        | UInt32(c.blueComponent * 255)
                    seen.insert(k)
                    if seen.count > 1 { return false }
                }
            }
        }
        return true
    }

    func write(_ image: NSImage, _ name: String) {
        guard let tiff = image.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff),
              let png = rep.representation(using: .png, properties: [:]) else {
            blocker("\(name) could not be encoded to PNG")
            return
        }
        let path = (outDir as NSString).appendingPathComponent("\(name).png")
        do {
            try png.write(to: URL(fileURLWithPath: path))
            print("NATIVEREF:wrote \(name) \(rep.pixelsWide)x\(rep.pixelsHigh)")
        } catch {
            blocker("\(name) could not be written: \(error)")
        }
    }

    func finish() {
        // AppKit's active control appearance follows the application being active and the
        // window being key/main, and they can disagree -- so all three are recorded and the
        // blocker fires only when the render really would be the inactive one. A single
        // isKeyWindow test reports a problem on any developer machine that simply has
        // another app in front, which would make this check noise rather than a gate.
        if !(window.isKeyWindow && NSApp.isActive) {
            blocker("the app did not become active (key=\(window.isKeyWindow) "
                + "main=\(window.isMainWindow) appActive=\(NSApp.isActive)): every AppKit "
                + "control would be captured in its inactive, greyed style, making the "
                + "whole set wrong in one direction")
        }

        // Assert rather than write these. Turning them off needs a cfprefsd restart to take
        // effect reliably, so if a future runner image ships them ON you want to be told,
        // not to paper over it and capture a low-transparency, high-contrast reference.
        let ua = UserDefaults(suiteName: "com.apple.universalaccess")
        if ua?.bool(forKey: "reduceTransparency") == true {
            blocker("reduceTransparency is on: every material would render as a flat fill")
        }
        if ua?.bool(forKey: "increaseContrast") == true {
            blocker("increaseContrast is on: control borders and fills are not the defaults")
        }

        var captureMethod = "cgwindowlist"
        var image = captureViaWindowList()
        if image == nil || isBlank(image!) {
            // Screen Recording consent is almost certainly the reason. Not a blocker by
            // itself -- the fallback renders every drawn control correctly -- but it does
            // decide whether vibrancy tiles can exist in this set.
            print("NATIVEREF:WARN CGWindowListCreateImage returned "
                + (image == nil ? "nil" : "a blank image")
                + "; falling back to NSView.cacheDisplay (no vibrancy capture)")
            captureMethod = "cachedisplay"
            image = captureViaCacheDisplay(button)
        }

        if let img = image, !isBlank(img) {
            write(img, isProbe ? "probe_Button_normal_light" : "Button_normal_light")
        } else {
            blocker("both capture paths produced nothing usable")
        }

        writeManifest(captureMethod: captureMethod)

        for b in blockers { FileHandle.standardError.write("NATIVEREF:BLOCKER \(b)\n".data(using: .utf8)!) }
        print("NATIVEREF:DONE exit=\(blockers.isEmpty ? 0 : 20)")
        exit(blockers.isEmpty ? 0 : 20)
    }

    func writeManifest(captureMethod: String) {
        let screen = NSScreen.main
        let accent = NSColor.controlAccentColor.usingColorSpace(.sRGB)
        let highlight = NSColor.selectedContentBackgroundColor.usingColorSpace(.sRGB)
        func hex(_ c: NSColor?) -> String {
            guard let c = c else { return "unknown" }
            return String(format: "#%02X%02X%02X",
                          Int(c.redComponent * 255), Int(c.greenComponent * 255), Int(c.blueComponent * 255))
        }
        let os = ProcessInfo.processInfo.operatingSystemVersion
        let json = """
        {
          "schema": 1,
          "platform": "macos",
          "golden_set": "\(jsonEscape(goldenSet))",
          "mode": "\(isProbe ? "probe" : "capture")",
          "os": {
            "version": "\(os.majorVersion).\(os.minorVersion).\(os.patchVersion)",
            "build": "\(jsonEscape(ProcessInfo.processInfo.operatingSystemVersionString))"
          },
          "toolkit": {
            "name": "AppKit",
            "deployment": "unsigned-bundle"
          },
          "display": {
            "backing_scale_factor": \(screen?.backingScaleFactor ?? 0),
            "capture_scale": \(CAPTURE_SCALE),
            "screen_size": "\(Int(screen?.frame.width ?? 0))x\(Int(screen?.frame.height ?? 0))"
          },
          "window": {
            "key": \(window.isKeyWindow),
            "main": \(window.isMainWindow),
            "app_active": \(NSApp.isActive)
          },
          "appearance": {
            "effective": "\(jsonEscape(NSApp.effectiveAppearance.name.rawValue))",
            "accent_color": "\(hex(accent))",
            "highlight_color": "\(hex(highlight))",
            "reduce_transparency": \(UserDefaults(suiteName: "com.apple.universalaccess")?.bool(forKey: "reduceTransparency") ?? false),
            "increase_contrast": \(UserDefaults(suiteName: "com.apple.universalaccess")?.bool(forKey: "increaseContrast") ?? false)
          },
          "capture": {
            "method": "\(captureMethod)",
            "vibrancy_capturable": \(captureMethod == "cgwindowlist")
          },
          "blockers": [\(blockers.map { "\"\(jsonEscape($0))\"" }.joined(separator: ", "))]
        }
        """
        let path = (outDir as NSString).appendingPathComponent("capture-manifest.json")
        try? (json + "\n").write(toFile: path, atomically: true, encoding: .utf8)
        print("NATIVEREF:INFO wrote \(path)")
    }
}

// -parse-as-library forbids top-level expressions, so the entry point is explicit. The
// delegate is held in a static: NSApplication.delegate is a weak reference, and a locally
// scoped delegate is deallocated before applicationDidFinishLaunching ever fires.
@main
struct NativeRefMain {
    static let delegate = RefApp()

    static func main() {
        let app = NSApplication.shared
        app.delegate = delegate
        app.run()
    }
}
