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

0. Run the audit from the project root:
   `java .agent-skills/codename-one/tools/StoreAudit.java --checklist submission-checklist.md`.
   It reads the build hints and scans the sources for most of the checks
   below (purpose strings, restore, account deletion, Sign in with Apple, AI
   consent, moderation, placeholders, local/staging/IPv4 backends, keys in the
   client, insecure token storage, sensitive Android permissions, debuggable
   builds, unused background modes, ATT, web wrappers, other-store mentions)
   and writes a Markdown checklist: one line per finding, then the console
   answers and listing items that depend on what the app uses. Add
   `--network --url <privacy-url> --url <support-url>` once the listing URLs
   exist: it fetches them and resolves the backend hosts. Exit status 1 means
   a BLOCKER remains (`--strict` also fails on HIGH). It is a heuristic, so
   step 3 still applies, and it cannot see what the sections below say is the
   user's to answer.
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
  that needs it, after a one-line explanation screen, never at launch. The
  explanation screen's button says "Continue", never "Allow" -- a custom
  screen that imitates the system prompt is rejected under 5.1.1(iv). Handle
  "denied": the feature says how to turn the permission on in the system
  Settings, and the rest of the app keeps working.
- **Data minimization**: use the system pickers when you only need what the
  user picks (`Display.openGallery` instead of full photo library access).
  Google Play enforces this: `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` need a
  declaration and are refused unless media access is the app's core purpose,
  and for apps targeting API 37 `READ_CONTACTS` is expected only when the
  contact picker cannot do the job. Never add them through
  `android.xpermissions` for a feature that only picks one item.
- **Purpose strings must describe this app.** A string copied from another
  app or template ("where your order is delivered" in a chat app) is as
  rejected as a vague one. Read each one against the feature that uses it.
- Location: "when in use" unless the feature truly works in the background,
  and then the background mode and an `Always` string that explains why.

## C. Tracking, third-party data and AI (Apple 5.1.2, ATT)

- **Tracking** (sharing data with others for ads or measurement across apps):
  needs App Tracking Transparency, `ios.NSUserTrackingUsageDescription`, and
  the prompt shown before any tracking SDK starts. Do not gate features on
  "Allow", and do not incentivize it. No tracking at all is simplest; then no
  ATT prompt and "Data Not Used to Track You".
- **Generative AI output** needs moderation: filter prompts and output, and
  let the user report a response (Apple 1.2, Google AI-Generated Content).
  On Play, an app that generates images or video also needs safeguards
  against non-consensual intimate imagery, and its test account must reach
  every generation feature.
- **Personal data sent to a third-party AI service** (OpenAI, Anthropic,
  Google, ...): disclose it on screen and get the user's opt-in before the
  first request, name the provider in the privacy policy, and list it in the
  privacy labels. Apple added this to guideline 5.1.2(i) in November 2025.
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
  the Apple token when the account is deleted. Use Apple's button style, at
  least as prominent as the others, and do not ask again for the name or
  email Apple already returned. Users may hide their email: register your
  sending domain for Apple's private email relay, or mail to those users
  never arrives (account recovery and receipts break).
- Email/password or the app's own accounts alone need no Sign in with Apple.
- **Reviewer access**: if anything is behind a sign-in, create a demo account
  with sample data and put its credentials in the App Review notes (App Store
  Connect) and the "App access" section (Play Console). Missing credentials is
  one of the most common rejections. Features that need special hardware or a
  location get a short note or video link. The review notes also say what
  to test and where it is; an incomplete note is its own rejection (2.1).
- Do not force sign-in before the app shows what it does, unless the app is
  account-based by nature (banking, messaging).

## E. User-generated content (Apple 1.2)

An app where users post content others see (chat, reviews, photos, profiles)
needs all four: a way to report content, a way to block users, a filter or
moderation for objectionable content, and published contact information.
Messaging and social templates (Parley) need these wired to the backend.
Apple expects action on a report within 24 hours. If App Store Connect asks
whether the app has social-media features, answer it from what the code
does before submitting.

