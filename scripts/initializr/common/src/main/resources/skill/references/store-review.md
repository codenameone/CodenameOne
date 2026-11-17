# Store review: pass App Store and Google Play review the first time

Read this before the first store submission, and again after a rejection. A
rejection costs the user days; most of them come from a short list of things an
agent can check in the code before uploading. Go through every section that
applies, fix what fails, and tell the user which answers they must give in App
Store Connect / Play Console (you cannot set those through the build).

The check list below is adapted, for Codename One apps, from
[Better App Store Review](https://github.com/kbcrtz/better-app-store-review)
(MIT License, Copyright (c) 2026 Kobe Cortez; notice at the end of this file),
which carries the full 80-check audit for React Native, Expo and Swift and
tracks the App Review Guidelines as they change. The guideline numbers are
Apple's. When a check here and the current guideline disagree, the guideline
wins: re-read it at <https://developer.apple.com/app-store/review/guidelines/>.

## How to run the review

1. List what the app uses: grep the sources for `Purchase`, `Capture`,
   `LocationManager`, `ContactsManager`, `Push`, `Login`/`AppleSignIn`,
   `Biometrics`, `BrowserComponent`, network calls to AI services, and every
   `ios.NS*UsageDescription` / `android.xpermissions` build hint.
2. Walk the sections below for each one. Record each finding as BLOCKER (Apple
   or Google rejects it), HIGH (often rejected) or NOTE.
3. Re-check every BLOCKER against the code before reporting it: a purchase
   flow that only sells physical goods does not need IAP, a permission that is
   requested nowhere does not need a purpose string. False alarms waste the
   user's attention.
4. Fix BLOCKERs and HIGHs, rebuild, and give the user the list of console
   answers (privacy labels, data safety, age rating, review notes) to approve.

## A. Payments (Apple 3.1.x, Google Payments policy)

- **Digital goods and subscriptions use the store's billing**:
  `com.codename1.payment.Purchase` (`Purchase.getInAppPurchase()`), which is
  StoreKit on iOS and Play Billing on Android. Physical goods and services
  used outside the app (rides, food, bookings) must NOT use IAP; take those
  through your payment provider. A link out to a web checkout for digital
  goods is allowed only in the storefronts that permit it; when in doubt, IAP.
- **Restore purchases**: a visible "Restore purchases" button that calls
  `Purchase.restore()`. Required for non-consumables and subscriptions.
- **Paywall disclosures**: price, billing period, what is included, that it
  renews automatically, and how to cancel, all on the screen with the buy
  button, plus links to the privacy policy and terms (EULA). A free trial says
  what is charged when it ends.
- **Manage subscription**: when `Purchase.isManageSubscriptionsSupported()`
  offer `manageSubscriptions(sku)` from settings.
- **The reviewer must be able to buy**: products configured and "Ready to
  Submit" in App Store Connect (and active in Play Console) before the build is
  submitted. Tell the user to do this; the build cannot.
- A paid feature must not depend on a permission the user can deny.

## B. Privacy and permissions (Apple 5.1.1, 5.1.5; Google User Data policy)

- **Privacy policy**: a URL in the store listing AND reachable inside the app
  (settings or sign-up screen). It says what is collected, why, who receives
  it, how long it is kept, and how to delete it. If the user has none, write
  one from the app's real behaviour, publish it (the backend can serve it, or
  any static page), and have the user approve the text.
- **Purpose strings**: every permission the app requests needs its
  `ios.NS...UsageDescription` build hint, and the text must say what the app
  does with it in this app ("Scan a receipt to add an expense"), never "This
  app needs camera access". Common ones: `ios.NSCameraUsageDescription`,
  `ios.NSPhotoLibraryUsageDescription`, `ios.NSMicrophoneUsageDescription`,
  `ios.NSLocationWhenInUseUsageDescription`,
  `ios.NSContactsUsageDescription`, `ios.NSFaceIDUsageDescription`,
  `ios.NSBluetoothAlwaysUsageDescription`, `ios.NSCalendarsUsageDescription`.
  A missing one crashes on first use and is an automatic rejection; one for an
  API the app never calls is a HIGH (reviewers ask why).
