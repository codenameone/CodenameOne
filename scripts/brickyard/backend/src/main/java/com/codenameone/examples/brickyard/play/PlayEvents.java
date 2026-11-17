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
package com.codenameone.examples.brickyard.play;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.log.Logger;
import com.codenameone.examples.brickyard.analytics.Analytics;
import com.codenameone.examples.brickyard.leaderboard.Leaderboards;
import com.codenameone.examples.brickyard.social.Social;

/// What a room's players mean to the rest of the backend: a VISIT when one
/// connects, a SESSION (seconds) and presence back to ONLINE when one
/// leaves. A failure here is logged and never reaches the room's thread.
@Component
public class PlayEvents implements PlayServers.Events {
    private static final Logger LOG = Logger.getLogger(PlayEvents.class);

    private final Social social;
    private final Analytics analytics;
    private final Leaderboards leaderboards;

    public PlayEvents(Social social, Analytics analytics, Leaderboards leaderboards) {
        this.social = social;
        this.analytics = analytics;
        this.leaderboards = leaderboards;
    }

    /// A leaderstat rose on a live server: the experience's leaderboards
    /// take it, through the same per-minute bound a write from an engine
    /// passes. A change past the bound is flagged and dropped, not thrown.
    @Override
    public void scored(PlayRoom room, String username, String stat, long delta) {
        try {
            leaderboards.record(room.getExperienceId(), stat, username, delta);
        } catch (Exception err) {
            LOG.warn("Leaderboard write refused: {}", err.getMessage());
        }
    }

    @Override
    public void joined(PlayRoom room, String username) {
        // The VISIT is recorded by Experiences.visit, which every play calls
        // first; recording it here too counted each online play twice.
    }

    @Override
    public void left(PlayRoom room, String username, long millis) {
        try {
            social.leftServer(username, room.getId());
            if (millis >= 0) {
                analytics.record(room.getExperienceId(), username, "SESSION", millis / 1000);
            }
        } catch (Exception err) {
            LOG.error("Recording {} leaving room {} failed", username, room.getId(), err);
        }
    }
}
