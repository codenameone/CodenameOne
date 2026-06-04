/// The modern, pluggable Codename One advertising API.
///
/// Start from [com.codename1.ads.AdManager], which selects an ad network
/// provider (supplied by an ad cn1lib such as the Google AdMob library) and
/// exposes the full set of contemporary ad formats:
///
/// - [com.codename1.ads.BannerAd] — an inline/adaptive banner component
/// - [com.codename1.ads.InterstitialAd] — full screen ads at natural breaks
/// - [com.codename1.ads.RewardedAd] — opt-in ads that grant a reward
/// - [com.codename1.ads.RewardedInterstitialAd] — incentivized transition ads
/// - [com.codename1.ads.AppOpenAd] — ads shown when the app is foregrounded
/// - [com.codename1.ads.NativeAd] — assets rendered with your own components
///
/// Privacy consent (GDPR via the User Messaging Platform, and iOS App Tracking
/// Transparency) is handled by [com.codename1.ads.AdConsent]. Ad networks plug
/// in by implementing [com.codename1.ads.spi.AdProvider]; the design stays
/// network agnostic so AdMob, AppLovin MAX, Unity LevelPlay or a custom
/// mediation layer can be swapped without touching application code.
///
/// #### Legacy API
///
/// The original banner ad service in this package — [com.codename1.ads.AdsService]
/// and [com.codename1.ads.InnerActive], along with `com.codename1.components.Ads`
/// and `com.codename1.impl.FullScreenAdService` — is deprecated. It targets ad
/// networks that no longer exist and predates consent management. Use
/// [com.codename1.ads.AdManager] for all new development.
package com.codename1.ads;