- **Privacy manifest**: the cloud iOS build writes `PrivacyInfo.xcprivacy`,
  including the required-reason entries for the APIs Codename One itself uses.
  An app that calls a required-reason API of its own through a native
  interface declares it with `ios.privacy.<Category>=<reason codes>`
  (see `build-hints.md`). Third-party SDKs in cn1libs or pods must ship their
  own manifests.
- **Ask in context**: request a permission when the user starts the feature
  that needs it, after a one-line explanation screen, never at launch. Handle
  "denied": the feature says how to turn the permission on in the system
  Settings, and the rest of the app keeps working.
- **Data minimization**: use the system pickers when you only need what the
  user picks (`Display.openGallery` instead of full photo library access).
- Location: "when in use" unless the feature truly works in the background,
  and then the background mode and an `Always` string that explains why.

## C. Tracking, third-party data and AI (Apple 5.1.2, ATT)

- **Tracking** (sharing data with others for ads or measurement across apps):
  needs App Tracking Transparency, `ios.NSUserTrackingUsageDescription`, and
  the prompt shown before any tracking SDK starts. Do not gate features on
  "Allow", and do not incentivize it. No tracking at all is simplest; then no
  ATT prompt and "Data Not Used to Track You".
- **Personal data sent to a third-party AI service** (OpenAI, Anthropic,
  Google, ...): disclose it on screen and get the user's opt-in before the
  first request, name the provider in the privacy policy, and list it in the
  privacy labels. This is enforced since November 2025.
- Analytics and crash SDKs: list what they collect in the privacy labels and
  in Google's Data safety form; never send emails, names or free text in
  analytics events.

## D. Accounts and sign-in (Apple 5.1.1(v), 4.8)

- **Account deletion inside the app**: any app that creates accounts needs a
  "Delete account" action in settings that deletes the account and its data on
  the server (the full-stack templates' backend needs an endpoint for it), not
  just a sign-out or an email link. Google Play also requires a web URL where
  users can request deletion without the app.
- **Sign in with Apple**: an iOS app that offers Google, Facebook or other
  third-party sign-in must also offer an equivalent private option; Sign in
  with Apple (`com.codename1.social.AppleSignIn`) is the usual answer. Revoke
  the Apple token when the account is deleted.
- Email/password or the app's own accounts alone need no Sign in with Apple.
- **Reviewer access**: if anything is behind a sign-in, create a demo account
  with sample data and put its credentials in the App Review notes (App Store
  Connect) and the "App access" section (Play Console). Missing credentials is
  one of the most common rejections. Features that need special hardware or a
  location get a short note or video link.
- Do not force sign-in before the app shows what it does, unless the app is
  account-based by nature (banking, messaging).

## E. User-generated content (Apple 1.2)

An app where users post content others see (chat, reviews, photos, profiles)
needs all four: a way to report content, a way to block users, a filter or
moderation for objectionable content, and published contact information.
Messaging and social templates (Parley) need these wired to the backend.

## F. Age rating and kids (Apple 2.3.6, 1.3)

Answer the age-rating questionnaire from what the code does: user-generated
content, unrestricted web access (`BrowserComponent` on arbitrary URLs),
gambling, mature themes. The answers go to the user to submit; mismatches are
rejected. Do not put the app in the Kids category unless the user asks, which
also forbids third-party analytics and ads.

## G. Health and fitness (Apple 5.1.3, 1.4.1)

No medical claims ("diagnoses", "treats", accuracy guarantees) in the app or
the listing. Health data stays out of advertising and analytics, and the
privacy policy names what is read. A workout or habit app without health
claims has nothing to do here.

## H. Push, ratings and Apple services (Apple 4.5.4, 5.6.1)

- Push notifications are never required to use the app and never carry
  advertising unless the user opted in to that explicitly.
- Ask for a rating with the system prompt only, never with a custom
  "rate 5 stars" screen or an incentive.
- No Apple product images or the Apple logo in the app or screenshots.

## I. Completeness (Apple 2.1, 2.3, 2.5; Google Minimum functionality)

- **No placeholders**: no "Lorem ipsum", "Coming soon" tabs, empty screens,
  test buttons, broken links, or sample data that looks like a demo. Every
  button in a screenshot works. This is the most common rejection of all.
- **The backend is live**: the full-stack templates need their server deployed
  and the app pointed at its public URL (not `localhost`) before submission;
  the reviewer's network is not yours. An app that fails to load is rejected.
- **iPad**: an iPhone app also runs on iPad; check the layout at iPad size in
  the simulator (`Sim.java` skin) so nothing is clipped. Screenshots for iPad
  are required when the app supports it.
- **Export compliance**: an app that uses only HTTPS and standard OS
  encryption sets `ios.appUsesNonExemptEncryption=false`, so each upload is
  not held waiting for the question.
- No mention of Android or other platforms in the iOS listing or app (and the
  reverse for Play). "Download on the App Store" links inside an Android app
  are rejected too.
- Remove debug logging of personal data; never ship API keys for paid
  services in the client -- call them from the backend.
- Version and build number go up with every upload
  (`codename1.version` in `codenameone_settings.properties`, or the
  `ios.bundleVersion` / `android.versionCode` build hints).

## J. Design (Apple 4.0-4.3)

- **Minimum functionality**: an app that is mostly a web page in a
  `BrowserComponent` is rejected. Codename One apps are native UI; keep it that
  way.
- **Templates are fine, clones are not**: an app started from a Codename One
  template must be changed into the user's own app (name, icon, brand, content,
  features). Shipping the template unchanged, or many near-identical apps, is
  rejected under 4.3 (spam).
