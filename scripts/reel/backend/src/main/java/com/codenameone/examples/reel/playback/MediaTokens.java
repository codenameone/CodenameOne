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
package com.codenameone.examples.reel.playback;

import com.codename1.backend.Crypto;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.crypto.SignedTokens;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.settings.Settings;

import java.io.IOException;

/// The tokens that open a stream: one in the path of every media URL, one in
/// the header of every license request.
///
/// A media token names the session, the video and the tallest rendition the
/// plan allows, and expires; the origin checks it on every playlist, segment
/// and key, so a URL copied out of the player is good for one video, for a
/// few hours, and stops working the moment the session is ended. A license
/// token is the same thing signed for another purpose, so neither can stand in
/// for the other.
///
/// The secret is `reel.media.secret` when set (32 or more characters, from
/// the environment in production). Unset, one is made on first start and kept
/// in the settings table, so every instance of the server signs alike.
@Component
public class MediaTokens {
    public static final String MEDIA = "reel-media";
    public static final String LICENSE = "reel-license";
    public static final String PROFILE = "reel-profile";
    public static final long PROFILE_SECONDS = 12L * 3600L;
    /// How long a streaming token lives. Longer than any title; a session that
    /// ends sooner is refused by the origin's session check, not by expiry.
    public static final long STREAM_SECONDS = 6L * 3600L;

    private final SignedTokens tokens;

    public MediaTokens(Settings settings, @Value("${reel.media.secret:}") String configured)
            throws IOException {
        String secret = configured.trim();
        if (secret.length() < 32) {
            secret = settings.read("media.secret", "");
            if (secret.length() == 0) {
                secret = Ids.hex(Crypto.randomBytes(32));
                settings.put("media.secret", secret, "server");
            }
        }
        this.tokens = new SignedTokens(secret.getBytes("UTF-8"));
    }

    /// A grant to play, as the tokens carry it.
    public static final class Grant {
        /// `s` for a streaming session, `d` for an offline download.
        public final String kind;
        public final String owner;
        public final String videoId;
        public final int maxHeight;

        Grant(String kind, String owner, String videoId, int maxHeight) {
            this.kind = kind;
            this.owner = owner;
            this.videoId = videoId;
            this.maxHeight = maxHeight;
        }

        public boolean isDownload() {
            return "d".equals(kind);
        }
    }

    public String media(String kind, String owner, String videoId, int maxHeight, long ttlSeconds) {
        return tokens.create(MEDIA, subject(kind, owner, videoId, maxHeight), ttlSeconds);
    }

    public String license(String kind, String owner, String videoId, long ttlSeconds) {
        return tokens.create(LICENSE, subject(kind, owner, videoId, 0), ttlSeconds);
    }

    /// The key [com.codenameone.examples.reel.account.Households#unlock] hands
    /// out: proof the PIN of one profile was given, good for half a day.
    public String profile(String username, String profileId) {
        return tokens.create(PROFILE, username + "\n" + profileId, PROFILE_SECONDS);
    }

    /// Whether `token` is a live profile key for this account and profile.
    public boolean verifyProfile(String token, String username, String profileId) {
        String subject = token == null || token.length() == 0 ? null : tokens.verify(PROFILE, token);
        return subject != null && subject.equals(username + "\n" + profileId);
    }

    /// The grant a media token carries, or null for one that is forged,
    /// expired or malformed.
    public Grant verifyMedia(String token) {
        return parse(token == null ? null : tokens.verify(MEDIA, token));
    }

    public Grant verifyLicense(String token) {
        return parse(token == null ? null : tokens.verify(LICENSE, token));
    }

    private static String subject(String kind, String owner, String videoId, int maxHeight) {
        return kind + ":" + owner + ":" + videoId + ":" + maxHeight;
    }

    private static Grant parse(String subject) {
        if (subject == null) {
            return null;
        }
        String[] parts = com.codenameone.examples.reel.Rows.split(subject, ':').toArray(new String[0]);
        if (parts.length != 4) {
            return null;
        }
        try {
            return new Grant(parts[0], parts[1], parts[2], Integer.parseInt(parts[3]));
        } catch (NumberFormatException bad) {
            return null;
        }
    }
}
