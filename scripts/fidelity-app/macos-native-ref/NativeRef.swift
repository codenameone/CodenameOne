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
// CAPTURE IS 1x, DELIBERATELY, and this is the one setting most likely to look wrong.
// Every real Mac is 2x and the iOS reference forces 2x for exactly that reason. The
// desktop tiles are different: they are specified in LOGICAL pixels (1/96 inch), the CN1
// side renders them at 1x, and the comparator overlays the two 1:1 after cropping to their
// common top-left region. A 2x native tile would therefore be compared against the CN1
// tile's top-left QUARTER, at double scale, and score near zero for a reason no one would
// find by looking at the widget.
//
// HOVER ON macOS IS THE SAME RENDER AS NORMAL, and that is a finding rather than a gap.
// AppKit exposes no rollover state for push buttons, fields, sliders, switches or popups
// (`showsBorderOnlyWhileMouseInside` is a different feature on a different bezel style).
// So the hover tiles are captured from an untouched control and the manifest records
// hover_supported: false. The CN1 desktop themes must therefore leave Aqua's hover styling
// equal to normal -- and because these goldens say so, the gate now enforces that rather
// than leaving it to whoever writes the CSS.
import AppKit

let outDir = ProcessInfo.processInfo.environment["NATIVEREF_OUT"] ?? ""
let isProbe = (ProcessInfo.processInfo.environment["NATIVEREF_MODE"] ?? "probe") != "capture"
let goldenSet = ProcessInfo.processInfo.environment["CN1SS_FIDELITY_GOLDEN_SET"] ?? "macos-aqua"

/// Logical pixels; see the note above. Must equal the CN1 tile renderer's scale.
let CAPTURE_SCALE: CGFloat = 1.0

/// The tile the widget is anchored top-left in. Mirrors tile_width_px / tile_height_px in
/// fidelity-tests.yaml; if those change, this must change with them.
let TILE_W: CGFloat = 240
let TILE_H: CGFloat = 56

var blockers: [String] = []

func blocker(_ msg: String) { blockers.append(msg) }

func jsonEscape(_ s: String) -> String {
    s.replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "\"", with: "\\\"")
}

/// The tile surface.
///
/// It draws windowBackgroundColor in draw(_:) rather than assigning
/// `NSColor.windowBackgroundColor.cgColor` to a layer, and the difference is not stylistic.
/// A dynamic NSColor resolves against `NSAppearance.current`, which is only set inside a
/// drawing context. Read as `.cgColor` from ordinary code it resolves against whatever the
/// MACHINE is set to -- so on a Mac in Dark Mode every "light" tile was captured with a
/// dark backdrop, and the capture still reported 60 tiles and zero blockers.
final class TileView: NSView {
    override var isFlipped: Bool { true }
    override func draw(_ dirtyRect: NSRect) {
        NSColor.windowBackgroundColor.setFill()
        dirtyRect.fill()
    }
}

/// The strip a window shows where its title bar is, for the DesktopToolbar row.
///
/// NSToolbar belongs to a window and cannot be rendered into a view, so the reference is the
/// surface the toolbar sits on plus the window title -- which is what the CN1 Toolbar UIID
/// draws, and what comparing against a detached NSToolbar would NOT be.
///
/// Drawn rather than layer-backed, like TileView and for the same reason: a CGColor taken
/// from a dynamic NSColor freezes at the appearance it was read in.
final class TitleBarStripView: NSView {
    override var isFlipped: Bool { true }
    override func draw(_ dirtyRect: NSRect) {
        NSColor.windowBackgroundColor.setFill()
        dirtyRect.fill()
    }
}

/// One row of the desktop matrix. `kind` is the native_mac key in fidelity-tests.yaml, and
/// the ids and states are that file's too: the two lists must agree or the comparator pairs
/// a CN1 render against nothing.
struct Spec {
    let id: String
    let kind: String
    let states: [String]
}

