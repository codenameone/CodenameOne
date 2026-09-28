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

// TabProbe: an instrumented native iOS tab bar -- the measuring instrument behind
// com.codename1.ui.TabGlassMotion and GlassRecipe.liquidPill27.
//
// A real UITabBarController (the only host UIKit gives the full Liquid Glass
// selection motion) over a known backdrop. For every display frame it logs the
// presentation geometry of every layer under the tab bar -- frame in window
// coordinates, bounds, position, transform, opacity, filters -- and every
// CAAnimation UIKit attaches, plus touch timestamps. UIKit drives this motion by
// writing layer values each frame rather than through CAAnimations, so the frame
// log IS the motion. Output: Documents/<PROBE_LOG> (default probe.log), one record
// per line; scripts/fidelity-app/tools/tab-motion/tabmotion.py reads it.
//
// Environment: PROBE_APPEARANCE light|dark, PROBE_BACKDROP grey|stripes|photo,
// PROBE_TABS 3..5, PROBE_LOG file name. See ../motion-probe/README.md.
import UIKit

var LOG: FileHandle?
var PROBE: Probe?
func out(_ s: String) {
    if let h = LOG { h.write((s + "\n").data(using: .utf8)!) }
}

final class ProbeWindow: UIWindow {
    override func sendEvent(_ event: UIEvent) {
        if let ts = event.allTouches {
            for t in ts {
                let p = t.location(in: self)
                out(String(format: "TOUCH t=%.5f phase=%d x=%.2f y=%.2f", CACurrentMediaTime(), t.phase.rawValue, p.x, p.y))
                if t.phase == .began {
                    PROBE?.touchDown(CACurrentMediaTime())
                }
            }
        }
        super.sendEvent(event)
    }
}

func describeValue(_ v: Any?) -> String {
    guard let v = v else { return "nil" }
    if let n = v as? NSValue {
        let type = String(cString: n.objCType)
        if type.contains("CGRect") { return NSCoder.string(for: n.cgRectValue) }
        if type.contains("CGPoint") { return NSCoder.string(for: n.cgPointValue) }
        if type.contains("CGSize") { return NSCoder.string(for: n.cgSizeValue) }
        if type.contains("CATransform3D") {
            let t = n.caTransform3DValue
            return String(format: "T[%.4f %.4f %.4f %.4f | %.3f %.3f]", t.m11, t.m12, t.m21, t.m22, t.m41, t.m42)
        }
        if let num = v as? NSNumber { return num.stringValue }
        return "\(n)"
    }
    if let a = v as? [Any] { return "[" + a.map { describeValue($0) }.joined(separator: ", ") + "]" }
    let s = "\(v)"
    return s.replacingOccurrences(of: "\n", with: " ")
}

func describeAnim(_ a: CAAnimation) -> String {
    var s = "class=\(type(of: a)) dur=\(a.duration) begin=\(a.beginTime) speed=\(a.speed) timeOffset=\(a.timeOffset) fill=\(a.fillMode.rawValue) additive="
    if let p = a as? CAPropertyAnimation {
        s += "\(p.isAdditive) keyPath=\(p.keyPath ?? "nil")"
        if let vf = p.valueFunction { s += " valueFunction=\(vf)" }
    }
    if let tf = a.timingFunction {
        var c1: [Float] = [0, 0], c2: [Float] = [0, 0]
        tf.getControlPoint(at: 1, values: &c1)
        tf.getControlPoint(at: 2, values: &c2)
        s += String(format: " tf=(%.3f,%.3f,%.3f,%.3f)", c1[0], c1[1], c2[0], c2[1])
    }
    if let sp = a as? CASpringAnimation {
        s += " mass=\(sp.mass) stiffness=\(sp.stiffness) damping=\(sp.damping) v0=\(sp.initialVelocity) settle=\(sp.settlingDuration)"
        if #available(iOS 17.0, *) {
            s += " perceptualDuration=\(sp.perceptualDuration) bounce=\(sp.bounce)"
        }
    }
    if let b = a as? CABasicAnimation {
        s += " from=\(describeValue(b.fromValue)) to=\(describeValue(b.toValue)) by=\(describeValue(b.byValue))"
    }
    if let k = a as? CAKeyframeAnimation {
        s += " values=\(describeValue(k.values)) keyTimes=\(describeValue(k.keyTimes)) calc=\(k.calculationMode.rawValue)"
    }
    if let g = a as? CAAnimationGroup, let subs = g.animations {
        s += " GROUP{" + subs.map { describeAnim($0) }.joined(separator: " ;; ") + "}"
    }
    return s
}

