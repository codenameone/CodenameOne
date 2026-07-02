// Standalone native iOS app that renders each reference UIKit widget in a REAL
// UIWindow and captures a real screenshot of it (drawHierarchy:afterScreenUpdates),
// so navigation/tab bars and other views that render blank off-screen come out
// correct. The PNGs land in the app's Documents dir and are pulled out by
// build-ios-native-ref.sh and committed as the iOS fidelity goldens.
//
// Sizing: the Codename One side renders each tile at CN1's pixel density
// (~18.1 px per logical mm on the reference simulator). We match that exactly so
// the native and CN1 renders overlay 1:1 with no scaling: the tile is laid out in
// points at PT_PER_MM and captured at scale PX_PER_MM/PT_PER_MM, yielding a PNG of
// (mm * PX_PER_MM) pixels -- identical to the CN1 tile.
import UIKit

// Keep these in sync with the CN1 iOS render density. PX_PER_MM is measured from a
// CN1 tile (60mm -> 1087px => 18.117). PT_PER_MM is the iOS point density
// (1pt = 1/163in @1x => 6.417 pt/mm) so a widget's natural point size maps to a
// physically sensible pixel size.
let PX_PER_MM: CGFloat = 18.117
let PT_PER_MM: CGFloat = 6.417
let CAPTURE_SCALE: CGFloat = PX_PER_MM / PT_PER_MM   // ~2.824

// Liquid Glass is translucent -- it only reveals itself by refracting/blurring
// content BEHIND it. For the glass widgets we therefore render over a fixed,
// committed backdrop PNG (glass-backdrop.png) shared 1:1 with the Codename One
// side, so the only variance between the two renders is the glass itself, not the
// background. Non-glass widgets keep the plain tile so their diff stays clean.
let GLASS_KINDS: Set<String> = [
    "ios_uibutton_system", "ios_uibutton_plain", "ios_uibutton_filled",
    "ios_uinavbar", "ios_uitabbar", "ios_uitabbar_one",
    "ios_glass_text", "ios_glass_icon",
]
// Genuinely full-width widgets that stretch to fill their tile on both sides
// (the CN1 harness fills these too). Content-sized controls -- buttons, switch,
// checkbox/radio glyphs -- keep their natural size pinned top-left so they line
// up with the content-sized CN1 widgets.
let FILL_KINDS: Set<String> = [
    "ios_uitextfield", "ios_uislider", "ios_uiprogress",
    "ios_uinavbar", "ios_uitabbar", "ios_uitabbar_one", "ios_alert_view", "ios_uipickerview",
]
let BACKDROP: UIImage? = {
    if let p = Bundle.main.path(forResource: "glass-backdrop", ofType: "png") {
        return UIImage(contentsOfFile: p)
    }
    return nil
}()

struct Spec {
    let component: String
    let kind: String
    let states: [String]
    let wMM: CGFloat
    let hMM: CGFloat
    // Tile backdrop behind the widget. Empty = use the kind default (glass kinds
    // get the photo, everything else gets none). A 6-hex value = solid fill;
    // "gradient" = vertical blue->green ramp; "photo" = the shared backdrop PNG.
    // Mirrors ComponentSpec.getBackdrop() on the CN1 side.
    var backdrop: String = ""
}

// Resolves the backdrop string for a spec, applying the same default as the CN1
// side: glass kinds fall back to the photo, every other kind to none.
func resolveBackdrop(_ spec: Spec) -> String {
    if !spec.backdrop.isEmpty { return spec.backdrop }
    return GLASS_KINDS.contains(spec.kind) ? "photo" : ""
}

// Parses a 6-hex RGB string ("808080") into an opaque UIColor. nil if malformed.
func colorFromHex(_ hex: String) -> UIColor? {
    guard hex.count == 6, let v = UInt32(hex, radix: 16) else { return nil }
    let r = CGFloat((v >> 16) & 0xff) / 255.0
    let g = CGFloat((v >> 8) & 0xff) / 255.0
    let b = CGFloat(v & 0xff) / 255.0
    return UIColor(red: r, green: g, blue: b, alpha: 1)
}

