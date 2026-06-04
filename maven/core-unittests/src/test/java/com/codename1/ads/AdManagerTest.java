package com.codename1.ads;

import com.codename1.ads.spi.AdConsentController;
import com.codename1.ads.spi.AdProvider;
import com.codename1.ads.spi.AdSessionCallback;
import com.codename1.ads.spi.BannerAdSession;
import com.codename1.ads.spi.FullScreenAdSession;
import com.codename1.ads.spi.NativeAdProvider;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.system.NativeLookup;
import com.codename1.ads.AdCallback;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Exercises the modern advertising abstraction against a fake provider, with no
/// native code. Because @FormTest runs the body on the EDT, the EDT-marshalled
/// listener callbacks fire synchronously.
class AdManagerTest extends UITestBase {

    /// Records the lifecycle events forwarded to the public listener.
    private static final class RecordingListener extends AdListener {
        final List<String> events = new ArrayList<String>();
        AdError lastError;

        @Override public void onLoaded() { events.add("loaded"); }
        @Override public void onFailedToLoad(AdError e) { events.add("failed"); lastError = e; }
        @Override public void onShown() { events.add("shown"); }
        @Override public void onShowFailed(AdError e) { events.add("showFailed"); lastError = e; }
        @Override public void onDismissed() { events.add("dismissed"); }
        @Override public void onImpression() { events.add("impression"); }
        @Override public void onClicked() { events.add("clicked"); }
    }

    private static final class FakeFullScreen implements FullScreenAdSession {
        AdSessionCallback cb;
        boolean loaded;
        ServerSideVerificationOptions ssv;
        boolean autoShow;

        @Override public void setCallback(AdSessionCallback callback) { this.cb = callback; }
        @Override public void setServerSideVerificationOptions(ServerSideVerificationOptions o) { ssv = o; }
        @Override public void load(AdRequest request) { loaded = true; cb.onLoaded(); }
        @Override public boolean isLoaded() { return loaded; }
        @Override public void show() { cb.onShown(); cb.onImpression(); }
        @Override public void setAutoShowOnForeground(boolean enabled) { autoShow = enabled; }
        @Override public void dispose() { loaded = false; }

        void fireReward() { cb.onUserEarnedReward(new RewardItem("coins", 5)); }
        void fireDismissed() { loaded = false; cb.onDismissed(); }
    }

    private static class FakeProvider implements AdProvider {
        FakeFullScreen lastFullScreen;
        boolean initialized;

        @Override public String getName() { return "Fake"; }
        @Override public boolean isSupported() { return true; }
        @Override public boolean isFormatSupported(AdFormat format) { return format != AdFormat.NATIVE; }
        @Override public void initialize(AdConfig config, AdCallback<Boolean> onComplete) {
            initialized = true;
            onComplete.onResult(Boolean.TRUE);
        }
        @Override public FullScreenAdSession createFullScreenAd(AdFormat format, String adUnitId) {
            lastFullScreen = new FakeFullScreen();
            return lastFullScreen;
        }
        @Override public BannerAdSession createBanner(String adUnitId, int bannerSize, int widthDp) {
            return null;
        }
        @Override public AdConsentController getConsentController() { return null; }
    }

    @FormTest
    void testProviderRegistrationAndFormatSupport() {
        FakeProvider p = new FakeProvider();
        AdManager.registerProvider(p);
        assertSame(p, AdManager.getProvider());
        assertTrue(AdManager.isSupported(AdFormat.INTERSTITIAL));
        assertTrue(AdManager.isSupported(AdFormat.REWARDED));
        assertFalse(AdManager.isSupported(AdFormat.NATIVE));
    }

    @FormTest
    void testInitializeInvokesCallback() {
        FakeProvider p = new FakeProvider();
        AdManager.registerProvider(p);
        final boolean[] result = {false};
        AdManager.initialize(new AdConfig().testMode(true), new AdCallback<Boolean>() {
            @Override public void onResult(Boolean value) { result[0] = value.booleanValue(); }
        });
        assertTrue(p.initialized);
        assertTrue(result[0]);
    }

    @FormTest
    void testInterstitialLifecycleDispatch() {
        FakeProvider p = new FakeProvider();
        AdManager.registerProvider(p);
        InterstitialAd ad = new InterstitialAd("unit/interstitial");
        RecordingListener l = new RecordingListener();
        ad.setAdListener(l);

        ad.load();
        assertTrue(l.events.contains("loaded"));
        assertTrue(ad.isLoaded());

        ad.show();
        assertTrue(l.events.contains("shown"));
        assertTrue(l.events.contains("impression"));

        p.lastFullScreen.fireDismissed();
        assertTrue(l.events.contains("dismissed"));
        assertFalse(ad.isLoaded());
    }

    @FormTest
    void testRewardedDeliversReward() {
        FakeProvider p = new FakeProvider();
        AdManager.registerProvider(p);
        RewardedAd ad = new RewardedAd("unit/rewarded");
        final RewardItem[] earned = {null};
        ad.setOnUserEarnedRewardListener(new OnUserEarnedRewardListener() {
            @Override public void onUserEarnedReward(RewardItem reward) { earned[0] = reward; }
        });
        ad.load();
        ad.show();
        p.lastFullScreen.fireReward();
        assertNotNull(earned[0]);
        assertEquals("coins", earned[0].getType());
        assertEquals(5, earned[0].getAmount());
    }