let SPECS: [Spec] = [
    Spec(id: "DesktopButton", kind: "appkit_push_button", states: ["normal", "hover", "pressed", "disabled"]),
    Spec(id: "DesktopAccentButton", kind: "appkit_push_button_default", states: ["normal", "hover", "pressed", "disabled"]),
    Spec(id: "DesktopTextField", kind: "appkit_textfield", states: ["normal", "hover", "disabled"]),
    Spec(id: "DesktopCheckBox", kind: "appkit_checkbox", states: ["normal", "selected", "hover", "disabled"]),
    Spec(id: "DesktopRadioButton", kind: "appkit_radio", states: ["normal", "selected", "hover", "disabled"]),
    Spec(id: "DesktopSwitch", kind: "appkit_switch", states: ["normal", "selected", "hover", "disabled"]),
    Spec(id: "DesktopSlider", kind: "appkit_slider", states: ["normal", "hover", "disabled"]),
    Spec(id: "DesktopProgressBar", kind: "appkit_progress", states: ["normal"]),
    Spec(id: "DesktopComboBox", kind: "appkit_popupbutton", states: ["normal", "hover", "disabled"]),

    // Second wave. No menu bar and no tooltip row here: NSMenu and an AppKit tooltip are
    // window-server surfaces, invisible to cacheDisplay, which is the capture path that needs
    // no Screen Recording consent. Those rows carry a platforms: list in the spec rather than
    // a blank golden that would score 0% forever and read as a theme bug -- the same call
    // already made for Aqua vibrancy.
    Spec(id: "DesktopSeparator", kind: "appkit_box_separator", states: ["normal"]),
    Spec(id: "DesktopGroupBox", kind: "appkit_box_titled", states: ["normal"]),
    Spec(id: "DesktopStepper", kind: "appkit_stepper", states: ["normal", "disabled"]),
    Spec(id: "DesktopLinkButton", kind: "appkit_link_button", states: ["normal", "hover", "disabled"]),
    Spec(id: "DesktopSearchField", kind: "appkit_searchfield", states: ["normal", "disabled"]),
    Spec(id: "DesktopListRow", kind: "appkit_tableview_row", states: ["normal", "selected", "hover"]),
    Spec(id: "DesktopTabs", kind: "appkit_tabview", states: ["normal"]),
    Spec(id: "DesktopToolbar", kind: "appkit_toolbar", states: ["normal"]),
    Spec(id: "DesktopDisclosure", kind: "appkit_disclosure", states: ["normal"]),
]

/// Controls that own the full tile width rather than sizing to their content. A slider, a
/// progress bar and a text field have no natural width -- AppKit gives each whatever it is
/// asked for -- so the tile width is the honest answer, and it is the same rule the CN1
/// renderer applies. Left to size themselves, a text field measures to its placeholder
/// (39px for "Text"), which is not a control anyone would recognise or ship.
let FULL_WIDTH_KINDS: Set<String> = [
    "appkit_slider", "appkit_progress", "appkit_textfield",
    // Second wave, same rule: none of these has a natural width either. A separator
    // measures to nothing at all, a search field to its placeholder, and a row, a box, a
    // tab view and a toolbar are all containers that take the width they are given.
    "appkit_box_separator", "appkit_searchfield", "appkit_tableview_row",
    "appkit_box_titled", "appkit_tabview", "appkit_toolbar",
]

/// Controls that own the full tile HEIGHT rather than sizing to their content.
///
/// A group box is a frame around other things, so its height is whatever it is given -- left
/// to measure itself it collapses onto its own title and draws no frame at all, which is a
/// heading, not a group box. The CN1 side applies the same rule through
/// DesktopTileRunner.FULL_HEIGHT_IDS, and the two lists are kept in step by hand exactly as
/// the full-width ones are.
let FULL_HEIGHT_KINDS: Set<String> = ["appkit_box_titled"]

final class RefApp: NSObject, NSApplicationDelegate {
    var window: NSWindow!
    var host: NSView!
    var written = 0
    /// Backdrop colour sampled from each appearance's tiles. See assertAppearancesDiffer().
    var backdropByAppearance: [String: String] = [:]

    /// Hash of each "<id>_normal_<appearance>" tile, and the states that came out
    /// identical to it. Reported in the manifest the way the Windows and GNOME references
    /// report theirs.
    ///
    /// On this platform that list is long and expected: AppKit draws no rollover state for
    /// any control in this matrix, so every hover tile matches its normal one. Saying so
    /// per tile is the point -- "the theme may leave hover equal here" is a claim a theme
    /// author should be able to check rather than take on trust from a comment.
    var normalHashes: [String: String] = [:]
    var identicalToNormal: [String] = []