## F. Age rating, age assurance and kids (Apple 2.3.6, 1.3)

Answer the age-rating questionnaire from what the code does: user-generated
content, unrestricted web access (`BrowserComponent` on arbitrary URLs),
gambling, mature themes. Apple announced a new questionnaire in 2025 (13+,
16+ and 18+ ratings and new questions), required from early 2026; an app
rated under the old one must answer the new one before its next update. Google removes apps with no
content rating. The answers go to the user to submit; mismatches are
rejected. Do not put the app in the Kids category unless the user asks, which
also forbids third-party analytics and ads.

US states with App Store Accountability Acts (Utah, Texas and others) make
the stores share an age range, and the developer must respect it. An app
with age-restricted content or features reads the store's signal (Apple's
Declared Age Range, Google's Play Age Signals) through a native interface,
uses it only to gate content -- never for ads or analytics -- and handles a
parent revoking consent. An app with nothing age-restricted needs none of
this; say so to the user rather than adding it.

## G. Health and fitness (Apple 5.1.3, 1.4.1)

No medical claims ("diagnoses", "treats", accuracy guarantees) in the app or
the listing. Health data stays out of advertising and analytics, and the
privacy policy names what is read. It must not reach iCloud either (5.1.3):
keep it on the server or in storage excluded from backup. A workout or habit app without health
claims has nothing to do here.

## H. Push, ratings and Apple services (Apple 4.5.4, 5.6.1)

- Push notifications are never required to use the app and never carry
  advertising unless the user opted in to that explicitly.
- Ask for a rating with the system prompt only, never with a custom
  "rate 5 stars" screen or an incentive.
- No Apple product images or the Apple logo in the app or screenshots, and
  no device frames around the app in a preview video (2.3.4).
- No text criticising Apple or its review process in the app or listing.
- Declare only the capabilities the app uses: a push or location background
  mode, iCloud or another entitlement with no feature behind it is rejected
  (2.4.5). `ios.background_modes` is the usual culprit.
- A Live Activity shows the state of one thing the user started, not ads or
  a stream of promotions (4.5.3).

## I. Completeness (Apple 2.1, 2.3, 2.5; Google Minimum functionality)

- **No placeholders**: no "Lorem ipsum", "Coming soon" tabs, empty screens,
  test buttons, broken links, or sample data that looks like a demo. Every
  button in a screenshot works. This is the most common rejection of all.
- **The backend is live**: the full-stack templates need their server deployed
  and the app pointed at its public URL (not `localhost`) before submission;
  the reviewer's network is not yours. An app that fails to load is rejected.
  It is the **production** server: a build pointed at staging or a tunnel
  (ngrok) is rejected when it goes down, and a "cloud" feature that only works
  in development is a 2.1 rejection.
- **IPv6**: Apple reviews on an IPv6-only network (NAT64). Host names work;
  an IPv4 address literal in a URL (`http://203.0.113.7/api`) does not
  (2.5.5).
- **No hidden features**: everything that will ship is in the reviewed build.
  A feature switched on remotely after approval, or a different app behind a
  flag, is a 2.5.19 rejection and can cost the developer account.
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

## J. Security (Apple 2.5, Google Data security)

- Tokens and passwords live in `SecureStorage`, never in `Preferences` or
  `Storage`, which are plain files and on Android are copied to the cloud
  backup (`android.allowBackup` defaults to true).
- A deep link (`AppArg`) never performs a sensitive action -- pay, delete,
  sign in, change settings -- without validating it and confirming on screen.
- No API keys for paid services in the client, and no debuggable build:
  `android.onDeviceDebug` is for development only.
- Every third-party SDK (cn1lib, pod, Gradle dependency) is one you chose and
  can list in the privacy labels; an unknown one is a supply-chain risk and a
  labels mismatch.

