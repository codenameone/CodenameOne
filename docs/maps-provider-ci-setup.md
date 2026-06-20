# Testing native map providers in CI (keys & secrets)

This is a contributor/CI guide for exercising the `NativeMap` providers
(`maps.provider=apple` / `google`) in automated builds. End-user app setup
(adding keys to your own app) is in the developer guide's *Maps* chapter.

## Why provider tests are different

The pure-vector `MapView` is fully covered by deterministic JVM unit tests and
offline screenshot tests (it renders the same everywhere). Native providers are
not:

1. **They only render on a real device/simulator.** In the JavaSE simulator
   `NativeMap` falls back to the vector `MapView`, so a native provider is only
   exercised by the **iOS / Android device-runner** jobs
   (`scripts/run-ios-ui-tests.sh`, `scripts/run-android-instrumentation-tests.sh`).
2. **A live native map is non-deterministic.** Map tiles, labels and styling
   come from the provider's servers and load asynchronously, so a pixel-exact
   screenshot baseline is not reliable. Provider coverage therefore comes in two
   forms:
   - **Smoke build tests (deterministic, recommended):** build the app with the
     provider selected and assert the build *succeeds* and the app launches and
     renders a map without crashing. This is exactly what catches regressions in
     the provider injection (the bytecode→native bridge, frameworks, gradle deps,
     `register()` wiring). Apple needs no key, so this runs with no secrets.
   - **Loose-tolerance screenshots (optional):** capture the live map with a high
     pixel-mismatch tolerance (`.tolerance` file next to the baseline). Use
     sparingly; prefer the smoke build test.

## The native-map screenshot test

`NativeMapProviderScreenshotTest` (in the hellocodenameone suite) is the visual
guard. It only emits a screenshot when a native provider is actually active
(`NativeMap.isNativeMap()`); otherwise it skips, and the vector-fallback path is
covered by `NativeMapFallbackScreenshotTest` (the two are complements -- exactly
one runs per platform/build). Because the test app sets
`codename1.arg.ios.maps.provider=apple`, the **iOS device-runner produces a real
MapKit baseline with no secret**; Android produces a Google baseline only when
the keys below are provided.

Variance is controlled deliberately so the baseline is stable yet a blocked map
still fails:

- **Low-variance scene:** a regional view of the Italian peninsula + the
  Mediterranean. Natural geography does not change, there is strong land/water
  contrast, and at this zoom there is no traffic layer, no street-level churn and
  minimal label movement. (Default standard map type, user-location dot off.)
- **Lenient comparison:** each baseline has a `.tolerance` file
  (`maxChannelDelta=20`, `maxMismatchPercent=12.0`) so day-to-day tile/label
  noise does not fail CI -- while a blank/blocked map, which differs from a real
  render across essentially the whole frame, still does.

This is what catches the failure mode you actually care about: a provider that
"builds and launches" but renders nothing (bad key, missing framework, Play
Services unavailable, a broken bridge) -- invisible to a smoke build, obvious in
the screenshot.

## Apple MapKit -- no key required

MapKit is a free iOS system framework. To exercise it, build hellocodenameone
for iOS with the hint set:

```
codename1.arg.ios.maps.provider=apple
```

The build injects the MapKit provider, adds the `MapKit`/`CoreLocation`
frameworks and the `NSLocationWhenInUseUsageDescription` plist string, and the
app renders an `MKMapView` through `NativeMap`. No secret is needed.

## Google Maps -- API keys + GitHub secrets

Google Maps needs an Android key and an iOS key.

### 1. Create the keys in Google Cloud

1. Go to <https://console.cloud.google.com/>, create (or pick) a project.
2. **APIs & Services -> Library**, enable **Maps SDK for Android** and **Maps
   SDK for iOS**.
3. **APIs & Services -> Credentials -> Create credentials -> API key**. Create
   two keys (one Android, one iOS) so you can restrict each.
