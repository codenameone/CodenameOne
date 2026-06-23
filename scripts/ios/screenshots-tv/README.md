# Apple TV (tvOS) screenshot goldens

Reference PNGs for the `build-ios-tv` CI job (`scripts/run-tv-ui-tests.sh`),
captured from the `hellocodenameone` `cn1ss` suite running on the tvOS simulator
(1920x1080) and streamed through `Cn1ssScreenshotServer`.

This set is seeded once the tvOS native slice compiles end-to-end (see
`Ports/iOSPort/nativeSources/TVOS_PORT.md`), the same way the watch goldens in
`../screenshots-watch` were bootstrapped in CI. Until then the `build-ios-tv`
job runs non-blocking.
