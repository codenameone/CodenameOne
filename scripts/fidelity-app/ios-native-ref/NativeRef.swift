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

struct Spec {
    let component: String
    let kind: String
    let states: [String]
    let wMM: CGFloat
    let hMM: CGFloat
}

let SPECS: [Spec] = [
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
]

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
    case "ios_uibutton_system", "ios_uibutton_plain":
        let b = UIButton(type: .system)
        b.setTitle(label, for: .normal)
        b.isEnabled = !disabled
        b.isHighlighted = pressed
        return b
    case "ios_uibutton_filled":
        let b: UIButton
        if #available(iOS 15.0, *) {
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
        tf.text = label
        tf.isEnabled = !disabled
        return tf
    case "ios_check_glyph":
        let b = UIButton(type: .system)
        let sym = selected ? "checkmark.circle.fill" : "circle"
        let cfg = UIImage.SymbolConfiguration(pointSize: 22)
        b.setImage(UIImage(systemName: sym, withConfiguration: cfg), for: .normal)
        b.isEnabled = !disabled
        return b
    case "ios_radio_glyph":
        let b = UIButton(type: .system)
        let sym = selected ? "largecircle.fill.circle" : "circle"
        let cfg = UIImage.SymbolConfiguration(pointSize: 22)
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
        let a = UITabBarItem(tabBarSystemItem: .featured, tag: 0)
        let b = UITabBarItem(tabBarSystemItem: .search, tag: 1)
        let c = UITabBarItem(tabBarSystemItem: .more, tag: 2)
        bar.items = [a, b, c]; bar.selectedItem = a
        return bar
    case "ios_uinavbar":
        let nav = UINavigationBar(frame: CGRect(x: 0, y: 0, width: wPt, height: hPt))
        let item = UINavigationItem(title: label)
        nav.items = [item]
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
    default:
        return nil
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
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.renderAll(host: w.rootViewController!.view)
        }
        return true
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
                    guard let control = buildControl(spec.kind, state, wPt, hPt) else { continue }
                    control.sizeToFit()
                    var cs = control.bounds.size
                    if cs.width <= 0 || cs.width > wPt { cs.width = wPt }
                    if cs.height <= 0 || cs.height > hPt { cs.height = hPt }
                    control.frame = CGRect(x: 0, y: 0, width: cs.width, height: cs.height)
                    container.addSubview(control)
                    host.addSubview(container)
                    container.setNeedsLayout()
                    container.layoutIfNeeded()

                    let fmt = UIGraphicsImageRendererFormat()
                    fmt.scale = CAPTURE_SCALE
                    fmt.opaque = true
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
