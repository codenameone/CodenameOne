/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
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
                    .collapseKey("broadcast").ttlSeconds(24 * 60 * 60).data("broadcast", id).build(), null);
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