let SPECS: [Spec] = [
    // "pressed" is a REAL highlighted state on the live control (isHighlighted
    // in a real window) -- something an off-screen rasterizer cannot produce.
    Spec(component: "Button",      kind: "ios_uibutton_system", states: ["normal","pressed","disabled"], wMM: 60, hMM: 14),
    Spec(component: "RaisedButton",kind: "ios_uibutton_filled", states: ["normal","pressed","disabled"], wMM: 60, hMM: 14),
    Spec(component: "FlatButton",  kind: "ios_uibutton_plain",  states: ["normal","pressed"],            wMM: 60, hMM: 14),
    Spec(component: "TextField",   kind: "ios_uitextfield",     states: ["normal","disabled"],           wMM: 60, hMM: 14),
    Spec(component: "CheckBox",    kind: "ios_check_glyph",     states: ["normal","selected","disabled"],wMM: 60, hMM: 14),
    Spec(component: "RadioButton", kind: "ios_radio_glyph",     states: ["normal","selected","disabled"],wMM: 60, hMM: 14),
    Spec(component: "Switch",      kind: "ios_uiswitch",        states: ["normal","selected","disabled"],wMM: 60, hMM: 14),
    Spec(component: "Slider",      kind: "ios_uislider",        states: ["normal","disabled"],           wMM: 60, hMM: 14),
    Spec(component: "ProgressBar", kind: "ios_uiprogress",      states: ["normal"],                      wMM: 60, hMM: 14),
    Spec(component: "Tabs",        kind: "ios_uitabbar",        states: ["normal"],                      wMM: 60, hMM: 16),
    Spec(component: "Toolbar",     kind: "ios_uinavbar",        states: ["normal"],                      wMM: 60, hMM: 16),
    Spec(component: "Dialog",      kind: "ios_alert_view",      states: ["normal"],                      wMM: 60, hMM: 40),
    Spec(component: "Spinner",     kind: "ios_uipickerview",    states: ["normal"],                      wMM: 60, hMM: 34),
    // Isolation tests (iOS only). GlassPanel = a bare glass rect over four
    // backdrops; TabsGeom = the tab bar over a flat grey so only geometry differs.
    Spec(component: "GlassPanelGrey",  kind: "ios_glass_panel", states: ["normal"], wMM: 60, hMM: 14, backdrop: "808080"),
    Spec(component: "GlassPanelRed",   kind: "ios_glass_panel", states: ["normal"], wMM: 60, hMM: 14, backdrop: "ff3b30"),
    Spec(component: "GlassPanelGrad",  kind: "ios_glass_panel", states: ["normal"], wMM: 60, hMM: 14, backdrop: "gradient"),
    Spec(component: "GlassPanelPhoto", kind: "ios_glass_panel", states: ["normal"], wMM: 60, hMM: 14, backdrop: "photo"),
    // (The GlassChar* reverse-engineering patches were retired once the glass
    // colour transform was fitted; their goldens are gone from the committed set.)
    Spec(component: "TabsGeom",        kind: "ios_uitabbar",    states: ["normal"], wMM: 60, hMM: 16, backdrop: "808080"),
    Spec(component: "TabOne",          kind: "ios_uitabbar_one", states: ["normal"], wMM: 60, hMM: 16, backdrop: "808080"),
    // Ladder rungs: a glass capsule (radius h/2) filling the tile minus 1mm, with
    // a single centred element. Identical authored geometry on both sides so each
    // rung isolates ONE element -- the centred text, then the icon -- on top of the
    // (already matched) glass capsule, over flat grey.
    Spec(component: "GlassText",       kind: "ios_glass_text",  states: ["normal"], wMM: 60, hMM: 14, backdrop: "808080"),
    Spec(component: "GlassIcon",       kind: "ios_glass_icon",  states: ["normal"], wMM: 60, hMM: 14, backdrop: "808080"),
]

