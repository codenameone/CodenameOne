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
package com.codenameone.examples.skirmish.engage;

import com.codename1.analytics.invite.Invite;
import com.codename1.analytics.invite.InviteAttribution;
import com.codename1.analytics.invite.InviteListener;
import com.codename1.analytics.invite.InviteRequest;
import com.codename1.analytics.invite.Invites;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codenameone.examples.skirmish.api.InviteClaimDto;
import com.codenameone.examples.skirmish.api.InviteDto;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.ui.Rewards;
import com.codenameone.examples.skirmish.ui.Ui;

/// Invite a friend, through Codename One's `com.codename1.analytics.invite`.
///
/// The inviter's side mints a link with [Invites#create] -- the campaign, the
/// channel, and the player's own code from the server in the payload -- and
/// hands it to the native share sheet. The friend's side calls
/// [Invites#checkForInvite] on every start: a link that opened the installed
/// app, or one that went through the store first (the Play install referrer,
/// an iOS App Clip), comes back as an [InviteAttribution]. From then on every
/// analytics event the friend's app sends carries the campaign and the
/// referrer, so the invite funnel, retention and revenue per campaign are in
/// the Codename One console without the game counting anything itself.
///
/// The server's part is the reward: the code is kept until the friend is
/// signed in, then claimed, and the inviter is paid once the friend has
/// played. [Invites#conversion] marks those steps in analytics too.
public final class Friends {
    public static final String CAMPAIGN = "friends";
    private static final String PENDING = "skirmish.invite.";

    private Friends() {
    }

    // tag::skirmish-invites[]
    /// From the app's start(): pick up an invite this launch carries.
    public static void start() {
        Invites.setInviteListener(new InviteListener() {
            @Override
            public void inviteReceived(InviteAttribution a) {
                if (a.getPayload() == null || a.getPayload().length() == 0) {
                    // A campaign link from the console: attributed in
                    // analytics, nobody to reward.
                    return;
                }
                Preferences.set(PENDING + "code", a.getPayload());
                Preferences.set(PENDING + "campaign", a.getCampaign() == null ? "" : a.getCampaign());
                Preferences.set(PENDING + "channel", a.getChannel() == null ? "" : a.getChannel());
                Preferences.set(PENDING + "match", a.getMatchType() == null ? "" : a.getMatchType());
                if (Session.user() != null) {
                    Ui.later(Friends::claimPending);
                }
            }

            @Override
            public void attributionUnavailable(String reason) {
                // The ordinary case: most installs were not invited.
            }
        });
        Invites.checkForInvite();
    }
    // end::skirmish-invites[]

    /// Signed in: credit a waiting invite to this account, once.
    public static void claimPending() {
        final String code = Preferences.get(PENDING + "code", "");
        if (code.length() == 0 || Session.user() == null) {
            return;
        }
        InviteClaimDto claim = new InviteClaimDto(code, Preferences.get(PENDING + "campaign", ""),
                Preferences.get(PENDING + "channel", ""), Preferences.get(PENDING + "match", ""));
        Api.social().claim(claim, Net.to(reward -> {
            clear();
            Telemetry.inviteAccepted();
            Ui.later(() -> Rewards.show(reward, () -> { }));
        }, (status, message) -> {
            // Refused for good (an old account, a second claim, our own code)
            // or the code is unknown: forget it. Anything else is retried on
            // the next sign-in.
            if (status >= 400 && status < 500) {
                clear();
            }
            Log.p("Invite not credited: " + status + " " + message);
        }));
    }

    private static void clear() {
        Preferences.delete(PENDING + "code");
        Preferences.delete(PENDING + "campaign");
        Preferences.delete(PENDING + "channel");
        Preferences.delete(PENDING + "match");
    }

    // tag::skirmish-invite-share[]
    /// Opens the share sheet with the player's invite link.
    public static void share(InviteDto mine) {
        Invite invite = Invites.create(InviteRequest.create()
                .campaign(CAMPAIGN)
                .channel("share_sheet")
                .payload(mine.code())
                .title("Join me in Skirmish")
                .description("A real-time card battle. Build a deck and fight me.")
                .build());
        Invites.share(invite, "Battle me in Skirmish! You get " + mine.friendCoins()
                + " coins to start with: " + invite.getUrl());
        Telemetry.inviteSent(CAMPAIGN);
    }
    // end::skirmish-invite-share[]

    /// A link for a marketing campaign, made from the admin console: no
    /// player's code in it, so it rewards nobody, and everything the players
    /// it brings in do is reported under `campaign` in analytics.
    public static Invite campaignLink(String campaign) {
        return Invites.create(InviteRequest.create()
                .campaign(campaign)
                .channel("console")
                .title("Skirmish")
                .description("A real-time card battle")
                .build());
    }

    /// Marks a step of an invited player's way in, for the invite funnel:
    /// the first battle and the first win, once each. A no-op on an install
    /// no invite brought in.
    public static void battleFinished(int result) {
        once("first_battle");
        if (result > 0) {
            once("first_win");
        }
    }

    private static void once(String action) {
        String key = PENDING + "conv." + action;
        if (!Preferences.get(key, false)) {
            Preferences.set(key, true);
            Invites.conversion(action);
        }
    }
}
