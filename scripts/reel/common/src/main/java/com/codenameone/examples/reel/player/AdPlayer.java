/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.player;

import com.codename1.media.Media;
import com.codename1.media.StreamRequest;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.reel.api.AdBreakDto;
import com.codenameone.examples.reel.api.AdDto;
import com.codenameone.examples.reel.api.AdEventDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Art;
import com.codenameone.examples.reel.ui.Lang;
import java.util.List;

/// One ad break: the server chooses the ads for it when the break comes (so
/// a campaign that ran out of budget meanwhile is not shown), and hears of
/// the impression, a skip, a click and the end, which is what it bills on.
final class AdPlayer {
    private AdPlayer() {
    }

    static void play(final PlayerForm player, AdBreakDto b, final Runnable then) {
        Api.playback().ads(b.id(), player.sessionId(), Net.to(list -> next(player, list, 0, then),
                (code, message) -> then.run()));
    }

    private static void next(final PlayerForm player, final List<AdDto> ads, final int index, final Runnable then) {
        if (player.closed()) {
            return;
        }
        if (ads == null || index >= ads.size()) {
            player.setAd(null);
            player.stage().removeAll();
            Media main = player.main();
            if (main != null) {
                player.stage().add(BorderLayout.CENTER, main.getVideoComponent());
            }
            player.form().revalidate();
            then.run();
            return;
        }
        final AdDto ad = ads.get(index);
        final Runnable after = () -> next(player, ads, index + 1, then);
        new StreamRequest(Art.url(ad.mediaUrl())).setOnCompletion(() -> CN.callSerially(() -> {
            event(player, ad, "complete");
            finish(player, after);
        })).createAsync().ready(m -> CN.callSerially(() -> {
            if (player.closed()) {
                m.cleanup();
                return;
            }
            player.setAd(m);
            Container frame = new Container(new BorderLayout());
            frame.add(BorderLayout.CENTER, m.getVideoComponent());
            final Button skip = new Button(Lang.tr("Ad"), "RlAdSkip");
            skip.setName("skipAd");
            skip.setEnabled(false);
            Container bar = new Container(new BorderLayout());
            bar.add(BorderLayout.WEST, new com.codename1.ui.Label(ad.advertiser(), "RlAdLabel"));
            bar.add(BorderLayout.EAST, skip);
            if (ad.clickUrl() != null && ad.clickUrl().length() > 0) {
                Button visit = new Button(Lang.tr("Learn more"), "RlAdLabel");
                visit.setName("adClick");
                visit.addActionListener(e -> {
                    event(player, ad, "click");
                    CN.execute(ad.clickUrl());
                });
                bar.add(BorderLayout.CENTER, visit);
            }
            frame.add(BorderLayout.SOUTH, bar);
            player.stage().removeAll();
            player.stage().add(BorderLayout.CENTER, frame);
            player.form().revalidate();
            m.play();
            event(player, ad, "impression");
            final long started = System.currentTimeMillis();
            final UITimer[] count = {null};
            count[0] = UITimer.timer(500, true, player.form(), () -> {
                int left = ad.skippableAfterSeconds() - (int) ((System.currentTimeMillis() - started) / 1000);
                if (ad.skippableAfterSeconds() <= 0) {
                    skip.setText(Lang.tr("Ad"));
                } else if (left > 0) {
                    skip.setText(Lang.tr("Skip in {0}", String.valueOf(left)));
                } else {
                    skip.setText(Lang.tr("Skip ad"));
                    skip.setEnabled(true);
                }
                if (player.closed()) {
                    count[0].cancel();
                }
            });
            skip.addActionListener(e -> {
                count[0].cancel();
                event(player, ad, "skip");
                finish(player, after);
            });
        })).except(err -> CN.callSerially(after));
    }

    private static void finish(PlayerForm player, Runnable after) {
        Media m = playerAd(player);
        if (m != null) {
            m.cleanup();
        }
        player.setAd(null);
        after.run();
    }

    private static Media playerAd(PlayerForm player) {
        return player.currentAd();
    }

    private static void event(PlayerForm player, AdDto ad, String kind) {
        Api.playback().adEvent(new AdEventDto(player.sessionId(), ad.id(), ad.campaignId(), kind),
                Net.to(ok -> { }, (code, message) -> { }));
    }
}
