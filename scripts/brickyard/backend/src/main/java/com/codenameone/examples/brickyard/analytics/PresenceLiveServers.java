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
package com.codenameone.examples.brickyard.analytics;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.ConditionalOnMissingBean;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.domain.Presence;
import com.codenameone.examples.brickyard.repository.PresenceRepository;

/// CCU from presence: accounts that reported a status other than OFFLINE in
/// the last two minutes. Steps aside for any other [LiveServers] bean.
@Component
@ConditionalOnMissingBean
public class PresenceLiveServers implements LiveServers {
    /// How recent a presence report has to be to count as online.
    public static final long FRESH = 2 * Clock.MINUTE;

    private final PresenceRepository presence;

    public PresenceLiveServers(PresenceRepository presence) {
        this.presence = presence;
    }

    @Override
    public long ccu() {
        long now = Clock.now();
        long n = 0;
        for (Presence p : presence.since(now - FRESH)) {
            if (p.getUpdatedAt() >= now - FRESH && !"OFFLINE".equals(p.getStatus())) {
                n++;
            }
        }
        return n;
    }
}
