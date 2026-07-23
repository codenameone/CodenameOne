# Evidence map

Source: `docs/website/content/blog/modern-advertising-api.md`
Canonical: https://www.codenameone.com/blog/modern-advertising-api/

## Thesis

Replacing legacy ad integrations with a pluggable provider-neutral subsystem

## Supported beats

- **One API, every format:** The public API lives in com.codename1.ads. AdManager is the entry point, and there is a type per format: InterstitialAd, RewardedAd, RewardedInterstitialAd, AppOpenAd, BannerAd, and NativeAdLoader. Consent is handled by AdConsent (UMP plus iOS ATT), and AdConfig, AdRequest, and AdListener round out the surface.
- **Pluggable by design:** The provider side is an SPI (Service Provider Interface) in com.codename1.ads.spi: a network-agnostic AdProvider plus session interfaces. Discovery is zero-wiring. AdManager resolves the provider through a single call to install(), so adding an ad cn1lib to your project auto-registers it with no setup code.
- **AdMob as the reference provider:** The reference provider is a real, modern AdMob integration shipped as maven/cn1-admob, modeled on the ML Kit cn1libs. It targets Google Mobile Ads v24 and up on Android, the current GAD APIs with UMP and ATT on iOS, and AdMob mediation works transparently behind it.
- **Debugging Ads:** The simulator ships a placeholder provider so you can exercise every format and the consent flow, without a device. The AdsSample in the samples repo runs every format against the simulator placeholder if you want to see the flows before you wire up a real account.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5169
