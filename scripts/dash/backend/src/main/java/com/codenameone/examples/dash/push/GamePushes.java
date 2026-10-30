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
package com.codenameone.examples.dash.push;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import java.util.ArrayList;
import java.util.List;

/// The notifications the game sends. Each one brings a player back for a
/// reason they care about; none is sent for the sake of sending.
// tag::dash-pushes[]
@Component
public class GamePushes {
    private final PushDevices devices;
    private final PushSender sender;

    public GamePushes(PushDevices devices, PushSender sender) {
        this.devices = devices;
        this.sender = sender;
    }

    /// Someone opened the host's challenge link while the host was not waiting.
    public void challengeAccepted(String host, String challenger, String code) {
        send(host, PushNotification.builder()
                .title("Challenge accepted")
                .body(challenger + " is waiting for you at the start line")
                .collapseKey("challenge-" + code)
                .ttlSeconds(600)
                .data("challenge", code));
    }

    /// The daily rewarded-ad allowance is back.
    public void rewardsReady(String username) {
        send(username, PushNotification.builder()
                .title("Free chest ready")
                .body("Your daily rewards are back. Come and claim them")
                .collapseKey("rewards")
                .data("screen", "shop"));
    }

    /// An admin learns about a new flag without watching the console.
    public void flagRaised(List<String> admins, String username, String reason) {
        for (String admin : admins) {
            send(admin, PushNotification.builder()
                    .title("Player flagged")
                    .body(username + ": " + reason)
                    .collapseKey("flags")
                    .data("screen", "admin")
                    .data("player", username));
        }
    }

    /// Someone joined through the player's invite.
    public void friendJoined(String inviter, String friend) {
        send(inviter, PushNotification.builder()
                .title("Your friend joined")
                .body(friend + " joined Dash through your invite")
                .collapseKey("friends")
                .data("screen", "friends"));
    }

    /// That friend finished a race, and the inviter was paid for it.
    public void friendPlayed(String inviter, String friend, int coins) {
        send(inviter, PushNotification.builder()
                .title("Invite reward")
                .body(friend + " played their first race. " + coins + " coins are yours")
                .collapseKey("friends")
                .data("screen", "friends"));
    }

    /// An operator's message to many players at once, through the same
    /// Codename One push service as everything else. `screen` and `campaign`
    /// ride in the data: the app opens that screen and reports the open to
    /// Codename One analytics under the campaign. Answers how many of the
    /// players had a device to send to.
    public int broadcast(List<String> usernames, String id, String title, String body, String screen,
            String campaign) {
        List<PushTarget> targets = new ArrayList<PushTarget>();
        int reached = 0;
        for (String u : usernames) {
            List<PushTarget> mine = devices.targetsOf(u);
            if (!mine.isEmpty()) {
                reached++;
                targets.addAll(mine);
            }
        }
        if (!targets.isEmpty()) {
            sender.send(targets, PushNotification.builder()
                    .id(id)
                    .title(title)
                    .body(body)
                    .collapseKey("broadcast")
                    .ttlSeconds(24 * 60 * 60)
                    .data("screen", screen)
                    .data("campaign", campaign)
                    .data("broadcast", id)
                    .build(), null);
        }
        return reached;
    }

    private void send(String username, PushNotification.Builder message) {
        List<PushTarget> targets = devices.targets(username);
        if (!targets.isEmpty()) {
            sender.send(targets, message.build(), null);
        }
    }
}
// end::dash-pushes[]