## K. Accessibility (Apple Accessibility Nutrition Labels, Google quality)

App Store Connect now asks which accessibility features the app supports, and
an untrue answer is a rejection. Before answering: run `Sim.java verify` on
every screen (unlabelled controls, cut-off text, contrast in light and dark,
large text), check VoiceOver and TalkBack read each control's purpose, text
scales with the system font size, and animation respects reduce motion. Claim
only what passes.

## L. Design (Apple 4.0-4.3)

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
  labels (what is collected, shared, encrypted in transit, deletable). A
  mismatch between the form and what the app or its SDKs do is a removal.
- **Developer verification**: Google requires the package name to be
  registered to a verified developer; an unregistered one cannot be
  installed on certified devices. Apps in regulated categories (finance,
  health, gambling, VPN, government) need an **organization** account, not a
  personal one.
- **Payments**: digital goods use Play Billing (`Purchase`) as on iOS.
  Donations to the developer go through Play Billing too; links to Patreon
  or PayPal for that are refused. An AI agent in the app must not complete a
  digital purchase outside Play Billing either.
- **Target API level**: the cloud build targets the level Play currently
  requires; do not lower `android.targetSDKVersion`.
- **New personal developer accounts** must run a closed test with at least 12
  testers for 14 days before production access. Plan for it: submit to the
  internal or closed track first and tell the user early.
- **Permissions**: declare only what is used. Sensitive ones (background
  location, SMS, call log, all-files access, exact alarms, query all
  packages, accessibility service, overlays, photo/video, contacts) need a
  declaration form in Play Console and are often refused; avoid them.
- **Large screens**: Android 16 ignores orientation locks on tablets and
  foldables. Check the layout on a tablet skin in both orientations.
- **Restore Credentials**: Play's quality guidance expects a signed-in user to
  stay signed in after moving to a new device; at least keep sign-in quick.
- Content rating questionnaire and the target audience section, answered from
  the code as in section F.

## Regional and legal requirements

None of these is a code change, but each one blocks distribution:

- **EU**: App Store Connect and Play Console ask for trader status under the
  Digital Services Act; a trader's address and contact are shown on the
  listing. An app selling physical goods in the EU names a contact for
  product safety (GPSR). Subscriptions sold to EU consumers need an easy way
  to withdraw.
- **Cancelling** a subscription is as easy as signing up (FTC and US state
  laws, Apple 3.1.2): no phone call or email to cancel.
- **Loot boxes**: show the odds before purchase (both stores).
- **Real-money gambling**: a licence for every country it is offered in
  (Brazil requires its own), and on Play an organization account.

## The submission checklist process

Do this for every store submission, not just the first:

1. Run `StoreAudit.java --checklist submission-checklist.md` and fix every
   BLOCKER and HIGH, re-checking each against the code. Re-run until it
   exits 0 with `--strict`, or the remaining HIGHs are explained.
2. Walk the sections above for each feature the audit lists under "Uses";
   anything the tool cannot see (a paywall's wording, a purpose string that
   describes the wrong feature) is yours to read.
3. Go through the checklist's "Confirm with the developer" items with the
   user. They answer the console questions; you draft the answers from the
   code.
4. Once the listing exists, run it again with `--network --url ...` for the
   privacy, support and marketing URLs.
5. Leave the completed checklist with the user and hand them the review
   notes (demo account, what to test). Then build the release with a higher
   build number and submit.

The short form, for a quick pass:

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
- [ ] Production backend, reachable by host name over IPv6 (I).
- [ ] No unused background modes or entitlements (H), no sensitive Android
      permissions without a declaration (Google Play).
- [ ] Generative AI: consent, moderation and reporting (C).
- [ ] Age rating on the 2026 questionnaire, content rating on Play,
      social-media question (E, F).
- [ ] `StoreAudit.java` exits 0.
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