    func applicationDidFinishLaunching(_ note: Notification) {
        // .regular so the app can actually become frontmost. An NSWindow that is not key
        // draws EVERY AppKit control in its inactive, greyed style -- the same class of
        // silent, uniform wrongness as a GTK window stuck in backdrop state.
        NSApp.setActivationPolicy(.regular)

        let rect = NSRect(x: 0, y: 0, width: TILE_W, height: TILE_H)
        window = NSWindow(contentRect: rect,
                          styleMask: [.titled, .closable],
                          backing: .buffered,
                          defer: false)
        window.title = "cn1-native-ref"
        window.animationBehavior = .none

        host = NSView(frame: rect)
        window.contentView = host

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

    // MARK: widget construction

    func makeWidget(_ kind: String) -> NSView? {
        switch kind {
        case "appkit_push_button":
            let b = NSButton(title: "Button", target: nil, action: nil)
            b.bezelStyle = .rounded
            return b
        case "appkit_push_button_default":
            let b = NSButton(title: "Button", target: nil, action: nil)
            b.bezelStyle = .rounded
            // The accent-filled button on macOS is the DEFAULT button, and the only
            // supported way to make one is to give it the return key. Setting bezelColor
            // instead produces a tinted button that is not what the system draws.
            b.keyEquivalent = "\r"
            return b
        case "appkit_textfield":
            let t = NSTextField(string: "Text")
            t.isEditable = true
            t.isBezeled = true
            t.bezelStyle = .roundedBezel
            return t
        case "appkit_checkbox":
            return NSButton(checkboxWithTitle: "Check", target: nil, action: nil)
        case "appkit_radio":
            return NSButton(radioButtonWithTitle: "Radio", target: nil, action: nil)
        case "appkit_switch":
            return NSSwitch()
        case "appkit_slider":
            let s = NSSlider(value: 0.5, minValue: 0, maxValue: 1, target: nil, action: nil)
            s.isContinuous = true
            return s
        case "appkit_popupbutton":
            let pop = NSPopUpButton(frame: .zero, pullsDown: false)
            pop.addItem(withTitle: "Option")
            return pop
        case "appkit_box_separator":
            let box = NSBox()
            box.boxType = .separator
            return box
        case "appkit_box_titled":
            // The label goes INSIDE the default content view. Assigning it AS the content view
            // replaces the view the box draws its frame around, so the frame disappeared and
            // the label was clipped by a box that had sized itself to nothing.
            let box = NSBox(frame: NSRect(x: 0, y: 0, width: TILE_W, height: TILE_H))
            box.title = "Group"
            box.titlePosition = .atTop
            box.boxType = .primary
            let body = NSTextField(labelWithString: "Item")
            body.sizeToFit()
            body.setFrameOrigin(NSPoint(x: 4, y: 4))
            box.contentView?.addSubview(body)
            return box
        case "appkit_stepper":
            // The NSStepper alone is the two chevrons; the number beside it is a separate
            // field, and the CN1 Stepper is the pair. Built as the pair so the two sides
            // compare the same control rather than half of one.
            //
            // A plain container with explicit frames, not an NSStackView: a stack view's
            // fittingSize came back with no width, so the tile showed the chevrons and no
            // field at all -- half a control, which is exactly what this pairing exists to
            // avoid.
            let field = NSTextField(string: "1")
            field.isBezeled = true
            field.bezelStyle = .roundedBezel
            field.sizeToFit()
            field.setFrameSize(NSSize(width: max(field.frame.width, 48),
                                      height: field.frame.height))
            let stepper = NSStepper()
            stepper.minValue = 0
            stepper.maxValue = 10
            stepper.doubleValue = 1
            stepper.sizeToFit()
            let h = max(field.frame.height, stepper.frame.height)
            let row = NSView(frame: NSRect(x: 0, y: 0,
                                           width: field.frame.width + 2 + stepper.frame.width,
                                           height: h))
            field.setFrameOrigin(NSPoint(x: 0, y: (h - field.frame.height) / 2))
            stepper.setFrameOrigin(NSPoint(x: field.frame.width + 2,
                                           y: (h - stepper.frame.height) / 2))
            row.addSubview(field)
            row.addSubview(stepper)
            return row
        case "appkit_link_button":
            // NSButton's own link style, not a text field with an attributed string: the
            // latter is what an application writes when the platform has no link control,
            // and AppKit has one.
            let b = NSButton(title: "Link", target: nil, action: nil)
            b.isBordered = false
            b.contentTintColor = .linkColor
            b.attributedTitle = NSAttributedString(
                string: "Link",
                attributes: [.foregroundColor: NSColor.linkColor,
                             .underlineStyle: NSUnderlineStyle.single.rawValue])
            return b
        case "appkit_searchfield":
            let f = NSSearchField(string: "Search")
            f.isEditable = true
            return f
        case "appkit_tableview_row":
            // A row view with a cell in it, which is what a single NSTableView row draws.
            //
            // The frame is explicit because NSTableRowView has no intrinsic size in either
            // axis -- measured: it laid out to 240x0 and produced no image at all, which the
            // zero-size blocker caught. 24pt is the standard NSTableView row height, which is
            // what a table would have given it.
            let rowHeight: CGFloat = 24
            let row = NSTableRowView(frame: NSRect(x: 0, y: 0, width: TILE_W, height: rowHeight))
            let label = NSTextField(labelWithString: "Row")
            label.sizeToFit()
            label.setFrameOrigin(NSPoint(x: 4, y: (rowHeight - label.frame.height) / 2))
            row.addSubview(label)
            return row
        case "appkit_tabview":
            let tv = NSTabView()
            let one = NSTabViewItem(identifier: "one")
            one.label = "One"
            let two = NSTabViewItem(identifier: "two")
            two.label = "Two"
            tv.addTabViewItem(one)
            tv.addTabViewItem(two)
            return tv
        case "appkit_toolbar":
            // NSToolbar belongs to a window and cannot be rendered into a view, so the
            // reference is the strip a window shows in its place: the title bar's own
            // background with the window title on it. That is what the CN1 Toolbar UIID
            // draws, and comparing it against a detached NSToolbar would compare two
            // different things.
            // The fill is DRAWN, not assigned to a layer. A CGColor taken from a dynamic
            // NSColor is resolved once, at whatever appearance was in force when it was read,
            // so the light tile came out with the dark window background painted across it.
            // TileView draws its own fill for exactly this reason.
            let strip = TitleBarStripView(frame: NSRect(x: 0, y: 0, width: TILE_W, height: TILE_H))
            let title = NSTextField(labelWithString: "Title")
            title.font = NSFont.titleBarFont(ofSize: NSFont.systemFontSize)
            title.sizeToFit()
            title.setFrameOrigin(NSPoint(
                x: (TILE_W - title.frame.width) / 2,
                y: (TILE_H - title.frame.height) / 2))
            strip.addSubview(title)
            return strip
        case "appkit_disclosure":
            // The triangle AND its label. AppKit's .disclosure bezel draws the triangle only
            // and ignores the title outright -- measured: the tile came out as a bare chevron
            // with no text, against a CN1 accordion header that is a labelled row. A titled
            // disclosure on macOS is the triangle with a label beside it, which is what a
            // sidebar or an inspector section actually shows.
            let triangle = NSButton(title: "", target: nil, action: nil)
            triangle.setButtonType(.pushOnPushOff)
            triangle.bezelStyle = .disclosure
            triangle.sizeToFit()
            let label = NSTextField(labelWithString: "Details")
            label.sizeToFit()
            let h = max(triangle.frame.height, label.frame.height)
            let row = NSView(frame: NSRect(x: 0, y: 0,
                                           width: triangle.frame.width + 4 + label.frame.width,
                                           height: h))
            triangle.setFrameOrigin(NSPoint(x: 0, y: (h - triangle.frame.height) / 2))
            label.setFrameOrigin(NSPoint(x: triangle.frame.width + 4,
                                         y: (h - label.frame.height) / 2))
            row.addSubview(triangle)
            row.addSubview(label)
            return row
        case "appkit_progress":
            let p = NSProgressIndicator()
            p.style = .bar
            p.isIndeterminate = false
            p.minValue = 0
            p.maxValue = 1
            p.doubleValue = 0.6
            // An animating bar is a different pixel every frame. The suite has no tolerance
            // file by design, so anything that moves has to be stopped rather than averaged.
            p.usesThreadedAnimation = false
            p.stopAnimation(nil)
            return p
        default:
            blocker("unknown native_mac kind '\(kind)'")
            return nil
        }
    }

    /// Applies one state. Returns false when the state cannot be expressed, which is a
    /// reason to skip the tile rather than to write a mislabelled one.
    func applyState(_ view: NSView, _ state: String, _ kind: String) -> Bool {
        switch state {
        case "normal":
            return true
        case "hover":
            // Deliberately a no-op: see the file header. AppKit draws no rollover state for
            // any control in this matrix, so the honest hover reference IS the normal one.
            return true
        case "pressed":
            guard let b = view as? NSButton else { return false }
            b.isHighlighted = true
            return true
        case "selected":
            if let sw = view as? NSSwitch { sw.state = .on; return true }
            if let row = view as? NSTableRowView { row.isSelected = true; return true }
            if let b = view as? NSButton { b.state = .on; return true }
            // A composite: the disclosure is a triangle plus a label, and the state belongs
            // to the triangle. Recursed rather than special-cased by kind, because the state
            // is always a property of one control inside the composite and the alternative is
            // a second table mapping kinds to which subview to reach for.
            for sub in view.subviews where applyState(sub, state, kind) {
                _ = sub
                return true
            }
            return false
        case "disabled":
            if let c = view as? NSControl { c.isEnabled = false; return true }
            // The stepper is a field plus a stepper and BOTH halves have to grey out; unlike
            // selected, this is not one control's state, so it does not stop at the first.
            var reached = false
            for sub in view.subviews {
                if applyState(sub, state, kind) { reached = true }
            }
            return reached
        default:
            blocker("unknown state '\(state)'")
            return false
        }
    }

    // MARK: capture

    /// Lays one widget out top-left in a tile-sized view on the window's own surface and
    /// renders it. Returns nil when the state could not be applied.
    ///
    /// NSView.cacheDisplay rather than CGWindowListCreateImage: it renders every DRAWN
    /// AppKit control correctly, needs no Screen Recording consent (which cannot be granted
    /// on a hosted runner at all), and the desktop matrix contains no vibrancy tile, which
    /// is the one thing it cannot see. Aqua vibrancy is recorded as out of scope in
    /// native-themes/COVERAGE.md rather than captured wrong.
    func renderTile(_ spec: Spec, _ state: String) -> NSImage? {
        guard let widget = makeWidget(spec.kind) else { return nil }
        // The tile surface is the window background, which is what the CN1 side paints its
        // tiles on. Read from the system rather than written down, so a macOS release that
        // retunes windowBackgroundColor moves both sides together. See TileView for why it
        // is drawn rather than assigned to a layer.
        let tile = TileView(frame: NSRect(x: 0, y: 0, width: TILE_W, height: TILE_H))

        if !applyState(widget, state, spec.kind) {
            return nil
        }
        // fittingSize, not sizeToFit: the latter is NSControl's, and NSSwitch and
        // NSProgressIndicator are not NSControls.
        if let control = widget as? NSControl {
            control.sizeToFit()
        }
        // The two dimensions are resolved SEPARATELY, because AppKit routinely answers one
        // and not the other. NSProgressIndicator measures at (0.0, 20.0) fitting and
        // (-1.0, 20.0) intrinsic -- it has a real height and genuinely no natural width,
        // NSView.noIntrinsicMetric being -1. Testing them together threw the good height
        // away with the missing width and laid the bar out at zero height, so it was
        // dropped from the set as "produced no image".
        var size = widget.fittingSize
        if size.height <= 0 { size.height = widget.intrinsicContentSize.height }
        if size.height <= 0 { size.height = widget.frame.height }
        if FULL_HEIGHT_KINDS.contains(spec.kind) {
            size.height = TILE_H
        }
        if FULL_WIDTH_KINDS.contains(spec.kind) {
            size.width = TILE_W
        } else {
            if size.width <= 0 { size.width = widget.intrinsicContentSize.width }
            if size.width <= 0 { size.width = widget.frame.width }
        }
        if size.width <= 0 || size.height <= 0 {
            blocker("\(spec.id) \(state) laid out to \(size.width)x\(size.height)")
            return nil
        }
        // TileView is flipped, so its origin is already top-left and matches the tile
        // contract directly rather than through a height subtraction.
        widget.setFrameSize(size)
        widget.setFrameOrigin(NSPoint(x: 0, y: 0))
        tile.addSubview(widget)

        // In the window, not detached: an AppKit control renders in its inactive style
        // unless it belongs to the key window, and a detached view has no window at all.
        host.subviews.forEach { $0.removeFromSuperview() }
        host.addSubview(tile)
        tile.layoutSubtreeIfNeeded()
        // Let the state land before it is read back. isHighlighted in particular is applied
        // through the cell and is not visible in the same turn of the run loop.
        RunLoop.current.run(until: Date().addingTimeInterval(0.05))

        guard let rep = NSBitmapImageRep(bitmapDataPlanes: nil,
                                         pixelsWide: Int(TILE_W * CAPTURE_SCALE),
                                         pixelsHigh: Int(TILE_H * CAPTURE_SCALE),
                                         bitsPerSample: 8,
                                         samplesPerPixel: 4,
                                         hasAlpha: true,
                                         isPlanar: false,
                                         colorSpaceName: .calibratedRGB,
                                         bytesPerRow: 0,
                                         bitsPerPixel: 0) else {
            blocker("\(spec.id) \(state): could not allocate the tile bitmap")
            return nil
        }
        rep.size = NSSize(width: TILE_W, height: TILE_H)
        tile.cacheDisplay(in: tile.bounds, to: rep)
        let img = NSImage(size: rep.size)
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
            written += 1
            print("NATIVEREF:wrote \(name) \(rep.pixelsWide)x\(rep.pixelsHigh)")
            // Bottom-right corner: every widget in the matrix anchors top-left and none is
            // as tall as the tile, so this pixel is always backdrop.
            if let c = rep.colorAt(x: rep.pixelsWide - 1, y: rep.pixelsHigh - 1),
               let appearance = name.split(separator: "_").last {
                backdropByAppearance[String(appearance)] = String(format: "#%02X%02X%02X",
                    Int(c.redComponent * 255), Int(c.greenComponent * 255), Int(c.blueComponent * 255))
            }
            noteIfIdenticalToNormal(name, png)
        } catch {
            blocker("\(name) could not be written: \(error)")
        }
    }

