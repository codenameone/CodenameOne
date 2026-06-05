package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ads.AdManager;
import com.codename1.ads.BannerAd;
import com.codename1.ads.NativeAd;
import com.codename1.ads.NativeAdLoader;
import com.codename1.ads.mock.MockAdProvider;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// Screenshot coverage for the advertising API. Uses the deterministic
/// [MockAdProvider] (no network, fixed text/colours/sizes) so the rendered
/// banner and native-ad feed row are pixel stable. Demonstrates the two inline
/// formats in a content-feed layout: a native ad styled as a feed row, and an
/// anchored banner pinned to the bottom.
public class AdsScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() throws Exception {
        MockAdProvider.install();

        Form form = createForm("Ads", new BorderLayout(), "AdsScreen");

        Container feed = new Container(BoxLayout.y());
        feed.getAllStyles().setBgColor(0x111827);
        feed.getAllStyles().setBgTransparency(255);
        feed.getAllStyles().setPadding(6, 6, 6, 6);

        feed.add(feedRow("Local team wins the cup", "Highlights and reaction from a dramatic final."));
        feed.add(feedRow("Five hikes near the city", "Trail guides for every fitness level."));

        // Native ad: rendered with our own components so it sits in the feed.
        NativeAdLoader loader = new NativeAdLoader("mock-native");
        loader.load(null, ad -> feed.addComponent(nativeRow(ad)), err -> { });

        feed.add(feedRow("Best coffee in town", "We ranked the ten most popular spots."));

        form.add(BorderLayout.CENTER, feed);

        // Anchored banner pinned to the bottom of the form.
        BannerAd banner = new BannerAd("mock-banner");
        form.add(BorderLayout.SOUTH, banner);
        banner.load();

        form.show();
        return AdManager.getProvider() != null;
    }

    private Component feedRow(String title, String body) {
        Container row = new Container(BoxLayout.y());
        row.getAllStyles().setBgColor(0x1f2937);
        row.getAllStyles().setBgTransparency(255);
        row.getAllStyles().setMargin(0, 4, 0, 0);
        row.getAllStyles().setPadding(6, 6, 6, 6);
        Label t = new Label(title);
        styleText(t, 0xf9fafb);
        Label b = new Label(body);
        styleText(b, 0x9ca3af);
        row.add(t);
        row.add(b);
        return row;
    }

    /// Builds a feed row from native ad assets, clearly labelled as an ad.
    private Component nativeRow(NativeAd ad) {
        Container row = new Container(BoxLayout.y());
        row.getAllStyles().setBgColor(0x1f2937);
        row.getAllStyles().setBgTransparency(255);
        row.getAllStyles().setMargin(0, 4, 0, 0);
        row.getAllStyles().setPadding(6, 6, 6, 6);

        Label sponsored = new Label("Sponsored");
        styleText(sponsored, 0xfbbf24);

        Label headline = new Label(ad.getHeadline());
        styleText(headline, 0xf9fafb);

        Label body = new Label(ad.getBody());
        styleText(body, 0x9ca3af);

        Button cta = new Button(ad.getCallToAction());
        cta.getAllStyles().setFgColor(0xffffff);
        cta.getAllStyles().setBgColor(0x2563eb);
        cta.getAllStyles().setBgTransparency(255);

        row.add(sponsored);
        row.add(headline);
        row.add(body);
        row.add(cta);
        return row;
    }

    /// Foreground colour on a transparent background so the dark feed shows
    /// through (the default Label UIID paints an opaque background).
    private static void styleText(Label l, int fg) {
        l.getAllStyles().setFgColor(fg);
        l.getAllStyles().setBgTransparency(0);
    }
}
