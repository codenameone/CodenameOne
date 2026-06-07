package com.codename1.samples;

import com.codename1.ads.AdConfig;
import com.codename1.ads.AdConsent;
import com.codename1.ads.AdError;
import com.codename1.ads.AdFormat;
import com.codename1.ads.AdListener;
import com.codename1.ads.AdManager;
import com.codename1.ads.AppOpenAd;
import com.codename1.ads.BannerAd;
import com.codename1.ads.InterstitialAd;
import com.codename1.ads.NativeAd;
import com.codename1.ads.NativeAdLoader;
import com.codename1.ads.RewardedAd;
import com.codename1.ads.RewardedInterstitialAd;
import com.codename1.components.SpanLabel;
import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

/// Demonstrates the Codename One advertising API ([com.codename1.ads.AdManager]
/// and friends). Add an ad library to the project and enable it once at startup,
/// e.g. with the AdMob library:
///
/// ```java
/// AdMobProvider.install(); // from the cn1-admob library
/// ```
///
/// or the deterministic mock provider used in tests/screenshots:
///
/// ```java
/// MockAdProvider.install(); // from the cn1-ads-mock library
/// ```
///
/// This sample only references the framework API, so it compiles without any ad
/// library; when no provider is installed it shows guidance instead of ads.
///
/// The ad unit ids below are Google's public test ids; replace them with yours.
public class AdsSample {
    private static final String INTERSTITIAL_UNIT = "ca-app-pub-3940256099942544/1033173712";
    private static final String REWARDED_UNIT = "ca-app-pub-3940256099942544/5224354917";
    private static final String REWARDED_INTERSTITIAL_UNIT = "ca-app-pub-3940256099942544/5354046379";
    private static final String APP_OPEN_UNIT = "ca-app-pub-3940256099942544/5575463023";
    private static final String BANNER_UNIT = "ca-app-pub-3940256099942544/6300978111";
    private static final String NATIVE_UNIT = "ca-app-pub-3940256099942544/2247696110";

    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);

        // Enable a provider here, e.g. AdMobProvider.install() or
        // MockAdProvider.install(). Then: initialize -> gather consent -> load.
        AdManager.initialize(new AdConfig().testMode(true), ready ->
                AdConsent.requestConsent(status ->
                        Log.p("Ad consent status: " + status + ", canRequestAds=" + AdConsent.canRequestAds())));
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Ads", new BorderLayout());

        if (AdManager.getProvider() == null) {
            hi.add(BorderLayout.CENTER, new SpanLabel(
                    "No ad provider is installed. Call AdMobProvider.install() (cn1-admob) "
                    + "or MockAdProvider.install() (cn1-ads-mock) at startup to see ads."));
            hi.show();
            return;
        }

        Container buttons = new Container(BoxLayout.y());
        buttons.add(interstitialButton());
        buttons.add(rewardedButton());
        buttons.add(rewardedInterstitialButton());
        buttons.add(appOpenButton());
        buttons.add(nativeFeedButton());
        hi.add(BorderLayout.CENTER, buttons);

        // An anchored adaptive banner pinned to the bottom of the form.
        if (AdManager.isSupported(AdFormat.BANNER)) {
            BannerAd banner = new BannerAd(BANNER_UNIT);
            hi.add(BorderLayout.SOUTH, banner);
            banner.load();
        }

        hi.show();
    }

    private Button interstitialButton() {
        Button b = new Button("Show Interstitial");
        final InterstitialAd ad = new InterstitialAd(INTERSTITIAL_UNIT);
        ad.setAdListener(new AdListener() {
            @Override public void onLoaded() { ad.show(); }
            @Override public void onDismissed() { Log.p("Interstitial dismissed"); }
            @Override public void onFailedToLoad(AdError e) { Log.p("Interstitial failed: " + e); }
        });
        b.addActionListener(e -> ad.load());
        return b;
    }

    private Button rewardedButton() {
        Button b = new Button("Show Rewarded");
        final RewardedAd ad = new RewardedAd(REWARDED_UNIT);
        ad.setAdListener(new AdListener() {
            @Override public void onLoaded() {
                ad.show(reward -> Log.p("Earned reward: " + reward.getAmount() + " " + reward.getType()));
            }
            @Override public void onFailedToLoad(AdError e) { Log.p("Rewarded failed: " + e); }
        });
        b.addActionListener(e -> ad.load());
        return b;
    }

    private Button rewardedInterstitialButton() {
        Button b = new Button("Show Rewarded Interstitial");
        final RewardedInterstitialAd ad = new RewardedInterstitialAd(REWARDED_INTERSTITIAL_UNIT);
        ad.setAdListener(new AdListener() {
            @Override public void onLoaded() {
                ad.show(reward -> Log.p("Earned reward: " + reward.getAmount() + " " + reward.getType()));
            }
            @Override public void onFailedToLoad(AdError e) { Log.p("Rewarded interstitial failed: " + e); }
        });
        b.addActionListener(e -> ad.load());
        return b;
    }

    private Button appOpenButton() {
        Button b = new Button("Enable App Open Ads");
        b.addActionListener(e -> AdManager.enableAppOpenAds(new AppOpenAd(APP_OPEN_UNIT)));
        return b;
    }

    private Button nativeFeedButton() {
        Button b = new Button("Open News Feed (Native Ad)");
        b.addActionListener(e -> showNativeFeed());
        return b;
    }

    /// Native ads shine in content driven screens. Here a news feed lists
    /// articles and slots a native ad in as another row - same styling as the
    /// articles, but clearly labelled "Sponsored" - so it reads as content
    /// rather than an interruption. The same pattern fits social feeds, store
    /// listings, chat lists and search results.
    private void showNativeFeed() {
        final Form feedForm = new Form("News Feed", BoxLayout.y());
        feedForm.getToolbar().addCommandToLeftBar("Back", null, e -> start());

        feedForm.add(article("Local team wins the cup", "Highlights and reaction from a dramatic final."));
        feedForm.add(article("Five hikes near the city", "Trail guides for every fitness level."));

        if (NativeAdLoader.isSupported()) {
            new NativeAdLoader(NATIVE_UNIT).load(null,
                    ad -> {
                        feedForm.addComponent(sponsoredRow(ad));
                        feedForm.revalidate();
                    },
                    err -> Log.p("Native ad failed: " + err));
        }

        feedForm.add(article("Best coffee in town", "We ranked the ten most popular spots."));
        feedForm.show();
    }

    private Component article(String title, String body) {
        Container row = new Container(BoxLayout.y());
        row.add(new Label(title));
        SpanLabel b = new SpanLabel(body);
        row.add(b);
        return row;
    }

    private Component sponsoredRow(NativeAd ad) {
        Container row = new Container(BoxLayout.y());
        Label sponsored = new Label("Sponsored");
        sponsored.getAllStyles().setFgColor(0xc0a000);
        row.add(sponsored);
        row.add(new Label(ad.getHeadline()));
        row.add(new SpanLabel(ad.getBody()));
        if (ad.getCallToAction() != null) {
            row.add(new Button(ad.getCallToAction()));
        }
        return row;
    }

    public void stop() {
        current = com.codename1.ui.CN.getCurrentForm();
        if (current instanceof com.codename1.ui.Dialog) {
            ((com.codename1.ui.Dialog) current).dispose();
            current = com.codename1.ui.CN.getCurrentForm();
        }
    }

    public void destroy() {
    }
}
