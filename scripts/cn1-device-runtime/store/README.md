# Shipping the device runtime

The device runtime submits through the Codename One build cloud, the same
pipeline customers use. Signing certificates, provisioning profiles, Play
service-account credentials and App Store Connect keys all live in the cloud
org's vault; none of them are held by this repository or by GitHub Actions.

There is no in-repo weekly workflow that builds and uploads the app. Doing
that from Actions -- local Gradle sign, local `xcodebuild archive`, then
`altool` / `upload-google-play` -- is the wrong shape twice over:

1. It duplicates a submission pipeline the build server already implements
   (`SubmissionService`, `GooglePlayPublisherService`,
   `AscCredentialService`), so drift is silent and only we would see it.
2. It bypasses the path customers use, so any friction they hit we do not,
   which is the opposite of dogfooding.

## The correct path

**Build:** send a cloud build with `codename1.buildTarget=android-device`
(for the AAB) and `codename1.buildTarget=ios-device-release` (for the
signed IPA). The framework compiles locally; the *native artifact* is
built and signed on the cloud, using the org's stored credentials.

**Submit:** the CN1 org (`com.codenameone.devruntime`) has a zero-touch
`SubmissionConfig` armed at the console for each target, targeting `BETA`
(TestFlight for iOS, Play internal-testing for Android). The build server
publishes the artifact the moment the build finishes -- inline server-side
for Google (streams the AAB from the artifact URL to the Publisher API)
and via the Mac daemon's `xcrun altool` for Apple, no S3 round-trip. See
`BuildCloud/src/main/java/com/codename1/buildcloud/submission/services/`
for the runners.

**Trigger:** the weekly schedule and the version-code monotonicity live on
the build server, not here. A GitHub Actions cron cannot fire a cloud
build without credentials the repo does not hold, and standing that up
would recreate the "wrong shape" this note is here to prevent.

## Promoting a beta build

`fastlane supply --track production` for Android, or advance the TestFlight
build in App Store Connect. Both stay a human decision -- neither store
should get an unreviewed release cadence.

## Guideline 2.5.2 / 4.7.2

The runtime runs code it did not ship with. That is squarely within App
Store Review Guideline 2.5.2, which permits it only for apps that
"teach, develop, or allow students to test executable code" and only when
"the source code provided by the app [is] completely viewable and editable
by the user". The runtime is built around that: it refuses to load a
bundle whose sources it does not have, and the source of whatever is
running is always on screen under **View source**. Removing that screen
would make the app unsubmittable.

Guideline 4.7.2 -- "may not extend or expose native platform APIs or
technologies to the software without prior permission from Apple" -- is
the sharper edge, and exposing the framework to pushed code is what this
app is. The argument to make is that this is a developer tool used point
to point on the developer's own network, with no distribution of anything
to anyone, which is the 2.5.2 case rather than the mini-apps case. Expect
to make it explicitly, in the review notes, and expect it to be a
conversation.

Google Play is the lower risk of the two. Its Device and Network Abuse
policy bans downloading executable code "such as dex, JAR, .so files" but
exempts "code that runs in a virtual machine or an interpreter where
either provides indirect access to Android APIs". A `.cn1ip` bundle is
none of those formats, and the interpreter reaches Android only through
the framework compiled into the app.

**If review says no:** TestFlight internal testing (100 users) needs no
review at all, and on Android a sideloaded APK has no gatekeeper. The
beta channel therefore keeps working regardless -- a rejection costs the
public listing and nothing else.
