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
package com.codenameone.examples.papercut.push;

import com.codename1.appkit.server.Text;
import com.codename1.appkit.server.push.PushSender;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.papercut.Clock;
import com.codenameone.examples.papercut.Ids;
import com.codenameone.examples.papercut.api.BroadcastDto;
import com.codenameone.examples.papercut.domain.Broadcast;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.repository.BroadcastRepository;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// A push from the console to every player, or to the ones not seen for some
/// days. Sent through the configured [PushSender]: the console prints it on a
/// development server, the recorder keeps it for the tests.
@Component
public class Broadcasts {
    private static final int PAGE = 500;

    private final ProfileRepository profiles;
    private final PushDevices devices;
    private final PushSender sender;
    private final BroadcastRepository broadcasts;

    public Broadcasts(ProfileRepository profiles, PushDevices devices, PushSender sender,
            BroadcastRepository broadcasts) {
        this.profiles = profiles;
        this.devices = devices;
        this.sender = sender;
        this.broadcasts = broadcasts;
    }

    /// Sends it and records it. `targets` in the answer counts players with at
    /// least one device, not devices: that is the number an admin means.
    public BroadcastDto send(BroadcastDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to send");
        }
        String title = Text.required(request.title(), 80, "the title");
        String body = Text.required(request.body(), 255, "the message");
        int days = Math.max(0, request.days());
        Instant seenBefore = days == 0 ? Instant.ofEpochMilli(Long.MAX_VALUE)
                : Instant.ofEpochMilli(Clock.now() - days * Clock.DAY);
        String id = Ids.next();
        List<PushTarget> targets = new ArrayList<PushTarget>();
        int reached = 0;
        for (int page = 0;; page++) {
            List<Profile> batch = profiles.audience(seenBefore, new PageRequest(page, PAGE));
            for (Profile p : batch) {
                List<PushTarget> mine = devices.targetsOf(p.getUsername());
                if (!mine.isEmpty()) {
                    reached++;
                    targets.addAll(mine);
                }
            }
            if (batch.size() < PAGE) {
                break;
            }
        }
        if (!targets.isEmpty()) {
            sender.send(targets, PushNotification.builder().id(id).title(title).body(body)
                    .collapseKey("broadcast").ttlSeconds(24 * 60 * 60).data("broadcast", id)
                    // The app opens today's sheet from it (Notifications).
                    .data("screen", "daily").build(), null);
        }
        Broadcast row = new Broadcast();
        row.setId(id);
        row.setTitle(title);
        row.setBody(body);
        row.setDays(days);
        row.setTargets(reached);
        row.setCreatedAt(Instant.now());
        broadcasts.insert(row);
        return new BroadcastDto(id, title, body, days, reached, row.getCreatedAt().toEpochMilli());
    }
}
