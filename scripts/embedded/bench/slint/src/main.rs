// The benchmark gauge in Slint, on Slint's software renderer through a minimal
// platform that renders each frame into a full-frame buffer -- what its
// linuxkms backend does with a dumb buffer on a board without a GPU.
use slint::platform::software_renderer::{MinimalSoftwareWindow, PremultipliedRgbaColor, RepaintBufferType};
use slint::platform::{Platform, WindowAdapter};
use std::rc::Rc;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

slint::slint! {
    export component Gauge inherits Window {
        width: 800px;
        height: 480px;
        in property <string> arc;
        in property <string> needle;
        in property <string> value;
        Rectangle { background: #101820; }
        Path { width: 800px; height: 480px; viewbox-width: 800; viewbox-height: 480;
               fill: #2a3a4a; commands: "M 630 240 A 230 230 0 1 0 170 240 A 230 230 0 1 0 630 240 Z"; }
        Path { width: 800px; height: 480px; viewbox-width: 800; viewbox-height: 480;
               fill: #33aaff; commands: root.arc; }
        Path { width: 800px; height: 480px; viewbox-width: 800; viewbox-height: 480;
               fill: #101820; commands: "M 606 240 A 206 206 0 1 0 194 240 A 206 206 0 1 0 606 240 Z"; }
        Path { width: 800px; height: 480px; viewbox-width: 800; viewbox-height: 480;
               stroke: white; stroke-width: 1px; commands: root.needle; }
        Text { y: 240px + 76px - 28px; width: 800px; horizontal-alignment: center;
               text: root.value; color: white; font-size: 28px; font-weight: 700; }
    }
}

struct Bench(Rc<MinimalSoftwareWindow>, Instant);

impl Platform for Bench {
    fn create_window_adapter(&self) -> Result<Rc<dyn WindowAdapter>, slint::PlatformError> {
        Ok(self.0.clone())
    }
    fn duration_since_start(&self) -> Duration {
        self.1.elapsed()
    }
}

fn read(path: &str) -> String {
    std::fs::read_to_string(path).unwrap_or_default()
}

fn cpu_ticks() -> i64 {
    let s = read("/proc/self/stat");
    let rest = &s[s.rfind(')').map(|i| i + 2).unwrap_or(0)..];
    let f: Vec<&str> = rest.split(' ').collect();
    f.get(11).and_then(|v| v.parse::<i64>().ok()).unwrap_or(0) + f.get(12).and_then(|v| v.parse::<i64>().ok()).unwrap_or(0)
}

fn rss_kb() -> i64 {
    read("/proc/self/statm").split(' ').nth(1).and_then(|v| v.parse::<i64>().ok()).unwrap_or(0) * 4
}

fn epoch_ms() -> u128 {
    SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis()
}

fn main() {
    let window = MinimalSoftwareWindow::new(RepaintBufferType::NewBuffer);
    slint::platform::set_platform(Box::new(Bench(window.clone(), Instant::now()))).unwrap();
    let ui = Gauge::new().unwrap();
    window.set_size(slint::PhysicalSize::new(800, 480));
    ui.show().unwrap();
    let mut buf = vec![PremultipliedRgbaColor::default(); 800 * 480];
    let secs: u64 = std::env::var("CN1_BENCH_SECONDS").ok().and_then(|s| s.parse().ok()).unwrap_or(10);
    let (cx, cy, r) = (400.0f64, 240.0f64, 230.0f64);
    let mut angle = 0.0f64;
    let mut frames = 0i64;
    let mut first = true;
    let start = Instant::now();
    let cpu0 = cpu_ticks();
    let mut next = Instant::now();
    while start.elapsed() < Duration::from_secs(secs) {
        angle += 0.04;
        let sweep = ((angle.sin() + 1.0) * 135.0) as i32;
        let a0 = (225.0f64).to_radians();
        let a1 = ((225 - sweep) as f64).to_radians();
        let large = if sweep > 180 { 1 } else { 0 };
        ui.set_arc(format!("M {} {} L {} {} A {r} {r} 0 {large} 1 {} {} Z", cx, cy,
            cx + a0.cos() * r, cy - a0.sin() * r, cx + a1.cos() * r, cy - a1.sin() * r).into());
        ui.set_needle(format!("M {cx} {cy} L {} {}", cx + a1.cos() * (r - 30.0), cy - a1.sin() * (r - 30.0)).into());
        ui.set_value(format!("{} %", sweep * 100 / 270).into());
        slint::platform::update_timers_and_animations();
        window.draw_if_needed(|renderer| {
            renderer.render(&mut buf, 800);
            frames += 1;
            if first {
                first = false;
                println!("CN1BENCH first_frame_epoch_ms={}", epoch_ms());
            }
        });
        next += Duration::from_millis(16);
        let now = Instant::now();
        if next > now {
            std::thread::sleep(next - now);
        }
    }
    let elapsed = start.elapsed().as_millis().max(1) as i64;
    println!("CN1BENCH fps={} frames={} rss_kb={} cpu_pct={}", frames * 1000 / elapsed, frames, rss_kb(),
        (cpu_ticks() - cpu0) * 1000 / elapsed);
}