// Minimal data source/delegate for the reference UIPickerView (5 string rows).
final class RefPickerDelegate: NSObject, UIPickerViewDataSource, UIPickerViewDelegate {
    static let shared = RefPickerDelegate()
    let rows = ["Value 1", "Value 2", "Value 3", "Value 4", "Value 5"]
    func numberOfComponents(in pickerView: UIPickerView) -> Int { return 1 }
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int { return rows.count }
    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? { return rows[row] }
}

func textFor(_ kind: String) -> String {
    switch kind {
    case "ios_uibutton_system": return "Default"
    case "ios_uibutton_filled": return "Raised"
    case "ios_uibutton_plain":  return "Flat"
    case "ios_uitextfield":     return "Hello"
    case "ios_uinavbar":        return "Title"
    default: return ""
    }
}

func buildControl(_ kind: String, _ state: String, _ wPt: CGFloat, _ hPt: CGFloat) -> UIView? {
    let disabled = state == "disabled"
    let pressed  = state == "pressed"
    let selected = state == "selected"
    let label = textFor(kind)
    switch kind {
    case "ios_uibutton_system":
        // Modern tinted action button -> iOS 26 Liquid Glass (regular glass).
        let b: UIButton
        if #available(iOS 26.0, *) {
            var cfg = UIButton.Configuration.glass()
            cfg.title = label
            b = UIButton(configuration: cfg)
        } else {
            b = UIButton(type: .system)
            b.setTitle(label, for: .normal)
        }
        b.isEnabled = !disabled
        b.isHighlighted = pressed
        return b
    case "ios_uibutton_plain":
        // Borderless text button: the clear-glass variant on iOS 26.
        let b: UIButton
        if #available(iOS 26.0, *) {
            var cfg = UIButton.Configuration.clearGlass()
            cfg.title = label
            b = UIButton(configuration: cfg)
        } else {
            b = UIButton(type: .system)
            b.setTitle(label, for: .normal)
        }
        b.isEnabled = !disabled
        b.isHighlighted = pressed
        return b
    case "ios_uibutton_filled":
        // Prominent / call-to-action button -> iOS 26 prominent Liquid Glass.
        let b: UIButton
        if #available(iOS 26.0, *) {
            var cfg = UIButton.Configuration.prominentGlass()
            cfg.title = label
            b = UIButton(configuration: cfg)
        } else if #available(iOS 15.0, *) {
            var cfg = UIButton.Configuration.filled()
            cfg.title = label
            b = UIButton(configuration: cfg)
        } else {
            b = UIButton(type: .system)
            b.setTitle(label, for: .normal)
            b.backgroundColor = .systemBlue
        }
        b.isEnabled = !disabled
        b.isHighlighted = pressed
        return b
    case "ios_uitextfield":
        let tf = UITextField(frame: CGRect(x: 0, y: 0, width: wPt, height: hPt))
        tf.borderStyle = .roundedRect
        // The roundedRect default fill collapses to ~pure black in dark mode (the
        // field becomes invisible). Use the system's elevated field fill so the
        // field reads as a filled control in both appearances -- secondary system
        // background is white in light and #1c1c1e in dark, matching CN1's field.
        tf.backgroundColor = .secondarySystemBackground
        tf.text = label
        tf.isEnabled = !disabled
        return tf
    case "ios_check_glyph":
        let b = UIButton(type: .system)
        let sym = selected ? "checkmark.circle.fill" : "circle"
        let cfg = UIImage.SymbolConfiguration(pointSize: 30)
        b.setImage(UIImage(systemName: sym, withConfiguration: cfg), for: .normal)
        b.isEnabled = !disabled
        return b
    case "ios_radio_glyph":
        let b = UIButton(type: .system)
        let sym = selected ? "largecircle.fill.circle" : "circle"
        let cfg = UIImage.SymbolConfiguration(pointSize: 30)
        b.setImage(UIImage(systemName: sym, withConfiguration: cfg), for: .normal)
        b.isEnabled = !disabled
        return b
    case "ios_uiswitch":
        let sw = UISwitch()
        sw.isOn = selected
        sw.isEnabled = !disabled
        return sw
    case "ios_uislider":
        let s = UISlider(frame: CGRect(x: 0, y: 0, width: wPt, height: hPt))
        s.minimumValue = 0; s.maximumValue = 100; s.value = 50
        s.isEnabled = !disabled
        return s
    case "ios_uiprogress":
        let p = UIProgressView(progressViewStyle: .default)
        p.frame = CGRect(x: 0, y: 0, width: wPt, height: hPt)
        p.progress = 0.5
        return p
    case "ios_uitabbar":
        let bar = UITabBar(frame: CGRect(x: 0, y: 0, width: wPt, height: hPt))
        // Three UNIFORM tab items (custom title+SF-symbol). The .search SYSTEM item
        // gets a special floating button on iOS 26, which a normal tab strip does
        // not have, so we avoid it to keep a representative glass-pill tab bar.
        let a = UITabBarItem(title: "Featured", image: UIImage(systemName: "star.fill"), tag: 0)
        let b = UITabBarItem(title: "Search", image: UIImage(systemName: "magnifyingglass"), tag: 1)
        let c = UITabBarItem(title: "More", image: UIImage(systemName: "ellipsis"), tag: 2)
        bar.items = [a, b, c]; bar.selectedItem = a
        // Modern Liquid Glass bar background (default = glass material on iOS 26),
        // not the legacy opaque fill.
        let ap = UITabBarAppearance()
        ap.configureWithDefaultBackground()
        bar.standardAppearance = ap
        if #available(iOS 15.0, *) { bar.scrollEdgeAppearance = ap }
        return bar
    case "ios_uitabbar_one":
        // Minimal tab bar: a single text-only item (no SF symbol). Isolates the
        // glass pill + one centred label from the icon/multi-tab confounds.
        let bar = UITabBar(frame: CGRect(x: 0, y: 0, width: wPt, height: hPt))
        let only = UITabBarItem(title: "Tab", image: nil, tag: 0)
        bar.items = [only]; bar.selectedItem = only
        let ap = UITabBarAppearance()
        ap.configureWithDefaultBackground()
        bar.standardAppearance = ap
        if #available(iOS 15.0, *) { bar.scrollEdgeAppearance = ap }
        return bar
    case "ios_uinavbar":
        let nav = UINavigationBar(frame: CGRect(x: 0, y: 0, width: wPt, height: hPt))
        // A representative nav bar carries a leading back button and a trailing
        // action -- bar button items are a defining part of the iOS nav-bar look.
        // Pushing a root item makes the system render the "< Back" chevron.
        let root = UINavigationItem(title: "Back")
        let item = UINavigationItem(title: label)
        item.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .add, target: nil, action: nil)
        nav.items = [root, item]
        // Modern Liquid Glass nav-bar background (default = glass on iOS 26).
        let ap = UINavigationBarAppearance()
        ap.configureWithDefaultBackground()
        nav.standardAppearance = ap
        nav.scrollEdgeAppearance = ap
        if #available(iOS 15.0, *) { nav.compactScrollEdgeAppearance = ap }
        return nav
    case "ios_alert_view":
        // The presented content view of a UIAlertController (built directly so it
        // renders off the presentation flow).
        let card = UIView()
        card.backgroundColor = UIColor.secondarySystemBackground
        card.layer.cornerRadius = 14
        let title = UILabel(); title.text = "Title"; title.font = .boldSystemFont(ofSize: 17); title.textAlignment = .center
        let body = UILabel(); body.text = "Message"; body.font = .systemFont(ofSize: 13); body.textAlignment = .center; body.textColor = .secondaryLabel
        let sep = UIView(); sep.backgroundColor = .separator
        let cancel = UILabel(); cancel.text = "Cancel"; cancel.textColor = .systemBlue; cancel.font = .systemFont(ofSize: 17); cancel.textAlignment = .center
        let ok = UILabel(); ok.text = "OK"; ok.textColor = .systemBlue; ok.font = .boldSystemFont(ofSize: 17); ok.textAlignment = .center
        let vsep = UIView(); vsep.backgroundColor = .separator
        for v in [title, body, sep, cancel, ok, vsep] { v.translatesAutoresizingMaskIntoConstraints = false; card.addSubview(v) }
        NSLayoutConstraint.activate([
            card.widthAnchor.constraint(equalToConstant: wPt * 0.92),
            title.topAnchor.constraint(equalTo: card.topAnchor, constant: 19),
            title.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            title.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            body.topAnchor.constraint(equalTo: title.bottomAnchor, constant: 4),
            body.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            body.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            sep.topAnchor.constraint(equalTo: body.bottomAnchor, constant: 19),
            sep.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            sep.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            sep.heightAnchor.constraint(equalToConstant: 0.5),
            cancel.topAnchor.constraint(equalTo: sep.bottomAnchor),
            cancel.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            cancel.bottomAnchor.constraint(equalTo: card.bottomAnchor),
            cancel.heightAnchor.constraint(equalToConstant: 44),
            ok.topAnchor.constraint(equalTo: sep.bottomAnchor),
            ok.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            ok.bottomAnchor.constraint(equalTo: card.bottomAnchor),
            ok.leadingAnchor.constraint(equalTo: vsep.trailingAnchor),
            cancel.widthAnchor.constraint(equalTo: ok.widthAnchor),
            vsep.topAnchor.constraint(equalTo: sep.bottomAnchor),
            vsep.bottomAnchor.constraint(equalTo: card.bottomAnchor),
            vsep.trailingAnchor.constraint(equalTo: cancel.trailingAnchor),
            vsep.widthAnchor.constraint(equalToConstant: 0.5),
        ])
        return card
    case "ios_uipickerview":
        let picker = UIPickerView(frame: CGRect(x: 0, y: 0, width: wPt, height: hPt))
        picker.dataSource = RefPickerDelegate.shared
        picker.delegate = RefPickerDelegate.shared
        picker.selectRow(2, inComponent: 0, animated: false)   // middle row selected
        return picker
    case "ios_glass_panel":
        // Bare Liquid Glass panel (no content) for the glass-blend isolation tests.
        // iOS 26 exposes the real glass material via UIGlassEffect; earlier OSes
        // fall back to the closest thin material blur.
        let effectView = makeGlassView()
        effectView.layer.cornerRadius = 8
        effectView.clipsToBounds = true
        return effectView
    case "ios_glass_text":
        // Ladder rung 1: glass capsule + a single centred text label. System font
        // sized to match CN1's tab font (1.8mm). Primary label colour.
        let v = makeGlassView()
        let label = UILabel()
        label.text = "Tab"
        label.font = .systemFont(ofSize: 1.8 * PT_PER_MM)
        label.textColor = .label
        label.translatesAutoresizingMaskIntoConstraints = false
        v.contentView.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: v.contentView.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: v.contentView.centerYAnchor),
        ])
        return v
    case "ios_glass_icon":
        // Ladder rung 2: glass capsule + a single centred SF symbol, primary label
        // colour, point size matched to CN1's tab icon (4.1mm).
        let v = makeGlassView()
        let cfg = UIImage.SymbolConfiguration(pointSize: 4.1 * PT_PER_MM)
        let iv = UIImageView(image: UIImage(systemName: "star.fill", withConfiguration: cfg))
        iv.tintColor = .label
        iv.translatesAutoresizingMaskIntoConstraints = false
        v.contentView.addSubview(iv)
        NSLayoutConstraint.activate([
            iv.centerXAnchor.constraint(equalTo: v.contentView.centerXAnchor),
            iv.centerYAnchor.constraint(equalTo: v.contentView.centerYAnchor),
        ])
        return v
    default:
        return nil
    }
}

