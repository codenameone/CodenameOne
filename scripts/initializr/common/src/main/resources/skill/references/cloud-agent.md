# Cloud Builds, Signing, Push and Store Submission from an Agent

BuildCloud (`https://cloud.codenameone.com`) is an MCP server. Through it you can do what the
developer does in the console: list and inspect builds, read a failed build's log, create Apple
signing assets, configure push, set up analytics, invites and crash reporting, and submit a build
to the App Store or Google Play.

Cloud builds need no local SDK. Xcode, the Android SDK and platform toolchains run on Codename
One's servers, so a developer on Windows or Linux can build and ship for iOS. Never tell the
developer to install Xcode or Android Studio to build. The developer approves you once, in their
browser; there is no API key to paste.

Read this when the developer asks you to ship the app, sign it, set up push, or submit it.

## Connect

Generated projects carry a `.mcp.json` naming the server `codenameone-cloud`
(`https://cloud.codenameone.com/api/v1/mcp`, HTTP transport). Claude Code asks the developer to
approve project MCP servers the first time; other clients take the same URL.

The first call answers `401` with a `WWW-Authenticate` header. Your MCP client handles that:
it registers itself, opens the developer's browser, the developer signs in and approves you, and
the client receives a token. The sign-in does not expire on its own: it lasts until the developer
disconnects it (in the console, or by asking you to call
`DELETE /api/v2/console/me/mcp-connections/{id}`; list them with `GET` on the same path). After a
disconnect, `/mcp` in Claude Code re-runs the sign-in.

The approval page has a checkbox for each kind of extra access. Ask the developer to tick the ones
the task needs **before** they click Allow:

| Checkbox | Needed for |
|---|---|
| Apple signing | Creating certificates, App IDs, devices and provisioning profiles; downloading the `.p12` and profile a build needs |
| Push setup | Creating a push app and storing its APNs / FCM credentials |
| Store submission | Submitting a finished build to the App Store or Google Play |

Without them you can still read everything and make everyday changes. Anything else -- an
unticked box, revoking a certificate, deleting a key, reading an API key or the App Store Connect
key, billing, organization or account-security changes -- is still possible, but each call needs
the developer's approval:

1. `call_api` returns `approvalRequired: true` with `approvalId`, `approvalUrl` and `reason`.
2. Open `approvalUrl` in the developer's browser (`open` on macOS, `xdg-open` on Linux,
   `start` on Windows) and tell them, in one sentence, what you are asking for and why. The page
   shows the exact method, path and body (secrets masked) with Allow and Deny.
3. Call `await_approval` with `approvalId`. It waits up to a minute per call; keep calling it
   while the status is pending. On Allow it runs that exact request once and returns its result.
   On Deny, stop and ask the developer how to proceed. A request stays open for 24 hours: if the developer is away or may be looking at another window, remind them or open the link again, and keep waiting.

Do not loop a refused call: retrying the same request returns the same pending approval.

## Tools

- `whoami` -- the account, plan and remaining builds.
- `list_builds` (filter by `platform`, `status`), `get_build` (by id; `done` tells you it finished),
  `cancel_build`.
- `get_build_log` (by id) -- the error log of a failed build (the last 64KB).
- `await_approval` -- see above.
- `list_api_endpoints` (filter by a word in the path) and `call_api` -- every other console action.
  Endpoints marked `needsApproval` go through the approval page.
  Paths start with `/api/v2/` or, for Apple signing, `/appsec/7.0/apple/`. Pass the query
  string as `query`, the JSON body as `body`. Binary responses (`.p12`, `.mobileprovision`) come
  back as `bodyBase64`. Endpoints marked `multipart` take files as `files`:
  `[{"field": "file", "filename": "1.png", "contentType": "image/png", "base64": "..."}]`, with
  the form's other fields in `query` (16 MB in all).
- `start_build` -- a cloud build from a prepared bundle (`application.jar`,
  `codenameone_settings.properties`, `icons/<icon>`), the input the browser IDE's Cloud build
  sends. With a local project, build with the wrapper as below instead; it needs no bundle.

## Build

Builds are sent from the project, not through MCP:

```bash
mvn package -Dcodename1.platform=android -Dcodename1.buildTarget=android-device
mvn package -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-device
```

The first cloud build on a machine opens the developer's browser to sign the build client in and
waits up to five minutes; say so before you run it. Then follow the build with `list_builds` /
`get_build` until `done`. See `build-and-run.md` for every target.

### When a build fails

1. `get_build_log` with the build id. Read from the first error, not the last line: the tail is
   usually the build tool giving up.
2. Classify it. Compile errors and unsupported Java APIs (Codename One supports a subset of the
   JDK; see the other references) are fixed in the source. Signing errors (expired or revoked
   certificate, profile not matching the bundle id, wrong `.p12` password) are fixed with the
   signing steps below. Missing build hints go in `codenameone_settings.properties`.
3. Fix the project, run the local simulator build if the change is code, then send one new
   build. Never resubmit an unchanged project hoping for a different result.

### Quotas and restrictions

Check `whoami` before a build: it reports the plan and remaining builds.

- Free accounts spend build credits: an iOS or macOS build costs 8, every other target 1. An
  upload refused with a bare `ERROR` usually means the credits are gone -- tell the developer
  (upgrading is their decision) instead of retrying.
- Below Pro only one build can be queued or running at a time. Wait for `done` before sending
  the next one; do not fire Android and iOS together.
