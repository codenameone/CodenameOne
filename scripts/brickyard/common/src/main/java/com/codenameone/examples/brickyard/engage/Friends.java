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
package com.codenameone.examples.brickyard.engage;

import com.codename1.analytics.invite.Invite;
import com.codename1.analytics.invite.InviteAttribution;
import com.codename1.analytics.invite.InviteListener;
import com.codename1.analytics.invite.InviteRequest;
import com.codename1.analytics.invite.Invites;
import com.codename1.components.InviteButton;
import com.codename1.gaming.GameServices;
import com.codename1.io.Preferences;
import com.codename1.ui.CN;
import com.codenameone.examples.brickyard.AppConfig;

/// Invites and presence. An invite is a link into one experience; the
/// experience id rides as the invite's payload, and a player who opens the
/// app from it lands on that experience's page after signing in.
public final class Friends {
    static final String CAMPAIGN = "friend_invite";
    private static final String PENDING = "brickyard.invite.experience";

    private Friends() {
    }

    /// Listens for an invite the app was opened from, on every start.
    public static void start() {
        Invites.setInviteListener(new InviteListener() {
            @Override
            public void inviteReceived(InviteAttribution a) {
                String id = AppConfig.experienceFromLink(a.getPayload());
                if (id == null) {
                    id = a.getPayload();
                }
                if (id != null && id.length() > 0) {
                    Preferences.set(PENDING, id);
                    Telemetry.inviteAccepted();
                }
            }

            @Override
            public void attributionUnavailable(String reason) {
                // Opened without an invite, or the platform cannot say: no-op.
            }
        });
        Invites.checkForInvite();
    }

    /// The experience an invite asked to open, once; null when there was none.
    public static String takePending() {
        String id = Preferences.get(PENDING, "");
        if (id.length() == 0) {
            return null;
        }
        Preferences.delete(PENDING);
        return id;
    }

    static InviteRequest request(String experienceId, String title) {
        return InviteRequest.create()
                .campaign(CAMPAIGN)
                .channel("share_sheet")
                .payload(AppConfig.experienceLink(experienceId))
                .title("Play " + title + " with me")
                .description("Join me in " + title + " on Brickyard.")
                .build();
    }

    /// Opens the share sheet with a link into an experience.
    public static void invite(String experienceId, String title) {
        Invite invite = Invites.create(request(experienceId, title));
        Invites.share(invite, "Come play " + title + " with me on Brickyard: " + invite.getUrl());
        Telemetry.inviteSent(experienceId);
    }

    /// A ready-made invite button for an experience page.
    public static InviteButton button(String experienceId, String title) {
        InviteButton b = new InviteButton("Invite friends");
        b.setUIID("SecondaryButton");
        b.setName("inviteButton");
        b.setCampaign(CAMPAIGN);
        b.setChannel("experience_page");
        b.setPayload(AppConfig.experienceLink(experienceId));
        b.setMessage("Come play " + title + " with me on Brickyard!");
        return b;
    }

    /// Tells the platform's game service what the player is doing, where one
    /// is connected. Nothing happens elsewhere.
    public static void presence(String title) {
        if (CN.isSimulator()) {
            return;
        }
        if (title == null) {
            GameServices.clearPresence();
        } else {
            GameServices.setPresence("experience", title);
        }
    }
}
