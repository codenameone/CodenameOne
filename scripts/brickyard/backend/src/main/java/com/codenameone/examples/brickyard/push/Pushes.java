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
package com.codenameone.examples.brickyard.push;

import com.codename1.appkit.server.push.PushSender;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.brickyard.domain.Follow;
import com.codenameone.examples.brickyard.repository.FollowRepository;
import java.util.List;

/// The notifications the platform sends: a friend request, and a creator
/// someone follows publishing an update. A send that fails is the push
/// service's to report; it never fails the request that caused it.
@Component
public class Pushes {
    private final PushDevices devices;
    private final PushSender sender;
    private final FollowRepository follows;

    public Pushes(PushDevices devices, PushSender sender, FollowRepository follows) {
        this.devices = devices;
        this.sender = sender;
        this.follows = follows;
    }

    public void friendRequest(String to, String from) {
        send(to, PushNotification.builder()
                .title("Friend request")
                .body(from + " wants to be friends")
                .collapseKey("friends")
                .data("screen", "friends"));
    }

    /// Tells everyone following `creator` that `experienceId` was published.
    public void published(String creator, String experienceId, String title) {
        for (Follow f : follows.followers(creator)) {
            send(f.getUsername(), PushNotification.builder()
                    .title(creator + " published an update")
                    .body(title)
                    .collapseKey("publish-" + experienceId)
                    .data("experience", experienceId));
        }
    }

    private void send(String username, PushNotification.Builder message) {
        try {
            List<PushTarget> targets = devices.targets(username);
            if (!targets.isEmpty()) {
                sender.send(targets, message.build(), null);
            }
        } catch (RuntimeException err) {
            err.printStackTrace();
        }
    }
}
