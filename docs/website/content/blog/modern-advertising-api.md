---
title: A New Advertising API, Built From The Ground Up
slug: modern-advertising-api
url: /blog/modern-advertising-api/
date: '2026-06-08'
author: Shai Almog
description: A single pluggable advertising subsystem in the core replaces a stack of dead-end legacy mechanisms, covering every modern ad format with a working AdMob reference provider.
feed_html: '<img src="https://www.codenameone.com/blog/modern-advertising-api.jpg" alt="A New Advertising API, Built From The Ground Up" /> A single pluggable advertising subsystem in the core replaces a stack of dead-end legacy mechanisms, covering every modern ad format with a working AdMob reference provider.'
---

![A New Advertising API, Built From The Ground Up](/blog/modern-advertising-api.jpg)

Advertising support in Codename One had quietly rotted. It was spread across three mechanisms that no longer work: the original `com.codename1.ads` and `FullScreenAdService` APIs built on the long-dead InnerActive and V-Serv networks, the decade-old `google.adUnitId` and `mopubId` banner build hints (MoPub is gone), and an AdMob cn1lib that was interstitial-only, built on iOS APIs Google has since removed, busy-polled, and had no consent flow. None of them supported rewarded, rewarded-interstitial, app-open, or native formats, GDPR consent, iOS App Tracking Transparency, server-side reward verification, or mediation.

[PR #5169](https://github.com/codenameone/CodenameOne/pull/5169) replaces all of it with one pluggable, format-complete advertising subsystem in the core, plus modern reference providers that actually work!

![A native ad in a feed plus a banner, rendered by the new API using mock ads](/blog/modern-advertising-api/ads-feed.png)

## One API, every format

The public API lives in `com.codename1.ads`. `AdManager` is the entry point, and there is a type per format: `InterstitialAd`, `RewardedAd`, `RewardedInterstitialAd`, `AppOpenAd`, `BannerAd`, and `NativeAdLoader`. Consent is handled by `AdConsent` (UMP plus iOS ATT), and `AdConfig`, `AdRequest`, and `AdListener` round out the surface. The whole thing is event-driven and every callback is marshaled onto the EDT, so you never touch ad SDK threading.

A rewarded ad, the format people most often ask about, reads like this:

```java
AdConfig cfg = new AdConfig().testMode(true);
AdManager.initialize(cfg, ok -> {
    if (!ok) {
        return;
    }
    RewardedAd ad = new RewardedAd("your-rewarded-ad-unit-id");
    ad.setAdListener(new AdListener() {
        public void onLoaded() {
            ad.show(reward ->
                grantCoins(reward.getType(), reward.getAmount()));
        }
    });
    ad.load();
});
```

Consent is a first-class step rather than an afterthought:

```java
AdConsent.requestConsent(status -> {
    if (AdConsent.canRequestAds()) {
        loadAds();
    }
});
```

## Pluggable by design

The provider side is an SPI (Service Provider Interface) in `com.codename1.ads.spi`: a network-agnostic `AdProvider` plus session interfaces. Discovery is zero-wiring. `AdManager` resolves the provider through a single call to `install()`, so adding an ad cn1lib to your project auto-registers it with no setup code. AdMob, AppLovin MAX, Unity LevelPlay, or your own mediation layer can be swapped without touching app code.

A few things only the core can do, and they are wired in directly:

```java
// Show an interstitial on form transitions, no more often than every two minutes
AdManager.bindInterstitialOnTransition(interstitial, 120000);

// Show an app-open ad when the app returns to the foreground
AdManager.enableAppOpenAds(appOpenAd);
```

Banners are peer-backed components, so they sit in your layout like any other component.

## AdMob as the reference provider

The reference provider is a real, modern AdMob integration shipped as `maven/cn1-admob`, modeled on the ML Kit cn1libs. It targets Google Mobile Ads v24 and up on Android, the current `GAD*` APIs with UMP and ATT on iOS, and AdMob mediation works transparently behind it. You will need to add these to your build hints:

```
android.xapplication=<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="ca-app-pub-XXXXXXXX~YYYYYYYY"/>
ios.plistInject=<key>GADApplicationIdentifier</key><string>ca-app-pub-XXXXXXXX~YYYYYYYY</string>
```

Those hints set the Android `APPLICATION_ID` manifest meta-data and the iOS `GADApplicationIdentifier`. For iOS you should also declare your `SKAdNetworkItems` and an `NSUserTrackingUsageDescription` (the ATT prompt copy) the same way.

## Debugging Ads

The simulator ships a placeholder provider so you can exercise every format and the consent flow, without a device. The `AdsSample` in the samples repo runs every format against the simulator placeholder if you want to see the flows before you wire up a real account.

A reminder that applies to any ad integration: if you do server-side reward verification, the verification has to happen on your server. Do not trust a reward granted purely on the client, and do not embed any server secret in the app.

The previous deep dive covered [WebSockets, gRPC, and GraphQL in the core](/blog/websockets-grpc-and-graphql/), and the [release post](/blog/mac-native-grpc-graphql-and-fewer-open-issues/) has the full index. Tomorrow's post, the last of the run, is about background work, push topics, and richer notifications.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