final class Probe: NSObject {
    let root: UIView
    let window: UIWindow
    var link: CADisplayLink?
    var seen = Set<ObjectIdentifier>()
    var ids = [ObjectIdentifier: Int]()
    var nextId = 0
    var lastSig = ""
    init(root: UIView, window: UIWindow) {
        self.root = root; self.window = window
        super.init()
        if snapEnabled {
            // Snapshots are numbered per launch; drop an earlier launch's set.
            let snaps = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                .appendingPathComponent("snap")
            try? FileManager.default.removeItem(at: snaps)
        }
        link = CADisplayLink(target: self, selector: #selector(tick(_:)))
        link!.preferredFrameRateRange = CAFrameRateRange(minimum: 60, maximum: 120, preferred: 120)
        link!.add(to: .main, forMode: .common)
    }
    func lid(_ l: CALayer) -> Int {
        let k = ObjectIdentifier(l)
        if let i = ids[k] { return i }
        nextId += 1; ids[k] = nextId
        return nextId
    }
    // PROBE_SNAP=1: also snapshot the bar region on every frame for 1.4 s after
    // each touch-down, as lossless PNGs named by milliseconds since the touch
    // (Documents/snap/<n>_<ms>.png), the native side of the frame-by-frame
    // comparison. A screen recording is lossy and variable-rate.
    var snapUntil: CFTimeInterval = 0
    var snapStart: CFTimeInterval = 0
    var snapIndex = 0
    let snapEnabled = ProcessInfo.processInfo.environment["PROBE_SNAP"] == "1"
    // PROBE_FILTERS=1: log every CoreAnimation filter on the tab bar's layers with
    // all of its input values (FILTER lines), whenever they change. This is how the
    // vibrancy colour matrices and the glass parameters are read exactly instead of
    // being fitted from pixels.
    let dumpFilters = ProcessInfo.processInfo.environment["PROBE_FILTERS"] == "1"
    var lastFilterSig = [Int: String]()
    var lastObjSig = [Int: String]()
    // PROBE_KEYS=<file>: extra candidate input keys (one per line) to query on every
    // CAFilter -- the private glass filters' keys are not discoverable at runtime.
    lazy var extraFilterKeys: [String] = {
        guard let p = ProcessInfo.processInfo.environment["PROBE_KEYS"],
              let t = try? String(contentsOfFile: p, encoding: .utf8) else { return [] }
        return t.split(separator: "\n").map(String.init).filter { !$0.isEmpty }
    }()

    var dumpedClasses = Set<String>()

    /// PROBE_FILTERS: once per class, the ObjC properties and zero-argument getters
    /// of the private classes that implement the glass (CAFilter, CASDF*Layer,
    /// CABackdropLayer), so their parameters can be read by name.
    func dumpClass(_ cls: AnyClass) {
        let n = NSStringFromClass(cls)
        if dumpedClasses.contains(n) { return }
        dumpedClasses.insert(n)
        var count: UInt32 = 0
        var names: [String] = []
        if let props = class_copyPropertyList(cls, &count) {
            for i in 0..<Int(count) { names.append("p:" + String(cString: property_getName(props[i]))) }
            free(props)
        }
        if let methods = class_copyMethodList(cls, &count) {
            for i in 0..<Int(count) {
                let sel = NSStringFromSelector(method_getName(methods[i]))
                if !sel.contains(":") && !sel.hasPrefix("_") && !sel.hasPrefix(".") { names.append(sel) }
            }
            free(methods)
        }
        out("CLASS " + n + " " + names.sorted().joined(separator: ","))
    }

    /// Every declared property of `obj` (walking up to NSObject) with its value,
    /// plus object ivars, recursing one level into nested private objects.
    func describeObject(_ obj: AnyObject, depth: Int = 0) -> String {
        var cls: AnyClass? = type(of: obj)
        var parts: [String] = []
        while let c = cls, c != NSObject.self {
            let cname = NSStringFromClass(c)
            if cname == "CALayer" || cname == "UIView" || cname == "UIResponder" { break }
            var count: UInt32 = 0
            if let props = class_copyPropertyList(c, &count) {
                for i in 0..<Int(count) {
                    let pn = String(cString: property_getName(props[i]))
                    if ["delegate", "superlayer", "sublayers", "sourceLayer", "debugDescription", "description", "hash", "superclass"].contains(pn) { continue }
                    if let v = obj.value(forKey: pn) {
                        var d = describeFilterValue(v)
                        if depth == 0, let o = v as AnyObject?, NSStringFromClass(type(of: o)).hasPrefix("CA"),
                           !(v is NSNumber), !(v is String), !(v is NSValue) {
                            d = "{" + NSStringFromClass(type(of: o)) + " " + describeObject(o, depth: 1) + "}"
                        }
                        parts.append(pn + "=" + d)
                    }
                }
                free(props)
            }
            if let ivars = class_copyIvarList(c, &count) {
                for i in 0..<Int(count) {
                    guard let n = ivar_getName(ivars[i]), let enc = ivar_getTypeEncoding(ivars[i]) else { continue }
                    if String(cString: enc).hasPrefix("@"), let v = object_getIvar(obj, ivars[i]) {
                        parts.append("ivar:" + String(cString: n) + "=" + describeFilterValue(v))
                    }
                }
                free(ivars)
            }
            cls = class_getSuperclass(c)
        }
        return parts.joined(separator: " ")
    }

    func dumpLayerFilters(_ layer: CALayer, id: Int, name: String) {
        let lc = String(describing: type(of: layer))
        if dumpFilters && (lc.hasPrefix("CASDF") || lc == "CABackdropLayer") && lastObjSig[id] == nil {
            let d = describeObject(layer)
            lastObjSig[id] = d
            out("OBJ layer=\(id) \(name) \(lc) " + d)
        }
        for f in (layer.filters ?? []) + (layer.backgroundFilters ?? []) {
            let o = f as AnyObject
            let key = "\(id):" + ((o.value(forKey: "name") as? String) ?? "")
            if lastObjSig[-abs(key.hashValue)] == nil {
                lastObjSig[-abs(key.hashValue)] = ""
                out("OBJ filter layer=\(id) \(name) " + describeObject(o))
            }
        }
        if String(describing: type(of: layer)).hasPrefix("CA") { dumpClass(type(of: layer)) }
        for f in (layer.filters ?? []) + (layer.backgroundFilters ?? []) { dumpClass(type(of: f as AnyObject)) }
        var parts: [String] = []
        let lists: [(String, [Any]?)] = [("filters", layer.filters), ("backgroundFilters", layer.backgroundFilters),
                                          ("compositingFilter", layer.compositingFilter.map { [$0] })]
        for (kind, list) in lists {
            for f in list ?? [] {
                let o = f as AnyObject
                var desc = kind + ":" + (((o.responds(to: Selector(("name"))) ? o.value(forKey: "name") : nil) as? String) ?? "\(type(of: f))")
                // CAFilter keeps its inputs in a dictionary, so KVC on a key the
                // filter does not use answers nil rather than throwing.
                let keys = ["inputColorMatrix", "inputRadius", "inputAmount", "inputScale", "inputBias",
                            "inputColor", "inputColor0", "inputColor1", "inputValues", "inputMatrix",
                            "inputHardEdges", "inputNormalizeEdges", "inputQuality", "inputOffset",
                            "inputIntensity", "inputSaturation", "inputBrightness", "inputContrast",
                            "inputAngle", "inputDisplacementScale", "inputSourceSublayerName",
                            "inputMaskImage", "inputReversed", "inputDither", "inputEnabled", "inputCornerRadius"]
                if String(describing: type(of: o)) == "CAFilter" {
                    for k in keys + extraFilterKeys where !keys.contains(k) || extraFilterKeys.isEmpty {
                        if let v = o.value(forKey: k) {
                            desc += " " + k + "=" + describeFilterValue(v)
                        }
                    }
                } else {
                    desc += " (" + String(describing: type(of: o)) + ") " + "\(f)".replacingOccurrences(of: "\n", with: " ")
                }
                parts.append(desc)
            }
        }
        // backdrop layers carry their own scale / group parameters
        let layerClass = String(describing: type(of: layer))
        if layerClass.contains("Backdrop") {
            for k in ["scale", "groupName", "captureOnly", "marginWidth", "zoom", "disablesOccludedBackdropBlurs"] {
                if layer.responds(to: Selector((k))), let v = layer.value(forKey: k) {
                    parts.append("prop:" + k + "=" + describeFilterValue(v))
                }
            }
        }
        if layerClass != "CALayer" {
            parts.append("class:" + layerClass)
        }
        let sig = parts.joined(separator: " || ")
        if !parts.isEmpty && lastFilterSig[id] != sig {
            lastFilterSig[id] = sig
            out(String(format: "FILTER t=%.5f layer=%d %@ ", CACurrentMediaTime(), id, name) + sig)
        }
    }

    func touchDown(_ t: CFTimeInterval) {
        guard snapEnabled else { return }
        snapStart = t
        snapUntil = t + 1.4
        snapIndex += 1
    }

    func snapshot(_ now: CFTimeInterval) {
        let bar = root.convert(root.bounds, to: window).insetBy(dx: -24, dy: -16)
        let fmt = UIGraphicsImageRendererFormat()
        fmt.scale = UIScreen.main.scale
        let img = UIGraphicsImageRenderer(size: bar.size, format: fmt).image { _ in
            window.drawHierarchy(in: CGRect(x: -bar.minX, y: -bar.minY, width: window.bounds.width,
                                            height: window.bounds.height), afterScreenUpdates: false)
        }
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("snap")
        try? FileManager.default.createDirectory(at: docs, withIntermediateDirectories: true)
        let name = String(format: "%d_%04d.png", snapIndex, Int(((now - snapStart) * 1000).rounded()))
        try? img.pngData()?.write(to: docs.appendingPathComponent(name))
    }

    @objc func tick(_ dl: CADisplayLink) {
        let now = CACurrentMediaTime()
        if snapEnabled && now < snapUntil {
            snapshot(now)
        }
        var lines: [String] = []
        walk(root.layer, depth: 0, into: &lines)
        let sig = lines.joined(separator: "\n")
        if sig != lastSig {
            out(String(format: "FRAME t=%.5f target=%.5f", CACurrentMediaTime(), dl.targetTimestamp))
            for l in lines { out(l) }
            lastSig = sig
        }
    }
    func walk(_ layer: CALayer, depth: Int, into lines: inout [String]) {
        let p = layer.presentation() ?? layer
        let id = lid(layer)
        let r = p.convert(p.bounds, to: window.layer.presentation() ?? window.layer)
        let t = p.transform
        var name = String(describing: type(of: layer))
        if let v = layer.delegate as? UIView { name = String(describing: type(of: v)) }
        let filters = (layer.filters ?? []).map { "\($0)".components(separatedBy: " ").first ?? "" }.joined(separator: "+")
        var cr = p.cornerRadius
        if cr.isNaN { cr = -1 }
        lines.append(String(format: "L %d d=%d %@ win=(%.2f,%.2f,%.2f,%.2f) b=(%.2f,%.2f) pos=(%.2f,%.2f) T=(%.4f,%.4f,%.4f,%.4f,%.2f,%.2f) op=%.3f hid=%d cr=%.2f bg=%@ f=%@",
                                id, depth, name, r.origin.x, r.origin.y, r.size.width, r.size.height,
                                p.bounds.width, p.bounds.height, p.position.x, p.position.y,
                                t.m11, t.m12, t.m21, t.m22, t.m41, t.m42, p.opacity, p.isHidden ? 1 : 0, cr,
                                p.backgroundColor.map { describeColor($0) } ?? "-", filters))
        if dumpFilters { dumpLayerFilters(layer, id: id, name: name) }
        for key in layer.animationKeys() ?? [] {
            if let a = layer.animation(forKey: key) {
                let k = ObjectIdentifier(a)
                if !seen.contains(k) {
                    seen.insert(k)
                    out(String(format: "ANIM t=%.5f layer=%d key=%@ ", CACurrentMediaTime(), id, key) + describeAnim(a))
                }
            }
        }
        for s in layer.sublayers ?? [] { walk(s, depth: depth + 1, into: &lines) }
    }
}

/// Filter input values: colour matrices are CAColorMatrix structs (20 floats),
/// colours are CGColors, the rest NSNumber / NSValue / nested descriptions.
func describeFilterValue(_ v: Any?) -> String {
    guard let v = v else { return "nil" }
    if let n = v as? NSNumber { return n.stringValue }
    if let s = v as? String { return s }
    let cf = v as CFTypeRef
    if CFGetTypeID(cf) == CGColor.typeID { return describeColor(cf as! CGColor) }
    if let val = v as? NSValue {
        let type = String(cString: val.objCType)
        if type.contains("CAColorMatrix") {
            var m = [Float](repeating: 0, count: 20)
            m.withUnsafeMutableBytes { val.getValue($0.baseAddress!, size: 80) }
            return "M[" + m.map { String(format: "%.5f", $0) }.joined(separator: ",") + "]"
        }
        return describeValue(val)
    }
    if let a = v as? [Any] { return "[" + a.map { describeFilterValue($0) }.joined(separator: ";") + "]" }
    return "\(v)".replacingOccurrences(of: "\n", with: " ")
}

func describeColor(_ c: CGColor) -> String {
    let comps = (c.components ?? []).map { String(format: "%.3f", $0) }.joined(separator: ",")
    return "(" + comps + ")"
}

final class StripesView: UIView {
    var mode = "stripes"
    override func draw(_ rect: CGRect) {
        let ctx = UIGraphicsGetCurrentContext()!
        if mode == "photo", let img = UIImage(named: "glass-backdrop") {
            img.draw(in: bounds); return
        }
        if mode == "grey" {
            ctx.setFillColor(UIColor(white: 0.5, alpha: 1).cgColor); ctx.fill(bounds); return
        }
        // solid:RRGGBB -- a uniform backdrop, so the glass under every glyph is known
        if mode.hasPrefix("solid:"), let v = UInt32(mode.dropFirst(6), radix: 16) {
            ctx.setFillColor(UIColor(red: CGFloat((v >> 16) & 0xff) / 255, green: CGFloat((v >> 8) & 0xff) / 255,
                                     blue: CGFloat(v & 0xff) / 255, alpha: 1).cgColor)
            ctx.fill(bounds); return
        }
        let cols: [UIColor] = [.systemRed, .systemOrange, .systemYellow, .systemGreen, .systemTeal, .systemBlue, .systemIndigo, .systemPurple, .white, .black]
        let h: CGFloat = 23
        var y: CGFloat = 0; var i = 0
        while y < bounds.height {
            ctx.setFillColor(cols[i % cols.count].cgColor)
            ctx.fill(CGRect(x: 0, y: y, width: bounds.width, height: h))
            y += h; i += 1
        }
        // vertical ruler lines so horizontal refraction/scale is measurable
        ctx.setFillColor(UIColor.black.cgColor)
        var x: CGFloat = 0
        while x < bounds.width { ctx.fill(CGRect(x: x, y: 0, width: 1, height: bounds.height)); x += 20 }
    }
}

/// PROBE_ICONS=grid: a template icon of 1 pt vertical lines every 3 pt and a
/// horizontal line every 6 pt, so magnification and refraction are measurable.
func gridIcon() -> UIImage {
    let size = CGSize(width: 30, height: 27)
    let r = UIGraphicsImageRenderer(size: size)
    return r.image { c in
        UIColor.black.setFill()
        var x: CGFloat = 0
        while x < size.width { c.fill(CGRect(x: x, y: 0, width: 1, height: size.height)); x += 3 }
        var y: CGFloat = 0
        while y < size.height { c.fill(CGRect(x: 0, y: y, width: size.width, height: 1)); y += 6 }
    }.withRenderingMode(.alwaysTemplate)
}

final class Page: UIViewController {
    let mode: String
    init(_ title: String, _ img: String, _ tag: Int, mode: String) {
        self.mode = mode
        super.init(nibName: nil, bundle: nil)
        let icon = ProcessInfo.processInfo.environment["PROBE_ICONS"] == "grid" ? gridIcon() : UIImage(systemName: img)
        tabBarItem = UITabBarItem(title: title, image: icon, tag: tag)
    }
    required init?(coder: NSCoder) { fatalError() }
    override func loadView() {
        let v = StripesView(); v.mode = mode; v.contentMode = .redraw
        view = v
    }
}

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication, configurationForConnecting s: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let c = UISceneConfiguration(name: nil, sessionRole: s.role)
        c.delegateClass = SceneDelegate.self
        return c
    }
}

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    var probe: Probe?
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options: UIScene.ConnectionOptions) {
        guard let ws = scene as? UIWindowScene else { return }
        let env = ProcessInfo.processInfo.environment
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let logURL = docs.appendingPathComponent(env["PROBE_LOG"] ?? "probe.log")
        FileManager.default.createFile(atPath: logURL.path, contents: nil)
        LOG = try? FileHandle(forWritingTo: logURL)
        let mode = env["PROBE_BACKDROP"] ?? "grey"
        let w = ProbeWindow(windowScene: ws)
        let tbc = UITabBarController()
        let all = [("Featured", "star.fill"), ("Search", "magnifyingglass"), ("More", "ellipsis"),
                   ("Library", "books.vertical.fill"), ("Settings", "gearshape.fill")]
        let n = Int(env["PROBE_TABS"] ?? "3") ?? 3
        tbc.viewControllers = (0..<n).map { Page(all[$0].0, all[$0].1, $0, mode: mode) }
        let dark = env["PROBE_APPEARANCE"] == "dark"
        w.overrideUserInterfaceStyle = dark ? .dark : .light
        if let sel = Int(env["PROBE_SELECT"] ?? "") { tbc.selectedIndex = sel }
        // PROBE_TINT / PROBE_UNTINT=RRGGBB: selected / unselected item tint, so the
        // vibrancy matrix can be measured as a function of the tint.
        func hex(_ k: String) -> UIColor? {
            guard let v = UInt32(env[k] ?? "", radix: 16) else { return nil }
            return UIColor(red: CGFloat((v >> 16) & 0xff) / 255, green: CGFloat((v >> 8) & 0xff) / 255,
                           blue: CGFloat(v & 0xff) / 255, alpha: 1)
        }
        if let t = hex("PROBE_TINT") { tbc.tabBar.tintColor = t }
        if let t = hex("PROBE_UNTINT") { tbc.tabBar.unselectedItemTintColor = t }
        w.rootViewController = tbc
        w.makeKeyAndVisible()
        window = w
        out(String(format: "START t=%.5f scale=%.1f screen=%@ appearance=%@ backdrop=%@", CACurrentMediaTime(), UIScreen.main.scale,
                   NSCoder.string(for: UIScreen.main.bounds), dark ? "dark" : "light", mode))
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            // PROBE_ROOT=window walks the whole window: the bar's own glass is not
            // under UITabBar.
            let root: UIView = env["PROBE_ROOT"] == "window" ? w : tbc.tabBar
            self.probe = Probe(root: root, window: w)
            // PROBE_TINTSWEEP=<file>: one RRGGBB per line; each is set as the tab
            // bar tint and the vibrancy matrices UIKit derives for it are logged
            // (TINT lines), so the tint -> matrix rule can be measured exactly.
            if let p = env["PROBE_TINTSWEEP"], let text = try? String(contentsOfFile: p, encoding: .utf8) {
                let tints = text.split(separator: "\n").compactMap { UInt32($0, radix: 16) }
                var i = 0
                func matrices() -> [String] {
                    var found: [String] = []
                    func walk(_ l: CALayer) {
                        for f in l.filters ?? [] {
                            let o = f as AnyObject
                            if (o.value(forKey: "name") as? String) == "vibrantColorMatrix",
                               let v = o.value(forKey: "inputColorMatrix") {
                                let d = String(describing: type(of: l.delegate as AnyObject? ?? l)) + ":" + describeFilterValue(v)
                                if !found.contains(d) { found.append(d) }
                            }
                        }
                        for s in l.sublayers ?? [] { walk(s) }
                    }
                    walk(tbc.tabBar.layer)
                    return found
                }
                Timer.scheduledTimer(withTimeInterval: 0.25, repeats: true) { t in
                    if i > 0 {
                        out(String(format: "TINT %06X ", tints[i - 1]) + matrices().joined(separator: " "))
                    }
                    if i >= tints.count { t.invalidate(); out("TINTDONE"); return }
                    let v = tints[i]
                    tbc.tabBar.tintColor = UIColor(red: CGFloat((v >> 16) & 0xff) / 255, green: CGFloat((v >> 8) & 0xff) / 255,
                                                   blue: CGFloat(v & 0xff) / 255, alpha: 1)
                    i += 1
                }
            }
            PROBE = self.probe
        }
    }
}
