# Shipping the device runtime

Metadata and automation for putting this app on Google Play and the App Store,
and for pushing a build to testers every week.

## What the weekly job does, and what it deliberately does not

`.github/workflows/device-runtime-store.yml` runs every Monday and on demand. It
builds the app and uploads it to **Google Play's internal testing track** and to
**TestFlight**. It does not promote anything to production and does not submit
for App Store review.

That is a decision, not an omission. A weekly automatic *release* would put an
unreviewed build in front of the public and, on iOS, would queue a review every
week whether or not anything changed. Internal testing and TestFlight are what
"ship weekly" usually means for a developer tool: the people who need the build
get it on Monday, and promoting it is a human decision.

Promotion is one command when you want it -- `fastlane supply --track production`
or advancing the TestFlight build in App Store Connect.

## Secrets the job needs

It fails with a named error if any are missing, rather than half-publishing.

| Secret | What it is | Where it comes from |
|---|---|---|
| `PLAY_SERVICE_ACCOUNT_JSON` | Google Play Developer API service account key | Play Console → Setup → API access |
| `ANDROID_KEYSTORE_BASE64` | Upload keystore, base64 | You generate it once; Play signs releases with its own key |
| `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | Keystore credentials | With the keystore |
| `APPSTORE_ISSUER_ID`, `APPSTORE_KEY_ID`, `APPSTORE_PRIVATE_KEY` | App Store Connect API key | App Store Connect → Users and Access → Integrations |
| `IOS_DIST_CERT_P12`, `IOS_DIST_CERT_PASSWORD`, `IOS_PROVISIONING_PROFILE` | Distribution signing material | Apple Developer account |

None of these exist yet. Until they do the job is a no-op that says so.

## Before the first submission

These are the things a human has to decide or supply; the automation cannot.

1. **Bundle identifiers and accounts.** `com.codenameone.devruntime` has to be
   registered in both consoles, and the App Store listing created once by hand.
2. **Screenshots.** Both stores require them per device class. Put PNGs in
   `fastlane/metadata/android/en-US/images/` and
   `fastlane/screenshots/ios/`. The fidelity harness in this repo can capture
   them, but somebody has to choose which ones tell the story.
3. **Privacy.** The app makes no network connection except to a computer you
   pair with, collects nothing and has no analytics. Play's Data Safety form and
   Apple's Privacy Nutrition Label both still have to be filled in, and Apple
   wants a Privacy Manifest declaring API reasons. See `privacy.md`.
4. **Content rating** questionnaire on Play.
5. **Export compliance** on Apple: the app uses no encryption beyond HTTPS, but
   the question is asked every submission and can be answered once in the
   listing.

## The review risk, stated plainly

This app runs code it did not ship with. That is squarely within App Store
Review Guideline 2.5.2, which permits it only for apps that "teach, develop, or
allow students to test executable code", and only when "the source code provided
by the app [is] completely viewable and editable by the user".

The runtime is built around that: it refuses to load a bundle whose sources it
does not have, and the source of whatever is running is always on screen under
**View source**. Removing that screen would make the app unsubmittable.

Guideline 4.7.2 is the sharper edge -- an app "may not extend or expose native
platform APIs or technologies to the software without prior permission from
Apple" -- and exposing the framework to pushed code is what this app is. The
argument to make is that this is a developer tool used point to point on the
developer's own network, with no distribution of anything to anyone, which is
the 2.5.2 case rather than the mini-apps case. Expect to make it explicitly, in
the review notes, and expect it to be a conversation.

Google Play is the lower risk of the two. Its Device and Network Abuse policy
bans downloading executable code, "such as dex, JAR, .so files", but exempts
"code that runs in a virtual machine or an interpreter where either provides
indirect access to Android APIs". A `.cn1ip` bundle is none of those formats,
and the interpreter reaches Android only through the framework compiled into
the app.

**If review says no:** TestFlight internal testing (100 users) needs no review
at all, and on Android a sideloaded APK has no gatekeeper. The weekly job
already targets exactly those two channels, so a rejection costs the public
listing and nothing else.
