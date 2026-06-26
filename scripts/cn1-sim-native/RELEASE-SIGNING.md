# Shipping the RPC simulator relay (signing, notarization, distribution)

The native simulator relay (`SimRelayService.app`, a Mac Catalyst Codename One
app) is pre-compiled by Codename One and shipped through Maven Central. It is
**not** compiled on the user's machine, so it must clear Gatekeeper and hold a
stable TCC identity on every Mac that downloads it.

## Why ad-hoc is not shippable

Local dev builds are `adhoc, linker-signed` (the linker applies that
automatically on Apple Silicon, which is why dev builds run). That is fine for
the build machine but fails for distribution:

- **Gatekeeper.** Maven downloads land in `~/.m2` via java HTTP and are extracted
  from a jar, so they usually have *no* `com.apple.quarantine` xattr and an
  ad-hoc app may run. But the moment anything quarantines it (a browser download,
  MDM, security tooling) Gatekeeper kills an ad-hoc app: *"cannot be opened
  because Apple cannot check it for malicious software."*
- **Signature fragility.** A zip/jar round-trip can drop framework symlinks and
  resource forks, invalidating an ad-hoc signature -> *"code signature invalid"*
  -> the kernel kills it.
- **TCC identity.** An ad-hoc app's identity is its cdhash, which changes on
  every rebuild/re-extract, so camera/mic grants do not persist and the user is
  re-prompted (or silently denied) repeatedly.

So the shipped relay must be **Developer ID Application signed + hardened runtime
+ entitlements + notarized + stapled**. After that it runs regardless of
quarantine and has one stable identity for TCC.

## One-time setup on the signing/release machine

```
# Developer ID Application cert must be in the login keychain.
xcrun notarytool store-credentials cn1-notary \
  --apple-id release@codenameone.com --team-id <TEAMID> \
  --password <app-specific-password>
```

## Build -> sign -> notarize

1. Build the relay (ad-hoc is fine as the starting point):
   `scripts/build-mac-native-app.sh` then `xcodebuild ... CODE_SIGNING_ALLOWED=NO`
   (see `project_sim_rpc_pivot` memory for the exact recipe).

2. The Codename One mac-native pipeline (`MacNativeBuilder.writeEntitlements`)
   already emits `SimRelayService-DeveloperID.entitlements` with:
   - `com.apple.security.cs.allow-jit=false`, hardened-runtime-compatible flags
   - `com.apple.security.network.client=true`
   - `com.apple.security.device.camera` / `.microphone` (App Store/sandbox
     channel only; for a hardened Developer ID build the **Info.plist usage
     strings + TCC** grant camera/mic, no device entitlement needed)
   Make sure the relay's settings carry real usage strings (NOT the CI stub):
   `codename1.arg.ios.NSCameraUsageDescription=...`,
   `codename1.arg.ios.NSMicrophoneUsageDescription=...`.

### Path A (preferred): the builder does it

`MacNativeBuilder.signAndNotarizeMacApp(request, appBundle, developerIdEntitlements)`
(in both `codenameone-maven-plugin` and the `BuildDaemon` mirror) signs +
notarizes + staples the produced `.app`. Supply the Developer ID cert the same
way as the iOS build -- the build's `certificate` (.p12) + `certificatePassword`
(imported into a throwaway keychain so the host needs nothing pre-installed) --
and turn it on with:

```
codename1.arg.macNative.notarize=true
codename1.arg.macNative.signingIdentity.developerID=Developer ID Application: Codename One (<TEAMID>)
# notary creds: either a stored profile...
codename1.arg.macNative.notarize.keychainProfile=cn1-notary
# ...or appleId/teamId/password:
codename1.arg.macNative.notarize.appleId=release@codenameone.com
codename1.arg.macNative.notarize.teamId=<TEAMID>
codename1.arg.macNative.notarize.password=<app-specific-password>
```

Default off, so existing builds are unchanged. The method is called by whatever
produces the Mac `.app` (the release pipeline after xcodebuild; a future daemon
Mac-build hook). The entitlements it signs with are the
`*-DeveloperID.entitlements` the builder already wrote.

### Path B: the standalone script (manual / CI without the builder)

3. Sign + notarize + staple + verify in one step:
   ```
   scripts/sign-notarize-mac-app.sh \
     --app /path/to/SimRelayService.app \
     --identity "Developer ID Application: Codename One (<TEAMID>)" \
     --entitlements /path/.../SimRelayService-DeveloperID.entitlements \
     --notary-profile cn1-notary
   ```
   (`--skip-notarize` for an offline sign+harden smoke test.)

## Packaging into the Maven artifact (preserve the signature!)

A plain `jar`/`zip` of a `.app` drops framework **symlinks** and the executable
bit, which invalidates the signature on extraction. Package the notarized bundle
with a format that preserves symlinks + perms:

- Build the payload with `ditto -c -k --keepParent SimRelayService.app payload.zip`
  (ditto preserves symlinks, xattrs, and the stapled ticket), and ship that zip
  *inside* the maven artifact; or ship a `.tar.gz` made with `tar` (preserves
  symlinks + mode).
- On the consumer side the simulator launcher must extract with `ditto -x -k`
  (or `tar`), then verify before launch:
  `codesign --verify --deep --strict` and `xcrun stapler validate`. Re-`chmod +x`
  the `Contents/MacOS/*` binary if the transport stripped the mode bit.

## Permissions model + deny fallback

Entitlements + usage strings make the TCC prompt *appear*; they never pre-grant.
First camera/mic/location use shows a system prompt. The simulator must degrade
gracefully when the user denies:

- **Camera capture** (`capturePhoto`): attempt the native camera; if authorization
  is denied/restricted or no camera exists, **fall back to the file picker**
  (gallery) so the app still receives an image instead of hanging. The relay
  reports authorization status; the JVM-side `BridgedSimImplementation.capturePhoto`
  chooses camera-vs-picker.
- **Microphone / video record**: on denial, surface a non-fatal status and return
  a null/empty media so the app's callback runs normally.
- **Location**: on denial, the `BridgedLocationManager` returns "unavailable"
  rather than blocking.

The principle: a denied permission is a *normal* outcome the relay reports back
over RPC, never a crash or an indefinite wait. (Implementation: relay-side
authorization check + the camera->picker fallback; tracked with the camera
capture-flow work.)
