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
package com.codenameone.examples.skirmish.push;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import java.util.List;

/// The notifications the game sends. Each one brings a player back for a
/// reason they care about; none is sent for the sake of sending.
// tag::skirmish-pushes[]
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
                .body(challenger + " is waiting for you in the arena")
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

    private void send(String username, PushNotification.Builder message) {
        List<PushTarget> targets = devices.targets(username);
        if (!targets.isEmpty()) {
            sender.send(targets, message.build(), null);
        }
    }
}
// end::skirmish-pushes[]