    /// Captures the whole matrix for one appearance.
    func captureAppearance(_ appearance: String) {
        let named: NSAppearance.Name = appearance == "dark" ? .darkAqua : .aqua
        let appAppearance = NSAppearance(named: named)
        NSApp.appearance = appAppearance
        // The WINDOW too. NSApp.appearance is only the fallback for windows that do not
        // declare their own, and the controls resolve theirs from the window they are in.
        window.appearance = appAppearance
        // The appearance change has to propagate through the view tree before anything is
        // rendered; without this the first tile of a dark pass comes out light.
        RunLoop.current.run(until: Date().addingTimeInterval(0.3))
        for spec in SPECS {
            for state in spec.states {
                let name = "\(spec.id)_\(state)_\(appearance)"
                guard let img = renderTile(spec, state) else {
                    blocker("\(name) produced no image")
                    continue
                }
                if isBlank(img) {
                    blocker("\(name) rendered blank")
                    continue
                }
                write(img, name)
            }
        }
    }

    /// Records whether a state tile is byte-identical to its own normal tile.
    func noteIfIdenticalToNormal(_ name: String, _ png: Data) {
        let parts = name.split(separator: "_").map(String.init)
        guard parts.count == 3 else { return }
        let (id, state, appearance) = (parts[0], parts[1], parts[2])
        let key = "\(id)_\(appearance)"
        // FNV-1a rather than CryptoKit: this needs to tell "same bytes" from "different
        // bytes" and nothing more, and it keeps the file free of another import.
        var hash: UInt64 = 0xcbf29ce484222325
        for byte in png {
            hash ^= UInt64(byte)
            hash = hash &* 0x100000001b3
        }
        let digest = String(hash, radix: 16)
        if state == "normal" {
            normalHashes[key] = digest
            return
        }
        if normalHashes[key] == digest {
            identicalToNormal.append(name)
            print("NATIVEREF:INFO \(name) is identical to its normal tile; AppKit does not "
                + "restyle this control for this state")
        }
    }

