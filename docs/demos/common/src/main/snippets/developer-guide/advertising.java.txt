// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::advertising-java-001[]
AdMobProvider.install();    // from cn1-admob
// AppLovinProvider.install();  // from cn1-applovin
// LevelPlayProvider.install(); // from cn1-unity-levelplay
// MockAdProvider.install();    // from cn1-ads-mock (tests / simulator)
// end::advertising-java-001[]

// tag::advertising-java-002[]
AdMobProvider.install();
AdManager.initialize(new AdConfig().testMode(true), ready ->
    AdConsent.requestConsent(status -> {
        if (AdConsent.canRequestAds()) {
            // safe to load ads now
        }
    }));
// end::advertising-java-002[]

// tag::advertising-java-003[]
BannerAd banner = new BannerAd("ca-app-pub-xxx/yyy");
form.add(BorderLayout.SOUTH, banner);
banner.load();
// end::advertising-java-003[]

// tag::advertising-java-004[]
InterstitialAd ad = new InterstitialAd("ca-app-pub-xxx/yyy");
ad.setAdListener(new AdListener() {
    public void onLoaded() { ad.show(); }
    public void onDismissed() { ad.load(); } // preload the next
});
ad.load();
// end::advertising-java-004[]

// tag::advertising-java-005[]
AdManager.bindInterstitialOnTransition(new InterstitialAd("ca-app-pub-xxx/yyy"), 60000);
// end::advertising-java-005[]

// tag::advertising-java-006[]
RewardedAd ad = new RewardedAd("ca-app-pub-xxx/yyy");
ad.setServerSideVerificationOptions(new ServerSideVerificationOptions(userId, "level=7"));
ad.setAdListener(new AdListener() {
    public void onLoaded() {
        ad.show(reward -> grantCoins(reward.getAmount()));
    }
});
ad.load();
// end::advertising-java-006[]

// tag::advertising-java-007[]
AdManager.enableAppOpenAds(new AppOpenAd("ca-app-pub-xxx/yyy"));
// end::advertising-java-007[]

// tag::advertising-java-008[]
if (NativeAdLoader.isSupported()) {
    new NativeAdLoader("ca-app-pub-xxx/yyy").load(null,
        ad -> feed.addComponent(buildSponsoredRow(ad)),  // your own layout
        err -> Log.p(err.toString()));
}
// end::advertising-java-008[]

// tag::advertising-java-009[]
MockAdProvider.install();
// end::advertising-java-009[]
