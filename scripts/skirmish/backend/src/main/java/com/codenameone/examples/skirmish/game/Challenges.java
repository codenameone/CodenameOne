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
package com.codenameone.examples.skirmish.game;

import com.codename1.backend.Crypto;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.Ids;
import com.codenameone.examples.skirmish.api.ChallengeDto;
import com.codenameone.examples.skirmish.push.GamePushes;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/// Private match links: "open this link and play me".
///
/// The link opens the installed app straight into the battle. Codes live in memory: a challenge is meant to be used in minutes,
/// and losing the open ones on a restart costs a new link, not data.
// tag::skirmish-challenge[]
@Component
public class Challenges {
    public static final int TTL_SECONDS = 600;

    private final GamePushes pushes;
    private final String publicUrl;
    private final Map<String, String> hosts = new HashMap<String, String>();
    private final Map<String, Long> expiry = new HashMap<String, Long>();

    public Challenges(GamePushes pushes, @Value("${skirmish.public-url:}") String publicUrl) {
        this.pushes = pushes;
        this.publicUrl = publicUrl;
    }

    public synchronized ChallengeDto create(String host) throws IOException {
        sweep();
        String code = Ids.hex(Crypto.randomBytes(5));
        hosts.put(code, host);
        expiry.put(code, Long.valueOf(Clock.now() + TTL_SECONDS * 1000L));
        // With a hosted browser build (skirmish.public-url) the link opens the
        // game in a browser; otherwise it opens the installed app through its
        // skirmish: URL scheme. Either way the app reads the code after
        // "challenge=".
        String link = publicUrl.length() > 0 ? publicUrl + "/?challenge=" + code
                : "skirmish://join?challenge=" + code;
        return new ChallengeDto(code, link, TTL_SECONDS);
    }

    synchronized String hostOf(String code) {
        sweep();
        return code == null ? null : hosts.get(code);
    }

    synchronized void used(String code) {
        hosts.remove(code);
        expiry.remove(code);
    }

    /// The challenged player arrived before the host: tell the host by push.
    void joinedWhileAway(String code, String host, String challenger) {
        pushes.challengeAccepted(host, challenger, code);
    }

    private void sweep() {
        long now = Clock.now();
        for (Iterator<Map.Entry<String, Long>> it = expiry.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue().longValue() < now) {
                hosts.remove(e.getKey());
                it.remove();
            }
        }
    }
}
// end::skirmish-challenge[]
