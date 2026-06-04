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
import com.codename1.ads.OnUserEarnedRewardListener;
import com.codename1.ads.RewardItem;
import com.codename1.ads.RewardedAd;
import com.codename1.ads.RewardedInterstitialAd;
import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.util.SuccessCallback;

/// Demonstrates the modern Codename One advertising API
/// ([com.codename1.ads.AdManager] and friends). Add the `cn1-admob` library to
/// the project and the AdMob provider registers itself automatically; in the
/// simulator the AdMob library's JavaSE module renders labelled placeholders so
/// every flow (load, show, dismiss, reward, banner, consent) can be exercised
/// without a device or an AdMob account.
///
/// Replace the ad unit ids below with your own (the strings here are Google's
/// public test ad unit ids).
public class AdsSample {
    // Google's documented test ad unit ids.
    private static final String INTERSTITIAL_UNIT = "ca-app-pub-3940256099942544/1033173712";
    private static final String REWARDED_UNIT = "ca-app-pub-3940256099942544/5224354917";
    private static final String REWARDED_INTERSTITIAL_UNIT = "ca-app-pub-3940256099942544/5354046379";
    private static final String APP_OPEN_UNIT = "ca-app-pub-3940256099942544/5575463023";
    private static final String BANNER_UNIT = "ca-app-pub-3940256099942544/6300978111";

    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);

        // 1) Initialize, 2) gather consent, then ads may be requested.
        AdManager.initialize(new AdConfig().testMode(true), new SuccessCallback<Boolean>() {
            @Override
            public void onSucess(Boolean ready) {
                AdConsent.requestConsent(new SuccessCallback<Integer>() {
                    @Override
                    public void onSucess(Integer status) {
                        Log.p("Ad consent status: " + status + ", canRequestAds=" + AdConsent.canRequestAds());
                    }
                });
            }
        });
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Ads", new BorderLayout());

        if (AdManager.getProvider() == null) {
            hi.add(BorderLayout.CENTER, new com.codename1.components.SpanLabel(
                    "No ad provider is installed. Add the cn1-admob library to the project "
                    + "to see placeholder ads in the simulator and real ads on device."));
            hi.show();
            return;
        }

        com.codename1.ui.Container buttons = new com.codename1.ui.Container(BoxLayout.y());
        buttons.add(interstitialButton());
        buttons.add(rewardedButton());
        buttons.add(rewardedInterstitialButton());
        buttons.add(appOpenButton());
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
                ad.show(new OnUserEarnedRewardListener() {
                    @Override public void onUserEarnedReward(RewardItem r) {
                        Log.p("Earned reward: " + r.getAmount() + " " + r.getType());
                    }
                });
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
                ad.show(new OnUserEarnedRewardListener() {
                    @Override public void onUserEarnedReward(RewardItem r) {
                        Log.p("Earned reward: " + r.getAmount() + " " + r.getType());
                    }
                });
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

    public void stop() {
        current = getCurrentForm();
        if (current instanceof com.codename1.ui.Dialog) {
            ((com.codename1.ui.Dialog) current).dispose();
            current = getCurrentForm();
        }
    }

    private static Form getCurrentForm() {
        return com.codename1.ui.CN.getCurrentForm();
    }

    public void destroy() {
    }
}