4. **Restrict each key** (Credentials -> the key -> *Edit*):
   - *Android key:* Application restrictions -> **Android apps**; add your
     package name (`com.codenameone.examples.hellocodenameone` for the test app)
     and the debug-keystore SHA-1 fingerprint
     (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`).
     API restrictions -> **Maps SDK for Android**.
   - *iOS key:* Application restrictions -> **iOS apps**; add the bundle id
     (`com.codenameone.examples.hellocodenameone`). API restrictions -> **Maps
     SDK for iOS**.

> The CI test app's package/bundle id is
> `com.codenameone.examples.hellocodenameone`. For ad-hoc local testing you may
> leave a key unrestricted, but always restrict keys used in CI.

### 2. Add the keys as GitHub Actions secrets

In the repository: **Settings -> Secrets and variables -> Actions -> New
repository secret**. Add:

| Secret name                     | Value                          |
|---------------------------------|--------------------------------|
| `GOOGLE_MAPS_ANDROID_API_KEY`   | the restricted Android key     |
| `GOOGLE_MAPS_IOS_API_KEY`       | the restricted iOS key         |

(Use organization-level secrets if you want them shared across repos.)

### 3. How the workflow consumes them

The provider build maps the secrets to build hints before building. Android key
goes into the manifest meta-data; iOS key into the app-delegate launch call:

```yaml
env:
  GOOGLE_MAPS_ANDROID_API_KEY: ${{ secrets.GOOGLE_MAPS_ANDROID_API_KEY }}
  GOOGLE_MAPS_IOS_API_KEY: ${{ secrets.GOOGLE_MAPS_IOS_API_KEY }}
steps:
  - name: Select Google provider + inject keys (skips if no secret)
    if: env.GOOGLE_MAPS_ANDROID_API_KEY != ''
    run: |
      SETTINGS=scripts/hellocodenameone/common/codenameone_settings.properties
      {
        echo "codename1.arg.maps.provider=google"
        echo "codename1.arg.android.xapplication=<meta-data android:name=\"com.google.android.maps.v2.API_KEY\" android:value=\"$GOOGLE_MAPS_ANDROID_API_KEY\"/>"
        echo "codename1.arg.ios.afterFinishLaunching=[GMSServices provideAPIKey:@\"$GOOGLE_MAPS_IOS_API_KEY\"];"
      } >> "$SETTINGS"
  - run: ./scripts/run-ios-ui-tests.sh        # or run-android-instrumentation-tests.sh
```

The `if: env.… != ''` guard means forks and PRs without the secret simply skip
the Google job rather than failing -- the Apple job (no key) always runs.

## Huawei Map Kit (HMS) -- key + agconnect config

`maps.provider=huawei` targets Huawei devices that ship HMS Core instead of
Google Play Services.

### Create the credentials

1. Register at <https://developer.huawei.com/consumer/en/> (a *verified* Huawei
   developer account is required; verification can take a couple of days).
2. In **AppGallery Connect** (<https://developer.huawei.com/consumer/en/service/josp/agc/index.html>)
   create a **project** and an **app** (platform: Android), using the same
   package name as your build.
3. **Project settings -> Manage APIs**: enable **Map Kit**.
4. **Project settings -> General information**: copy the **App ID** and the
   **API key** ("Client -> API key"). Download **`agconnect-services.json`**.
5. Add the SHA-256 fingerprint of your signing certificate under
   *Project settings -> General -> SHA-256 certificate fingerprint*.

### Add as secrets

| Secret name                 | Value                                  |
|-----------------------------|----------------------------------------|
| `HUAWEI_MAPS_API_KEY`       | the AppGallery Connect API key         |
| `HUAWEI_AGCONNECT_JSON`     | the contents of `agconnect-services.json` (base64 or raw) |

Wire them into the build the same way (provider hint + key/JSON build hints).

> **CI testability:** HMS maps render only on a device/emulator that has **HMS
> Core** installed. The standard Google Android emulators used in CI do not, so
> the Huawei provider cannot be screenshot-tested on ordinary CI runners -- the
> map would (correctly) fall back to vector. Cover Huawei with the smoke build
> (it injects + compiles against the HMS SDK) and validate the render on a real
> Huawei device. The keys above are still needed for that manual/device run.

## Bing Maps (Windows / UWP) -- key

The Windows native provider uses Bing Maps.

1. Sign in at the **Bing Maps Dev Center** (<https://www.bingmapsportal.com/>).
2. **My account -> My keys -> Create a new key** (key type: *Basic* or
   *Enterprise*; application type: as appropriate).
3. Provide the token to the app at runtime:
   `Display.setProperty("windows.bingmaps.token", "YOUR_BING_KEY");` (or as a
   build hint). As a secret: `BING_MAPS_TOKEN`.

> **CI testability:** only relevant to Windows/UWP builds; not part of the
> iOS/Android device-runner screenshot suite.

## Summary

| Provider | Key? | Secret(s) | What's tested in CI |
|----------|------|-----------|---------------------|
| Vector `MapView` (OSM) | no | -- | JVM unit tests + offline + real-OSM screenshot |
| Apple MapKit | no | -- | iOS smoke build **+ live MapKit screenshot** (`NativeMapProvider`), no secret |
| Google Maps | yes | `GOOGLE_MAPS_ANDROID_API_KEY`, `GOOGLE_MAPS_IOS_API_KEY` | iOS + Android smoke build + screenshot, gated on secrets |
| Huawei Map Kit | yes | `HUAWEI_MAPS_API_KEY`, `HUAWEI_AGCONNECT_JSON` | smoke build only (render needs an HMS device, not CI emulators) |
| Bing Maps (Windows) | yes | `BING_MAPS_TOKEN` | Windows build only (not in the device-runner suite) |
