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

## Summary

| Provider | Key? | Secret(s) | What's tested in CI |
|----------|------|-----------|---------------------|
| Vector `MapView` (OSM) | no | -- | JVM unit tests + offline + real-OSM screenshot |
| Apple MapKit | no | -- | iOS smoke build (provider injects, links, renders) |
| Google Maps | yes | `GOOGLE_MAPS_ANDROID_API_KEY`, `GOOGLE_MAPS_IOS_API_KEY` | iOS + Android smoke build, gated on secrets |