    @FormTest
    void testServerSideVerificationForwarded() {
        FakeProvider p = new FakeProvider();
        AdManager.registerProvider(p);
        RewardedAd ad = new RewardedAd("unit/rewarded");
        ad.setServerSideVerificationOptions(new ServerSideVerificationOptions("user-1", "level=7"));
        ad.load();
        assertNotNull(p.lastFullScreen.ssv);
        assertEquals("user-1", p.lastFullScreen.ssv.getUserId());
        assertEquals("level=7", p.lastFullScreen.ssv.getCustomData());
    }

    @FormTest
    void testNoProviderFailsGracefully() {
        NativeLookup.setVerbose(false);
        AdManager.registerProvider(null);
        InterstitialAd ad = new InterstitialAd("unit/none");
        RecordingListener l = new RecordingListener();
        ad.setAdListener(l);
        ad.load();
        assertTrue(l.events.contains("failed"));
        assertEquals(AdError.CODE_UNSUPPORTED, l.lastError.getCode());
    }

    @FormTest
    void testBindInterstitialOnTransitionPreloads() {
        FakeProvider p = new FakeProvider();
        AdManager.registerProvider(p);
        InterstitialAd ad = new InterstitialAd("unit/interstitial");
        AdManager.bindInterstitialOnTransition(ad, 60000);
        // binding triggers an initial load
        assertNotNull(p.lastFullScreen);
        assertTrue(ad.isLoaded());
    }

    @FormTest
    void testEnableAppOpenAdsSetsAutoShow() {
        FakeProvider p = new FakeProvider();
        AdManager.registerProvider(p);
        AppOpenAd ad = new AppOpenAd("unit/appopen");
        AdManager.enableAppOpenAds(ad);
        assertNotNull(p.lastFullScreen);
        assertTrue(p.lastFullScreen.autoShow);
    }

    @FormTest
    void testAdRequestBuilding() {
        AdRequest r = new AdRequest()
                .addKeyword("games")
                .addKeyword("puzzle")
                .contentUrl("https://example.com/level")
                .nonPersonalized(true);
        assertEquals("games,puzzle", r.getKeywordString());
        assertEquals("https://example.com/level", r.getContentUrl());
        assertTrue(r.isNonPersonalized());
        assertEquals(2, r.getKeywords().size());
    }

    @FormTest
    void testAdConfigTargeting() {
        AdConfig c = new AdConfig()
                .testMode(true)
                .addTestDevice("ABC")
                .addTestDevice("DEF")
                .tagForChildDirectedTreatment(AdConfig.TAG_FALSE)
                .maxAdContentRating(AdConfig.RATING_PG);
        assertTrue(c.isTestMode());
        assertEquals("ABC,DEF", c.getTestDeviceIdString());
        assertEquals(AdConfig.TAG_FALSE, c.getTagForChildDirectedTreatment());
        assertEquals(AdConfig.RATING_PG, c.getMaxAdContentRating());
    }

    @FormTest
    void testBannerUnsupportedProviderAddsNoView() {
        FakeProvider p = new FakeProvider(); // createBanner returns null
        AdManager.registerProvider(p);
        BannerAd banner = new BannerAd("unit/banner");
        RecordingListener l = new RecordingListener();
        banner.setAdListener(l);
        banner.load();
        // provider returns a null banner session -> a graceful failure, no child added
        assertTrue(l.events.contains("failed"));
        assertEquals(0, banner.getComponentCount());
    }

    /// A provider that additionally supports native ads.
    private static final class FakeNativeProvider extends FakeProvider implements NativeAdProvider {
        @Override public boolean isFormatSupported(AdFormat format) { return true; }
        @Override public void loadNativeAd(String adUnitId, AdRequest request,
                AdCallback<NativeAd> onSuccess, AdCallback<AdError> onError) {
            onSuccess.onResult(new NativeAd("Headline", "Body", "Install", "Acme", null, null, 4.5));
        }
    }

    @FormTest
    void testNativeAdLoadDeliversAssets() {
        FakeNativeProvider p = new FakeNativeProvider();
        AdManager.registerProvider(p);
        assertTrue(NativeAdLoader.isSupported());
        final NativeAd[] loaded = {null};
        final AdError[] failed = {null};
        new NativeAdLoader("unit/native").load(null,
                ad -> loaded[0] = ad,
                err -> failed[0] = err);
        assertNull(failed[0]);
        assertNotNull(loaded[0]);
        assertEquals("Headline", loaded[0].getHeadline());
        assertEquals("Install", loaded[0].getCallToAction());
    }

    @FormTest
    void testNativeAdUnsupportedReportsError() {
        FakeProvider p = new FakeProvider(); // not a NativeAdProvider
        AdManager.registerProvider(p);
        assertFalse(NativeAdLoader.isSupported());
        final AdError[] failed = {null};
        new NativeAdLoader("unit/native").load(null, ad -> {}, err -> failed[0] = err);
        assertNotNull(failed[0]);
        assertEquals(AdError.CODE_UNSUPPORTED, failed[0].getCode());
    }
}