// Shared Liquid Glass effect view (iOS 26 UIGlassEffect, else thin material blur).
func makeGlassView() -> UIVisualEffectView {
    if #available(iOS 26.0, *) {
        return UIVisualEffectView(effect: UIGlassEffect())
    } else {
        return UIVisualEffectView(effect: UIBlurEffect(style: .systemThinMaterial))
    }
}

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        let w = UIWindow(frame: UIScreen.main.bounds)
        w.rootViewController = UIViewController()
        w.makeKeyAndVisible()
        self.window = w
        let env = ProcessInfo.processInfo.environment
        if env["NATIVEREF_MODE"] == "animate" {
            // Animation-reference mode (record-ios-native-anim.sh): loop a REAL
            // native animation -- the iOS 26 tab-selection lens morph or the
            // UISwitch toggle -- while the host records the simulator screen.
            // The resulting video is the native motion reference the CN1
            // deterministic morph frames are compared against.
            let anim = env["NATIVEREF_ANIM"] ?? "tabs"
            let appearance = env["NATIVEREF_APPEARANCE"] ?? "light"
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.runAnimation(host: w.rootViewController!.view, anim: anim, appearance: appearance)
            }
        } else {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.renderAll(host: w.rootViewController!.view)
            }
        }
        return true
    }

    func runAnimation(host: UIView, anim: String, appearance: String) {
        host.overrideUserInterfaceStyle = appearance == "dark" ? .dark : .light
        host.backgroundColor = UIColor(white: 0.5, alpha: 1)   // the morph frames' flat grey
        if anim == "switch" {
            let sw = UISwitch()
            sw.isOn = false
            sw.translatesAutoresizingMaskIntoConstraints = false
            host.addSubview(sw)
            NSLayoutConstraint.activate([
                sw.centerXAnchor.constraint(equalTo: host.centerXAnchor),
                sw.centerYAnchor.constraint(equalTo: host.centerYAnchor),
            ])
            print("NATIVEREF:ANIMATING switch \(appearance)")
            Timer.scheduledTimer(withTimeInterval: 1.2, repeats: true) { _ in
                sw.setOn(!sw.isOn, animated: true)
            }
            return
        }
        // Default: the tab bar selection morph, first tab -> last tab and back,
        // mirroring the deterministic CN1 TabsMorph frames (travel 0 -> last).
        let wPt = 60.0 * PT_PER_MM
        let hPt = 16.0 * PT_PER_MM
        let bar = UITabBar(frame: CGRect(x: 0, y: 0, width: wPt, height: hPt))
        let a = UITabBarItem(title: "Featured", image: UIImage(systemName: "star.fill"), tag: 0)
        let b = UITabBarItem(title: "Search", image: UIImage(systemName: "magnifyingglass"), tag: 1)
        let c = UITabBarItem(title: "More", image: UIImage(systemName: "ellipsis"), tag: 2)
        bar.items = [a, b, c]
        bar.selectedItem = a
        let ap = UITabBarAppearance()
        ap.configureWithDefaultBackground()
        bar.standardAppearance = ap
        if #available(iOS 15.0, *) { bar.scrollEdgeAppearance = ap }
        bar.center = CGPoint(x: host.bounds.midX, y: host.bounds.midY)
        host.addSubview(bar)
        print("NATIVEREF:ANIMATING tabs \(appearance)")
        var toLast = true
        Timer.scheduledTimer(withTimeInterval: 1.4, repeats: true) { _ in
            bar.selectedItem = toLast ? c : a
            toLast = !toLast
        }
    }

    func renderAll(host: UIView) {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        var count = 0
        for spec in SPECS {
            for appearance in ["light", "dark"] {
                for state in spec.states {
                    let wPt = spec.wMM * PT_PER_MM
                    let hPt = spec.hMM * PT_PER_MM
                    let container = UIView(frame: CGRect(x: 0, y: 0, width: wPt, height: hPt))
                    container.backgroundColor = appearance == "dark" ? .black : .white
                    container.overrideUserInterfaceStyle = appearance == "dark" ? .dark : .light
                    // Paint the spec's backdrop behind the widget, matching the CN1
                    // side (FidelityDeviceRunner.applyBackdrop) pixel-for-pixel: a
                    // solid hex fill, a vertical blue->green gradient, or the shared
                    // photo PNG. Empty/unknown leaves the plain appearance background.
                    let backdrop = resolveBackdrop(spec)
                    if backdrop == "photo", let bd = BACKDROP {
                        let iv = UIImageView(frame: container.bounds)
                        iv.image = bd
                        iv.contentMode = .scaleToFill
                        iv.autoresizingMask = [.flexibleWidth, .flexibleHeight]
                        container.addSubview(iv)
                    } else if backdrop == "gradient" {
                        let gl = CAGradientLayer()
                        gl.frame = container.bounds
                        gl.colors = [colorFromHex("1e64ff")!.cgColor, colorFromHex("28c850")!.cgColor]
                        gl.startPoint = CGPoint(x: 0.5, y: 0.0)   // blue at top
                        gl.endPoint = CGPoint(x: 0.5, y: 1.0)     // green at bottom
                        container.layer.addSublayer(gl)
                    } else if let col = colorFromHex(backdrop) {
                        container.backgroundColor = col
                    }
                    guard let control = buildControl(spec.kind, state, wPt, hPt) else { continue }
                    // The CN1 fidelity harness renders every widget filling its tile and
                    // (for text widgets) centring the content vertically. Match that for
                    // the widgets that stretch -- buttons, fields, bars, slider, progress --
                    // so the comparison measures the WIDGET rendering (colour, font, corner
                    // radius, glass) rather than a layout difference the app controls.
                    // Intrinsically-sized controls (switch, checkbox/radio glyphs) keep
                    // their natural size pinned top-left, matching how CN1 lays them out.
                    if spec.kind == "ios_glass_panel" {
                        // The glass panel fills the tile minus a ~1mm inset, matching
                        // the CN1 GlassPanel's 1mm margin.
                        let inset = 1.0 * PT_PER_MM
                        control.frame = container.bounds.insetBy(dx: inset, dy: inset)
                    } else if spec.kind == "ios_glass_text" || spec.kind == "ios_glass_icon" {
                        // Glass CAPSULE filling the tile minus 1mm: corner radius = half
                        // the height so it matches the CN1 cn1-pill-border capsule exactly.
                        let inset = 1.0 * PT_PER_MM
                        control.frame = container.bounds.insetBy(dx: inset, dy: inset)
                        control.layer.cornerRadius = control.frame.height / 2
                        control.clipsToBounds = true
                    } else if FILL_KINDS.contains(spec.kind) {
                        control.frame = CGRect(x: 0, y: 0, width: wPt, height: hPt)
                    } else {
                        control.sizeToFit()
                        var cs = control.bounds.size
                        if cs.width <= 0 || cs.width > wPt { cs.width = wPt }
                        if cs.height <= 0 || cs.height > hPt { cs.height = hPt }
                        control.frame = CGRect(x: 0, y: 0, width: cs.width, height: cs.height)
                    }
                    container.addSubview(control)
                    host.addSubview(container)
                    container.setNeedsLayout()
                    container.layoutIfNeeded()

                    let fmt = UIGraphicsImageRendererFormat()
                    fmt.scale = CAPTURE_SCALE
                    fmt.opaque = true
                    // iOS 26 defaults the renderer to extended (16-bit, wide-gamut)
                    // range, which produces 16-bit PNGs the host comparator can't
                    // read. Force standard 8-bit sRGB output.
                    fmt.preferredRange = .standard
                    let renderer = UIGraphicsImageRenderer(size: CGSize(width: wPt, height: hPt), format: fmt)
                    let img = renderer.image { _ in
                        container.drawHierarchy(in: container.bounds, afterScreenUpdates: true)
                    }
                    container.removeFromSuperview()
                    let name = "\(spec.component)_\(state)_\(appearance).png"
                    if let data = img.pngData() {
                        try? data.write(to: docs.appendingPathComponent(name))
                        count += 1
                        let px = Int(wPt * CAPTURE_SCALE)
                        print("NATIVEREF:wrote \(name) \(px)x\(Int(hPt*CAPTURE_SCALE)) bytes=\(data.count)")
                    }
                }
            }
        }
        print("NATIVEREF:DONE count=\(count) dir=\(docs.path)")
        exit(0)
    }
}
