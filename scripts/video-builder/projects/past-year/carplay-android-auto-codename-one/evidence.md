# Evidence map

Source: `docs/website/content/blog/carplay-android-auto-codename-one.md`
Canonical: https://www.codenameone.com/blog/carplay-android-auto-codename-one/

## Thesis

A portable template model for driver-safe CarPlay and Android Auto screens

## Supported beats

- **The Template Model:** A car app registers a CarApplication. When a head unit connects, the application returns a root CarScreen. Each screen returns one CarTemplate: list, grid, pane, message, navigation, or now-playing.
- **Navigation And Lifecycle:** That is a good place to start and stop work that only exists while the dashboard is connected.
- **Zero Cost When Unused:** The build system scans your bytecode. If it sees com.codename1.car, it injects the native wiring. If it does not, the app carries none of this.
- **Build Hints And Approval:** Car app categories matter. The build can inject wiring, but Apple and Google still decide whether an app is allowed into the car.
- **Simulator Head Units:** The simulator has a Car menu now. Car > Connect CarPlay and Car > Connect Android Auto open a simulated head-unit window that renders the same portable template tree.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5281

## Independent problem evidence

- Apple CarPlay: https://developer.apple.com/carplay/ — Apple exposes constrained vehicle experiences whose capabilities depend on the app's approved CarPlay category.
- Build Apps for Cars: https://developer.android.com/training/cars — Google's car app library provides driver-optimized templates and category rules instead of arbitrary handset layouts.

## Product proof

- `docs/website/static/blog/carplay-android-auto-codename-one/car-sim-list.png`
- `docs/website/static/blog/carplay-android-auto-codename-one/android-auto-grid.png`
- `docs/website/static/blog/carplay-android-auto-codename-one/carplay-list.png`
