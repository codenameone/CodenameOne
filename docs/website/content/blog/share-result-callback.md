---
title: Share Result Callbacks (And An iOS Share Extension Authoring Helper)
slug: share-result-callback
url: /blog/share-result-callback/
date: '2026-06-02'
author: Shai Almog
description: Display.share() and ShareButton finally tell you what the user did. A new ShareResult lets you observe SHARED_TO(packageName) / DISMISSED / FAILED outcomes. iOS uses UIActivityViewController.completionWithItemsHandler; Android uses Intent.createChooser with an IntentSender. Plus a Maven-plugin helper that generates an iOS Share Extension target programmatically.
feed_html: '<img src="https://www.codenameone.com/blog/share-result-callback.jpg" alt="Share Result Callbacks (And An iOS Share Extension Authoring Helper)" /> Display.share() and ShareButton finally tell you what the user did. A new ShareResult observes SHARED_TO(packageName) / DISMISSED / FAILED, plus a Maven-plugin helper that generates an iOS Share Extension target programmatically.'
---

![Share Result Callbacks (And An iOS Share Extension Authoring Helper)](/blog/share-result-callback.jpg)

The native share sheet has been one of those small gaps in Codename One that nobody complains about in isolation, but that hits you the day you try to do something useful with the result. The framework has had `Display.share(...)` and `ShareButton` since approximately forever. What it has not had is a way to know whether the user actually shared the content, where they shared it to, or whether they hit Cancel and walked away. [PR #5036](https://github.com/codenameone/CodenameOne/pull/5036) closes that.

It also brings something I have wanted for a different reason: a programmatic way to generate the iOS Share Extension target that an app needs in order to *receive* shared content from other apps. The two pieces fit together well enough that they ship in the same PR.

## The result callback

`com.codename1.share.ShareResult` is the new type. `ShareResultListener` is the SAM you register. The shape mirrors the rest of the framework's outcome-typed callbacks:

```java
ShareButton btn = new ShareButton();
btn.setTextToShare("Look at this fox");
btn.setImageToShare("/fox.jpg");
btn.setShareResultListener(result -> {
    switch (result.getStatus()) {
        case SHARED_TO:
            // result.getTargetPackage() is "com.whatsapp" / "com.tinder.android" /
            // "com.apple.mobilemail" / etc. on iOS and Android. Use it to attribute
            // your share funnel.
            track("share_completed", result.getTargetPackage());
            break;
        case DISMISSED:
            track("share_dismissed");
            break;
        case FAILED:
            track("share_failed", result.getError());
            break;
    }
});
```

The same listener works on `Display.share(...)`:

```java
Display.getInstance().share(
        text, image, mimeType,
        result -> trackShareOutcome(result));
```

On iOS the implementation routes through `UIActivityViewController.completionWithItemsHandler`. Apple's API hands you a `UIActivityType` (a string like `com.apple.UIKit.activity.PostToFacebook` or `com.apple.UIKit.activity.Message` or, for third-party apps, the bundle identifier of the destination), plus a boolean indicating whether the user actually completed the activity. We normalise that into `SHARED_TO(packageName)` or `DISMISSED` on the framework side.

On Android the implementation routes through `Intent.createChooser` with an `IntentSender` callback, which is the API path that landed in API 22. The `IntentSender` is invoked with the `ComponentName` of the chosen target, so the framework sees the package name of the receiving app. Earlier-API devices fall back to `DISMISSED` when the chooser closes without confirmation, which is the best the platform exposes.

The practical reason this matters: share funnels are a metric every consumer app tracks, and "we showed the sheet but we have no idea what happened next" is the version of that funnel that ends in "we have no data". The new callback closes the funnel.

## The iOS Share Extension authoring helper

The second half of the PR is the part I want to call out separately. iOS apps that want to *receive* shared content from other apps need a Share Extension target. The Share Extension is a separate bundle inside the .ipa, with its own `Info.plist`, its own entitlements, an App Group string that connects it to the host app, and a `ShareViewController.swift` (or .m / .h, depending on your preference) that handles the incoming payload. Historically the recommendation was to bootstrap that target inside Xcode by hand, paste the result into your Codename One project's `ios/app_extensions/` directory, and let the build server's extractor consume it. That works. It is also a workflow nobody enjoys.

The new `IOSShareExtensionBuilder` in the Maven plugin generates the whole bundle programmatically:

```java
new IOSShareExtensionBuilder()
    .bundleIdentifier("com.example.myapp.share")
    .displayName("MyApp")
    .appGroup("group.com.example.myapp")
    .acceptedContent(SharedContent.PUBLIC_URL, SharedContent.PUBLIC_IMAGE)
    .writeTo(targetDir);
```

You get back a complete `.ios.appext` bundle: an `Info.plist` with the right `NSExtension` activation rules for the content types you accept, the App Group entitlement, a `ShareViewController.swift` that reads the incoming payload, writes it through `UserDefaults(suiteName:)`, and dismisses, plus the `buildSettings.properties` the rest of the iOS build pipeline expects. The result composes with the existing `IPhoneBuilder.extractAppExtensions` pipeline, so apps that use the builder do not need to bootstrap anything in Xcode and apps that already have a hand-rolled extension keep working.

The Swift `ShareViewController` is intentionally minimal. Its job is to land the payload in the App Group's `UserDefaults` (or in the App Group container if the payload is a file) and dismiss; the host app reads it on next launch via the matching `NSUserDefaults(suiteName:)` API on the Codename One side. That is the smallest possible contract that lets your app handle "user shared a URL to me from Safari" without my having to write a full Swift UI on your behalf.

## Relationship to PR #3427

This PR is complementary to the older [#3427](https://github.com/codenameone/CodenameOne/pull/3427) discussion of provisioning-profile generation for extensions. The builder here emits exactly the artefact the existing server-side extractor consumes; if a future `ios/app_extensions/` directory pipeline lands, the same builder feeds into it. The provisioning-profile / certificate-wizard piece of #3427 is deliberately out of scope. The right home for that is the build server, not the Maven plugin.

## Wrapping up

Two small things this week: a callback that closes a hole in the share funnel, and a builder that turns a Share Extension target into one Maven invocation. The dev guide coverage is under [The-Components-Of-Codename-One.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/The-Components-Of-Codename-One.asciidoc) under the `ShareButton` entry.

Tomorrow: JUnit 5 tests for Codename One apps.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
