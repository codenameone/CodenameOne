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
package com.codenameone.examples.brickyard.moderation;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.domain.Sanction;
import com.codenameone.examples.brickyard.repository.SanctionRepository;

/// Whether an account is banned or muted now.
///
/// A ban is global: [BanGuard] refuses a banned account signing in,
/// refreshing its token and every write under `/api`, with [#banMessage].
/// Reading stays open, and so does filing an appeal, so a banned player can
/// still see why and ask for it to be lifted. The routes that spend or chat
/// also ask here themselves, so they hold without the filter too.
@Component
public class Sanctions {
    public static final String WARN = "WARN";
    public static final String TEMP_BAN = "TEMP_BAN";
    public static final String PERM_BAN = "PERM_BAN";
    public static final String MUTE = "MUTE";

    private final SanctionRepository sanctions;

    public Sanctions(SanctionRepository sanctions) {
        this.sanctions = sanctions;
    }

    public boolean has(String username, String kind) {
        long now = Clock.now();
        for (Sanction s : sanctions.of(username)) {
            if (s.getLifted() == 0 && s.getUntilAt() > now && s.getKind().equals(kind)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBanned(String username) {
        return has(username, TEMP_BAN) || has(username, PERM_BAN);
    }

    /// The ban in force on `username` that ends last, or null.
    public Sanction activeBan(String username) {
        if (username == null) {
            return null;
        }
        long now = Clock.now();
        Sanction ban = null;
        for (Sanction s : sanctions.of(username)) {
            if (s.getLifted() == 0 && s.getUntilAt() > now
                    && (TEMP_BAN.equals(s.getKind()) || PERM_BAN.equals(s.getKind()))
                    && (ban == null || s.getUntilAt() > ban.getUntilAt())) {
                ban = s;
            }
        }
        return ban;
    }

    /// What a banned account is told, or null when it is not banned.
    public String banMessage(String username) {
        Sanction ban = activeBan(username);
        if (ban == null) {
            return null;
        }
        String reason = ban.getReason() == null || ban.getReason().length() == 0 ? "" : " (" + ban.getReason() + ")";
        if (PERM_BAN.equals(ban.getKind())) {
            return "This account is banned permanently" + reason
                    + ". You can read your sanctions and appeal at /api/me/appeals.";
        }
        long minutes = Math.max(1, (ban.getUntilAt() - Clock.now() + Clock.MINUTE - 1) / Clock.MINUTE);
        return "This account is banned for " + minutes + " more minute" + (minutes == 1 ? "" : "s") + reason
                + ". You can read your sanctions and appeal at /api/me/appeals.";
    }

    public void requireNotBanned(String username) {
        String why = banMessage(username);
        if (why != null) {
            throw new ResponseStatusException(403, why);
        }
    }

    public void requireCanChat(String username) {
        requireNotBanned(username);
        if (has(username, MUTE)) {
            throw new ResponseStatusException(403, "This account is muted");
        }
    }
}
