# JavaSE simulator screenshot baselines

This directory stores baseline PNG files for JavaSE simulator integration screenshot tests:

- `javase-single-window.png`
- `javase-multi-window.png`
- `javase-single-landscape.png`
- `javase-multi-landscape.png`
- `javase-single-component-inspector.png`
- `javase-single-network-monitor.png`
- `javase-single-test-recorder.png`
- `javase-single-native-theme-ios-modern.png`
- `javase-single-native-theme-ios7.png`
- `javase-single-native-theme-android-material.png`
- `javase-single-native-theme-android-holo.png`
- `javase-single-ar-demo.png`

The CI workflow compares generated simulator screenshots with these files.
If screenshots differ (or are missing), CI uploads artifacts and posts a PR comment with visual previews.

The `javase-single-ar-demo.png` baseline exercises the simulated AR
backend (`JavaSEARImpl`): the harness opens a session, waits for the
virtual room's floor plane, hit-tests it and anchors a model, and the
capture shows that model resting on the detected floor. A small
`.tolerance` sidecar accompanies it because the anchored model is
rendered through the software 3D rasterizer.

The `javase-single-native-theme-*` baselines exercise the Simulator's
"Native Theme" menu - each one runs with the corresponding
`simulatorNativeTheme` preference set, the same preference the menu
writes when the user picks a theme. The captured chrome should
visibly differ between them: iOS Modern shows rounded blue buttons
and the modern iOS title bar, iOS 7 shows the flat 2014-era style,
Android Material shows raised purple buttons + Material chrome, and
Android Holo Light shows the all-caps Holo button labels in light
blue.
