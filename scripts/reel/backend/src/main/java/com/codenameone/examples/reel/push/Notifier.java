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
package com.codenameone.examples.reel.push;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.domain.Household;
import com.codenameone.examples.reel.domain.ListEntry;
import com.codenameone.examples.reel.domain.Progress;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.repository.HouseholdRepository;
import com.codenameone.examples.reel.repository.ListEntryRepository;
import com.codenameone.examples.reel.repository.ProgressRepository;
import com.codenameone.examples.reel.repository.VideoRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/// What Reel tells people without being asked: a title on their list is out,
/// or has a new episode; and their account's own news. Each device chose which
/// of the two it wants ([PushDevices#targets(String, boolean)]).
///
/// A notification carries the title it is about as `titleId`, and the app
/// opens that title; nothing in it is needed to act, so a lost one costs
/// nothing but the nudge. Sending is asynchronous and never fails the caller.
@Component
public class Notifier {
    /// More recipients than this for one release are not sent to from the
    /// request that published it; a production deployment hands a fan-out
    /// that size to a queue.
    static final int MAX_FAN_OUT = 5000;

    private final PushSender sender;
    private final PushDevices devices;
    private final ListEntryRepository lists;
    private final HouseholdRepository households;
    private final ProgressRepository progress;
    private final VideoRepository videos;

    public Notifier(PushSender sender, PushDevices devices, ListEntryRepository lists,
            HouseholdRepository households, ProgressRepository progress, VideoRepository videos) {
        this.sender = sender;
        this.devices = devices;
        this.lists = lists;
        this.households = households;
        this.progress = progress;
        this.videos = videos;
    }

    /// `title` was approved. When it has videos nobody following it was told
    /// about, tells them -- the first time "now streaming", after that "new
    /// episode" -- and records the count on the title, which the caller saves.
    /// An approved edit of the synopsis tells nobody.
    public void released(Title title) {
        int ready = 0;
        for (Video v : videos.findByTitleIdOrderBySeasonAscNumberAsc(title.getId())) {
            if (Kinds.READY.equals(v.getStatus()) && !Kinds.TRAILER.equals(v.getRole())) {
                ready++;
            }
        }
        int before = title.getAnnouncedVideos();
        if (ready <= before) {
            return;
        }
        title.setAnnouncedVideos(ready);
        Set<String> people = new LinkedHashSet<String>();
        for (ListEntry entry : lists.findByTitleId(title.getId())) {
            Household profile = households.findById(entry.getProfileId());
            if (profile != null) {
                people.add(profile.getUsername());
            }
        }
        if (before > 0) {
            // A new episode is news to whoever has been watching too.
            for (Progress p : progress.findByTitleId(title.getId())) {
                people.add(p.getUsername());
            }
        }
        List<PushTarget> to = new ArrayList<PushTarget>();
        int count = 0;
        for (String username : people) {
            if (++count > MAX_FAN_OUT) {
                break;
            }
            to.addAll(devices.targets(username, true));
        }
        String body = before == 0 ? "Now streaming on Reel"
                : ready - before == 1 ? "A new episode is out" : (ready - before) + " new episodes are out";
        send(to, title.getName(), body, title.getId());
    }

    /// News of `username`'s own account: a sign-in, a failed payment, a
    /// moderation decision. `titleId` may be null.
    public void account(String username, String heading, String body, String titleId) {
        send(devices.targets(username, false), heading, body, titleId);
    }

    private void send(List<PushTarget> to, String heading, String body, String titleId) {
        if (to.isEmpty()) {
            return;
        }
        try {
            PushNotification.Builder b = PushNotification.builder().title(heading).body(body);
            if (titleId != null && titleId.length() > 0) {
                b.data("titleId", titleId);
            }
            sender.send(to, b.build(), null);
        } catch (RuntimeException err) {
            System.err.println("[push] nothing was sent (" + heading + "): " + err);
        }
    }
}