- No other company's names, logos or trademarks in the app name, icon or
  keywords.

## Google Play specifics

- **Data safety form**: answer from the same inventory as Apple's privacy
  labels (what is collected, shared, encrypted in transit, deletable).
- **Target API level**: the cloud build targets the level Play currently
  requires; do not lower `android.targetSDKVersion`.
- **New personal developer accounts** must run a closed test with at least 12
  testers for 14 days before production access. Plan for it: submit to the
  internal or closed track first and tell the user early.
- **Permissions**: declare only what is used. Sensitive ones (background
  location, SMS, call log, all-files access, exact alarms) need a declaration
  form in Play Console and are often refused; avoid them.
- Content rating questionnaire and the target audience section, answered from
  the code as in section F.

## Before you submit

- [ ] Every screen in the screenshots works; no placeholders (I).
- [ ] Purpose strings for every permission requested, specific to this app (B).
- [ ] Privacy policy URL reachable in the app and in the listing (B).
- [ ] Account deletion in the app, and a deletion URL for Play, if there are
      accounts (D).
- [ ] Sign in with Apple, if any other third-party sign-in exists on iOS (D).
- [ ] Demo account in the review notes, if anything is behind a sign-in (D).
- [ ] IAP with restore and full disclosures, for digital goods (A).
- [ ] Backend deployed and reachable from the internet (I).
- [ ] `ios.appUsesNonExemptEncryption=false` when it applies (I).
- [ ] The user approved the listing text, screenshots, privacy labels, Data
      safety answers and age rating.

## After a rejection

Read the whole message: it names a guideline number. Fix the cause in the app
(not only the listing), rebuild with a higher build number, and reply in App
Store Connect's Resolution Center (the user sends it; draft the reply for
them) saying what changed and where to find it. If the reviewer misunderstood
the app, a short screen recording link in the reply resolves most cases. Do
not resubmit an unchanged build.

## License of the adapted material

The structure and the checks above are adapted from Better App Store Review:

```
MIT License

Copyright (c) 2026 Kobe Cortez

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
