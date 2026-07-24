---
title: "Pricing"
date: 2020-09-02
slug: "pricing"
layout: "pricing"
description: "Compare Codename One cloud-build plans, quotas, Crash Protection, push, native sources, store submission, and support. The framework remains open source and royalty free."
keywords:
  - "Codename One Pricing"
---

### What is Codename One? How does it work?
Codename One is an open-source framework for building native applications in Java. It provides a shared runtime and custom-rendered UI across mobile, desktop, web, watch, TV, and vehicle targets.

Platform builds use native toolchains. You can also call platform SDKs or embed heavyweight native views when a shared component is not the right fit.

### Is Codename One free? Is it open source?
Yes. Codename One is open source and can be used commercially and non-commercially with no royalties or restrictions.

The optional cloud build service includes a free quota and lets developers on Windows/Linux build native iOS apps.

### What are the limits of the free version?
These limits apply to cloud builds only.

- 100 free cloud build credits each month.
- 1 build credit per build (iOS uses 8 credits due to Mac build costs).
- 8.5 MB user JAR size limit.

You can still use generated apps commercially. Paid plans mainly add higher quotas and advanced cloud features.

### Are JavaScript builds limited to a paid plan?
No. The ParparVM JavaScript target is open source and available for cloud builds on every account tier, including Free.

You can also build it locally without a Codename One account:

```bash
mvn -pl javascript package \
  -Dcodename1.platform=javascript \
  -Dcodename1.buildTarget=local-javascript
```

ParparVM is the default. Add `javascript.port=teavm` to `codenameone_settings.properties` if you need to compare a regression with the original TeaVM target.

### Can I cancel or change plan anytime?
Yes. Codename One includes a 30-day money-back guarantee for paid purchases.

### What payment methods do you accept?
Credit card and debit card through the Codename One secure signup flow.

For annual Pro/Enterprise subscriptions, SWIFT transfer and invoicing are available. Use the site chat to request a pro-forma invoice.

### Are there limits on number of apps?
No. Pricing is per developer seat, not per app.

### Are there limits on submitting my app to the App Store or Google Play?
Manual submission is always unlimited and free. You can upload as many builds as you like to App Store Connect or the Google Play Console yourself, on any plan including Free — Codename One never sits between you and the stores.

The limits shown above apply only to the optional *automated* submission feature: one-click submit from the build console and pushing your store listing as code (`cn1:metadata-push`). That's a convenience that delivers the binary and updates the listing for you; higher plans raise how many automated submissions and metadata updates you get per month, and add production review and zero-touch auto-submit. It never restricts your ability to publish manually.

### Will my app still work if I cancel subscription?
Yes. Built apps continue to work.

Cloud runtime services (e.g., push) require an active subscription.

### Is source code sent to the cloud?
No source code is sent for normal builds. Cloud builds process compiled bytecode.

Native platform sources may be uploaded when required for native compilation.

### What is Crash Protection?
Pro and Enterprise plans include a managed crash-reporting service that captures uncaught exceptions in your shipping app, symbolicates the stack trace, and files a deduplicated issue on your own GitHub repository. There is no separate dashboard — you triage from GitHub Issues. Crashes hit by multiple devices roll up into a single issue with a counter and a "last seen" timestamp.

### How do I turn Crash Protection on?
Add `codename1.crashProtection.enabled=true` to your `codenameone_settings.properties`, install the Codename One Crash Protection GitHub App on the repo you want crashes to land in, and map your package to that repo from `cloud.codenameone.com/console/index.html → Repo mappings`. See the [Developer Guide](/developer-guide) for the full Crash Protection chapter, including the on-device `CrashProtection` API.

### What are the storage limits for Crash Protection?
Symbol bundles (Android `mapping.txt`, iOS dSYM, native-port debug info) are gzipped and stored against your account's quota:

- **Pro:** 100 MB compressed, 3-week retention
- **Enterprise:** 500 MB compressed, 6-week retention

`mapping.txt` compresses ~10x and dSYMs ~2x, so the quota typically holds several recent release builds per app. You can free space at any time by removing an old build's symbols (or a whole app's enrollment) from the *Tracked apps* tab in the console.

### Where do my users' crashes end up?
On your GitHub repository, as issues. Each unique stack-trace fingerprint becomes one issue; subsequent occurrences bump a counter on the same issue (not a new one). Close the issue when the underlying bug is fixed; if the same fingerprint recurs later, the issue is reopened.

Codename One never reads or persists the unscrubbed crash payload — emails are partially redacted on the device before transmission, and the issue body is the only canonical record.

### Can I disable Crash Protection on specific platforms?
Yes. Per-platform opt-outs are independent of the master switch — set e.g. `codename1.crashProtection.and.enabled=false` to skip Android while still capturing iOS / Mac / Linux / Windows crashes.

### What is Commerce?
Commerce is an optional managed service for apps that sell in-app purchases or subscriptions. It validates store receipts server-side, normalizes the subscription lifecycle across Apple and Google into one state machine (renewals, cancellations, refunds, billing retries, grace periods), forwards lifecycle webhooks to your backend, and gives you a revenue console with MRR, churn, cohorts and LTV. Your app then asks one store-agnostic question — *does this user have this entitlement right now?* It's available on every plan, including Free.

### Do I need Commerce to sell in-app purchases? Is it required?
**No. Commerce is 100% optional.** In-app purchases and subscriptions work without it through the standard Codename One `Purchase` API — purchases always go through the platform store. If you prefer to validate receipts and track subscription state yourself, you can run your own home-grown server and never touch Commerce at all. Commerce exists only to save you from building and operating that receipt-validation/state-tracking server yourself; it is a convenience, never a gate on selling.

### What does Commerce cost?
Nothing extra — it's part of your existing plan, charged as a flat subscription. Codename One does not take a percentage of your sales and never touches your money (the stores, or your own payment processor for physical goods, handle that). What varies by plan is the monthly *validated transaction volume*:

- **Free:** $200 / month
- **Basic:** $10,000 / month
- **Pro:** $200,000 / month
- **Enterprise:** unlimited

Volume is measured in validated transactions normalized to USD via daily exchange rates, per developer seat (not per app).

### What happens if I exceed my Commerce volume cap?
It degrades, it never blocks. Once you pass the monthly cap, the service stops server-validating and stops metering for the rest of the month, and the device SDK automatically falls back to store-direct — the store-signed receipt still grants the entitlement locally. **A real purchase is never rejected.** You simply lose the server cross-check and console reporting until the next month (or until you move to a higher plan).

### How do I turn Commerce on?
It's on by default for cloud builds — there's nothing to wire up, since it reuses the build key your app already carries. Drive purchases through `CommerceManager`, connect your store credentials and notification URLs with the guided **Setup** wizard in the console, and gate features with `CommerceManager.isEntitled("...")`. See the [Developer Guide](/developer-guide) for the full Commerce chapter. To opt out entirely, set the `commerce.cloud.enabled=false` build hint.