    /// Fails the run when the light and dark passes were captured on the same backdrop.
    ///
    /// This is here because it happened. On this file it was NSColor.windowBackgroundColor
    /// read as `.cgColor`, which resolves against NSAppearance.current rather than the
    /// appearance being set, so on a Mac in Dark Mode the whole light pass came out dark.
    /// On the Windows reference it was the same shape of mistake in a different API. Both
    /// produced a full tile count and zero blockers.
    ///
    /// Nothing downstream catches it: the CN1 side renders its tiles on the real surface
    /// for each appearance, so the pair simply scores badly and reads as a theme that needs
    /// work rather than as a reference that was captured wrong.
    func assertAppearancesDiffer() {
        for (appearance, colour) in backdropByAppearance.sorted(by: { $0.key < $1.key }) {
            print("NATIVEREF:INFO \(appearance) backdrop \(colour)")
        }
        if Set(backdropByAppearance.values).count == 1, let only = backdropByAppearance.values.first {
            blocker("the light and dark passes were both captured on backdrop \(only): the "
                + "appearance did not actually change, so half the set is mislabelled")
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

        if isProbe {
            // One tile is enough to answer "can this environment render a control at all",
            // and it is prefixed so it can never be mistaken for a golden.
            if let img = renderTile(SPECS[0], "normal"), !isBlank(img) {
                write(img, "probe_DesktopButton_normal_light")
            } else {
                blocker("the probe tile produced nothing usable")
            }
        } else {
            captureAppearance("light")
            captureAppearance("dark")
            assertAppearancesDiffer()
            let expected = SPECS.reduce(0) { $0 + $1.states.count } * 2
            if written != expected {
                blocker("wrote \(written) tiles, expected \(expected): a partial set would be "
                    + "committed as if it were the whole matrix")
            }
        }

        writeManifest(captureMethod: "cachedisplay")

        for b in blockers { FileHandle.standardError.write("NATIVEREF:BLOCKER \(b)\n".data(using: .utf8)!) }
        print("NATIVEREF:DONE tiles=\(written) exit=\(blockers.isEmpty ? 0 : 20)")
        // Explicit flush. Launched through `open --stdout <file>`, stdout is a FILE, so it
        // is block buffered rather than line buffered, and the build script reads the exit
        // status back out of that last line. exit() does flush stdio, but the verdict line
        // is the one thing the whole run is judged on and a macOS runner slot costs over an
        // hour of queueing, so it is not left to inference.
        fflush(stdout)
        exit(blockers.isEmpty ? 0 : 20)
    }

    func writeManifest(captureMethod: String) {
        let screen = NSScreen.main
        let accent = NSColor.controlAccentColor.usingColorSpace(.sRGB)
        let highlight = NSColor.selectedContentBackgroundColor.usingColorSpace(.sRGB)
        let windowBg = NSColor.windowBackgroundColor.usingColorSpace(.sRGB)
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
          "tiles_written": \(written),
          "backdrop_by_appearance": {\(backdropByAppearance.sorted(by: { $0.key < $1.key }).map { "\"\($0.key)\": \"\($0.value)\"" }.joined(separator: ", "))},
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
            "tile_size": "\(Int(TILE_W))x\(Int(TILE_H))",
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
            "window_background": "\(hex(windowBg))",
            "reduce_transparency": \(UserDefaults(suiteName: "com.apple.universalaccess")?.bool(forKey: "reduceTransparency") ?? false),
            "increase_contrast": \(UserDefaults(suiteName: "com.apple.universalaccess")?.bool(forKey: "increaseContrast") ?? false)
          },
          "states_identical_to_normal": [\(identicalToNormal.map { "\"\($0)\"" }.joined(separator: ", "))],
          "capture": {
            "method": "\(captureMethod)",
            "vibrancy_capturable": false,
            "hover_supported": false
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