- The uploaded project is capped at 100MB; keep build output and large assets out of it.
- A build still running after about 8 hours is cancelled.
- Free accounts keep no build history and artifacts; download what you need as soon as a build
  finishes.
- Paid plans: include-source builds, crash protection, versioned builds (older Codename One
  releases). Pro: Java desktop builds (`desktop_windows`, `desktop_macosx`) and TestFlight upload.
  Enterprise: synchronous builds, hardening and App Shield.
- iOS signing and App Store submission need the developer's own paid Apple Developer account;
  Google Play needs their Play Console account. You cannot create those.

## Sign for iOS (needs "Apple signing")

1. Apple access is the developer's own App Store Connect API key, uploaded once. Check with
   `GET /appsec/7.0/apple/credential` (`configured: false` means none is stored; calls that need
   it answer `409`). Either the developer uploads it
   in the console, or they give you the `.p8`, key id and issuer id and you store it with
   `PUT /appsec/7.0/apple/credential` `{"keyId","issuerId","privateKeyP8"}`. Never write the `.p8`
   into the project or commit it.
2. App ID: `GET /appsec/7.0/apple/bundle-ids`; create it if missing with
   `POST /appsec/7.0/apple/bundle-ids` `{"identifier": "<codename1.ios.appid without the team prefix>",
   "name", "platform": "IOS"}`. Capabilities (push, app groups):
   `POST /appsec/7.0/apple/bundle-ids/{id}/capabilities`.
3. Certificate: reuse one from `GET /appsec/7.0/apple/certificates` when it fits; Apple allows only a few
   distribution certificates per team. Otherwise `POST /appsec/7.0/apple/certificates`
   `{"certificateType": "DISTRIBUTION" | "DEVELOPMENT", "displayName"}`.
4. Profile: `POST /appsec/7.0/apple/profiles` `{"name", "profileType": "IOS_APP_STORE" |
   "IOS_APP_DEVELOPMENT" | "IOS_APP_ADHOC", "bundleIdAppleId", "certificateIds", "deviceIds"}`
   (devices for development and ad-hoc only: `GET`/`POST /appsec/7.0/apple/devices`).
5. Download into the project, outside source control:
   `GET /appsec/7.0/apple/certificates/{id}/p12?password=<a password you choose>` and
   `GET /appsec/7.0/apple/profiles/{id}/download`. Decode `bodyBase64`, write the files, then set
   in `common/codenameone_settings.properties`:

   ```properties
   codename1.ios.release.certificate=<path to .p12>
   codename1.ios.release.certificatePassword=<that password>
   codename1.ios.release.provision=<path to .mobileprovision>
   ```

   (`codename1.ios.debug.*` for a development build.) Add the files to `.gitignore`. Tell the
   developer where they are: the `.p12` holds the private key.

## Sign for Android

No server step. Generate the upload keystore locally with `keytool` and set
`codename1.android.keystore`, `codename1.android.keystorePassword` and
`codename1.android.keystoreAlias`. **The keystore cannot be replaced once an app is on Google
Play**: tell the developer where it is and that they must back it up and keep it out of git.

## Push (needs "Push setup")

1. Create or find the push app: `GET /api/v2/console/push/apps`, `POST /api/v2/console/push/apps`
   `{"name"}`.
2. iOS: store the APNs auth key the developer gives you (`.p8`, key id, team id) with
   `PUT /appsec/7.0/apple/apns-keys` `{"keyId","teamId","privateKeyP8","displayName"}`, and make sure
   the App ID has the push capability (step 2 above).
3. Android: store the FCM service-account JSON with
   `PUT /api/v2/console/push/apps/{appId}/credentials/fcm` (body: the JSON itself).
4. `GET /api/v2/console/push/apps/{appId}/credentials` shows what is configured. Credentials are
   write-only; nothing reads them back.

## Store listing and screenshots

Make the screenshots from the running app (`tools/Sim.java screenshot`, one per screen, in
store order), then size them for each store slot:

```
java tools/StoreScreenshots.java --in shots --out store --captions captions.txt --bg 1f5eff [--ipad]
```

Upload each file with `call_api` (no approval needed):

```
POST /api/v2/console/apps/{package}/metadata/screenshots
query: {"store": "apple" | "google", "locale": "en-US", "displayType": "<folder name>", "ordinal": "0"}
files: [{"field": "file", "filename": "1.png", "contentType": "image/png", "base64": "..."}]
```

The folder names under `store/<store>/` are the display types. The listing text
(`PUT /api/v2/console/apps/{package}/metadata`, check it first with `POST .../metadata/validate`)
carries public support, marketing and privacy URLs, so `call_api` holds it for the developer, who
sees the exact text on the approval page.

## Submit (needs "Store submission")

Store credentials (App Store Connect key, Google Play service account) must already be stored.
Before the first submission, and always before `PRODUCTION`, go through
`store-review.md` and its checklist: most rejections are things you can find in the code
first. Then, for a finished build from `list_builds`:

```
POST /api/v2/console/builds/{id}/submit
{"store": "APPLE" | "GOOGLE", "destination": "BETA" | "PRODUCTION", "releaseNotes": "...",
 "applePlatform": "ios"}
```

`BETA` is TestFlight or the Play internal track; `PRODUCTION` goes to store review. Confirm the
destination with the developer before a `PRODUCTION` submission. Follow progress with
`GET /api/v2/console/submissions`.
