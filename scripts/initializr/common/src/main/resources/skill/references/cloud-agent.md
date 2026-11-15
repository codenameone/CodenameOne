# Cloud Builds, Signing, Push and Store Submission from an Agent

BuildCloud (`https://cloud.codenameone.com`) is an MCP server. Through it you can do what the
developer does in the console: list and inspect builds, create Apple signing assets, configure
push, and submit a build to the App Store or Google Play. The developer approves you once, in their
browser; there is no API key to paste.

Read this when the developer asks you to ship the app, sign it, set up push, or submit it.

## Connect

Generated projects carry a `.mcp.json` naming the server `codenameone-cloud`
(`https://cloud.codenameone.com/api/v1/mcp`, HTTP transport). Claude Code asks the developer to
approve project MCP servers the first time; other clients take the same URL.

The first call answers `401` with a `WWW-Authenticate` header. Your MCP client handles that:
it registers itself, opens the developer's browser, the developer signs in and approves you, and
the client receives a token valid for 24 hours. In Claude Code, `/mcp` shows the server and
re-runs the sign-in.

The approval page has a checkbox for each kind of extra access. Ask the developer to tick the ones
the task needs **before** they click Allow:

| Checkbox | Needed for |
|---|---|
| Apple signing | Creating certificates, App IDs, devices and provisioning profiles; downloading the `.p12` and profile a build needs |
| Push setup | Creating a push app and storing its APNs / FCM credentials |
| Store submission | Submitting a finished build to the App Store or Google Play |

Without them you can still read everything and make everyday changes. A call that needs a box
you were not given fails with a message naming the box: tell the developer, and reconnect so
they can sign in again and tick it. Agents can never revoke certificates, delete stored keys,
read API keys or the App Store Connect key, or change billing, the organization or account
security. Send the developer to the console for those.

## Tools

- `whoami` -- the account, plan and remaining builds.
- `list_builds` (filter by `platform`, `status`), `get_build` (by id; `done` tells you it finished),
  `cancel_build`.
- `list_api_endpoints` (filter by a word in the path) and `call_api` -- every other console action.
  Paths start with `/api/v2/` or, for Apple signing, `/appsec/7.0/apple/`. Pass the query
  string as `query`, the JSON body as `body`. Binary responses (`.p12`, `.mobileprovision`) come
  back as `bodyBase64`.

## Build

Builds are sent from the project, not through MCP:

```bash
mvn package -Dcodename1.platform=android -Dcodename1.buildTarget=android-device
mvn package -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-device
```

The first cloud build on a machine opens the developer's browser to sign the build client in and
waits up to five minutes; say so before you run it. Then follow the build with `list_builds` /
`get_build` until `done`. See `build-and-run.md` for every target.

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

## Submit (needs "Store submission")

Store credentials (App Store Connect key, Google Play service account) must already be stored.
Then, for a finished build from `list_builds`:

```
POST /api/v2/console/builds/{id}/submit
{"store": "APPLE" | "GOOGLE", "destination": "BETA" | "PRODUCTION", "releaseNotes": "...",
 "applePlatform": "ios"}
```

`BETA` is TestFlight or the Play internal track; `PRODUCTION` goes to store review. Confirm the
destination with the developer before a `PRODUCTION` submission. Follow progress with
`GET /api/v2/console/submissions`.
